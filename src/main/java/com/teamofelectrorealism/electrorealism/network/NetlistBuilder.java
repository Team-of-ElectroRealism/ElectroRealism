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

/**
 * NetlistBuilder to convert electrical network adjacency graphs to SPICE netlists.
 */
public class NetlistBuilder {
    // Node numbering
    private int nextNodeNumber;
    private Map<NodeKey, Integer> nodeNumberMap = new HashMap<>();

    // Component counters
    private int voltageSourceCount;
    private int wireResistorCount;
    private int machineResistorCount;

    // Component tracking
    // private List<String> netlistComponents = new ArrayList<>(); // Not used directly, components collected in buildNetlist
    private Set<EdgeKey> processedEdges = new HashSet<>(); // To avoid generating duplicate RW components for the same edge
    private Set<INetworkMember> processedMachines = new HashSet<>(); // To avoid generating duplicate R components for the same machine

    // Path tracking
    // Stores lists of NodeKeys representing paths found by DFS from generator(+) to generator(-) or ground.
    private List<List<NodeKey>> foundPaths = new ArrayList<>();


    // Logging
    private static final Logger LOGGER = LogUtils.getLogger();

    // Constants for terminal indices, making the code more readable
    private static final int POSITIVE_TERMINAL_INDEX = 0; // Standard index for positive terminal
    private static final int NEGATIVE_TERMINAL_INDEX = 1; // Standard index for negative terminal
    private static final int INPUT_TERMINAL_INDEX = 0;    // Standard index for machine input
    private static final int OUTPUT_TERMINAL_INDEX = 1;   // Standard index for machine output (if applicable)
    private static final int MACHINE_CONNECTION_INDEX = -1; // Special index for connector's link to a machine/generator

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

        // Find generators to use as voltage sources
        List<INetworkMember> generators = findGenerators(classifiedMembers);

        // Create voltage sources connecting generators to ground
        List<String> voltageComponents = createVoltageSourceComponents(generators, nodeNumbering);

        // Identify paths through the network, populating foundPaths
        identifyNetworkPaths(adjacencyList, generators, nodeNumbering);

        // Generate resistor components for machine connections using the found paths
        List<String> machineResistors = createMachineResistorComponents(nodeNumbering); // Removed machines list param

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
        foundPaths.clear(); // Clear paths from previous runs

        // Log initialization
        LOGGER.debug("NetlistBuilder initialized for a new build");
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
     *
     * @param adjacencyList The network topology as an adjacency list
     * @return Map from NodeKey (member + connection point) to SPICE node number
     */
    private Map<NodeKey, Integer> assignNodeNumbers(Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        Map<NodeKey, Integer> nodeNumberMap = new HashMap<>();
        // int nextNodeNumber = 1; // Moved to instance variable

        // Iterate through all members and their connections to assign nodes
        for (Map.Entry<INetworkMember, List<ConnectionInfo>> entry : adjacencyList.entrySet()) {
            INetworkMember member = entry.getKey();
            List<ConnectionInfo> connections = entry.getValue();

            // Ensure the member itself has node numbers assigned if needed (e.g., internal machine nodes)
            // Assign nodes based on member type and connection indices used in ConnectionInfo
            if (member instanceof IPowerProvider) {
                ensureNodeAssigned(member, POSITIVE_TERMINAL_INDEX, nodeNumberMap);
                // Negative terminal node assignment depends on whether it's grounded or connects elsewhere.
                // If not grounded, ensure its node index is assigned.
                if (!isNegativeTerminalGrounded(member)) {
                    ensureNodeAssigned(member, NEGATIVE_TERMINAL_INDEX, nodeNumberMap);
                }
            } else if (member instanceof IPowerReceiver) {
                ensureNodeAssigned(member, INPUT_TERMINAL_INDEX, nodeNumberMap);
                if (hasSeparateOutputTerminal(member)) { // Check if machine needs a separate output node
                    ensureNodeAssigned(member, OUTPUT_TERMINAL_INDEX, nodeNumberMap);
                }
                // We don't assign the output node here if it's not separate;
                // it will be assigned when processing the connector it attaches to.
            } else if (member instanceof IWireNode wireNode) {
                // Assign nodes for explicit connection points used in the network
                for (int i = 0; i < wireNode.getConnectionPointCount(); i++) {
                    // Check if this point is actually used in any connection before assigning
                    int finalI = i;
                    int finalI1 = i;
                    boolean isUsed = connections.stream().anyMatch(c -> c.sourceNodeIndex() == finalI) ||
                            adjacencyList.values().stream().flatMap(List::stream)
                                    .anyMatch(c -> c.neighbor().equals(member) && c.neighborNodeIndex() == finalI1);
                    if(isUsed) {
                        ensureNodeAssigned(member, i, nodeNumberMap);
                    }
                }
                // Assign node for the implicit machine connection point if it's a connector and connected to a machine/generator
                boolean connectedToMachineOrGen = connections.stream().anyMatch(c -> c.neighborNodeIndex() == MACHINE_CONNECTION_INDEX) ||
                        adjacencyList.values().stream().flatMap(List::stream)
                                .anyMatch(c -> c.neighbor().equals(member) && c.sourceNodeIndex() == MACHINE_CONNECTION_INDEX);
                if(connectedToMachineOrGen) {
                    ensureNodeAssigned(member, MACHINE_CONNECTION_INDEX, nodeNumberMap);
                }
            }


            // Ensure neighbors mentioned in connections also have nodes assigned for the *specific index* they connect to
            for (ConnectionInfo info : connections) {
                INetworkMember neighbor = info.neighbor();
                ensureNodeAssigned(neighbor, info.neighborNodeIndex(), nodeNumberMap);
            }
        }


        // Log summary of node assignment
        LOGGER.debug("Assigned {} unique node numbers for the network (starting from 1).", nextNodeNumber - 1);
        // Optional: Print the map for debugging
        // nodeNumberMap.forEach((key, value) -> LOGGER.trace("Node Map: {} -> {}", key, value));

        this.nodeNumberMap = nodeNumberMap; // Store the generated map
        return nodeNumberMap;
    }

    /**
     * Helper method to assign a node number if not already present.
     * @param member The network member.
     * @param index The connection point index on the member.
     * @param map The map to store node numbers.
     * @return The assigned or existing node number.
     */
    private int ensureNodeAssigned(INetworkMember member, int index, Map<NodeKey, Integer> map) {
        NodeKey key = new NodeKey(member, index);
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
                LOGGER.error("Could not find assigned node number for positive terminal of generator at {}!", genMember.getPos().toShortString());
                continue; // Skip this generator if positive node is missing
            }

            // Determine the negative node number
            // Default to 0 (ground) unless specific logic determines otherwise
            int negativeNode = 0; // Assume ground
            if (!isNegativeTerminalGrounded(genMember)) {
                NodeKey negativeKey = new NodeKey(genMember, NEGATIVE_TERMINAL_INDEX);
                negativeNode = nodeNumbering.getOrDefault(negativeKey, 0); // Default to 0 if not found
                if (negativeNode == 0) {
                    LOGGER.warn("Generator {} negative terminal node not found, defaulting to ground (0).", genMember.getPos().toShortString());
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
     * Identifies paths through the network using Depth First Search (DFS).
     * Starts from each generator's positive terminal and explores paths until
     * the corresponding negative terminal (or ground) is reached.
     * Stores found paths in the `foundPaths` list.
     *
     * @param adjacencyList The network topology.
     * @param generators    List of generators to start DFS from.
     * @param nodeNumbering Map to look up node numbers.
     */
    private void identifyNetworkPaths(
            Map<INetworkMember, List<ConnectionInfo>> adjacencyList,
            List<INetworkMember> generators,
            Map<NodeKey, Integer> nodeNumbering) {

        LOGGER.debug("Starting path identification...");
        foundPaths.clear(); // Clear previous paths

        if (generators == null || adjacencyList == null || nodeNumbering == null) {
            LOGGER.error("Cannot identify paths: Missing generators, adjacency list, or node numbering.");
            return;
        }

        for (INetworkMember genMember : generators) {
            NodeKey startNodeKey = new NodeKey(genMember, POSITIVE_TERMINAL_INDEX);
            NodeKey targetNodeKey; // The node representing the generator's negative terminal

            // Determine the target node (ground or specific negative terminal)
            if (isNegativeTerminalGrounded(genMember)) {
                targetNodeKey = new NodeKey(genMember, NEGATIVE_TERMINAL_INDEX); // Conceptual target representing ground connection
                LOGGER.debug("Starting DFS from generator {} (Node {}) targeting Ground (represented by {})",
                        genMember.getPos().toShortString(), nodeNumbering.get(startNodeKey), targetNodeKey);
            } else {
                targetNodeKey = new NodeKey(genMember, NEGATIVE_TERMINAL_INDEX);
                Integer targetNodeNum = nodeNumbering.get(targetNodeKey);
                if (targetNodeNum == null) {
                    LOGGER.error("Cannot start DFS for generator {}: Target negative node {} not found in numbering.",
                            genMember.getPos().toShortString(), targetNodeKey);
                    continue;
                }
                LOGGER.debug("Starting DFS from generator {} (Node {}) targeting Node {} ({})",
                        genMember.getPos().toShortString(), nodeNumbering.get(startNodeKey), targetNodeNum, targetNodeKey);
            }

            // Start DFS from the positive terminal
            LinkedList<NodeKey> currentPath = new LinkedList<>();
            Set<NodeKey> visitedInPath = new HashSet<>(); // Track visited nodes *for this specific DFS run* to prevent cycles in path
            dfsFindPathsRecursive(startNodeKey, targetNodeKey, adjacencyList, nodeNumbering, currentPath, visitedInPath);
        }

        LOGGER.debug("Finished path identification. Found {} potential paths.", foundPaths.size());
    }

    /**
     * Recursive helper function for Depth First Search path finding.
     *
     * @param currentNodeKey The current node being visited.
     * @param targetNodeKey  The conceptual target node (generator's negative terminal, represents ground if grounded).
     * @param adjacencyList  The network graph.
     * @param nodeNumbering  Map for node lookups.
     * @param currentPath    The path taken so far.
     * @param visitedInPath  Nodes visited in the current DFS traversal to prevent cycles.
     */
    private void dfsFindPathsRecursive(NodeKey currentNodeKey, NodeKey targetNodeKey,
                                       Map<INetworkMember, List<ConnectionInfo>> adjacencyList,
                                       Map<NodeKey, Integer> nodeNumbering,
                                       LinkedList<NodeKey> currentPath, Set<NodeKey> visitedInPath) {

        // Add current node to path and mark as visited for this path
        currentPath.addLast(currentNodeKey);
        visitedInPath.add(currentNodeKey);

        LOGGER.trace("DFS: Visiting {}, Path: {}", currentNodeKey, currentPath);

        // Check if we reached the target
        boolean targetReached = false;
        if (isNegativeTerminalGrounded(targetNodeKey.member())) {
            // For grounded generators, the target is reached if the current node connects back to the generator's negative side (which is ground 0)
            if (isNodeConnectedToGround(currentNodeKey, adjacencyList, nodeNumbering)) {
                targetReached = true;
                LOGGER.trace("DFS: Reached ground connection at {}", currentNodeKey);
            }
        } else {
            // For non-grounded generators, check if we reached the specific negative terminal node
            if (currentNodeKey.equals(targetNodeKey)) {
                targetReached = true;
                LOGGER.trace("DFS: Reached target node {}", targetNodeKey);
            }
        }


        if (targetReached) {
            // Found a complete path from start to target/ground
            foundPaths.add(new ArrayList<>(currentPath)); // Store a copy of the path
            LOGGER.debug("DFS: Found a complete path: {}", currentPath);
            // Don't explore further from the target/ground node in this path
        } else {
            // Explore neighbors
            INetworkMember currentMember = currentNodeKey.member();
            List<ConnectionInfo> connections = adjacencyList.getOrDefault(currentMember, Collections.emptyList());

            for (ConnectionInfo info : connections) {
                // Ensure the connection originates from the current node index we are exploring
                if (info.sourceNodeIndex() == currentNodeKey.nodeIndex()) {
                    NodeKey neighborNodeKey = new NodeKey(info.neighbor(), info.neighborNodeIndex());

                    // Check if the neighbor is already in the current path (cycle detection)
                    if (!visitedInPath.contains(neighborNodeKey)) {
                        // Recursively explore the neighbor
                        dfsFindPathsRecursive(neighborNodeKey, targetNodeKey, adjacencyList, nodeNumbering, currentPath, visitedInPath);
                    } else {
                        LOGGER.trace("DFS: Skipping neighbor {} (already in current path)", neighborNodeKey);
                    }
                }
            }
        }

        // Backtrack: Remove current node from path and visited set for this path
        visitedInPath.remove(currentNodeKey);
        currentPath.removeLast();
        LOGGER.trace("DFS: Backtracking from {}", currentNodeKey);
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
        List<ConnectionInfo> connections = adjacencyList.getOrDefault(nodeKey.member(), Collections.emptyList());
        for(ConnectionInfo info : connections) {
            if(info.sourceNodeIndex() == nodeKey.nodeIndex()) { // Connection originates from our node
                NodeKey neighborKey = new NodeKey(info.neighbor(), info.neighborNodeIndex());
                if(nodeNumbering.getOrDefault(neighborKey, -1) == 0) {
                    // Connects to a node explicitly numbered 0
                    return true;
                }
                // If the neighbor is the negative terminal of a grounded generator, it's effectively ground 0
                if (info.neighbor() instanceof IPowerProvider &&
                        isNegativeTerminalGrounded(info.neighbor()) &&
                        info.neighborNodeIndex() == NEGATIVE_TERMINAL_INDEX) {
                    // We don't need to check nodeNumbering here, the fact it's the grounded negative terminal is enough
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Creates resistor components (R prefix) for machines (IPowerReceiver).
     * Uses the `foundPaths` to determine connectivity between machine input and output nodes.
     *
     * @param nodeNumbering Map assigning SPICE node numbers to NodeKeys.
     * @return List of SPICE resistor definitions for machines.
     */
    private List<String> createMachineResistorComponents(Map<NodeKey, Integer> nodeNumbering) {
        List<String> machineResistors = new ArrayList<>();
        // Use instance variable processedMachines to avoid duplicates
        // processedMachines.clear(); // Cleared in initialize

        if (foundPaths.isEmpty()) {
            LOGGER.warn("Cannot create machine resistors: No paths found by DFS.");
            return machineResistors;
        }
        if (nodeNumbering == null) {
            LOGGER.error("Cannot create machine resistors: nodeNumbering map is null.");
            return machineResistors;
        }

        // Iterate through all found paths to identify machine connections
        for (List<NodeKey> path : foundPaths) {
            for (int i = 0; i < path.size(); i++) {
                NodeKey currentNodeKey = path.get(i);
                INetworkMember currentMember = currentNodeKey.member();

                // Check if this node represents the input of a machine
                if (currentMember instanceof IPowerReceiver machine && currentNodeKey.nodeIndex() == INPUT_TERMINAL_INDEX) {

                    // Check if we've already processed this machine
                    if (processedMachines.add(currentMember)) {
                        int resistance = machine.getResistance(); // Get dynamic resistance

                        // Node 1 is the machine's input node
                        Integer node1 = nodeNumbering.get(currentNodeKey);
                        if (node1 == null) {
                            LOGGER.error("Node number missing for machine input {}!", currentNodeKey);
                            continue; // Skip this machine
                        }

                        // Node 2 is the *next* node in this specific path
                        Integer node2 = null;
                        if (i + 1 < path.size()) {
                            NodeKey nextNodeKey = path.get(i + 1);
                            node2 = nodeNumbering.get(nextNodeKey);
                            if (node2 == null) {
                                LOGGER.error("Node number missing for node {} following machine {}!", nextNodeKey, currentNodeKey);
                                // Decide how to handle: skip, default to ground? Defaulting to 0 for now.
                                node2 = 0;
                            }
                        } else {
                            // Machine input is the last node in the path? This implies it connects back to ground/target directly.
                            LOGGER.warn("Machine input {} is the last node in a path. Assuming connection to ground (0).", currentNodeKey);
                            node2 = 0; // Assume connects to ground if it's the end of the path
                        }

                        // Avoid creating a resistor connected between the same node
                        if (node1.equals(node2)) {
                            LOGGER.warn("Skipping resistor for machine {} as node1 ({}) and node2 ({}) are the same.", currentMember.getPos().toShortString(), node1, node2);
                            continue;
                        }

                        // Format SPICE line: R<n> <node1> <node2> <resistance>
                        String componentName = "R" + machineResistorCount++;
                        String spiceLine = String.format(Locale.US, "%s %d %d %d",
                                componentName, node1, node2, resistance);

                        machineResistors.add(spiceLine);
                        LOGGER.debug("Generated machine resistor: {}", spiceLine);
                    }
                }
            }
        }
        return machineResistors;
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
            // Ensure member1 is a connector for wire-to-wire OR connector-to-machine/gen connections
            if (!(member1 instanceof IWireNode wireNode1)) continue;

            for (ConnectionInfo info : entry.getValue()) {
                INetworkMember member2 = info.neighbor();

                // Case 1: Connector <-> Connector
                if (member2 instanceof IWireNode wireNode2 && info.sourceNodeIndex() >= 0 && info.neighborNodeIndex() >= 0) {
                    NodeKey key1 = new NodeKey(member1, info.sourceNodeIndex());
                    NodeKey key2 = new NodeKey(member2, info.neighborNodeIndex());
                    EdgeKey edge = new EdgeKey(key1, key2);

                    if (processedEdges.add(edge)) { // If true, this edge is new
                        processWireEdge(wireNode1, key1, wireNode2, key2, nodeNumbering, wireResistors);
                    }
                }
                // Case 2: Connector <-> Machine/Generator (Implicit connection)
                else if ((member2 instanceof IPowerReceiver || member2 instanceof IPowerProvider) && info.neighborNodeIndex() == MACHINE_CONNECTION_INDEX) {
                    // Connection from Connector's specific terminal (info.sourceNodeIndex()) to Machine/Gen's implicit input (INPUT_TERMINAL_INDEX)
                    NodeKey key1 = new NodeKey(member1, info.sourceNodeIndex()); // Connector's terminal
                    NodeKey key2 = new NodeKey(member2, INPUT_TERMINAL_INDEX); // Machine/Gen's input terminal
                    EdgeKey edge = new EdgeKey(key1, key2);

                    if (processedEdges.add(edge)) {
                        // Treat this implicit connection as having negligible resistance for now, or assign a small default.
                        // A true wire resistance isn't directly applicable here.
                        // We might skip generating RW for these, or add a very small R value if needed by the simulator.
                        // Let's skip generating RW for these implicit links for now.
                        LOGGER.trace("Skipping RW generation for implicit connection: {} <-> {}", key1, key2);
                    }
                }
                // Case 3: Machine/Generator -> Connector (Implicit connection)
                // This direction is handled when iterating where member1 is the Machine/Gen, so we don't need extra logic here.
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
    private static class EdgeKey {
        private final NodeKey node1;
        private final NodeKey node2;

        public EdgeKey(NodeKey node1, NodeKey node2) {
            // Ensure consistent ordering for hashing/equality
            if (node1.hashCode() <= node2.hashCode()) { // Use <= for consistency
                this.node1 = node1;
                this.node2 = node2;
            } else {
                this.node1 = node2;
                this.node2 = node1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EdgeKey edgeKey = (EdgeKey) o;
            // Uses the ordered nodes for comparison
            return node1.equals(edgeKey.node1) && node2.equals(edgeKey.node2);
        }

        @Override
        public int hashCode() {
            // Uses the ordered nodes for hashing
            return Objects.hash(node1, node2);
        }

        @Override
        public String toString() {
            return "Edge[" + node1 + " <-> " + node2 + "]";
        }
    }
}
