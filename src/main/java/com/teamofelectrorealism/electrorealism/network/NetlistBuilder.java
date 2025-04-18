package com.teamofelectrorealism.electrorealism.network;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.block.IPowerProvider;
import com.teamofelectrorealism.electrorealism.block.IPowerReceiver;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType; // Added import
import net.minecraft.world.phys.Vec3; // Added import
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors; // Added import

/**
 * NetlistBuilder to convert electrical network adjacency graphs to SPICE netlists.
 */
public class NetlistBuilder {
    // Node numbering
    private int nextNodeNumber; // Starts at 1, 0 is ground
    private Map<NodeKey, Integer> nodeNumberMap = new HashMap<>();

    // Component counters
    private int voltageSourceCount;
    private int wireResistorCount;
    private int machineResistorCount;

    // Component tracking
    private Set<EdgeKey> processedEdges = new HashSet<>(); // To avoid generating duplicate RW components for the same edge
    private Set<INetworkMember> processedMachines = new HashSet<>(); // To avoid generating duplicate R components for the same machine

    // Logging
    private static final Logger LOGGER = LogUtils.getLogger();

    // Constants for terminal indices, making the code more readable
    private static final int POSITIVE_TERMINAL_INDEX = 0; // Standard index for positive terminal
    private static final int NEGATIVE_TERMINAL_INDEX = 1; // Standard index for negative terminal
    private static final int INPUT_TERMINAL_INDEX = 0;    // Standard index for machine input
    private static final int OUTPUT_TERMINAL_INDEX = 1;   // Standard index for machine output (if applicable)
    // MACHINE_CONNECTION_INDEX (-1) is used in ConnectionInfo but NOT directly for node numbering.
    // We map the corresponding machine/generator terminal (e.g., INPUT_TERMINAL_INDEX) instead.


    // Constants for classification keys
    private static final String GENERATORS = "generators";
    private static final String MACHINES = "machines";
    private static final String CONNECTORS = "connectors";


    /**
     * Main entry point: builds a SPICE netlist from network adjacency list
     * @param adjacencyList The network topology represented as an adjacency list.
     * @return A string containing the generated SPICE netlist.
     */
    public String buildNetlist(Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        // Initialize data structures and counters
        initializeNetlistBuilder();

        // Classify network members by type (generator, machine, connector)
        Map<String, List<INetworkMember>> classifiedMembers = classifyNetworkMembers(adjacencyList);

        // Assign node numbers to all relevant connection points
        Map<NodeKey, Integer> nodeNumbering = assignNodeNumbers(adjacencyList);
        if (nodeNumbering.isEmpty() && !adjacencyList.isEmpty()) {
            LOGGER.error("Node numbering failed to assign any nodes! Aborting netlist generation.");
            return "* Error: Node numbering failed.\n.END\n";
        }

        // Find generators to use as voltage sources
        List<INetworkMember> generators = findGenerators(classifiedMembers);

        // Create voltage sources connecting generators to ground
        List<String> voltageComponents = createVoltageSourceComponents(generators, nodeNumbering);

        // Get the list of machines we classified earlier
        List<INetworkMember> machines = classifiedMembers.get(MACHINES);

        // Generate resistor components for machine connections
        List<String> machineResistors = createMachineResistorComponents(machines, adjacencyList, nodeNumbering);


        // Generate wire resistor components for connector connections
        List<String> wireResistors = createWireResistorComponents(adjacencyList, nodeNumbering);

        // Combine components generated so far
        List<String> allComponents = new ArrayList<>();
        allComponents.addAll(voltageComponents);
        allComponents.addAll(machineResistors);
        allComponents.addAll(wireResistors);


        // Remove any duplicate or redundant components
        List<String> cleanComponentList = removeRedundantComponents(allComponents);

        // Format and assemble the final SPICE netlist
        return formatSpiceNetlist(cleanComponentList);
    }

    /**
     * Reset state and initialize counters for a fresh netlist build
     */
    private void initializeNetlistBuilder() {
        // Reset node numbering
        nextNodeNumber = 1;  // Start node numbering at 1 (0 is reserved for ground)
        nodeNumberMap.clear();

        // Reset component counters
        voltageSourceCount = 1;
        wireResistorCount = 1;
        machineResistorCount = 1;

        // Clear data structures
        processedEdges.clear();
        processedMachines.clear(); // Clear processed machines set

        // Log initialization
        LOGGER.debug("NetlistBuilder initialized for a new build");
    }

    /**
     * For each machine in the network, walk its adjacency entries
     * and emit one R‑component per machine→connector link.
     */
    private List<String> createMachineResistorComponents(
            List<INetworkMember> machines,
            Map<INetworkMember, List<ConnectionInfo>> adjacencyList,
            Map<NodeKey, Integer> nodeNumbering
    ) {
        List<String> machineResistors = new ArrayList<>();
        if (machines == null || adjacencyList == null) return machineResistors;

        for (INetworkMember member : machines) {
            if (!(member instanceof IPowerReceiver machine)) continue;

            // Map machine’s input terminal
            NodeKey machineKey = new NodeKey(member, INPUT_TERMINAL_INDEX);
            Integer node1 = nodeNumbering.get(machineKey);
            if (node1 == null) {
                LOGGER.warn("No node number for machine input {}", machineKey);
                continue;
            }

            // Walk every cable connected to this machine
            for (ConnectionInfo info : adjacencyList.getOrDefault(member, Collections.emptyList())) {
                // neighbor should be a connector
                NodeKey neighborKey = getNodeKeyForConnectionEnd(info.neighbor(), info.neighborNodeIndex());
                Integer node2 = nodeNumbering.get(neighborKey);
                if (node2 == null) {
                    LOGGER.warn("No node number for machine neighbor {}", neighborKey);
                    continue;
                }

                // Skip zero‑length resistor
                if (node1.equals(node2)) continue;

                // Format R‑component
                String name = "R" + machineResistorCount++;
                int resistance = machine.getResistance();
                String line = String.format(Locale.US, "%s %d %d %d", name, node1, node2, resistance);
                machineResistors.add(line);
                LOGGER.debug("Generated machine resistor: {}", line);
            }
        }
        return machineResistors;
    }


    /**
     * Group network members by their type using implemented interfaces:
     * IPowerProvider (generators), IPowerUser (machines), and IWireNode (connectors)
     *
     * @param adjacencyList The network topology as an adjacency list
     * @return Map with member types as keys and lists of members as values
     */
    private Map<String, List<INetworkMember>> classifyNetworkMembers(Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        Map<String, List<INetworkMember>> classified = new HashMap<>();
        classified.put(GENERATORS, new ArrayList<>());
        classified.put(MACHINES, new ArrayList<>());
        classified.put(CONNECTORS, new ArrayList<>());

        if (adjacencyList == null || adjacencyList.isEmpty()) {
            LOGGER.warn("Empty or null adjacency list provided for classification");
            return classified;
        }

        for (INetworkMember member : adjacencyList.keySet()) {
            // Classify based on implemented interfaces
            boolean isGenerator = false;
            boolean isMachine = false;
            boolean isConnector = false;

            if (member instanceof IPowerProvider) {
                classified.get(GENERATORS).add(member);
                LOGGER.trace("Classified {} as generator (IPowerProvider)", member.getPos().toShortString());
                isGenerator = true;
            }
            if (member instanceof IPowerReceiver) {
                classified.get(MACHINES).add(member);
                LOGGER.trace("Classified {} as machine (IPowerReceiver)", member.getPos().toShortString());
                isMachine = true;
            }
            // Connectors might also be providers/receivers (e.g., a powered connector block)
            // but their primary role in the graph for wiring is as a node.
            if (member instanceof IWireNode) {
                // Only add to connectors list if not already added as generator/machine?
                // Or maybe connectors list should contain ALL IWireNode instances? Let's add all for now.
                classified.get(CONNECTORS).add(member);
                LOGGER.trace("Classified {} as connector (IWireNode)", member.getPos().toShortString());
                isConnector = true;
            }


            // Log if a member wasn't classified (potential issue)
            if (!isGenerator && !isMachine && !isConnector) {
                LOGGER.warn("Member at {} of type {} was not classified as Generator, Machine, or Connector.", member.getPos().toShortString(), member.getClass().getSimpleName());
            }
        }

        // Log classification summary
        LOGGER.debug("Network classification - Generators: {}, Machines: {}, Connectors: {}",
                classified.get(GENERATORS).size(),
                classified.get(MACHINES).size(),
                classified.get(CONNECTORS).size());

        return classified;
    }

    /**
     * Assign unique node numbers to all connection points in the network.
     * Node 0 is reserved for ground in SPICE netlists.
     * Revised approach: Explicitly assign core terminals first, then process connections.
     *
     * @param adjacencyList The network topology as an adjacency list
     * @return Map from NodeKey (member + connection point) to SPICE node number
     */
    private Map<NodeKey, Integer> assignNodeNumbers(Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        LOGGER.debug("Starting node assignment (Revised 2)...");
        Map<NodeKey, Integer> nodeNumberMap = new HashMap<>();
        // nextNodeNumber is an instance variable initialized to 1

        if (adjacencyList == null || adjacencyList.isEmpty()) {
            LOGGER.warn("Cannot assign nodes: Adjacency list is empty or null.");
            return nodeNumberMap;
        }

        // --- Pass 1: Assign nodes to essential Generator/Machine terminals ---
        LOGGER.debug("Node Assignment Pass 1: Generators & Machines");
        for (INetworkMember member : adjacencyList.keySet()) {
            if (member instanceof IPowerProvider) {
                ensureNodeAssigned(new NodeKey(member, POSITIVE_TERMINAL_INDEX), nodeNumberMap);
                // Assign negative terminal only if not grounded, otherwise it will be handled in Pass 3
                if (!isNegativeTerminalGrounded(member)) {
                    ensureNodeAssigned(new NodeKey(member, NEGATIVE_TERMINAL_INDEX), nodeNumberMap);
                }
            } else if (member instanceof IPowerReceiver) {
                ensureNodeAssigned(new NodeKey(member, INPUT_TERMINAL_INDEX), nodeNumberMap);
                if (hasSeparateOutputTerminal(member)) {
                    ensureNodeAssigned(new NodeKey(member, OUTPUT_TERMINAL_INDEX), nodeNumberMap);
                }
            }
        }

        // --- Pass 2: Iterate connections and assign nodes to connector points ---
        LOGGER.debug("Node Assignment Pass 2: Connectors via Connections");
        for (Map.Entry<INetworkMember, List<ConnectionInfo>> entry : adjacencyList.entrySet()) {
            INetworkMember sourceMember = entry.getKey();
            List<ConnectionInfo> connections = entry.getValue();

            for (ConnectionInfo info : connections) {
                INetworkMember neighborMember = info.neighbor();

                // Assign node for the source point of this connection
                NodeKey sourceKey = getNodeKeyForConnectionEnd(sourceMember, info.sourceNodeIndex());
                if (sourceKey != null) {
                    ensureNodeAssigned(sourceKey, nodeNumberMap);
                } else {
                    LOGGER.warn("Could not determine source NodeKey for connection info: {}", info);
                }

                // Assign node for the neighbor point of this connection
                NodeKey neighborKey = getNodeKeyForConnectionEnd(neighborMember, info.neighborNodeIndex());
                if (neighborKey != null) {
                    ensureNodeAssigned(neighborKey, nodeNumberMap);
                } else {
                    LOGGER.warn("Could not determine neighbor NodeKey for connection info: {}", info);
                }
            }
        }

        // --- Pass 3: Handle Grounding ---
        LOGGER.debug("Node Assignment Pass 3: Grounding");
        NodeKey groundedNodeKey = null; // The NodeKey that should be mapped to 0
        INetworkMember groundedGenerator = null; // Keep track of which generator is grounded

        // Find the grounded generator first
        for (INetworkMember member : adjacencyList.keySet()) {
            if (member instanceof IPowerProvider && isNegativeTerminalGrounded(member)) {
                groundedGenerator = member;
                break;
            }
        }

        if (groundedGenerator != null) {
            // Now find the connection *to* this generator's negative terminal
            for(Map.Entry<INetworkMember, List<ConnectionInfo>> potentialSourceEntry : adjacencyList.entrySet()) {
                for(ConnectionInfo conn : potentialSourceEntry.getValue()) {
                    // Check if the neighbor is our grounded generator and the index is the negative terminal
                    if(conn.neighbor().equals(groundedGenerator) && conn.neighborNodeIndex() == NEGATIVE_TERMINAL_INDEX) {
                        // The source of this connection is the node that should be ground
                        // Use the helper to get the correct NodeKey for the source
                        groundedNodeKey = getNodeKeyForConnectionEnd(conn.neighbor(), conn.sourceNodeIndex());
                        LOGGER.trace("Found connection to grounded generator's negative terminal from: {}", groundedNodeKey);
                        break; // Found the connection to ground
                    }
                }
                if (groundedNodeKey != null) break;
            }

            if (groundedNodeKey != null) {
                Integer existingNum = nodeNumberMap.put(groundedNodeKey, 0); // Force assignment to 0
                if (existingNum != null && existingNum != 0) {
                    LOGGER.warn("Node {} was previously assigned {} but is now grounded (0).", groundedNodeKey, existingNum);
                    // This might indicate an issue if a node other than the intended ground point gets mapped to 0.
                } else {
                    LOGGER.trace("Assigned node number 0 (Ground) to {}", groundedNodeKey);
                }
            } else {
                LOGGER.warn("Could not find any node connecting TO the grounded negative terminal of generator {} to assign node 0.", groundedGenerator.getPos().toShortString());
                // If no explicit connection found, maybe the generator's negative terminal itself should be 0?
                // This case shouldn't happen if the graph is connected correctly.
                NodeKey genNegativeKey = new NodeKey(groundedGenerator, NEGATIVE_TERMINAL_INDEX);
                nodeNumberMap.put(genNegativeKey, 0); // Assign conceptual key to 0 as fallback
                LOGGER.warn("Assigned conceptual node number 0 (Ground) to {} as fallback.", genNegativeKey);
            }
        } else {
            LOGGER.debug("No grounded generator found in this network.");
        }


        // Log summary of node assignment
        LOGGER.debug("Node assignment complete. Assigned {} unique node numbers (starting from 1). Total map size: {}", nextNodeNumber - 1, nodeNumberMap.size());
        // Log the final map for detailed debugging
        if (LOGGER.isTraceEnabled()) {
            String mapString = nodeNumberMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue()) // Sort by node number for readability
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ", "{", "}"));
            LOGGER.trace("Final Node Number Map: {}", mapString);
        }


        this.nodeNumberMap = nodeNumberMap; // Store the generated map
        return nodeNumberMap;
    }

    /**
     * Helper to determine the correct NodeKey for a connection endpoint based on member type and index.
     * Handles mapping of special indices (like MACHINE_CONNECTION_INDEX) to standard terminal indices.
     */
    private NodeKey getNodeKeyForConnectionEnd(INetworkMember member, int index) {
        if (index >= 0) { // Explicit connector point or standard terminal index
            if (member instanceof IWireNode && index < ((IWireNode) member).getConnectionPointCount()) {
                return new NodeKey(member, index);
            } else if (member instanceof IPowerProvider && (index == POSITIVE_TERMINAL_INDEX || index == NEGATIVE_TERMINAL_INDEX)) {
                return new NodeKey(member, index);
            } else if (member instanceof IPowerReceiver && (index == INPUT_TERMINAL_INDEX || (index == OUTPUT_TERMINAL_INDEX && hasSeparateOutputTerminal(member)))) {
                return new NodeKey(member, index);
            }
            LOGGER.error("Invalid explicit index {} for member type {} at {}", index, member.getClass().getSimpleName(), member.getPos().toShortString());
            return null;
        } else { // Implicit connection index (e.g., -1)
            if (member instanceof IPowerReceiver) {
                LOGGER.trace("Mapping implicit index {} for Machine {} to INPUT_TERMINAL_INDEX", index, member.getPos().toShortString());
                return new NodeKey(member, INPUT_TERMINAL_INDEX);
            } else if (member instanceof IPowerProvider) {
                LOGGER.trace("Mapping implicit index {} for Generator {} to POSITIVE_TERMINAL_INDEX", index, member.getPos().toShortString());
                return new NodeKey(member, POSITIVE_TERMINAL_INDEX);
            } else if (member instanceof IWireNode) {
                // Map implicit connector indices to first connection point
                LOGGER.trace("Mapping implicit index {} for Connector {} to first available terminal (0)", index, member.getPos().toShortString());
                return new NodeKey(member, 0);
            } else {
                LOGGER.error("Unexpected implicit index {} for member type {} at {}", index, member.getClass().getSimpleName(), member.getPos().toShortString());
                return null;
            }
        }
    }


    /**
     * Helper method to assign a node number if not already present.
     * Uses the instance variable `nextNodeNumber`.
     * @param key The NodeKey representing the connection point.
     * @param map The map to store node numbers.
     * @return The assigned or existing node number. Returns -1 if key is null.
     */
    private int ensureNodeAssigned(NodeKey key, Map<NodeKey, Integer> map) {
        if (key == null) {
            LOGGER.warn("ensureNodeAssigned called with null key!");
            return -1; // Indicate error
        }
        // Don't assign a new number if the key should be ground (0)
        if (key.member() instanceof IPowerProvider && key.nodeIndex() == NEGATIVE_TERMINAL_INDEX && isNegativeTerminalGrounded(key.member())) {
            if (!map.containsKey(key)) {
                LOGGER.trace("Skipping assignment for grounded key {}, will be set to 0 later.", key);
            }
            return map.getOrDefault(key, 0); // Return 0 or existing value (which should become 0)
        }

        // Use the instance variable nextNodeNumber
        return map.computeIfAbsent(key, k -> {
            int assignedNode = this.nextNodeNumber++;
            LOGGER.trace("Assigned node number {} to {}", assignedNode, key);
            return assignedNode;
        });
    }


    /**
     * Check if the negative terminal of a generator is directly grounded
     * (Placeholder - needs specific logic based on your mod's implementation)
     */
    private boolean isNegativeTerminalGrounded(INetworkMember generator) {
        // TODO: Implement logic to check if the generator's negative side connects to ground.
        // This might involve checking specific block states, connection types, or configurations.
        // For the example circuit, it IS grounded.
        return true; // Assume grounded for simplicity for now
    }

    /**
     * Check if a machine has separate input and output terminals
     * (Placeholder - needs specific logic based on your machine types)
     */
    private boolean hasSeparateOutputTerminal(INetworkMember machine) {
        // TODO: Implement logic based on your machine types.
        // For the example circuit, machines act as simple two-terminal devices.
        return false; // Default assumption
    }

    /**
     * Find all generator components in the network.
     * Retrieves the list of generators identified during classification.
     *
     * @param classifiedMembers A map containing lists of members categorized by type.
     * @return A list of INetworkMember instances identified as generators. Returns an empty list if none are found or the input map is invalid.
     */
    private List<INetworkMember> findGenerators(Map<String, List<INetworkMember>> classifiedMembers) {
        if (classifiedMembers == null || !classifiedMembers.containsKey(GENERATORS)) {
            LOGGER.warn("Cannot find generators: Classified members map is null or missing GENERATORS key.");
            return Collections.emptyList(); // Return empty list instead of null
        }
        List<INetworkMember> generators = classifiedMembers.get(GENERATORS);
        LOGGER.debug("Found {} generator(s).", generators.size());
        return generators; // Return the list directly
    }


    /**
     * Create SPICE voltage source components (like V1, V2) for all generators.
     *
     * @param generators    List of network members identified as generators (IPowerProvider).
     * @param nodeNumbering Map assigning SPICE node numbers to NodeKeys.
     * @return A list of strings, each representing a SPICE voltage source definition.
     */
    private List<String> createVoltageSourceComponents(List<INetworkMember> generators, Map<NodeKey, Integer> nodeNumbering) {
        List<String> voltageSources = new ArrayList<>();
        if (generators == null || nodeNumbering == null) {
            LOGGER.error("Cannot create voltage sources: generators list or nodeNumbering map is null.");
            return voltageSources;
        }

        for (INetworkMember genMember : generators) {
            if (!(genMember instanceof IPowerProvider generator)) {
                LOGGER.warn("Member classified as generator is not an instance of IPowerProvider: {}", genMember.getPos().toShortString());
                continue;
            }

            // Get the voltage dynamically from the generator BlockEntity
            int voltage = generator.getVoltage();

            // Determine the positive node number
            NodeKey positiveKey = new NodeKey(genMember, POSITIVE_TERMINAL_INDEX);
            Integer positiveNode = nodeNumbering.get(positiveKey);

            if (positiveNode == null) {
                LOGGER.error("Could not find assigned node number for positive terminal {}!", positiveKey);
                continue; // Skip this generator if positive node is missing
            }

            // Determine the negative node number
            int negativeNode;
            if (isNegativeTerminalGrounded(genMember)) {
                negativeNode = 0; // Grounded case
            } else {
                NodeKey negativeKey = new NodeKey(genMember, NEGATIVE_TERMINAL_INDEX);
                negativeNode = nodeNumbering.getOrDefault(negativeKey, -1); // Use -1 to indicate error if not found
                if (negativeNode == -1) {
                    LOGGER.error("Generator negative terminal node {} not found!", negativeKey);
                    continue; // Skip if negative node needed but not found
                }
            }


            // Format the SPICE line: V<n> <pos_node> <neg_node> DC <voltage>
            String componentName = "V" + voltageSourceCount++;
            String spiceLine = String.format(Locale.US, "%s %d %d DC %d", // Use US locale for decimal points if voltage becomes float
                    componentName,
                    positiveNode,
                    negativeNode,
                    voltage);

            voltageSources.add(spiceLine);
            LOGGER.debug("Generated voltage source: {}", spiceLine);
        }

        return voltageSources;
    }

    /**
     * Determines the correct NodeKey for the neighbor described in ConnectionInfo,
     * considering the direction of traversal.
     */
    private NodeKey determineNeighborNodeKey(ConnectionInfo info, INetworkMember currentMember, int currentNodeIndex) {
        // If the ConnectionInfo's source is the current member and index, then the neighbor is straightforward
        if (info.sourceNodeIndex() == currentNodeIndex && info.neighbor().equals(currentMember)) {
            return new NodeKey(info.neighbor(), info.neighborNodeIndex());
        }
        // If the ConnectionInfo's neighbor is the current member and index, then the source is the neighbor we want
        else if (info.neighborNodeIndex() == currentNodeIndex && info.neighbor().equals(currentMember)) {
            return new NodeKey(info.neighbor(), info.sourceNodeIndex());
        }
        // Handle implicit machine connections if needed based on how ConnectionInfo is structured
        // This part might need adjustment depending on NetworkGraphBuilder's output for machine links
        else {
            // This connection doesn't seem to directly involve the current node index being explored
            // This might happen if multiple connections exist for the member, but only one should match the current exploration step.
            // Or, the ConnectionInfo structure for machine links needs specific handling here.
            LOGGER.trace("determineNeighborNodeKey: ConnectionInfo {} does not directly match current node {}", info, new NodeKey(currentMember, currentNodeIndex));
            return null; // Indicate the neighbor key couldn't be determined from this info for this step
        }
    }


    /**
     * Helper to check if a node is effectively connected to ground (node 0).
     * Checks if the node's assigned number is 0 or if it connects to a node that is 0.
     */
    private boolean isNodeConnectedToGround(NodeKey nodeKey, Map<INetworkMember, List<ConnectionInfo>> adjacencyList, Map<NodeKey, Integer> nodeNumbering) {
        // Check if the node itself maps to 0
        if (nodeNumbering.getOrDefault(nodeKey, -1) == 0) {
            return true;
        }

        // Check if any neighbor node maps to 0
        if (adjacencyList == null) return false; // Guard against null adjList if called early
        List<ConnectionInfo> connections = adjacencyList.getOrDefault(nodeKey.member(), Collections.emptyList());
        for(ConnectionInfo info : connections) {
            NodeKey neighborKey = null;
            // Determine the neighbor key regardless of direction
            if (info.neighbor().equals(nodeKey.member()) && info.sourceNodeIndex() == nodeKey.nodeIndex()) {
                neighborKey = getNodeKeyForConnectionEnd(info.neighbor(), info.neighborNodeIndex());
            } else if (info.neighbor().equals(nodeKey.member()) && info.neighborNodeIndex() == nodeKey.nodeIndex()) {
                neighborKey = getNodeKeyForConnectionEnd(info.neighbor(), info.sourceNodeIndex());
            }


            if (neighborKey != null) {
                if(nodeNumbering.getOrDefault(neighborKey, -1) == 0) {
                    // Connects to a node explicitly numbered 0
                    return true;
                }
                // If the neighbor IS the negative terminal of a grounded generator, it's effectively ground 0
                if (neighborKey.member() instanceof IPowerProvider &&
                        isNegativeTerminalGrounded(neighborKey.member()) &&
                        neighborKey.nodeIndex() == NEGATIVE_TERMINAL_INDEX) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Creates resistor components (RW prefix) for wire connections between connectors (IWireNode).
     * Iterates through the adjacency list, calculates resistance, and avoids duplicates using `processedEdges`.
     *
     * @param adjacencyList The network graph.
     * @param nodeNumbering Map assigning SPICE node numbers to NodeKeys.
     * @return List of SPICE resistor definitions for wires.
     */
    private List<String> createWireResistorComponents(Map<INetworkMember, List<ConnectionInfo>> adjacencyList, Map<NodeKey, Integer> nodeNumbering) {
        List<String> wireResistors = new ArrayList<>();
        // Use the instance variable processedEdges to track processed connections
        // processedEdges.clear(); // Already cleared in initialize

        if (adjacencyList == null || nodeNumbering == null) {
            LOGGER.error("Cannot create wire resistors: adjacency list or node numbering is null.");
            return wireResistors;
        }

        for (Map.Entry<INetworkMember, List<ConnectionInfo>> entry : adjacencyList.entrySet()) {
            INetworkMember member1 = entry.getKey();
            // Ensure member1 is a connector for wire-to-wire connections
            if (!(member1 instanceof IWireNode wireNode1)) continue;

            for (ConnectionInfo info : entry.getValue()) {
                INetworkMember member2 = info.neighbor();
                // Ensure member2 is also a connector for wire-to-wire connections
                if (!(member2 instanceof IWireNode wireNode2)) continue;

                // Ensure we are dealing with explicit connector-to-connector points, not machine links
                // Note: getNodeKeyForConnectionEnd handles mapping implicit indices if needed,
                // but we only want RW for explicit connector-connector links here.
                if (info.sourceNodeIndex() < 0 || info.neighborNodeIndex() < 0) continue;

                // Define the two connected nodes (terminals) using the helper
                NodeKey key1 = getNodeKeyForConnectionEnd(member1, info.sourceNodeIndex());
                NodeKey key2 = getNodeKeyForConnectionEnd(member2, info.neighborNodeIndex());

                if (key1 == null || key2 == null) {
                    LOGGER.warn("Skipping wire resistor due to null NodeKey for connection: {}", info);
                    continue;
                }

                // Create an EdgeKey to check if we've processed this connection (in either direction)
                EdgeKey edge = new EdgeKey(key1, key2);
                if (processedEdges.add(edge)) { // If true, this edge is new
                    processWireEdge(wireNode1, key1, wireNode2, key2, nodeNumbering, wireResistors);
                }
            }
        }
        return wireResistors;
    }

    /**
     * Helper method to process a single wire edge between two connectors.
     * Calculates resistance, gets node numbers, formats SPICE line, and adds to list.
     */
    private void processWireEdge(IWireNode wireNode1, NodeKey key1, IWireNode wireNode2, NodeKey key2, Map<NodeKey, Integer> nodeNumbering, List<String> wireResistors) {
        // Get WireType from the connection point data stored on the source node (key1)
        // Need to handle the case where the connection point might not exist if nodeIndex is implicit (shouldn't happen here)
        if (key1.nodeIndex() < 0) {
            LOGGER.error("processWireEdge called with implicit source node index: {}", key1); return;
        }
        ConnectionPoint cp1 = wireNode1.getConnectionPoint(key1.nodeIndex());
        if (cp1 == null) {
            LOGGER.error("ConnectionPoint missing for {} index {} during wire resistor creation.", key1, key1.nodeIndex());
            return;
        }
        WireType wireType = cp1.getWireType();
        if(wireType == null) {
            LOGGER.error("WireType is null for connection between {} and {}", key1, key2);
            return;
        }

        // Calculate resistance using the helper method
        // Need to ensure key2.nodeIndex() is also valid before calling helper
        if (key2.nodeIndex() < 0) {
            LOGGER.error("processWireEdge called with implicit neighbor node index: {}", key2); return;
        }
        double resistance = calculateWireResistance(wireNode1, key1.nodeIndex(), wireNode2, key2.nodeIndex(), wireType);

        // Get node numbers
        Integer node1 = nodeNumbering.get(key1);
        Integer node2 = nodeNumbering.get(key2);

        if (node1 == null || node2 == null) {
            LOGGER.error("Missing node number for wire connection between {} ({}) and {} ({})", key1, node1, key2, node2);
            return;
        }

        // Avoid creating a resistor between the same node (can happen with very short wires/loops)
        if (node1.equals(node2)) {
            LOGGER.warn("Skipping wire resistor between {} and {} as they map to the same node ({}).", key1, key2, node1);
            return;
        }

        // Format SPICE line: RW<n> <node1> <node2> <resistance>
        String componentName = "RW" + wireResistorCount++;
        // Format resistance with reasonable precision, using scientific notation if small
        String resistanceStr = String.format(Locale.US, "%.6G", resistance);
        String spiceLine = String.format(Locale.US, "%s %d %d %s",
                componentName, node1, node2, resistanceStr);

        wireResistors.add(spiceLine);
        LOGGER.debug("Generated wire resistor: {}", spiceLine);
    }


    /**
     * Calculates the resistance of a wire segment between two connection points.
     * R = rho * (L / A)
     * @param node1       The first wire node.
     * @param index1      The connection index on the first node.
     * @param node2       The second wire node.
     * @param index2      The connection index on the second node.
     * @param wireType    The type of wire.
     * @return The calculated resistance in Ohms.
     */
    private double calculateWireResistance(IWireNode node1, int index1, IWireNode node2, int index2, WireType wireType) {
        // Get world positions of the connection points
        Vec3 offset1 = node1.getConnectionPointOffset(index1);
        Vec3 offset2 = node2.getConnectionPointOffset(index2);
        Vec3 pos1 = Vec3.atCenterOf(node1.getPos()).add(offset1);
        Vec3 pos2 = Vec3.atCenterOf(node2.getPos()).add(offset2);

        // Calculate length (distance)
        double length = pos1.distanceTo(pos2);
        if (length < 1e-6) return 0.0; // Avoid division by zero or tiny lengths

        // Get resistivity
        double resistivity = wireType.getResistivity();

        // Assume a cross-sectional area (Needs to be defined based on your mod's scale)
        // Example: Assume a small area like 1 mm^2 = 1e-6 m^2
        double area = 1.0e-6; // TODO: Define wire area based on WireType or config? Make configurable?

        double resistance = resistivity * (length / area);

        // Add a minimum resistance to avoid ideal wires if desired
        double minResistance = 1e-9; // Example minimum resistance
        resistance = Math.max(resistance, minResistance);


        LOGGER.trace("Calculated wire resistance: rho={}, L={}, A={} -> R={}", resistivity, length, area, resistance);
        return resistance;
    }


    /**
     * Filter out duplicate components from a combined list.
     * @param allComponents List of all generated SPICE component lines.
     * @return A list containing only unique component lines.
     */
    private List<String> removeRedundantComponents(List<String> allComponents) {
        // Use LinkedHashSet to preserve insertion order while ensuring uniqueness
        Set<String> uniqueComponents = new LinkedHashSet<>(allComponents);
        if (uniqueComponents.size() < allComponents.size()) {
            LOGGER.debug("Removed {} redundant component entries.", allComponents.size() - uniqueComponents.size());
        }
        return new ArrayList<>(uniqueComponents);
    }

    /**
     * Format the final SPICE netlist string.
     * @param components List of unique SPICE component lines.
     * @return The complete SPICE netlist as a string.
     */
    private String formatSpiceNetlist(List<String> components) {
        StringBuilder sb = new StringBuilder();
        sb.append("* Generated ElectroRealism Netlist (").append(new Date()).append(")\n"); // Add timestamp
        if (components.isEmpty()) {
            sb.append("* \n* No components generated. Check implementation or network structure.\n*\n");
            LOGGER.warn("Formatting netlist, but component list is empty!");
        } else {
            // Separate components by type for readability (optional)
            List<String> voltages = components.stream().filter(s -> s.startsWith("V")).toList();
            List<String> machineResistors = components.stream().filter(s -> s.startsWith("R") && !s.startsWith("RW")).toList();
            List<String> wireResistors = components.stream().filter(s -> s.startsWith("RW")).toList();

            if (!voltages.isEmpty()) {
                sb.append("\n* Voltage Sources\n");
                voltages.forEach(line -> sb.append(line).append("\n"));
            }
            if (!machineResistors.isEmpty()) {
                sb.append("\n* Machine Resistances\n");
                machineResistors.forEach(line -> sb.append(line).append("\n"));
            }
            if (!wireResistors.isEmpty()) {
                sb.append("\n* Wire Resistances\n");
                wireResistors.forEach(line -> sb.append(line).append("\n"));
            }
            // Add any other component types here if needed
        }
        sb.append("\n.END\n"); // Ensure .END is on its own line after a newline
        LOGGER.debug("Formatted SPICE netlist:\n{}", sb);
        return sb.toString();
    }

    /**
     * Class to uniquely identify network nodes (member + connection point index)
     * Used as keys in the nodeNumberMap.
     */
    private record NodeKey(INetworkMember member, int nodeIndex) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NodeKey nodeKey = (NodeKey) o;
            // Compare based on BlockPos and nodeIndex for reliable hashing/equality
            return nodeIndex == nodeKey.nodeIndex && member.getPos().equals(nodeKey.member.getPos());
        }

        @Override
        public int hashCode() {
            return Objects.hash(member.getPos(), nodeIndex);
        }

        @Override
        public String toString() {
            // More descriptive string representation
            String type = member.getClass().getSimpleName().replace("BlockEntity", "");
            return type + "@" + member.getPos().toShortString() + "[" + nodeIndex + "]";
        }
    }

    /**
     * Class to uniquely identify network edges (connections between two nodes).
     * Useful for tracking processed connections during path finding or component generation.
     * Ensures that an edge from A to B is treated the same as an edge from B to A.
     */
    private record EdgeKey(NodeKey node1, NodeKey node2) {
//        private final NodeKey node1; // Record components are implicitly final
//        private final NodeKey node2;

        // Custom constructor to ensure consistent ordering for hashing/equality
        private EdgeKey(NodeKey node1, NodeKey node2) {
            if (node1.hashCode() <= node2.hashCode()) { // Use <= for consistency
                this.node1 = node1;
                this.node2 = node2;
            } else {
                this.node1 = node2;
                this.node2 = node1;
            }
        }

        // equals and hashCode are automatically generated for records based on components

        @Override
        public String toString() {
            // Use the ordered nodes from the constructor
            return "Edge[" + this.node1 + " <-> " + this.node2 + "]";
        }
    }
}
