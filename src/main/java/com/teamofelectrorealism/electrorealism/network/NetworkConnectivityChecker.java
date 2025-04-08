package com.teamofelectrorealism.electrorealism.network;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorPolarity;
import com.teamofelectrorealism.electrorealism.block.machine.generator.AbstractGeneratorBlockEntity;
import org.slf4j.Logger;

import java.util.*;

/**
 * Utility class to verify the connectivity and closed circuit properties of an electrical network.
 * <p>
 * A network is considered a valid closed circuit if:
 * <ul>
 *     <li>All network members are interconnected, forming a single connected component.</li>
 *     <li>There exists at least one generator whose positive and negative connections are linked by a path that
 *         excludes the generator itself.</li>
 * </ul>
 * <p>
 * Note: The connectivity check is based solely on graph reachability. The closed loop check (from the generator’s
 * positive node to its negative node) ensures that at least one pathway exists for power to flow.
 */
public class NetworkConnectivityChecker {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Checks if all provided network members form a single connected component.
     *
     * @param members       the complete set of network members that are expected to form the circuit.
     * @param adjacencyList a map representing the network's connectivity, where keys are network members and values are
     *                      lists of connection information representing the neighbors of each member.
     * @return {@code true} if all network members are connected; {@code false} otherwise.
     */
    public boolean checkConnectivity(Set<INetworkMember> members, Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        if (members == null || members.isEmpty()) {
            LOGGER.trace("Connectivity check: Empty member set is not a valid closed circuit.");
            return false;
        }
        if (adjacencyList == null) {
            LOGGER.error("Connectivity check: Adjacency list is null.");
            return false;
        }

        // Pick an arbitrary starting node from the set.
        INetworkMember startNode = members.stream().findAny().orElse(null);
        if (startNode == null) {
            LOGGER.error("Connectivity check: Member set is not empty but failed to retrieve a starting node.");
            return false;
        }

        // Perform DFS to visit all reachable nodes.
        Set<INetworkMember> visited = new HashSet<>();
        Stack<INetworkMember> stack = new Stack<>();
        stack.push(startNode);
        visited.add(startNode);

        while (!stack.isEmpty()) {
            INetworkMember current = stack.pop();
            List<ConnectionInfo> neighborsInfo = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (ConnectionInfo info : neighborsInfo) {
                INetworkMember neighbor = info.neighbor();
                if (members.contains(neighbor) && visited.add(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }

        boolean allVisited = visited.size() == members.size();
        if (!allVisited) {
            LOGGER.warn("Connectivity check: Incomplete connectivity. Visited {} members out of {}.", visited.size(), members.size());
            // Log missing members for debugging.
            Set<INetworkMember> missing = new HashSet<>(members);
            missing.removeAll(visited);
            missing.forEach(m -> LOGGER.warn("  - Missing member: {} at {}", m.getClass().getSimpleName(), m.getPos().toShortString()));
        } else {
            LOGGER.debug("Connectivity check: Success. All {} members are connected.", members.size());
        }
        return allVisited;
    }

    /**
     * Checks if there exists a closed loop starting from a generator in the network.
     * <p>
     * This method performs two validations:
     * <ol>
     *     <li>The network must be fully connected (all members are reachable).</li>
     *     <li>The selected generator must have both a positive and a negative connection, and these must be
     *         connected via a path that does not traverse the generator itself.</li>
     * </ol>
     *
     * @param members       the complete set of network members that form the circuit.
     * @param adjacencyList a map representing the network's connectivity.
     * @return {@code true} if a closed loop exists (i.e. a valid path exists between the generator’s positive and
     *         negative connections, excluding the generator); {@code false} otherwise.
     */
    public boolean isClosedCircuit(Set<INetworkMember> members, Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        // First, ensure the network is fully connected.
        if (!checkConnectivity(members, adjacencyList)) {
            LOGGER.warn("Closed circuit check failed: Network is not fully connected.");
            return false;
        }

        // Find a generator in the network.
        Optional<INetworkMember> generatorOpt = members.stream().filter(m -> m instanceof AbstractGeneratorBlockEntity).findFirst();
        if (generatorOpt.isEmpty()) {
            LOGGER.warn("Closed circuit check failed: No generator found in the network.");
            return false;
        }
        INetworkMember generator = generatorOpt.get();

        // Retrieve the generator's connections.
        List<ConnectionInfo> generatorConnections = adjacencyList.getOrDefault(generator, Collections.emptyList());
        INetworkMember positiveNeighbor = null;
        INetworkMember negativeNeighbor = null;

        // Identify the positive and negative connections.
        for (ConnectionInfo info : generatorConnections) {
            if (info.polarityAtNeighborEntry() == ConnectorPolarity.POSITIVE) {
                positiveNeighbor = info.neighbor();
            } else if (info.polarityAtNeighborEntry() == ConnectorPolarity.NEGATIVE) {
                negativeNeighbor = info.neighbor();
            }
        }

        if (positiveNeighbor == null || negativeNeighbor == null) {
            LOGGER.warn("Closed circuit check failed: Generator at {} does not have both positive and negative connections.",
                    generator.getPos().toShortString());
            return false;
        }

        // Check if there's a path between the positive and negative neighbors while excluding the generator.
        boolean loopExists = isReachableExcludingNode(positiveNeighbor, negativeNeighbor, generator, members, adjacencyList);
        if (!loopExists) {
            LOGGER.warn("Closed circuit check failed: No valid path between positive and negative connections of generator at {}.",
                    generator.getPos().toShortString());
        } else {
            LOGGER.debug("Closed circuit check succeeded: Valid closed loop exists for generator at {}.",
                    generator.getPos().toShortString());
        }
        return loopExists;
    }

    /**
     * Helper method to determine if there is a path between two nodes in the network while excluding a specific node.
     *
     * @param start         the starting node for the search.
     * @param target        the target node to reach.
     * @param excluded      the node to exclude from the search (e.g., the generator).
     * @param members       the complete set of network members.
     * @param adjacencyList the map representing the network's connectivity.
     * @return {@code true} if a path exists from {@code start} to {@code target} without traversing the {@code excluded} node;
     *         {@code false} otherwise.
     */
    private boolean isReachableExcludingNode(INetworkMember start, INetworkMember target, INetworkMember excluded,
                                             Set<INetworkMember> members, Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        Set<INetworkMember> visited = new HashSet<>();
        Stack<INetworkMember> stack = new Stack<>();

        // Start from the positive neighbor.
        stack.push(start);
        visited.add(start);

        while (!stack.isEmpty()) {
            INetworkMember current = stack.pop();
            if (current.equals(target)) {
                return true;
            }
            List<ConnectionInfo> neighborsInfo = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (ConnectionInfo info : neighborsInfo) {
                INetworkMember neighbor = info.neighbor();
                // Skip the excluded node and nodes already visited.
                if (neighbor.equals(excluded) || visited.contains(neighbor)) {
                    continue;
                }
                // Ensure the neighbor is part of the overall network.
                if (members.contains(neighbor)) {
                    visited.add(neighbor);
                    stack.push(neighbor);
                }
            }
        }
        return false;
    }
}
