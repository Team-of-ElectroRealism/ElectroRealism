package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;

import java.util.UUID;

public class Connection {
    private final UUID connectionId;
    private boolean isValid;

    private final ConnectionPoint sourceConnectionPoint;
    private final ConnectionPoint targetConnectionPoints;

    public Connection(ConnectionPoint sourceConnectionPoint, ConnectionPoint targetConnectionPoints) {
        this.connectionId = UUID.randomUUID();
        this.isValid = true;
        this.sourceConnectionPoint = sourceConnectionPoint;
        this.targetConnectionPoints = targetConnectionPoints;
    }

    public ConnectionPoint getSourceConnectionPoint() {
        return sourceConnectionPoint;
    }

    public ConnectionPoint getConnectingConnectionPoint() {
        return targetConnectionPoints;
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

    public boolean hasConnectionPoint(ConnectionPoint connectionPoint) {
        return connectionPoint == this.sourceConnectionPoint || connectionPoint == targetConnectionPoints;
    }
}
