package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;

import java.util.UUID;

public class Connection {
    private final UUID connectionId;
    private boolean isValid;

    private final ConnectionPoint sourceNode;
    private final ConnectionPoint targetNode;

    public Connection(ConnectionPoint sourceNode, ConnectionPoint targetNode) {
        this.connectionId = UUID.randomUUID();
        this.isValid = true;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
    }

    public ConnectionPoint getConnectionPoint() {
        return sourceNode;
    }

    public ConnectionPoint getConnectingConnectionPoint() {
        return targetNode;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public boolean isValid() {
        return isValid;
    }

    public void invalidate() {
        this.isValid = false;
    }
}
