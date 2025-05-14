package com.teamofpowersim.powersim.network;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.block.connector.ConnectorPolarity;
import com.teamofpowersim.powersim.block.machine.provider.AbstractPowerProviderBlockEntity;
import com.teamofpowersim.powersim.rendering.HighlightCircuits;
import org.slf4j.Logger;

import java.util.*;

/**
 * Utility class to verify the connectivity and closed circuit properties of an electrical network.
 * <p>
 * The closed circuit logic is based on:
 * <ul>
 *     <li>Validating that the network is fully connected.</li>
 *     <li>Finding the generator and determining its positive and negative neighbors.</li>
 *     <li>Using a DFS-based search (as in the polarity validator) to identify all closed circuits between
 *         the positive and negative neighbors (excluding the generator itself).</li>
 * </ul>
 */
public class NetworkCircuitChecker {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Checks if the network forms a closed circuit.
     * <p>
     * This method first checks that the network is fully connected, then locates a generator and validates
     * that a path exists between the generator’s positive and negative neighbors.
     *
     * @param members       the set of network members.
     * @param adjacencyList the connectivity map of the network.
     * @return {@code true} if the network has at least one closed circuit; {@code false} otherwise.
     */
    public boolean isClosedCircuit(Set<INetworkMember> members, Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        // Validate connectivity first.
        if (!checkConnectivity(members, adjacencyList)) {
            LOGGER.warn("Closed circuit check failed: Network is not fully connected.");
            return false;
        }

        // Identify a generator.
        Optional<INetworkMember> generatorOpt = members.stream()
                .filter(m -> m instanceof AbstractPowerProviderBlockEntity)
                .findFirst();
        if (generatorOpt.isEmpty()) {
            LOGGER.warn("Closed circuit check failed: No generator found in the network.");
            return false;
        }
        INetworkMember generator = generatorOpt.get();

        // Retrieve the generator's connections and get positive/negative neighbors.
        List<ConnectionInfo> generatorConnections = adjacencyList.getOrDefault(generator, Collections.emptyList());
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
            LOGGER.warn("Closed circuit check failed: Generator at {} does not have both positive and negative connections.",
                    generator.getPos().toShortString());
            return false;
        }

        // Use DFS (excluding the generator) to verify that a path exists.
        boolean loopExists = isReachableExcludingNode(positiveNeighbor, negativeNeighbor, generator, members, adjacencyList);
        if (!loopExists) {
            LOGGER.warn("Closed circuit check failed: No valid path between positive and negative connections of generator at {}.",
                    generator.getPos().toShortString());
        }
        return loopExists;
    }

    /**
     * Retrieves the count of closed circuits in the network.
     * <p>
     * This method uses the same logic as your polarity validator. It locates a generator,
     * determines its positive and negative neighbors, then performs a DFS-based search to
     * identify and count all simple paths (closed circuits) from the positive neighbor to the negative neighbor.
     *
     * @param members       the set of network members.
     * @param adjacencyList the connectivity map.
     * @return the number of closed circuits found.
     */
    public int getClosedCircuitCount(Set<INetworkMember> members, Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        // Identify a generator.
        Optional<INetworkMember> generatorOpt = members.stream()
                .filter(m -> m instanceof AbstractPowerProviderBlockEntity)
                .findFirst();
        if (generatorOpt.isEmpty()) {
            LOGGER.warn("Cannot count closed circuits: No generator found.");
            return 0;
        }
        INetworkMember generator = generatorOpt.get();

        // Retrieve generator's connections and determine positive and negative neighbors.
        List<ConnectionInfo> generatorConnections = adjacencyList.getOrDefault(generator, Collections.emptyList());
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
            LOGGER.warn("Cannot count closed circuits: Generator at {} does not have both positive and negative connections.",
                    generator.getPos().toShortString());
            return 0;
        }

        // Use DFS to get all closed circuits from positiveNeighbor to negativeNeighbor (excluding the generator).
        List<List<INetworkMember>> closedCircuits = findClosedCircuits(positiveNeighbor, negativeNeighbor, adjacencyList, generator);
        HighlightCircuits.highlightCircuits(closedCircuits);
        LOGGER.debug("Found {} closed circuit(s) from generator at {}.", closedCircuits.size(), generator.getPos().toShortString());
        return closedCircuits.size();
    }

    // --- Helper Methods (Connectivity Check and DFS-based Closed Circuit Search) ---

    private boolean checkConnectivity(Set<INetworkMember> members, Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        if (members == null || members.isEmpty()) {
            LOGGER.trace("Connectivity check: Empty member set is not a valid closed circuit.");
            return false;
        }
        if (adjacencyList == null) {
            LOGGER.error("Connectivity check: Adjacency list is null.");
            return false;
        }

        INetworkMember startNode = members.stream().findAny().orElse(null);
        if (startNode == null) {
            LOGGER.error("Connectivity check: Member set is not empty but failed to retrieve a starting node.");
            return false;
        }

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
        } else {
            LOGGER.debug("Connectivity check: Success. All {} members are connected.", members.size());
        }
        return allVisited;
    }

    private List<List<INetworkMember>> findClosedCircuits(INetworkMember start, INetworkMember target,
                                                          Map<INetworkMember, List<ConnectionInfo>> adjList, INetworkMember generator) {
        List<List<INetworkMember>> circuits = new ArrayList<>();
        LinkedList<INetworkMember> path = new LinkedList<>();
        Set<INetworkMember> visited = new HashSet<>();
        dfsFindCircuits(start, target, adjList, generator, visited, path, circuits);
        return circuits;
    }

    private void dfsFindCircuits(INetworkMember current, INetworkMember target,
                                 Map<INetworkMember, List<ConnectionInfo>> adjList, INetworkMember generator,
                                 Set<INetworkMember> visited, LinkedList<INetworkMember> path,
                                 List<List<INetworkMember>> circuits) {
        // Exclude the generator from the DFS.
        if (current.equals(generator)) return;
        if (visited.contains(current)) return;

        visited.add(current);
        path.add(current);

        if (current.equals(target)) {
            // A valid closed circuit (simple path) has been found.
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

    private boolean isReachableExcludingNode(INetworkMember start, INetworkMember target, INetworkMember excluded,
                                             Set<INetworkMember> members, Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        Set<INetworkMember> visited = new HashSet<>();
        Stack<INetworkMember> stack = new Stack<>();

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
                if (neighbor.equals(excluded) || visited.contains(neighbor)) {
                    continue;
                }
                if (members.contains(neighbor)) {
                    visited.add(neighbor);
                    stack.push(neighbor);
                }
            }
        }
        return false;
    }
}
