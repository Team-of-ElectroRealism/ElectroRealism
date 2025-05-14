package com.teamofpowersim.powersim.network;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.block.connector.AbstractConnectorBlock;
import com.teamofpowersim.powersim.block.connector.AbstractConnectorBlockEntity;
import com.teamofpowersim.powersim.block.connector.ConnectorPolarity;
import com.teamofpowersim.powersim.block.connector.duo.DuoConnectorBlock;
import com.teamofpowersim.powersim.block.connector.duo.DuoConnectorBlockEntity;
import com.teamofpowersim.powersim.block.connector.large.LargeConnectorBlock;
import com.teamofpowersim.powersim.block.connector.small.SmallConnectorBlock;
import com.teamofpowersim.powersim.power.ConnectionPoint;
import com.teamofpowersim.powersim.power.IWireNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.*;

/**
 * Builds an adjacency list representation of an electrical network graph.
 */
public class NetworkGraphBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Constants for machine face indices (ensure these are consistent across classes)
    private static final int MACHINE_FACE_INDEX_SINGLE = -1;
    private static final int MACHINE_FACE_INDEX_DUO_0 = -2;
    private static final int MACHINE_FACE_INDEX_DUO_1 = -3;

    /**
     * Builds the adjacency list map for the given network members.
     */
    public Map<INetworkMember, List<ConnectionInfo>> buildAdjacencyList(Set<INetworkMember> members, Level level) {
        Map<INetworkMember, List<ConnectionInfo>> adj = new HashMap<>();
        if (members == null || level == null) {
            LOGGER.error("buildAdjacencyList called with null members or level.");
            return adj;
        }

        for (INetworkMember member : members) {
            adj.putIfAbsent(member, new ArrayList<>()); // Ensure all members have an entry

            // --- 1. Handle Wire Connections (IWireNode to IWireNode) ---
            if (member instanceof IWireNode wireNode && member instanceof AbstractConnectorBlockEntity sourceConnector) { // Assuming only connectors are wire nodes for now
                for (int i = 0; i < wireNode.getConnectionPointCount(); i++) {
                    ConnectionPoint cp = wireNode.getConnectionPoint(i);
                    if (cp != null) {
                        IWireNode neighborNode = wireNode.getWireNode(i);
                        if (neighborNode instanceof INetworkMember neighborMember &&
                                neighborNode instanceof AbstractConnectorBlockEntity neighborConnector && // Ensure neighbor is also a connector for wire links
                                members.contains(neighborMember))
                        {
                            // Determine polarity AT the neighbor's entry point
                            ConnectorPolarity neighborPolarityAtEntry = getConnectorPolarityAtIndex(neighborConnector, cp.getConnectingPointIndex(), level);

                            adj.computeIfAbsent(member, k -> new ArrayList<>())
                                    .add(new ConnectionInfo(neighborMember, i, cp.getConnectingPointIndex(), neighborPolarityAtEntry));
                        }
                    }
                }
            }

            // --- 2. Handle Implicit Machine-Connector Connections ---
            if (member instanceof AbstractConnectorBlockEntity connector) {
                INetworkMember attachedMachine = connector.findNetworkMember();
                if (attachedMachine != null && members.contains(attachedMachine)) {
                    BlockState connectorState = level.getBlockState(connector.getPos());

                    // Link: Connector -> Machine
                    if (connector instanceof DuoConnectorBlockEntity) {
                        adj.computeIfAbsent(member, k -> new ArrayList<>()).add(new ConnectionInfo(attachedMachine, MACHINE_FACE_INDEX_DUO_0, MACHINE_FACE_INDEX_SINGLE, ConnectorPolarity.NONE));
                        adj.computeIfAbsent(member, k -> new ArrayList<>()).add(new ConnectionInfo(attachedMachine, MACHINE_FACE_INDEX_DUO_1, MACHINE_FACE_INDEX_SINGLE, ConnectorPolarity.NONE));
                    } else { // Small or Large
                        adj.computeIfAbsent(member, k -> new ArrayList<>()).add(new ConnectionInfo(attachedMachine, MACHINE_FACE_INDEX_SINGLE, MACHINE_FACE_INDEX_SINGLE, ConnectorPolarity.NONE));
                    }

                    // Link: Machine -> Connector
                    if (connector instanceof DuoConnectorBlockEntity) {
                        ConnectorPolarity p0 = getConnectorPolarityAtIndex(connector, MACHINE_FACE_INDEX_DUO_0, level); // Polarity of Terminal 0 facing machine
                        ConnectorPolarity p1 = getConnectorPolarityAtIndex(connector, MACHINE_FACE_INDEX_DUO_1, level); // Polarity of Terminal 1 facing machine
                        adj.computeIfAbsent(attachedMachine, k -> new ArrayList<>()).add(new ConnectionInfo(member, MACHINE_FACE_INDEX_SINGLE, MACHINE_FACE_INDEX_DUO_0, p0));
                        adj.computeIfAbsent(attachedMachine, k -> new ArrayList<>()).add(new ConnectionInfo(member, MACHINE_FACE_INDEX_SINGLE, MACHINE_FACE_INDEX_DUO_1, p1));
                    } else { // Small or Large
                        ConnectorPolarity connectorPolarity = getConnectorPolarityAtIndex(connector, MACHINE_FACE_INDEX_SINGLE, level); // Polarity facing machine
                        adj.computeIfAbsent(attachedMachine, k -> new ArrayList<>()).add(new ConnectionInfo(member, MACHINE_FACE_INDEX_SINGLE, MACHINE_FACE_INDEX_SINGLE, connectorPolarity));
                    }
                }
            }
        }
        printAdjacencyList(adj); // Keep or remove debug logging
        return adj;
    }

    /**
     * Helper method to reliably get connector polarity from its state.
     * Moved here as it's essential for building the graph correctly.
     */
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

    // Optional: Keep the print helper for debugging
    private void printAdjacencyList(Map<INetworkMember, List<ConnectionInfo>> adj) {
        LOGGER.debug("--- Adjacency List ---");
        adj.forEach((member, connections) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(member.getPos().toShortString())
                    .append(" (").append(member.getClass().getSimpleName()).append("):");
            if (connections.isEmpty()) {
                sb.append(" [No outgoing connections]");
            } else {
                for (ConnectionInfo info : connections) {
                    sb.append("\n    -> ")
                            .append(info.neighbor().getPos().toShortString())
                            .append(" [SrcIdx:").append(info.sourceNodeIndex())
                            .append(", NeighborIdx:").append(info.neighborNodeIndex())
                            .append(", PolarityAtEntry:").append(info.polarityAtNeighborEntry()).append("]");
                }
            }
            LOGGER.debug(sb.toString());
        });
        LOGGER.debug("----------------------");
    }
}
