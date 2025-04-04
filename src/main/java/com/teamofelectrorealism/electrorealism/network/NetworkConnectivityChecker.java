package com.teamofelectrorealism.electrorealism.network;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.block.machine.generator.AbstractGeneratorBlockEntity;
import org.slf4j.Logger;

import java.util.*;

/**
 * Checks if all members within a provided network member set form a single
 * connected graph containing at least one generator. Ignores polarity.
 */
public class NetworkConnectivityChecker {
    private static final Logger LOGGER = LogUtils.getLogger();
    /**
     * Checks the connectivity of the network represented by the members and adjacency list.
     * @param members The full set of members expected to be in the network.
     * @param adjacencyList The pre-built adjacency list for the network.
     * @return true if all members form a single connected component with at least one generator, false otherwise.
     */
    public boolean checkConnectivity(Set<INetworkMember> members, Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        if (members == null || members.isEmpty()) {
            LOGGER.trace("Connectivity check: Empty member set is considered connected.");
            return true; // An empty network is arguably "connected"
        }
        if (adjacencyList == null) {
            LOGGER.error("Connectivity check: Adjacency list is null.");
            return false;
        }

        // Find a starting point (ideally a generator, but any member works for pure connectivity)
        INetworkMember startNode = members.stream().findAny().orElse(null);
        if (startNode == null) {
            LOGGER.error("Connectivity check: Member set not empty but failed to get a start node.");
            return false; // Should not happen if members is not empty
        }

        // Check for presence of at least one generator
        boolean hasGenerator = members.stream().anyMatch(m -> m instanceof AbstractGeneratorBlockEntity);
        if (!hasGenerator) {
            LOGGER.warn("Connectivity check: Network has members but no generator found.");
            // Decide if this is an error based on your rules. Assuming requires generator for now.
            return false;
        }

        // Perform BFS or DFS to find all reachable members from the start node
        Set<INetworkMember> visited = new HashSet<>();
        Queue<INetworkMember> queue = new LinkedList<>();

        queue.add(startNode);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            INetworkMember current = queue.poll();

            List<ConnectionInfo> neighborsInfo = adjacencyList.getOrDefault(current, Collections.emptyList());
            for (ConnectionInfo info : neighborsInfo) {
                INetworkMember neighbor = info.neighbor();
                // Check if neighbor is in the original set and not yet visited
                if (members.contains(neighbor) && visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        // Check if all members in the original set were visited
        boolean allVisited = visited.size() == members.size();

        if (!allVisited) {
            LOGGER.warn("Connectivity check: Failed. Visited {} members, but expected {}. Network is potentially split.", visited.size(), members.size());
            // Log missing members for debugging:
            Set<INetworkMember> missing = new HashSet<>(members);
            missing.removeAll(visited);
            missing.forEach(m -> LOGGER.warn("  - Missing member: {} at {}", m.getClass().getSimpleName(), m.getPos().toShortString()));
        } else {
            LOGGER.debug("Connectivity check: Passed. All {} members are connected.", members.size());
        }

        return allVisited;
    }
}
