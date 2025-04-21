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
 * Converts an adjacency list of INetworkMember→ConnectionInfo
 * into a SPICE‐compatible netlist.
 */
public class NetlistBuilder {
    private int voltageSourceCount   = 1;
    private int machineResistorCount = 1;
    private int wireResistorCount    = 1;
    private final Logger LOGGER = LogUtils.getLogger();
    private final Set<EdgeKey> processedEdges = new HashSet<>();

    private static final String GENERATORS = "generators";
    private static final String MACHINES   = "machines";
    private static final String CONNECTORS = "connectors";

    private static final int POSITIVE_TERMINAL_INDEX = 0;
    private static final int NEGATIVE_TERMINAL_INDEX = 1;
    private static final int INPUT_TERMINAL_INDEX    = 0;
    private static final int OUTPUT_TERMINAL_INDEX   = 1;

    /**
     * Top‐level: build a SPICE netlist from your adjacency map.
     */
    public String buildNetlist(Map<INetworkMember,List<ConnectionInfo>> adjacencyList) {
        // 1) classify members
        Map<String,List<INetworkMember>> classified = classifyNetworkMembers(adjacencyList);
        // 2) assign SPICE node numbers
        Map<NodeKey,Integer> nodeNumbers = assignNodeNumbers(adjacencyList);
        // 3) emit sources, machine‐R, and wire‐R
        List<String> vsrcs = createVoltageSourceComponents(classified.get(GENERATORS), nodeNumbers);
        List<String> mres  = createMachineResistorComponents(classified.get(MACHINES), nodeNumbers);
        List<String> wres  = createWireResistorComponents(adjacencyList, nodeNumbers);
        // 4) combine & dedupe
        List<String> all = new ArrayList<>();
        all.addAll(vsrcs);
        all.addAll(mres);
        all.addAll(wres);
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(all));
        // 5) format final text
        return formatSpiceNetlist(unique);
    }

    /** 1) Only pure IPowerProvider→gens; pure IPowerReceiver→machines; all IWireNode→connectors. */
    private Map<String,List<INetworkMember>> classifyNetworkMembers(Map<INetworkMember,List<ConnectionInfo>> adj) {
        Map<String,List<INetworkMember>> cls = new HashMap<>();
        cls.put(GENERATORS, new ArrayList<>());
        cls.put(MACHINES,   new ArrayList<>());
        cls.put(CONNECTORS, new ArrayList<>());

        for (INetworkMember m : adj.keySet()) {
            if (m instanceof IPowerProvider && !(m instanceof IWireNode)) {
                cls.get(GENERATORS).add(m);
            } else if (m instanceof IPowerReceiver && !(m instanceof IWireNode)) {
                cls.get(MACHINES).add(m);
            } else if (m instanceof IWireNode) {
                cls.get(CONNECTORS).add(m);
            } else {
                LOGGER.warn("Unclassified member at {}", m.getPos().toShortString());
            }
        }
        LOGGER.debug("Classified: {} gens, {} machines, {} connectors",
                cls.get(GENERATORS).size(),
                cls.get(MACHINES).size(),
                cls.get(CONNECTORS).size());
        return cls;
    }

    /**
     * 2) Use union–find to:
     *    a) collapse each connector’s own terminals into one DSU set,
     *    b) union each machine’s input/output onto its two copper neighbors,
     *    c) union each generator “+” onto its copper neighbor,
     *    d) ground the first generator “–” → node 0,
     *    e) assign 1,2,3… to each remaining DSU root.
     */
    private Map<NodeKey,Integer> assignNodeNumbers(Map<INetworkMember,List<ConnectionInfo>> adj) {
        DSU dsu = new DSU();

        // a) collapse internal terminals of each connector block
        for (INetworkMember m : adj.keySet()) {
            if (m instanceof IWireNode w) {
                NodeKey base = new NodeKey(m, 0);
                for (int i = 1; i < w.getConnectionPointCount(); i++) {
                    dsu.union(base, new NodeKey(m, i));
                }
            }
        }

        // b) find which two connectors each machine sits between
        Map<INetworkMember,List<NodeKey>> machineEnds = new HashMap<>();
        for (var entry : adj.entrySet()) {
            INetworkMember m = entry.getKey();
            if (!(m instanceof IWireNode)) continue;
            for (ConnectionInfo info : entry.getValue()) {
                INetworkMember nb = info.neighbor();
                if (nb instanceof IPowerReceiver && info.sourceNodeIndex() >= 0) {
                    machineEnds
                            .computeIfAbsent(nb, k->new ArrayList<>())
                            .add(new NodeKey(m, info.sourceNodeIndex()));
                }
            }
        }
        // union each machine’s own terminals to those two nets
        for (var me : machineEnds.entrySet()) {
            INetworkMember mach = me.getKey();
            List<NodeKey> ends = me.getValue();
            if (ends.size() != 2) {
                LOGGER.warn("Machine {} has {} wire neighbors; skipping", mach.getPos().toShortString(), ends.size());
                continue;
            }
            dsu.union(new NodeKey(mach, INPUT_TERMINAL_INDEX), ends.get(0));
            dsu.union(new NodeKey(mach, OUTPUT_TERMINAL_INDEX), ends.get(1));
        }

        // c) stitch generator “+” onto its copper neighbor
        for (var entry : adj.entrySet()) {
            INetworkMember gen = entry.getKey();
            if (!(gen instanceof IPowerProvider)) continue;
            for (ConnectionInfo info : entry.getValue()) {
                if (info.polarityAtNeighborEntry() == ConnectorPolarity.POSITIVE
                        && info.neighbor() instanceof IWireNode
                        && info.neighborNodeIndex() >= 0
                ) {
                    dsu.union(
                            new NodeKey(gen, POSITIVE_TERMINAL_INDEX),
                            new NodeKey(info.neighbor(), info.neighborNodeIndex())
                    );
                }
            }
        }

        // d+e) collect all NodeKeys, ground first generator “–”→0, then number 1…N
        List<NodeKey> allKeys = new ArrayList<>();
        for (INetworkMember m : adj.keySet()) {
            if (m instanceof IPowerProvider) {
                allKeys.add(new NodeKey(m, POSITIVE_TERMINAL_INDEX));
                allKeys.add(new NodeKey(m, NEGATIVE_TERMINAL_INDEX));
            }
            if (m instanceof IPowerReceiver) {
                allKeys.add(new NodeKey(m, INPUT_TERMINAL_INDEX));
                allKeys.add(new NodeKey(m, OUTPUT_TERMINAL_INDEX));
            }
            if (m instanceof IWireNode w) {
                for (int i = 0; i < w.getConnectionPointCount(); i++) {
                    allKeys.add(new NodeKey(m, i));
                }
            }
        }

        Map<NodeKey,Integer> numbering = new HashMap<>();
        // ground the negative of the first generator we find
        for (INetworkMember m : adj.keySet()) {
            if (m instanceof IPowerProvider) {
                numbering.put(new NodeKey(m, NEGATIVE_TERMINAL_INDEX), 0);
                break;
            }
        }

        int next = 1;
        Set<NodeKey> seenRoots = new HashSet<>();
        for (NodeKey k : allKeys) {
            NodeKey r = dsu.find(k);
            if (!seenRoots.contains(r) && !numbering.containsKey(r)) {
                numbering.put(r, next++);
                seenRoots.add(r);
            }
        }
        // finally map every key to its root’s number
        for (NodeKey k : allKeys) {
            numbering.putIfAbsent(k, numbering.get(dsu.find(k)));
        }

        LOGGER.debug("Assigned SPICE nodes (0=ground): total {}", next-1);
        return numbering;
    }

    /** 3) One voltage source per generator, between + node and 0. */
    private List<String> createVoltageSourceComponents(
            List<INetworkMember> generators,
            Map<NodeKey,Integer> nums
    ) {
        List<String> lines = new ArrayList<>();
        if (generators == null) return lines;
        for (INetworkMember m : generators) {
            IPowerProvider p = (IPowerProvider)m;
            int pos = nums.get(new NodeKey(m, POSITIVE_TERMINAL_INDEX));
            int neg = nums.get(new NodeKey(m, NEGATIVE_TERMINAL_INDEX)); // should be 0
            String name = "V" + voltageSourceCount++;
            lines.add(String.format(Locale.US, "%s %d %d DC %d", name, pos, neg, p.getVoltage()));
        }
        return lines;
    }

    /** 4) One resistor per machine between its input/output nodes. */
    private List<String> createMachineResistorComponents(
            List<INetworkMember> machines,
            Map<NodeKey,Integer> nums
    ) {
        List<String> lines = new ArrayList<>();
        if (machines == null) return lines;
        for (INetworkMember m : machines) {
            IPowerReceiver r = (IPowerReceiver)m;
            NodeKey inKey  = new NodeKey(m, INPUT_TERMINAL_INDEX);
            NodeKey outKey = new NodeKey(m, OUTPUT_TERMINAL_INDEX);
            int ni = nums.get(inKey), no = nums.get(outKey);
            if (ni == no) {
                LOGGER.warn("Machine {} collapsed to one node {}; skipping", m.getPos().toShortString(), ni);
                continue;
            }
            String name = "R" + machineResistorCount++;
            lines.add(String.format(Locale.US, "%s %d %d %d", name, ni, no, r.getResistance()));
        }
        return lines;
    }

    /** 5) One wire resistor per connector‐to‐connector edge, using real ρ·L/A. */
    private List<String> createWireResistorComponents(
            Map<INetworkMember,List<ConnectionInfo>> adj,
            Map<NodeKey,Integer> nums
    ) {
        List<String> lines = new ArrayList<>();
        for (var entry : adj.entrySet()) {
            INetworkMember m1 = entry.getKey();
            if (!(m1 instanceof IWireNode w1)) continue;
            for (ConnectionInfo info : entry.getValue()) {
                INetworkMember m2 = info.neighbor();
                if (!(m2 instanceof IWireNode w2)) continue;
                int i1 = info.sourceNodeIndex(), i2 = info.neighborNodeIndex();
                if (i1 < 0 || i2 < 0) continue;

                NodeKey k1 = new NodeKey(m1, i1);
                NodeKey k2 = new NodeKey(m2, i2);
                EdgeKey edge = new EdgeKey(k1, k2);
                if (!processedEdges.add(edge)) continue;

                int n1 = nums.get(k1), n2 = nums.get(k2);
                if (n1 == n2) {
                    LOGGER.warn("Zero‐length resistor between {}↔{} (node {})", k1, k2, n1);
                    continue;
                }

                WireType wt = w1.getWireType(i1);
                double R = calculateWireResistance(w1, i1, w2, i2, wt);
                String name = "RW" + wireResistorCount++;
                lines.add(String.format(Locale.US, "%s %d %d %.7f", name, n1, n2, R));
            }
        }
        return lines;
    }

    /** Helper to compute R = ρ·L/A for a wire segment. */
    private double calculateWireResistance(
            IWireNode n1, int idx1,
            IWireNode n2, int idx2,
            WireType wireType
    ) {
        Vec3 p1 = Vec3.atCenterOf(n1.getPos()).add(n1.getConnectionPointOffset(idx1));
        Vec3 p2 = Vec3.atCenterOf(n2.getPos()).add(n2.getConnectionPointOffset(idx2));
        double length = p1.distanceTo(p2);        // in blocks → meters
        if (length < 1e-6) return 0.0;
        double rho  = wireType.getResistivity();  // Ω·m
        double area = 1e-6;                       // e.g. 1 mm²
        return rho * (length / area);
    }

    /** Pretty‐print into SPICE netlist text. */
    private String formatSpiceNetlist(List<String> comps) {
        StringBuilder sb = new StringBuilder();
        sb.append("* Generated ElectroRealism Netlist (").append(new Date()).append(")\n");
        var V = comps.stream().filter(s->s.startsWith("V")).toList();
        var R = comps.stream().filter(s->s.startsWith("R") && !s.startsWith("RW")).toList();
        var W = comps.stream().filter(s->s.startsWith("RW")).toList();
        if (!V.isEmpty()) { sb.append("\n* Voltage Sources\n");  V.forEach(l->sb.append(l).append("\n")); }
        if (!R.isEmpty()) { sb.append("\n* Machine Resistances\n"); R.forEach(l->sb.append(l).append("\n")); }
        if (!W.isEmpty()) { sb.append("\n* Wire Resistances\n");    W.forEach(l->sb.append(l).append("\n")); }
        sb.append("\n.END\n");
        return sb.toString();
    }

    // —— Union–Find for collapsing nets ——
    private static class DSU {
        private final Map<NodeKey,NodeKey> p = new HashMap<>();
        NodeKey find(NodeKey x) {
            p.putIfAbsent(x, x);
            NodeKey y = p.get(x);
            if (!y.equals(x)) {
                y = find(y);
                p.put(x, y);
            }
            return y;
        }
        void union(NodeKey a, NodeKey b) {
            NodeKey ra = find(a), rb = find(b);
            if (!ra.equals(rb)) p.put(ra, rb);
        }
    }

    /** Unique identifier for a single terminal on a member. */
    private record NodeKey(INetworkMember member, int idx) {
        @Override public boolean equals(Object o) {
            if (!(o instanceof NodeKey k)) return false;
            return idx == k.idx && member.getPos().equals(k.member.getPos());
        }
        @Override public int hashCode() {
            return Objects.hash(member.getPos(), idx);
        }
        @Override public String toString() {
            return member.getClass().getSimpleName()
                    + "@" + member.getPos().toShortString()
                    + "[" + idx + "]";
        }
    }

    /** Undirected edge for de‑duping wire resistors. */
    private record EdgeKey(NodeKey a, NodeKey b) {
        public EdgeKey {
            if (a.hashCode() > b.hashCode()) {
                var tmp = a; a = b; b = tmp;
            }
        }
        @Override public String toString() {
            return "Edge[" + a + "↔" + b + "]";
        }
    }
}
