package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;

public class Connection {
    private int id;
    private boolean isValid;

    private final ConnectionPoint sourceNode;
    private final ConnectionPoint targetNode;

    public Connection(ConnectionPoint sourceNode, ConnectionPoint targetNode) {
        this.id = 0;
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

    public int getId() {
        return id;
    }

    public boolean isValid() {
        return isValid;
    }

    public void removed() {
    }

    public void invalidate() {
        this.isValid = false;
    }

    public void tick(int index) {
        this.id = index;
    }
}
