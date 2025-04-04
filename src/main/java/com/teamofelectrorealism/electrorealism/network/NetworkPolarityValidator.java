package com.teamofelectrorealism.electrorealism.network;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.connector.ConnectorPolarity;
import com.teamofelectrorealism.electrorealism.block.connector.duo.DuoConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.large.LargeConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.small.SmallConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.machine.generator.AbstractGeneratorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.machine.user.AbstractPowerUserBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates the polarity rules within a structurally connected network graph.
 */
public class NetworkPolarityValidator {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Constants for machine face indices
    private static final int MACHINE_FACE_INDEX_SINGLE = -1;
    private static final int MACHINE_FACE_INDEX_DUO_0 = -2;
    private static final int MACHINE_FACE_INDEX_DUO_1 = -3;

    /**
     * Validates the polarity flow in the network described by the adjacency list.
     *
     * @param adjacencyList The pre-built adjacency list representing the network connections.
     * @param members The full set of members in the network (used to find generators).
     * @param level   The world level, needed to check connector block states for polarity.
     * @return true if polarity rules are satisfied for all generators, false otherwise.
     */
    public boolean validatePolarity(Map<INetworkMember, List<ConnectionInfo>> adjacencyList, Set<INetworkMember> members, Level level) {
        if (adjacencyList == null || members == null || level == null) {
            LOGGER.error("validatePolarity called with null arguments.");
            return false;
        }
        if (members.isEmpty()) {
            LOGGER.trace("Polarity validation: Empty network is valid.");
            return true;
        }

        List<AbstractGeneratorBlockEntity> generators = findGenerators(members);
        if (generators.isEmpty()) {
            LOGGER.warn("Polarity validation: No generators found in the member set.");
            // Decide if this is valid. Usually requires a generator.
            return false;
        }

        LOGGER.debug("Validating polarity for network with {} members, {} generators.", members.size(), generators.size());

        for (AbstractGeneratorBlockEntity generator : generators) {
            boolean foundValidPathForGenerator = false;
            List<ConnectionInfo> generatorConnections = adjacencyList.getOrDefault(generator, Collections.emptyList());

            LOGGER.trace("Checking paths for generator at {}. Found {} connections.", generator.getPos().toShortString(), generatorConnections.size());

            for (ConnectionInfo startConnection : generatorConnections) {
                // Start trace from connectors attached to the generator that have POSITIVE polarity where they attach
                if (startConnection.neighbor() instanceof AbstractConnectorBlockEntity startConnector &&
                        startConnection.polarityAtNeighborEntry() == ConnectorPolarity.POSITIVE)
                {
                    LOGGER.trace("  Starting polarity trace from POSITIVE connection via connector at {}", startConnector.getPos().toShortString());
                    Set<INetworkMember> visitedNodesInPath = new HashSet<>();
                    // Start trace AT the connector, entering from the machine face (neighborNodeIndex should be -1, -2, or -3)
                    if (tracePolarityPath(startConnector, startConnection.neighborNodeIndex(), adjacencyList, visitedNodesInPath, ConnectorPolarity.POSITIVE, level)) {
                        LOGGER.debug("  Valid polarity path found for generator {} starting from connector {}.", generator.getPos().toShortString(), startConnector.getPos().toShortString());
                        foundValidPathForGenerator = true;
                        break; // Found a valid path for this generator
                    } else {
                        LOGGER.trace("  Polarity trace started from connector {} (POSITIVE) did not find a valid path.", startConnector.getPos().toShortString());
                    }
                }
            }

            if (!foundValidPathForGenerator) {
                LOGGER.warn("Polarity Validation Fail: Generator at {} has no valid positive-to-negative path.", generator.getPos().toShortString());
                return false; // This generator is not part of a valid circuit
            }
        }

        LOGGER.info("Polarity Validation Success: All generators have a valid path.");
        return true; // All generators checked out
    }


    /**
     * Recursively traces a path, checking polarity rules.
     */
    private boolean tracePolarityPath(INetworkMember currentNode, int entryIndex,
                                      Map<INetworkMember, List<ConnectionInfo>> adjacencyList,
                                      Set<INetworkMember> visitedNodesInPath,
                                      ConnectorPolarity expectedPolarity,
                                      Level level)
    {
        // --- Cycle Detection ---
        if (!visitedNodesInPath.add(currentNode)) {
            // LOGGER.trace("Polarity cycle detected at {} during trace.", currentNode.getPos().toShortString());
            return false; // Loops are only valid if they terminate correctly at Gen(-)
        }
        // LOGGER.trace("PolarityTrace: Entering node {} ({}), expecting {}", currentNode.getPos().toShortString(), currentNode.getClass().getSimpleName(), expectedPolarity);

        // --- Polarity Check & Determine Exit Polarity ---
        ConnectorPolarity actualEntryPolarity = expectedPolarity; // Assume matches if not a connector
        ConnectorPolarity exitPolarity = expectedPolarity;
        boolean isPassThrough = false;

        if (currentNode instanceof AbstractConnectorBlockEntity connector) {
            actualEntryPolarity = getConnectorPolarityAtIndex(connector, entryIndex, level); // Use the helper
            isPassThrough = (actualEntryPolarity == ConnectorPolarity.NONE);

            // Check if entry polarity matches expectation (unless it's pass-through)
            if (!isPassThrough && actualEntryPolarity != expectedPolarity) {
                LOGGER.trace("  Polarity Fail: Mismatch entering connector {}. Expected {}, Got {}.", connector.getPos().toShortString(), expectedPolarity, actualEntryPolarity);
                visitedNodesInPath.remove(currentNode); return false;
            }
            // Determine exit polarity
            exitPolarity = isPassThrough ? expectedPolarity : actualEntryPolarity;

        } else if (currentNode instanceof AbstractPowerUserBlockEntity) {
            // Entered a User Machine. Assume entry polarity check passed implicitly. Determine exit polarity.
            isPassThrough = false; // Users transform polarity
            if (expectedPolarity == ConnectorPolarity.POSITIVE) { exitPolarity = ConnectorPolarity.NEGATIVE; }
            else if (expectedPolarity == ConnectorPolarity.NEGATIVE) { exitPolarity = ConnectorPolarity.NEGATIVE; }
            else { // expectedPolarity == NONE
                LOGGER.trace("  Polarity Fail: Path reached User {} with unexpected NONE polarity.", currentNode.getPos().toShortString());
                visitedNodesInPath.remove(currentNode); return false;
            }
            LOGGER.trace("  Entering User {} expecting {}, exit polarity determined as {}.", currentNode.getPos().toShortString(), expectedPolarity, exitPolarity);

        } else if (currentNode instanceof AbstractGeneratorBlockEntity) {
            // --- Base Case: Reached a Generator ---
            if (expectedPolarity == ConnectorPolarity.NEGATIVE) {
                LOGGER.trace("  Polarity Success: Path terminated correctly at Generator {}.", currentNode.getPos().toShortString());
                return true; // Valid termination at *any* generator's negative side
            } else {
                LOGGER.trace("  Polarity Fail: Path terminated at Generator {} but expected polarity was {}.", currentNode.getPos().toShortString(), expectedPolarity);
                visitedNodesInPath.remove(currentNode); return false;
            }
        }
        //LOGGER.trace("  Node {} processed. Exit polarity determined as {}.", currentNode.getPos().toShortString(), exitPolarity);


        // --- Explore Neighbors ---
        List<ConnectionInfo> neighborsInfo = adjacencyList.getOrDefault(currentNode, Collections.emptyList());
        for (ConnectionInfo info : neighborsInfo) {
            INetworkMember neighbor = info.neighbor();
            int neighborEntryIndex = info.neighborNodeIndex();
            // Polarity the neighbor expects based on *our* exit polarity
            ConnectorPolarity nextExpectedPolarity = exitPolarity; // Usually the same

            //LOGGER.trace("    Exploring neighbor {} at index {}, next expects {}", neighbor.getPos().toShortString(), neighborEntryIndex, nextExpectedPolarity);

            // Recursive call with a COPY of visited set to allow exploring parallel paths independently
            if (tracePolarityPath(neighbor, neighborEntryIndex, adjacencyList, new HashSet<>(visitedNodesInPath), nextExpectedPolarity, level)) {
                return true; // Valid path found down this branch
            }
        }

        // --- Backtrack ---
        // No valid path found from this node down any branch
        visitedNodesInPath.remove(currentNode); // Not needed as we pass copies, but good practice if changing strategy
        //LOGGER.trace("  Backtracking from node {}: No valid continuing polarity paths found.", currentNode.getPos().toShortString());
        return false;
    }

    // --- Helpers ---

    // Copy the refined getConnectorPolarityAtIndex helper method here
    private ConnectorPolarity getConnectorPolarityAtIndex(AbstractConnectorBlockEntity connector, int index, Level level) {
        // --- Use the robust implementation from the previous response ---
        BlockPos pos = connector.getPos();
        if (level == null || connector == null) { LOGGER.error("getConnectorPolarityAtIndex: null level or connector for pos {}", pos); return ConnectorPolarity.NONE; }
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (!(block instanceof AbstractConnectorBlock)) { LOGGER.error("State at {} is not an AbstractConnectorBlock! Found: {}.", pos.toShortString(), block); return ConnectorPolarity.NONE; }

        try {
            if (block instanceof DuoConnectorBlock) {
                if (index == 0 || index == MACHINE_FACE_INDEX_DUO_0) { return state.hasProperty(DuoConnectorBlock.TERMINAL_TYPE_0) ? state.getValue(DuoConnectorBlock.TERMINAL_TYPE_0) : ConnectorPolarity.NONE; }
                else if (index == 1 || index == MACHINE_FACE_INDEX_DUO_1) { return state.hasProperty(DuoConnectorBlock.TERMINAL_TYPE_1) ? state.getValue(DuoConnectorBlock.TERMINAL_TYPE_1) : ConnectorPolarity.NONE; }
                else if (index == MACHINE_FACE_INDEX_SINGLE) { LOGGER.error("Ambiguous index -1 for DuoConnector at {}", pos.toShortString()); return ConnectorPolarity.NONE; }
            } else if (block instanceof SmallConnectorBlock) {
                if (state.hasProperty(SmallConnectorBlock.TERMINAL_TYPE)) { return state.getValue(SmallConnectorBlock.TERMINAL_TYPE); }
                else { LOGGER.warn("SmallConnector state at {} missing TERMINAL_TYPE property!", pos.toShortString()); return ConnectorPolarity.NONE;}
            } else if (block instanceof LargeConnectorBlock) {
                if (state.hasProperty(LargeConnectorBlock.TERMINAL_TYPE)) { return state.getValue(LargeConnectorBlock.TERMINAL_TYPE); }
                else { LOGGER.warn("LargeConnector state at {} missing TERMINAL_TYPE property!", pos.toShortString()); return ConnectorPolarity.NONE;}
            }
            LOGGER.warn("Could not determine polarity for connector {} ({}) at index {}: Unhandled type or property missing.", pos.toShortString(), block.getClass().getSimpleName(), index);
            return ConnectorPolarity.NONE;
        } catch (Exception e) {
            LOGGER.error("Error getting polarity for connector {} at index {}: {}", pos.toShortString(), index, e.getMessage(), e);
            return ConnectorPolarity.NONE;
        }
    }


    private List<AbstractGeneratorBlockEntity> findGenerators(Set<INetworkMember> members) {
        if (members == null) return Collections.emptyList();
        return members.stream()
                .filter(m -> m instanceof AbstractGeneratorBlockEntity)
                .map(m -> (AbstractGeneratorBlockEntity) m)
                .collect(Collectors.toList());
    }
}
