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

    public void addConnection(Connection connection) {
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

    public void tick() {
        List<Connection> validConnections = new ArrayList<Connection>();
        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            if (connection.isValid()) {
                connection.tick(i);
                validConnections.add(connection);
            }
            connection.removed();
        }
        connections = validConnections;
    }
}
