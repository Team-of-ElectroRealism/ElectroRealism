package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.block.connector.ConnectorPolarity;
import com.teamofelectrorealism.electrorealism.block.machine.generator.AbstractGeneratorBlockEntity;
import com.teamofelectrorealism.electrorealism.rendering.HighlightCircuits;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;

public class NetworkPolarityValidator {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Validates the polarity of a network by:
     *  1. Finding the generator and its positive/negative neighbors.
     *  2. Identifying closed circuits (loops) from the generator (excluding the generator itself).
     *  3. Propagating expected polarity assignments along connections (ignoring ones with NONE)
     *     and checking for any conflicts.
     *
     * @param adjList The network’s adjacency list mapping members to their outgoing ConnectionInfo.
     * @param members The set of network members.
     * @param level   The Level (world) context.
     * @return true if the polarity is consistent across all nodes; false if a conflict is detected.
     */
    public boolean validatePolarity(Map<INetworkMember, List<ConnectionInfo>> adjList, Set<INetworkMember> members, Level level) {
        // --- Step 1: Identify the generator ---
        Optional<INetworkMember> generatorOpt = members.stream()
                .filter(m -> m instanceof AbstractGeneratorBlockEntity)
                .findFirst();
        if (generatorOpt.isEmpty()) {
            LOGGER.warn("Polarity validation failed: No generator found in network.");
            return false;
        }
        INetworkMember generator = generatorOpt.get();

        // Retrieve the generator’s connections and identify its positive and negative neighbors.
        List<ConnectionInfo> generatorConnections = adjList.getOrDefault(generator, Collections.emptyList());
        INetworkMember positiveNeighbor = null;
        INetworkMember negativeNeighbor = null;
        for (ConnectionInfo info : generatorConnections) {
            if (info.polarityAtNeighborEntry() == ConnectorPolarity.POSITIVE) {
                positiveNeighbor = info.neighbor();
            } else if (info.polarityAtNeighborEntry() == ConnectorPolarity.NEGATIVE) {
                negativeNeighbor = info.neighbor();
            }
        }
        if (positiveNeighbor == null || negativeNeighbor == null) {
            LOGGER.warn("Polarity validation failed: Generator at {} does not have both positive and negative connections.",
                    generator.getPos().toShortString());
            return false;
        }

        // --- Step 2: Identify closed circuits from the generator ---
        // Here we find all simple paths (loops) from the positive neighbor to the negative neighbor,
        // excluding the generator.
        List<List<INetworkMember>> closedCircuits = findClosedCircuits(positiveNeighbor, negativeNeighbor, adjList, generator);
        HighlightCircuits.highlightCircuits(closedCircuits);
        LOGGER.debug("Found {} closed circuit(s) from generator at {}.", closedCircuits.size(), generator.getPos().toShortString());
        for (List<INetworkMember> circuit : closedCircuits) {
            StringBuilder sb = new StringBuilder();
            sb.append("Closed circuit: ");
            for (INetworkMember member : circuit) {
                sb.append(member.getPos().toShortString()).append(" -> ");
            }
            sb.append("(back to start)");
            LOGGER.debug(sb.toString());
        }

        // --- Step 3: Propagate expected polarity assignments via BFS ---
        // We use a map to record the expected polarity for each node. Starting with the generator’s
        // positive and negative neighbors, we propagate the polarity along every defined connection.
        Map<INetworkMember, ConnectorPolarity> polarityAssignments = new HashMap<>();
        Queue<INetworkMember> queue = new LinkedList<>();

        // Initialize the polarity assignments from the generator.
        polarityAssignments.put(positiveNeighbor, ConnectorPolarity.POSITIVE);
        polarityAssignments.put(negativeNeighbor, ConnectorPolarity.NEGATIVE);
        queue.add(positiveNeighbor);
        queue.add(negativeNeighbor);

        while (!queue.isEmpty()) {
            INetworkMember current = queue.poll();
            ConnectorPolarity currentAssignment = polarityAssignments.get(current);
            List<ConnectionInfo> connections = adjList.getOrDefault(current, Collections.emptyList());
            for (ConnectionInfo info : connections) {
                INetworkMember neighbor = info.neighbor();
                // Skip the generator to avoid looping back.
                if (neighbor.equals(generator)) continue;

                ConnectorPolarity definedPolarity = info.polarityAtNeighborEntry();
                // If no polarity is defined on this connection and the block is not a machine block, skip it.
                if (definedPolarity == ConnectorPolarity.NONE && !(neighbor instanceof AbstractGeneratorBlockEntity)) {
                    continue;
                }
                // The expected polarity for the neighbor is taken from the connection.
                ConnectorPolarity expected = definedPolarity;
                LOGGER.info("Expected polarity from {} to {} is {}.",
                        current.getPos().toShortString(),
                        neighbor.getPos().toShortString(),
                        expected);
                if (polarityAssignments.containsKey(neighbor)) {
                    if (polarityAssignments.get(neighbor) != expected) {
                        LOGGER.warn("Polarity conflict at node {}: expected {} but found {} (from connection via node {}).",
                                neighbor.getPos().toShortString(),
                                expected,
                                polarityAssignments.get(neighbor),
                                current.getPos().toShortString());
                        return false;
                    }
                } else {
                    polarityAssignments.put(neighbor, expected);
                    queue.add(neighbor);
                }
            }
        }

        LOGGER.debug("Polarity assignments: {}", polarityAssignments);
        return true;
    }

    /**
     * Finds all simple paths (closed circuits) from the start node to the target node,
     * excluding the generator node.
     *
     * @param start     The starting node (typically the positive neighbor of the generator).
     * @param target    The target node (typically the negative neighbor of the generator).
     * @param adjList   The network’s adjacency list.
     * @param generator The generator node to be excluded.
     * @return A list of paths (each path is a list of INetworkMember) representing closed circuits.
     */
    private List<List<INetworkMember>> findClosedCircuits(INetworkMember start, INetworkMember target,
                                                          Map<INetworkMember, List<ConnectionInfo>> adjList, INetworkMember generator) {
        List<List<INetworkMember>> circuits = new ArrayList<>();
        LinkedList<INetworkMember> path = new LinkedList<>();
        Set<INetworkMember> visited = new HashSet<>();
        dfsFindCircuits(start, target, adjList, generator, visited, path, circuits);
        return circuits;
    }

    /**
     * Depth-first search helper to find circuits (simple paths) from the current node to the target.
     *
     * @param current   The current node in the DFS.
     * @param target    The target node.
     * @param adjList   The network’s adjacency list.
     * @param generator The generator node (excluded from paths).
     * @param visited   A set of visited nodes.
     * @param path      The current path being built.
     * @param circuits  The list of found circuits.
     */
    private void dfsFindCircuits(INetworkMember current, INetworkMember target,
                                 Map<INetworkMember, List<ConnectionInfo>> adjList, INetworkMember generator,
                                 Set<INetworkMember> visited, LinkedList<INetworkMember> path,
                                 List<List<INetworkMember>> circuits) {
        // Do not include the generator.
        if (current.equals(generator)) return;
        if (visited.contains(current)) return;

        visited.add(current);
        path.add(current);

        if (current.equals(target)) {
            // Found a simple path from start to target.
            circuits.add(new ArrayList<>(path));
        } else {
            for (ConnectionInfo info : adjList.getOrDefault(current, Collections.emptyList())) {
                INetworkMember neighbor = info.neighbor();
                if (neighbor.equals(generator)) continue;
                dfsFindCircuits(neighbor, target, adjList, generator, visited, path, circuits);
            }
        }

        path.removeLast();
        visited.remove(current);
    }
}
