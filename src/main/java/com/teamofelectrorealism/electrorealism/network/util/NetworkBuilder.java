package com.teamofelectrorealism.electrorealism.network.util;

public class NetworkBuilder {
    /**
     * Take in single network
     * parse connectionPoints, gives connected machines and parent connectorEntity
     * parse connected machines for positive and negative sides
     * draw connections between connectionsPoints using connections
     *
     * Machines has the following
     *  Internal resistance
     *  maxAmp
     *  minAmp
     *
     * Connectors has the following
     *  pointCount
     *  Internal resistance
     *  maxAmp
     *
     * Connections has the following
     *  wireType
     *  posPoint
     *  negPoint
     *  wireLength
     *
     * WireType has the following
     *  color
     *  internal resistance
     *  maxAmp
     *  maxLength
     *  RESISTIVITY (ρ)
     *  thickness / cross-sectional area
     *
     * AbstractConnectorBlockEntity
     *  Network
     *  ConnectionPoints
     *  SetNetwork
     *  SetConnection
     *  Saves ConnectionPoints
     *
     */
    public NetworkBuilder() {

    }
}
