package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.power.LocalNode;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

public class Network {
    private List<Connection> connections;
    private List<LocalNode> nodes;

    public Network() {
        this.connections = new ArrayList<Connection>();
        this.nodes = new ArrayList<LocalNode>();
    }

    void addConnection(Connection connection) {
        connections.add(connection);
        nodes.add(connection.getSourceNode());
        nodes.add(connection.getTargetNode());
    }

    public boolean containsNode(LocalNode node) {
        return nodes.contains(node);
    }

    public boolean isValid() {
        throw new NotImplementedException();
    }

    private void removeInvalidConnections() {
        connections.removeIf(connection -> !connection.isValid());
    }

    public void tick() {
        removeInvalidConnections();
    }
}
