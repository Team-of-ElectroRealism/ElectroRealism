package com.teamofelectrorealism.electrorealism.network;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.block.IPowerProvider;
import com.teamofelectrorealism.electrorealism.block.IPowerReceiver;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorPolarity;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.*;

/**
 * Constructs a SPICE-compatible netlist from a network adjacency list.
 * <p>
 * The builder performs the following steps:
 * <ol>
 *   <li>Classify members into generators, machines, and connectors.</li>
 *   <li>Assign SPICE node numbers using a disjoint-set (union-find) algorithm.</li>
 *   <li>Generate voltage source, machine resistor, and wire resistor component definitions.</li>
 *   <li>Combine, deduplicate, and format the final netlist text.</li>
 * </ol>
 * <p>
 * Example usage:
 * <pre>
 *   NetlistBuilder builder = new NetlistBuilder();
 *   String netlist = builder.buildNetlist(adjacencyMap);
 * </pre>
 */
public class NetlistBuilder {
    private int voltageSourceIndex   = 1;
    private int machineResistorIndex = 1;
    private int wireResistorIndex    = 1;
    private final Logger logger = LogUtils.getLogger();
    private final Set<EdgeKey> processedEdges = new HashSet<>();

    private static final String GENERATORS_KEY = "generators";
    private static final String MACHINES_KEY   = "machines";
    private static final String CONNECTORS_KEY = "connectors";

    private static final int POSITIVE_TERMINAL = 0;
    private static final int NEGATIVE_TERMINAL = 1;
    private static final int INPUT_TERMINAL    = 0;
    private static final int OUTPUT_TERMINAL   = 1;

    /**
     * Build a SPICE netlist from the provided network adjacency list.
     *
     * @param adjacencyMap map of network member to list of its connection information
     * @return formatted SPICE netlist as a String
     */
    public String buildNetlist(Map<INetworkMember, List<ConnectionInfo>> adjacencyMap) {
        Map<String, List<INetworkMember>> classifiedMembers = classifyNetworkMembers(adjacencyMap);
        Map<NodeKey, Integer> nodeIdMap = assignNodeNumbers(adjacencyMap);

        List<String> voltageSources = createVoltageSourceComponents(
                classifiedMembers.get(GENERATORS_KEY), nodeIdMap);
        List<String> machineResistors = createMachineResistorComponents(
                classifiedMembers.get(MACHINES_KEY), nodeIdMap);
        List<String> wireResistors = createWireResistorComponents(adjacencyMap, nodeIdMap);

        List<String> allComponents = new ArrayList<>();
        allComponents.addAll(voltageSources);
        allComponents.addAll(machineResistors);
        allComponents.addAll(wireResistors);

        // Remove duplicates while preserving order
        List<String> uniqueComponents = new ArrayList<>(new LinkedHashSet<>(allComponents));
        return formatSpiceNetlist(uniqueComponents);
    }

    /**
     * Classify network members into generators, machines, and connectors.
     * IPowerProvider instances (not wire nodes) are generators.
     * IPowerReceiver instances (not wire nodes) are machines.
     * IWireNode instances are connectors.
     *
     * @param adjacencyMap network adjacency list
     * @return map with keys "generators", "machines", "connectors" mapping to lists of members
     */
    private Map<String, List<INetworkMember>> classifyNetworkMembers(
            Map<INetworkMember, List<ConnectionInfo>> adjacencyMap
    ) {
        Map<String, List<INetworkMember>> classified = new HashMap<>();
        classified.put(GENERATORS_KEY, new ArrayList<>());
        classified.put(MACHINES_KEY, new ArrayList<>());
        classified.put(CONNECTORS_KEY, new ArrayList<>());

        for (INetworkMember member : adjacencyMap.keySet()) {
            if (member instanceof IPowerProvider && !(member instanceof IWireNode)) {
                classified.get(GENERATORS_KEY).add(member);
            } else if (member instanceof IPowerReceiver && !(member instanceof IWireNode)) {
                classified.get(MACHINES_KEY).add(member);
            } else if (member instanceof IWireNode) {
                classified.get(CONNECTORS_KEY).add(member);
            } else {
                logger.warn("Unclassified member at {}", member.getPos().toShortString());
            }
        }

        logger.debug("Classified: {} generators, {} machines, {} connectors",
                classified.get(GENERATORS_KEY).size(),
                classified.get(MACHINES_KEY).size(),
                classified.get(CONNECTORS_KEY).size());

        return classified;
    }

    /**
     * Assign SPICE node numbers using union-find to collapse connected terminals.
     *<p>
     * The algorithm:
     * <ol>
     *   <li>Collapse internal terminals of each connector block.</li>
     *   <li>Union each machine's input/output terminals with adjacent wire nodes.</li>
     *   <li>Union generator terminals with wire neighbors based on polarity.</li>
     *   <li>Ground the negative terminal of the first generator to node 0.</li>
     *   <li>Assign incremental node numbers to remaining nets.</li>
     * </ol>
     *
     * @param adjacencyMap network adjacency list
     * @return map from NodeKey to SPICE node number
     */
    private Map<NodeKey, Integer> assignNodeNumbers(
            Map<INetworkMember, List<ConnectionInfo>> adjacencyMap
    ) {
        DSU unionFind = new DSU();

        // Collapse internal connector terminals
        for (INetworkMember member : adjacencyMap.keySet()) {
            if (member instanceof IWireNode wireNode) {
                NodeKey baseKey = new NodeKey(member, 0);
                for (int i = 1; i < wireNode.getConnectionPointCount(); i++) {
                    unionFind.union(baseKey, new NodeKey(member, i));
                }
            }
        }

        // Map each machine to its neighboring wire terminals
        Map<INetworkMember, List<NodeKey>> machineTerminalMap = new HashMap<>();
        for (var entry : adjacencyMap.entrySet()) {
            INetworkMember source = entry.getKey();
            if (!(source instanceof IWireNode)) continue;

            for (ConnectionInfo connectionInfo : entry.getValue()) {
                INetworkMember neighbor = connectionInfo.neighbor();
                if (!(neighbor instanceof IPowerReceiver)) continue;

                int index = connectionInfo.sourceNodeIndex() >= 0
                        ? connectionInfo.sourceNodeIndex() : 0;
                machineTerminalMap
                        .computeIfAbsent(neighbor, k -> new ArrayList<>())
                        .add(new NodeKey(source, index));
            }
        }

        // Union machine terminals with wire neighbors
        for (var entry : machineTerminalMap.entrySet()) {
            INetworkMember machine = entry.getKey();
            List<NodeKey> terminals = entry.getValue();
            if (terminals.size() != 2) {
                logger.warn("Machine {} has {} wire neighbors; skipping",
                        machine.getPos().toShortString(), terminals.size());
                continue;
            }
            unionFind.union(new NodeKey(machine, INPUT_TERMINAL), terminals.get(0));
            unionFind.union(new NodeKey(machine, OUTPUT_TERMINAL), terminals.get(1));
        }

        // Union generator terminals with wire neighbors by polarity
        for (var entry : adjacencyMap.entrySet()) {
            INetworkMember provider = entry.getKey();
            if (!(provider instanceof IPowerProvider)) continue;

            for (ConnectionInfo connectionInfo : entry.getValue()) {
                if (connectionInfo.polarityAtNeighborEntry() == ConnectorPolarity.POSITIVE
                        && connectionInfo.neighbor() instanceof IWireNode) {
                    int idx = connectionInfo.neighborNodeIndex() >= 0
                            ? connectionInfo.neighborNodeIndex() : 0;
                    unionFind.union(
                            new NodeKey(provider, POSITIVE_TERMINAL),
                            new NodeKey(connectionInfo.neighbor(), idx)
                    );
                }
            }
        }

        // Ground negative generator terminal
        for (var entry : adjacencyMap.entrySet()) {
            INetworkMember provider = entry.getKey();
            if (!(provider instanceof IPowerProvider)) continue;

            for (ConnectionInfo connectionInfo : entry.getValue()) {
                if (connectionInfo.polarityAtNeighborEntry() == ConnectorPolarity.NEGATIVE
                        && connectionInfo.neighbor() instanceof IWireNode) {
                    int idx = connectionInfo.neighborNodeIndex() >= 0
                            ? connectionInfo.neighborNodeIndex() : 0;
                    unionFind.union(
                            new NodeKey(provider, NEGATIVE_TERMINAL),
                            new NodeKey(connectionInfo.neighbor(), idx)
                    );
                }
            }
        }

        // Collect all NodeKeys
        List<NodeKey> allTerminals = new ArrayList<>();
        for (INetworkMember member : adjacencyMap.keySet()) {
            if (member instanceof IPowerProvider) {
                allTerminals.add(new NodeKey(member, POSITIVE_TERMINAL));
                allTerminals.add(new NodeKey(member, NEGATIVE_TERMINAL));
            }
            if (member instanceof IPowerReceiver) {
                allTerminals.add(new NodeKey(member, INPUT_TERMINAL));
                allTerminals.add(new NodeKey(member, OUTPUT_TERMINAL));
            }
            if (member instanceof IWireNode wireNode) {
                for (int i = 0; i < wireNode.getConnectionPointCount(); i++) {
                    allTerminals.add(new NodeKey(member, i));
                }
            }
        }

        // Group by DSU root
        Map<NodeKey, List<NodeKey>> netGroups = new HashMap<>();
        for (NodeKey key : allTerminals) {
            NodeKey root = unionFind.find(key);
            netGroups.computeIfAbsent(root, __ -> new ArrayList<>()).add(key);
        }

        // Determine anchor for each net group
        record Anchor(NodeKey root, int x, int y, int z, int idx) {}
        List<Anchor> anchors = new ArrayList<>();
        for (var net : netGroups.entrySet()) {
            NodeKey root = net.getKey();
            NodeKey anchorTerminal = Collections.min(net.getValue(), Comparator
                    .comparing((NodeKey nk) -> nk.member.getPos().getX())
                    .thenComparing(nk -> nk.member.getPos().getY())
                    .thenComparing(nk -> nk.member.getPos().getZ())
                    .thenComparing(nk -> nk.idx)
            );
            var pos = anchorTerminal.member.getPos();
            anchors.add(new Anchor(root, pos.getX(), pos.getY(), pos.getZ(), anchorTerminal.idx));
        }

        // Sort anchors lexicographically by position and terminal index
        anchors.sort(Comparator
                .comparing((Anchor a) -> a.x)
                .thenComparing(a -> a.y)
                .thenComparing(a -> a.z)
                .thenComparing(a -> a.idx)
        );

        Map<NodeKey, Integer> numbering = new HashMap<>();
        // Ground the first generator's negative terminal to node 0
        adjacencyMap.keySet().stream()
                .filter(m -> m instanceof IPowerProvider)
                .findFirst()
                .ifPresent(gen -> {
                    NodeKey negKey = new NodeKey(gen, NEGATIVE_TERMINAL);
                    NodeKey negRoot = unionFind.find(negKey);
                    numbering.put(negRoot, 0);
                });

        // Assign sequential node numbers to remaining nets
        int nextNodeId = 1;
        for (Anchor anchor : anchors) {
            if (!numbering.containsKey(anchor.root)) {
                numbering.put(anchor.root, nextNodeId++);
            }
        }

        // Propagate node numbers to all terminals
        for (NodeKey key : allTerminals) {
            NodeKey root = unionFind.find(key);
            numbering.put(key, numbering.get(root));
        }

        return numbering;
    }

    /**
     * Generate SPICE voltage source definitions for each generator.
     *
     * @param generators list of IPowerProvider instances
     * @param nodeIdMap map from NodeKey to assigned node number
     * @return list of SPICE lines defining voltage sources
     */
    private List<String> createVoltageSourceComponents(
            List<INetworkMember> generators,
            Map<NodeKey, Integer> nodeIdMap
    ) {
        List<String> components = new ArrayList<>();
        if (generators == null) return components;

        for (INetworkMember member : generators) {
            IPowerProvider provider = (IPowerProvider) member;
            int positiveNode = nodeIdMap.get(new NodeKey(member, POSITIVE_TERMINAL));
            int negativeNode = nodeIdMap.get(new NodeKey(member, NEGATIVE_TERMINAL));
            String sourceName = "V" + voltageSourceIndex++;
            components.add(
                    String.format(Locale.US,
                            "%s %d %d DC %d",
                            sourceName, positiveNode, negativeNode, provider.getVoltage())
            );
        }
        return components;
    }

    /**
     * Generate SPICE resistor definitions for each machine.
     *
     * @param machines list of IPowerReceiver instances
     * @param nodeIdMap map from NodeKey to assigned node number
     * @return list of SPICE lines defining machine resistors
     */
    private List<String> createMachineResistorComponents(
            List<INetworkMember> machines,
            Map<NodeKey, Integer> nodeIdMap
    ) {
        List<String> components = new ArrayList<>();
        if (machines == null) return components;

        for (INetworkMember member : machines) {
            IPowerReceiver receiver = (IPowerReceiver) member;
            NodeKey inputKey = new NodeKey(member, INPUT_TERMINAL);
            NodeKey outputKey = new NodeKey(member, OUTPUT_TERMINAL);
            int inputNode = nodeIdMap.get(inputKey);
            int outputNode = nodeIdMap.get(outputKey);

            if (inputNode == outputNode) {
                logger.warn("Machine {} collapsed to one node {}; skipping",
                        member.getPos().toShortString(), inputNode);
                continue;
            }

            String resistorName = "R" + machineResistorIndex++;
            components.add(
                    String.format(Locale.US,
                            "%s %d %d %d",
                            resistorName, inputNode, outputNode, receiver.getResistance())
            );
        }
        return components;
    }

    /**
     * Generate SPICE resistor definitions for each wire segment.
     *
     * @param adjacencyMap network adjacency list
     * @param nodeIdMap map from NodeKey to assigned node number
     * @return list of SPICE lines defining wire resistors
     */
    private List<String> createWireResistorComponents(
            Map<INetworkMember, List<ConnectionInfo>> adjacencyMap,
            Map<NodeKey, Integer> nodeIdMap
    ) {
        List<String> components = new ArrayList<>();
        for (var entry : adjacencyMap.entrySet()) {
            INetworkMember candidate = entry.getKey();
            if (!(candidate instanceof IWireNode wireA)) continue;

            for (ConnectionInfo connectionInfo : entry.getValue()) {
                INetworkMember neighbor = connectionInfo.neighbor();
                if (!(neighbor instanceof IWireNode wireB)) continue;

                int indexA = connectionInfo.sourceNodeIndex();
                int indexB = connectionInfo.neighborNodeIndex();
                if (indexA < 0 || indexB < 0) continue;

                NodeKey keyA = new NodeKey(candidate, indexA);
                NodeKey keyB = new NodeKey(neighbor, indexB);
                EdgeKey edge = new EdgeKey(keyA, keyB);
                if (!processedEdges.add(edge)) continue;

                int nodeA = nodeIdMap.get(keyA);
                int nodeB = nodeIdMap.get(keyB);
                if (nodeA == nodeB) {
                    logger.warn("Zero-length resistor between {}↔{} (node {})",
                            keyA, keyB, nodeA);
                    continue;
                }

                WireType type = wireA.getWireType(indexA);
                double resistance = calculateWireResistance(wireA, indexA, wireB, indexB, type);
                String name = "RW" + wireResistorIndex++;
                components.add(
                        String.format(Locale.US, "%s %d %d %.7f", name, nodeA, nodeB, resistance)
                );
            }
        }
        return components;
    }

    /**
     * Compute resistance of a wire segment: R = ρ · (length / area).
     *
     * @param nodeA first wire node endpoint
     * @param idxA connection point index on the first node
     * @param nodeB second wire node endpoint
     * @param idxB connection point index on the second node
     * @param wireType type of the wire (provides resistivity)
     * @return resistance in ohms
     */
    private double calculateWireResistance(
            IWireNode nodeA, int idxA,
            IWireNode nodeB, int idxB,
            WireType wireType
    ) {
        Vec3 pointA = Vec3.atCenterOf(nodeA.getPos()).add(nodeA.getConnectionPointOffset(idxA));
        Vec3 pointB = Vec3.atCenterOf(nodeB.getPos()).add(nodeB.getConnectionPointOffset(idxB));
        double distance = pointA.distanceTo(pointB);
        if (distance < 1e-6) {
            return 0.0;
        }
        double resistivity = wireType.getResistivity();
        double crossSectionalArea = 1e-6; // 1 mm²
        return resistivity * (distance / crossSectionalArea);
    }

    /**
     * Format the list of SPICE component definitions into complete netlist text.
     *
     * @param components list of SPICE lines (voltage sources, resistors)
     * @return full netlist text including directives
     */
    private String formatSpiceNetlist(List<String> components) {
        StringBuilder builder = new StringBuilder();
        builder.append("* Generated ElectroRealism Netlist (").append(new Date()).append(")\n");

        var sources = components.stream().filter(s -> s.startsWith("V")).toList();
        var machineRes = components.stream().filter(s -> s.startsWith("R") && !s.startsWith("RW")).toList();
        var wireRes    = components.stream().filter(s -> s.startsWith("RW")).toList();

        if (!sources.isEmpty()) {
            builder.append("\n* Voltage Sources\n");
            sources.forEach(line -> builder.append(line).append("\n"));
        }
        if (!machineRes.isEmpty()) {
            builder.append("\n* Machine Resistances\n");
            machineRes.forEach(line -> builder.append(line).append("\n"));
        }
        if (!wireRes.isEmpty()) {
            builder.append("\n* Wire Resistances\n");
            wireRes.forEach(line -> builder.append(line).append("\n"));
        }

        builder.append("\n* Simulation Control\n");
        builder.append(".control\n");
        builder.append("    run\n");
        builder.append("    set wr_singlescale\n");
        builder.append("    set wr_vecnames\n");
        builder.append("    option numdgt=9\n");
        builder.append("    wrdata results.csv time all\n");
        builder.append("    quit\n");
        builder.append(".endc\n");
        builder.append("\n* Transient Analysis\n");
        builder.append(".tran 0.1ms 1ms\n");
        builder.append("\n.END\n");

        return builder.toString();
    }

    /**
     * Disjoint-set (union-find) structure for merging node terminals into nets.
     */
    private static class DSU {
        private final Map<NodeKey, NodeKey> parent = new HashMap<>();

        /** Find the representative of the set containing x. */
        NodeKey find(NodeKey x) {
            parent.putIfAbsent(x, x);
            NodeKey p = parent.get(x);
            if (!p.equals(x)) {
                p = find(p);
                parent.put(x, p);
            }
            return p;
        }

        /** Union the sets containing a and b. */
        void union(NodeKey a, NodeKey b) {
            NodeKey rootA = find(a);
            NodeKey rootB = find(b);
            if (!rootA.equals(rootB)) {
                parent.put(rootA, rootB);
            }
        }
    }

    /**
     * Identifier for a single terminal on a network member.
     * @param member network component instance
     * @param idx terminal index on the component
     */
    private record NodeKey(INetworkMember member, int idx) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NodeKey other)) return false;
            return idx == other.idx && member.getPos().equals(other.member.getPos());
        }

        @Override
        public int hashCode() {
            return Objects.hash(member.getPos(), idx);
        }

        @Override
        public String toString() {
            return member.getClass().getSimpleName() + "@"
                    + member.getPos().toShortString() + "[" + idx + "]";
        }
    }

    /**
     * Undirected edge key for deduplicating wire resistor components.
     * Ensures consistent ordering of endpoints.
     */
    private record EdgeKey(NodeKey a, NodeKey b) {
        public EdgeKey {
            if (a.hashCode() > b.hashCode()) {
                var temp = a; a = b; b = temp;
            }
        }

        @Override
        public String toString() {
            return "Edge[" + a + "↔" + b + "]";
        }
    }
}
