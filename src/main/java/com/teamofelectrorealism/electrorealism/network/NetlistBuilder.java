package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.block.IPowerProvider;
import com.teamofelectrorealism.electrorealism.block.IPowerReceiver;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorPolarity;
import com.teamofelectrorealism.electrorealism.block.machine.generator.AbstractGeneratorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.machine.user.AbstractPowerUserBlockEntity;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NetlistBuilder {
    // Node name mapping: Stores the assigned SPICE name for each identified electrical node.
    // The key could be a canonical representation of the node (e.g., the BlockPos+Index of one point within it).
    private final Map<String, String> electricalNodeNames = new HashMap<>();
    private static final String GROUND_NODE = "0";

    // Component Counters
    private final AtomicInteger vCounter = new AtomicInteger(0);
    private final AtomicInteger rMachineCounter = new AtomicInteger(0);
    private final AtomicInteger rWireCounter = new AtomicInteger(0);
    private final AtomicInteger nodeNameCounter = new AtomicInteger(1); // For generating names like Net_1, Net_2 if needed

    // Component definitions built during the process
    private final List<String> componentDefinitions = new ArrayList<>();
    private final List<String> wireDefinitions = new ArrayList<>();

    // --- Helper Methods ---
    private String getConnectionPointKey(BlockPos pos, int index) {
        return pos.toShortString().replace(" ", "") + "_idx" + index;
    }

    private String sanitizeName(String name) {
        name = name.replaceAll("[^a-zA-Z0-9_]", "_");
        if (name.length() > 0 && !Character.isLetter(name.charAt(0))) name = "N_" + name;
        if (name.equals(GROUND_NODE)) return name;
        if (name.matches("^[0-9]+$")) name = "Node_" + name;
        if (name.length() == 1 && "VvRrIiCcLl".indexOf(name.charAt(0)) != -1) name = name + "_node";
        return name;
    }

    private UUID generateConnectionUUID(BlockPos pos1, BlockPos pos2) {
        long h1 = pos1.asLong(); long h2 = pos2.asLong();
        return UUID.nameUUIDFromBytes(((h1 < h2) ? (pos1.toString() + pos2.toString()) : (pos2.toString() + pos1.toString())).getBytes());
    }

    private INetworkMember findMemberAt(BlockPos pos, Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        for (INetworkMember member : adjacencyList.keySet()) {
            if (member.getPos().equals(pos)) {
                return member;
            }
        }
        return null;
    }
    // --- End Helper Methods ---


    // --- Core Logic ---

    // Step 1a: Find connection points linked to generator negative terminals (via NEGATIVE polarity connector) and mark them as GROUND_NODE ('0')
    private void findAndAssignGroundNodes(Map<INetworkMember, List<ConnectionInfo>> adjacencyList, Level level, Set<String> visitedPoints) {
        System.out.println("[NetlistBuilder] Starting ground node identification (Rule: Gen -> Negative Polarity Connector)..."); // Debug
        Queue<Map.Entry<BlockPos, Integer>> groundQueue = new LinkedList<>();

        // 1. Find initial ground connection points from Generator(s) connecting to NEGATIVE connector terminals
        for (INetworkMember member : adjacencyList.keySet()) {
            if (member instanceof AbstractGeneratorBlockEntity generator) {
                System.out.println("  [GroundFind] Checking Generator at " + generator.getPos().toShortString()); // Debug
                List<ConnectionInfo> genConnections = adjacencyList.getOrDefault(generator, Collections.emptyList());

                for (ConnectionInfo conn : genConnections) {
                    // We are looking at the connection FROM the Generator TO the Neighbor (Connector)
                    if (conn.neighbor() instanceof AbstractConnectorBlockEntity connector) {
                        BlockPos connectorPos = connector.getPos();
                        int connectorIndex = conn.neighborNodeIndex(); // The index *on the connector* where the generator connects

                        // Use the polarity stored in ConnectionInfo, assuming NetworkGraphBuilder set it correctly
                        // This polarity represents the state of the *connector's* terminal being entered.
                        ConnectorPolarity polarityAtConnectorTerminal = conn.polarityAtNeighborEntry();

                        System.out.println("    [GroundFind] Gen connects to Connector at " + connectorPos.toShortString() + " idx " + connectorIndex + ". Connector terminal polarity: " + polarityAtConnectorTerminal); // Debug

                        // If the connector's terminal polarity is NEGATIVE, this is a ground start point
                        if (polarityAtConnectorTerminal == ConnectorPolarity.NEGATIVE) {
                            String key = getConnectionPointKey(connectorPos, connectorIndex);
                            if (!visitedPoints.contains(key) && !electricalNodeNames.containsKey(key)) { // Check if not already processed
                                System.out.println("      -> Identified NEGATIVE connector terminal. Adding '" + key + "' to ground queue."); // Debug
                                groundQueue.add(Map.entry(connectorPos, connectorIndex));
                                // Mark it immediately to avoid issues if multiple generators connect to the same ground point
                                electricalNodeNames.put(key, GROUND_NODE);
                                visitedPoints.add(key);
                            }
                        }
                    }
                }
            }
        }

        // 2. Perform BFS from initial ground points to find all connected ground points
        Set<String> processedInThisGroundSearch = new HashSet<>(); // Track points added via BFS to avoid loops
        processedInThisGroundSearch.addAll(electricalNodeNames.keySet()); // Add initial points found above

        while (!groundQueue.isEmpty()) {
            Map.Entry<BlockPos, Integer> current = groundQueue.poll();
            BlockPos currentPos = current.getKey();
            int currentIndex = current.getValue();
            String currentKey = getConnectionPointKey(currentPos, currentIndex);

            // This check might be redundant if we add to processedInThisGroundSearch when adding to queue, but safer
            if (!processedInThisGroundSearch.add(currentKey)) continue;

            System.out.println("    [GroundBFS] Processing: " + currentKey + " (Assigning '0')"); // Debug
            // Already assigned '0' when added to queue or found initially

            // Find neighbors connected via wires/connectors (assumed zero resistance for node ID spread)
            INetworkMember currentMember = findMemberAt(currentPos, adjacencyList);
            if (!(currentMember instanceof IWireNode currentWireNode)) continue;

            List<ConnectionInfo> connections = adjacencyList.getOrDefault(currentMember, Collections.emptyList());
            for (ConnectionInfo info : connections) {
                if (info.sourceNodeIndex() != currentIndex) continue; // Only follow connections *from* the index we are currently exploring

                // Only traverse to other connectors to spread the ground node '0'
                if (info.neighbor() instanceof AbstractConnectorBlockEntity) {
                    String neighborKey = getConnectionPointKey(info.neighbor().getPos(), info.neighborNodeIndex());
                    // If the neighbor hasn't been assigned '0' yet and isn't already queued/processed in this search
                    if (!electricalNodeNames.containsKey(neighborKey) && !processedInThisGroundSearch.contains(neighborKey)) {
                        System.out.println("      [GroundBFS] Adding neighbor '" + neighborKey + "' to ground queue."); // Debug
                        groundQueue.add(Map.entry(info.neighbor().getPos(), info.neighborNodeIndex()));
                        // Assign '0' and mark visited immediately when adding to queue
                        electricalNodeNames.put(neighborKey, GROUND_NODE);
                        visitedPoints.add(neighborKey);
                        processedInThisGroundSearch.add(neighborKey); // Mark for this specific BFS run
                    }
                }
            }
        }
        System.out.println("[NetlistBuilder] Ground node identification finished. Found " + electricalNodeNames.size() + " ground points."); // Debug
    }


    // ** Step 1b + Traversal ** (Placeholder logic still needs replacement for non-ground nodes)
    private void traverseAndAssignNodeName(BlockPos startPos, int startIndex, Map<INetworkMember, List<ConnectionInfo>> adjacencyList, Level level, Set<String> visitedPoints) {
        // ... (Previous Placeholder Logic - Needs replacing with actual BFS/DFS for non-ground nodes) ...
        // This traversal should:
        // 1. Generate a NEW unique name (Net_X).
        // 2. Explore all reachable points *from (startPos, startIndex)* via connectors/wires *that haven't been visited*.
        // 3. Assign the SAME generated name (Net_X) to *all* points in this group.
        // 4. Mark all points in the group as visited in the main `visitedPoints` set.

        // --- Start Placeholder ---
        String startKey = getConnectionPointKey(startPos, startIndex);
        // Double check it wasn't somehow visited or assigned between the outer loop check and this call
        if (!visitedPoints.contains(startKey) && !electricalNodeNames.containsKey(startKey)) {
            String newNodeName = "Net_" + nodeNameCounter.getAndIncrement();
            System.out.println("  [NodeAssign] Assigning NEW node name '" + newNodeName + "' to placeholder starting point: " + startKey); // Debug
            electricalNodeNames.put(startKey, newNodeName);
            visitedPoints.add(startKey); // Mark visited
        } else {
            System.out.println("  [NodeAssign] Skipping traversal start for already visited/assigned point: " + startKey); // Debug
        }
        // !!! This placeholder only assigns to the start point, not the connected group !!!
        // !!! Replace with actual traversal !!!
        // --- End Placeholder ---
    }


    // Step 1: Identify all distinct electrical nodes and assign unique SPICE names
    private void identifyElectricalNodes(Map<INetworkMember, List<ConnectionInfo>> adjacencyList, Level level) {
        electricalNodeNames.clear();
        nodeNameCounter.set(1);
        Set<String> visitedPoints = new HashSet<>(); // Track visited (BlockPos, Index) keys globally

        // 1a. Assign Ground Node(s) '0'
        findAndAssignGroundNodes(adjacencyList, level, visitedPoints);

        // 1b. Traverse remaining graph to identify other nodes
        System.out.println("[NetlistBuilder] Starting assignment of non-ground nodes..."); // Debug
        for (INetworkMember member : adjacencyList.keySet()) {
            if (!(member instanceof IWireNode wireNode)) continue; // Typically connectors

            for (int i = 0; i < wireNode.getConnectionPointCount(); i++) {
                String currentPointKey = getConnectionPointKey(member.getPos(), i);
                // If point hasn't been visited (e.g. by ground search) and doesn't have a name yet
                if (!visitedPoints.contains(currentPointKey) /* && !electricalNodeNames.containsKey(currentPointKey) redundant */) {
                    System.out.println("  [NodeAssign] Found unassigned/unvisited point: " + currentPointKey + ". Starting traversal..."); // Debug
                    traverseAndAssignNodeName(member.getPos(), i, adjacencyList, level, visitedPoints);
                }
            }
        }
        System.out.println("[NetlistBuilder] Finished assigning all node names."); // Debug
        // Debug: Print final node assignments
        System.out.println("  Final Node Assignments (" + electricalNodeNames.size() + " total):");
        electricalNodeNames.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()) // Sort by assigned name for readability
                .forEach(entry -> System.out.println("    '" + entry.getKey() + "' -> '" + entry.getValue() + "'"));
    }


    // Step 2: Define Components and Wires using assigned node names
    private void defineCircuitElements(Map<INetworkMember, List<ConnectionInfo>> adjacencyList, Level level) {
        // ... (Component/Wire definition logic - same as before, using lookups in `electricalNodeNames`) ...
        componentDefinitions.clear(); wireDefinitions.clear();
        vCounter.set(0); rMachineCounter.set(0); rWireCounter.set(0);
        Set<UUID> processedWireConnections = new HashSet<>();

        System.out.println("[NetlistBuilder] Defining circuit elements..."); // Debug
        for (Map.Entry<INetworkMember, List<ConnectionInfo>> entry : adjacencyList.entrySet()) {
            INetworkMember member = entry.getKey();
            List<ConnectionInfo> connections = entry.getValue();

            // --- Define V, R components ---
            if (member instanceof AbstractGeneratorBlockEntity generator) {
                String compName = "V" + vCounter.incrementAndGet(); int voltage = ((IPowerProvider) generator).getVoltage();
                String posNode = "NODE_ERROR"; String negNode = GROUND_NODE;
                for (ConnectionInfo conn : connections) { /* Find positive node key */
                    if (conn.neighbor() instanceof AbstractConnectorBlockEntity) {
                        // Assuming the *first* connection found TO a connector is the POSITIVE terminal for simplicity
                        String key = getConnectionPointKey(conn.neighbor().getPos(), conn.neighborNodeIndex());
                        posNode = electricalNodeNames.getOrDefault(key, "NODE_ERROR_V_" + key.replace(",","."));
                        break;
                    }
                }
                String def = String.format("%s %s %s DC %d", compName, posNode, negNode, voltage);
                System.out.println("  Defined: " + def); componentDefinitions.add(def);
            } else if (member instanceof AbstractPowerUserBlockEntity user) {
                String compName = "R" + rMachineCounter.incrementAndGet(); int resistance = ((IPowerReceiver) user).getResistance();
                String node1 = "NODE1_ERROR"; String node2 = "NODE2_ERROR"; int foundNodes = 0;
                for (ConnectionInfo conn : connections) { /* find node1, node2 keys */
                    if (conn.neighbor() instanceof AbstractConnectorBlockEntity) {
                        String key = getConnectionPointKey(conn.neighbor().getPos(), conn.neighborNodeIndex());
                        String nodeName = electricalNodeNames.getOrDefault(key, "NODE_ERROR_R_" + key.replace(",",".") + "_N" + (foundNodes+1));
                        if (foundNodes == 0) node1 = nodeName; else node2 = nodeName; foundNodes++; if (foundNodes >= 2) break;
                    }
                }
                if (!node1.contains("ERROR") && !node2.contains("ERROR")) {
                    String def = String.format("%s %s %s %d", compName, node1, node2, resistance);
                    System.out.println("  Defined: " + def); componentDefinitions.add(def);
                } else { /* Add error comment */ }
            }

            // --- Define Wires (RW) ---
            if (member instanceof IWireNode sourceWireNode && member instanceof AbstractConnectorBlockEntity) {
                for (ConnectionInfo info : connections) { /* ... */
                    if (!(info.neighbor() instanceof IWireNode neighborWireNode && info.neighbor() instanceof AbstractConnectorBlockEntity)) continue;
                    UUID connectionUUID = generateConnectionUUID(sourceWireNode.getPos(), neighborWireNode.getPos());
                    if (processedWireConnections.contains(connectionUUID)) continue;
                    // ... (calculate wireResistance) ...
                    double wireResistance = 1e-9; // Placeholder calc
                    ConnectionPoint sourceCP = sourceWireNode.getConnectionPoint(info.sourceNodeIndex()); if(sourceCP==null) continue; WireType wireType = sourceCP.getWireType(); if(wireType==null) continue; //Null checks
                    Vec3 offset1 = sourceWireNode.getConnectionPointOffset(info.sourceNodeIndex()); Vec3 offset2 = neighborWireNode.getConnectionPointOffset(info.neighborNodeIndex()); Vec3 worldPos1 = Vec3.atCenterOf(sourceWireNode.getPos()).add(offset1); Vec3 worldPos2 = Vec3.atCenterOf(neighborWireNode.getPos()).add(offset2); double length = worldPos1.distanceTo(worldPos2); double resistivity = wireType.getResistivity(); double area = 1.0e-6; wireResistance = (length > 1e-9) ? (resistivity * length / area) : 1e-9;


                    String key1 = getConnectionPointKey(sourceWireNode.getPos(), info.sourceNodeIndex());
                    String key2 = getConnectionPointKey(neighborWireNode.getPos(), info.neighborNodeIndex());
                    String nodeName1 = electricalNodeNames.getOrDefault(key1, "NODE_ERROR_W_" + key1);
                    String nodeName2 = electricalNodeNames.getOrDefault(key2, "NODE_ERROR_W_" + key2);

                    if (wireResistance > 1e-9 && !nodeName1.equals(nodeName2) && !nodeName1.contains("ERROR") && !nodeName2.contains("ERROR")) {
                        String wireName = "RW" + rWireCounter.incrementAndGet();
                        String def = String.format("%s %s %s %.6G", wireName, nodeName1, nodeName2, wireResistance);
                        System.out.println("  Defined: " + def); wireDefinitions.add(def);
                        processedWireConnections.add(connectionUUID);
                    } else if (!nodeName1.equals(nodeName2)) { /* Ideal comment */ processedWireConnections.add(connectionUUID); }
                }
            }
        }
        System.out.println("[NetlistBuilder] Finished defining circuit elements."); // Debug
    }


    // --- Main Build Method ---
    String buildNetlist(Map<INetworkMember, List<ConnectionInfo>> adjacencyList, Level level) {

        // Step 1: Identify all distinct electrical nodes and assign unique SPICE names
        identifyElectricalNodes(adjacencyList, level);

        // Step 2: Define components (V, R) and wires (RW) using the assigned node names
        defineCircuitElements(adjacencyList, level);

        // Step 3: Assemble the final netlist string
        StringBuilder netlistContent = new StringBuilder();
        netlistContent.append("* Generated Netlist (Standard Format, Descriptive Nodes V3)\n");
        componentDefinitions.forEach(line -> netlistContent.append(line).append("\n"));
        wireDefinitions.forEach(line -> netlistContent.append(line).append("\n"));
        netlistContent.append(".END\n");

        System.out.println("---- Generated Netlist ----");
        System.out.println(netlistContent.toString());
        System.out.println("---------------------------");

        return netlistContent.toString();
    }
}
