package com.teamofelectrorealism.electrorealism.network;

import com.ibm.icu.util.CodePointTrie;
import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorPolarity;
import com.teamofelectrorealism.electrorealism.block.connector.duo.DuoConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.duo.DuoConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.large.LargeConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.large.LargeConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.small.SmallConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.small.SmallConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.machine.generator.AbstractGeneratorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.machine.user.AbstractPowerUserBlockEntity;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

/**
 * Validates the structure and polarity connections of an electrical network.
 * Ensures paths exist from Generator(+) to Generator(-) following polarity rules.
 * Based on graph traversal. Does not perform numerical circuit simulation.
 */
public class NetworkValidator {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Special indices to represent machine-facing connections in ConnectionInfo
    private static final int MACHINE_FACE_INDEX_SINGLE = -1; // For Small/Large connectors
    private static final int MACHINE_FACE_INDEX_DUO_0 = -2; // For Duo connector terminal 0 facing machine
    private static final int MACHINE_FACE_INDEX_DUO_1 = -3; // For Duo connector terminal 1 facing machine


    /**
     * Inner class to hold connection details for graph building.
     */
    private static class ConnectionInfo {
        @NotNull INetworkMember neighbor;
        int sourceNodeIndex;     // Index on the *source* node where connection originates (-1, -2, -3 for machine faces)
        int neighborNodeIndex;   // Index on the *neighbor* node where connection terminates (-1, -2, -3 for machine faces)
        ConnectorPolarity polarityAtNeighborEntry; // Polarity of the *neighbor's* specific connection point/face being entered

        ConnectionInfo(@NotNull INetworkMember neighbor, int sourceNodeIndex, int neighborNodeIndex, ConnectorPolarity polarityAtNeighborEntry) {
            this.neighbor = neighbor;
            this.sourceNodeIndex = sourceNodeIndex;
            this.neighborNodeIndex = neighborNodeIndex;
            this.polarityAtNeighborEntry = polarityAtNeighborEntry;
        }

        @Override
        public String toString() {
            return " -> " + neighbor.getPos() + " [SrcIdx:" + sourceNodeIndex + ", NeighborIdx:" + neighborNodeIndex + ", PolarityAtEntry:" + polarityAtNeighborEntry + "]";
        }
    }

    /**
     * Validates the given network structure and polarity.
     *
     * @param network The network to validate.
     * @param level   The level access, needed to check block states for polarity.
     * @return true if the network structure is considered valid, false otherwise.
     */
    public boolean validate(Network network, Level level) {
        if (network == null || !network.isValid()) {
            LOGGER.debug("Network is null or marked invalid. Validation skipped (returns true).");
            return true; // Or false if invalid networks should fail validation
        }

        Set<INetworkMember> members = network.getNetworkMembers();
        LOGGER.debug("Network {} members retrieved for validation:", network.getNetworkId());
        for (INetworkMember member : members) {
            LOGGER.debug("  - Member at {}: Type {}", member.getPos(), member.getClass().getName());
        }

        if (members.isEmpty()) {
            LOGGER.debug("Network {} is empty. Validation skipped (returns true).", network.getNetworkId());
            return true; // Empty network is valid
        }

        List<AbstractGeneratorBlockEntity> generators = findGenerators(members);
        if (generators.isEmpty()) {
            LOGGER.warn("Validation Fail (Network {}): No generators found.", network.getNetworkId());
            return false; // Must have at least one generator
        }

        LOGGER.debug("Validating Network {}: Found {} members, {} generators.", network.getNetworkId(), members.size(), generators.size());

        Map<INetworkMember, List<ConnectionInfo>> adjacencyList = buildAdjacencyList(members, level);
        // printAdjacencyList(adjacencyList); // Optional: for debugging

        for (AbstractGeneratorBlockEntity generator : generators) {
            boolean foundValidPathForGenerator = false;
            List<ConnectionInfo> generatorConnections = adjacencyList.getOrDefault(generator, Collections.emptyList());
            LOGGER.trace("Checking paths for generator at {}. Found {} connections.", generator.getPos(), generatorConnections.size());

            for (ConnectionInfo startConnection : generatorConnections) {
                // We start from a connector attached to the generator.
                if (startConnection.neighbor instanceof AbstractConnectorBlockEntity startConnector) {
                    // polarityAtNeighborEntry in this context is the polarity of the connector terminal facing the generator.
                    if (startConnection.polarityAtNeighborEntry == ConnectorPolarity.POSITIVE) {
                        LOGGER.trace("  Starting trace from POSITIVE connection via connector at {}", startConnector.getPos());
                        Set<INetworkMember> visitedNodesInPath = new HashSet<>();
                        // Start trace at the connector, entering from the machine face (use specific index)
                        if (tracePath(startConnector, startConnection.neighborNodeIndex, adjacencyList, visitedNodesInPath, ConnectorPolarity.POSITIVE, level)) {
                            LOGGER.debug("  Valid path found for generator {} starting from connector {}.", generator.getPos(), startConnector.getPos());
                            foundValidPathForGenerator = true;
                            break; // Found a valid path for this generator
                        } else {
                            LOGGER.trace("  Trace started from connector {} (POSITIVE) did not find a valid path.", startConnector.getPos());
                        }
                    }
                }
            }

            if (!foundValidPathForGenerator) {
                LOGGER.warn("Validation Fail (Network {}): Generator at {} has no valid positive-to-negative path.", network.getNetworkId(), generator.getPos());
                return false; // This generator is not part of a valid circuit
            }
        }

        LOGGER.info("Validation Success: Network {} passed validation.", network.getNetworkId());
        return true; // All generators had at least one valid path
    }

    /**
     * Builds an adjacency list representing the network connections.
     *
     * @param members The set of members in the network.
     * @param level   The level access.
     * @return A map where keys are members and values are lists of their connections.
     */
    private Map<INetworkMember, List<ConnectionInfo>> buildAdjacencyList(Set<INetworkMember> members, Level level) {
        Map<INetworkMember, List<ConnectionInfo>> adj = new HashMap<>();

        for (INetworkMember member : members) {
            adj.putIfAbsent(member, new ArrayList<>()); // Ensure all members have an entry

            // 1. Handle Wire Connections (IWireNode to IWireNode)
            if (member instanceof IWireNode wireNode) {
                for (int i = 0; i < wireNode.getConnectionPointCount(); i++) {
                    ConnectionPoint cp = wireNode.getConnectionPoint(i);
                    if (cp != null) {
                        IWireNode neighborNode = wireNode.getWireNode(i); // Method to get neighbor BE
                        if (neighborNode instanceof INetworkMember neighborMember && members.contains(neighborMember)) {
                            ConnectorPolarity neighborPolarityAtEntry = getConnectorPolarityAtIndex((AbstractConnectorBlockEntity) neighborNode, cp.getConnectingPointIndex(), level); // Assumes wires only connect connectors for now

                            adj.computeIfAbsent(member, k -> new ArrayList<>())
                                    .add(new ConnectionInfo(neighborMember, i, cp.getConnectingPointIndex(), neighborPolarityAtEntry));
                        }
                    }
                }
            }

            // 2. Handle Implicit Machine-Connector Connections
            if (member instanceof AbstractConnectorBlockEntity connector) {
                INetworkMember attachedMachine = connector.findNetworkMember(); // Method to find adjacent machine BE
                if (attachedMachine != null && members.contains(attachedMachine)) {
                    BlockState connectorState = level.getBlockState(connector.getPos());

                    // Determine connection from Connector -> Machine
                    if (connector instanceof DuoConnectorBlockEntity duo) {
                        // Duo: Add two links TO machine, one for each terminal
                        ConnectorPolarity p0 = connectorState.getValue(DuoConnectorBlock.TERMINAL_TYPE_0);
                        ConnectorPolarity p1 = connectorState.getValue(DuoConnectorBlock.TERMINAL_TYPE_1);
                        adj.computeIfAbsent(member, k -> new ArrayList<>()).add(new ConnectionInfo(attachedMachine, MACHINE_FACE_INDEX_DUO_0, MACHINE_FACE_INDEX_SINGLE, ConnectorPolarity.NONE)); // Machine side polarity is NONE
                        adj.computeIfAbsent(member, k -> new ArrayList<>()).add(new ConnectionInfo(attachedMachine, MACHINE_FACE_INDEX_DUO_1, MACHINE_FACE_INDEX_SINGLE, ConnectorPolarity.NONE));
                    } else {
                        // Small/Large: Add one link TO machine
                        ConnectorPolarity connectorPolarity = getConnectorPolarityAtIndex(connector, MACHINE_FACE_INDEX_SINGLE, level); // Get polarity facing machine
                        adj.computeIfAbsent(member, k -> new ArrayList<>()).add(new ConnectionInfo(attachedMachine, MACHINE_FACE_INDEX_SINGLE, MACHINE_FACE_INDEX_SINGLE, ConnectorPolarity.NONE)); // Machine side polarity is NONE
                    }

                    // Determine connection from Machine -> Connector (Reverse)
                    if (connector instanceof DuoConnectorBlockEntity duo) {
                        // Duo: Add two links FROM machine, one for each terminal entry point on connector
                        ConnectorPolarity p0 = connectorState.getValue(DuoConnectorBlock.TERMINAL_TYPE_0);
                        ConnectorPolarity p1 = connectorState.getValue(DuoConnectorBlock.TERMINAL_TYPE_1);
                        adj.computeIfAbsent(attachedMachine, k -> new ArrayList<>()).add(new ConnectionInfo(member, MACHINE_FACE_INDEX_SINGLE, MACHINE_FACE_INDEX_DUO_0, p0)); // Polarity entering connector is p0
                        adj.computeIfAbsent(attachedMachine, k -> new ArrayList<>()).add(new ConnectionInfo(member, MACHINE_FACE_INDEX_SINGLE, MACHINE_FACE_INDEX_DUO_1, p1)); // Polarity entering connector is p1
                    } else {
                        // Small/Large: Add one link FROM machine
                        ConnectorPolarity connectorPolarity = getConnectorPolarityAtIndex(connector, MACHINE_FACE_INDEX_SINGLE, level); // Polarity entering connector
                        adj.computeIfAbsent(attachedMachine, k -> new ArrayList<>()).add(new ConnectionInfo(member, MACHINE_FACE_INDEX_SINGLE, MACHINE_FACE_INDEX_SINGLE, connectorPolarity));
                    }
                }
            }
        }
        return adj;
    }


    /**
     * Recursively traces a path through the network, validating polarity rules.
     *
     * @param currentNode        The current network member being visited.
     * @param entryIndex         The connection index used to enter this node (-1, -2, -3 for machine faces).
     * @param adj                The network's adjacency list.
     * @param visitedNodesInPath A set tracking nodes visited in the *current* path to detect cycles.
     * @param expectedPolarity   The polarity expected upon entering the currentNode.
     * @param level              The world level.
     * @return true if a valid path to a generator's negative terminal is found from this point, false otherwise.
     */
    private boolean tracePath(INetworkMember currentNode,
                              int entryIndex,
                              Map<INetworkMember, List<ConnectionInfo>> adj,
                              Set<INetworkMember> visitedNodesInPath,
                              ConnectorPolarity expectedPolarity,
                              Level level)
    {
        // --- Cycle Detection ---
        if (!visitedNodesInPath.add(currentNode)) {
            // LOGGER.trace("Cycle detected at {} during trace.", currentNode.getPos());
            return false; // Found a loop in this path without reaching Gen(-)
        }
        //LOGGER.trace("TraceStep: Entering node {} ({}), expecting polarity {}", currentNode.getPos(), currentNode.getClass().getSimpleName(), expectedPolarity);


        // --- Polarity Check & Determine Exit Polarity ---
        ConnectorPolarity actualEntryPolarity = expectedPolarity; // Assume matches if not a connector
        ConnectorPolarity exitPolarity = expectedPolarity; // Default pass-through behavior
        boolean isPassThrough = false;

        if (currentNode instanceof AbstractConnectorBlockEntity connector) {
            actualEntryPolarity = getConnectorPolarityAtIndex(connector, entryIndex, level);
            isPassThrough = (actualEntryPolarity == ConnectorPolarity.NONE);

            if (!isPassThrough && actualEntryPolarity != expectedPolarity) {
                LOGGER.trace("  Validation Fail: Polarity mismatch at connector {}. Expected {}, Got {}.", connector.getPos(), expectedPolarity, actualEntryPolarity);
                visitedNodesInPath.remove(currentNode);
                return false;
            }

            // Determine exit polarity based on the connector's actual state (if not pass-through)
            if (!isPassThrough) {
                exitPolarity = actualEntryPolarity;
            }
            // If isPassThrough, exitPolarity remains the same as expectedPolarity (passed through)

        } else if (currentNode instanceof AbstractPowerUserBlockEntity) {
            // User machines consume power based on polarity flow.
            // Standard flow: POSITIVE in means NEGATIVE out (towards ground/Gen-).
            // NEGATIVE in means NEGATIVE out (continuing towards ground/Gen-).
            if (expectedPolarity == ConnectorPolarity.POSITIVE) {
                exitPolarity = ConnectorPolarity.NEGATIVE;
            } else if (expectedPolarity == ConnectorPolarity.NEGATIVE) {
                exitPolarity = ConnectorPolarity.NEGATIVE;
            } else { // expectedPolarity == NONE
                // A User shouldn't operate correctly if entered via a NONE path?
                LOGGER.trace("  Validation Fail: Path reached User {} with NONE polarity expectation.", currentNode.getPos());
                visitedNodesInPath.remove(currentNode);
                return false; // Or handle differently based on rules. Assume Users need defined polarity.
            }

        } else if (currentNode instanceof AbstractGeneratorBlockEntity) {
            // Base Case: Reached a Generator. Check if it's a valid termination.
            if (expectedPolarity == ConnectorPolarity.NEGATIVE) {
                LOGGER.trace("  Validation Success: Path terminated correctly at Generator {}.", currentNode.getPos());
                return true; // Valid termination
            } else {
                LOGGER.trace("  Validation Fail: Path terminated at Generator {} but expected polarity was {}.", currentNode.getPos(), expectedPolarity);
                visitedNodesInPath.remove(currentNode);
                return false; // Invalid termination (e.g., looped back to positive)
            }
        }

        //LOGGER.trace("  Node {} entered with polarity {}. Exit polarity determined as {}.", currentNode.getPos(), actualEntryPolarity, exitPolarity);


        // --- Explore Neighbors ---
        List<ConnectionInfo> neighbors = adj.getOrDefault(currentNode, Collections.emptyList());
        for (ConnectionInfo nextConnection : neighbors) {

            // Simple check to avoid immediately returning the way we came.
            // A more robust check might involve comparing entryIndex with nextConnection.neighborNodeIndex if applicable.
            // if (nextConnection.neighbor.equals(previousNode)) continue; // Needs 'previousNode' passed in or edge tracking

            // Determine the polarity the neighbor expects based on our exitPolarity.
            // Usually the same, unless specific rules apply (like User(-) -> User(+), which is not used here)
            ConnectorPolarity nextExpectedPolarity = exitPolarity;

            //LOGGER.trace("    Exploring neighbor {} via exit index {}, next expects {}", nextConnection.neighbor.getPos(), nextConnection.sourceNodeIndex, nextExpectedPolarity);

            // Recursive call. Pass a *copy* of visited set if parallel paths are possible,
            // or pass the same set for pure DFS. Using a copy prevents visited status
            // from one branch affecting another independent branch from the same node.
            if (tracePath(nextConnection.neighbor, nextConnection.neighborNodeIndex, adj, new HashSet<>(visitedNodesInPath), nextExpectedPolarity, level)) {
                return true; // Valid path found down this branch
            }
            // If recursive call returns false, continue checking other neighbors.
        }

        // --- Backtrack ---
        // If we explored all neighbors from this node and none led to a valid termination
        //LOGGER.trace("  Backtracking from node {}: No valid continuing paths found.", currentNode.getPos());
        visitedNodesInPath.remove(currentNode); // Crucial for allowing this node to be visited via different paths
        return false;
    }

    // --- Helper Methods ---

    private List<AbstractGeneratorBlockEntity> findGenerators(Set<INetworkMember> members) {
        List<AbstractGeneratorBlockEntity> list = new ArrayList<>();
        for (INetworkMember member : members) {
            if (member instanceof AbstractGeneratorBlockEntity gen) {
                list.add(gen);
            }
        }
        return list;
    }

    /**
     * Gets the effective polarity of a connector at a specific index, considering its type and state.
     * Handles wire indices (0+) and special machine face indices (-1, -2, -3).
     */
    private ConnectorPolarity getConnectorPolarityAtIndex(AbstractConnectorBlockEntity connector, int index, Level level) {
        BlockState state = level.getBlockState(connector.getPos());
        if (!state.hasProperty(AbstractConnectorBlock.FACING)) {
            return ConnectorPolarity.NONE; // Should not happen
        }

        if (connector instanceof DuoConnectorBlockEntity) {
            // Duo uses specific indices for wires (0, 1) and machine faces (-2, -3)
            if (index == 0 || index == MACHINE_FACE_INDEX_DUO_0) {
                return state.getValue(DuoConnectorBlock.TERMINAL_TYPE_0);
            } else if (index == 1 || index == MACHINE_FACE_INDEX_DUO_1) {
                return state.getValue(DuoConnectorBlock.TERMINAL_TYPE_1);
            }
        } else if (connector instanceof SmallConnectorBlockEntity || connector instanceof LargeConnectorBlockEntity) {
            if(state.hasProperty(LargeConnectorBlock.TERMINAL_TYPE)){
                return state.getValue(LargeConnectorBlock.TERMINAL_TYPE);
            }
            if (state.hasProperty(SmallConnectorBlock.TERMINAL_TYPE)){
                return state.getValue(SmallConnectorBlock.TERMINAL_TYPE);
            }
        }

        LOGGER.warn("Could not determine polarity for connector {} at index {}", connector.getPos(), index);
        return ConnectorPolarity.NONE; // Fallback
    }

    // Optional: Helper to print the adjacency list for debugging
    private void printAdjacencyList(Map<INetworkMember, List<ConnectionInfo>> adj) {
        LOGGER.debug("--- Adjacency List ---");
        for (Map.Entry<INetworkMember, List<ConnectionInfo>> entry : adj.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(entry.getKey().getPos()).append(" (").append(entry.getKey().getClass().getSimpleName()).append("):");
            for (ConnectionInfo info : entry.getValue()) {
                sb.append("\n    ").append(info.toString());
            }
            LOGGER.debug(sb.toString());
        }
        LOGGER.debug("----------------------");
    }
}
