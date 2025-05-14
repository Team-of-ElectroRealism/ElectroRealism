package com.teamofpowersim.powersim.network;

import com.teamofpowersim.powersim.block.connector.ConnectorPolarity;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a directed edge in the network graph for validation purposes.
 */
public record ConnectionInfo(@NotNull INetworkMember neighbor,          // The destination node
                             int sourceNodeIndex,                       // Index on the source node where connection originates (-1, -2, -3 for machine faces)
                             int neighborNodeIndex,                     // Index on the neighbor node where connection terminates (-1, -2, -3 for machine faces)
                             ConnectorPolarity polarityAtNeighborEntry  // Polarity of the neighbor's specific point being entered (determined during graph build)
) {
}