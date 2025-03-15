package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Network {
    private Map<ConnectionPoint, BlockEntity> connectorMachineMap = new HashMap<>();
    private List<Connection> connections;
    private List<ConnectionPoint> connectionPoints;

    public Network() {
        this.connections = new ArrayList<Connection>();
        this.connectionPoints = new ArrayList<ConnectionPoint>();
    }

    void registerConnection(Connection connection) {
        connections.add(connection);
        connectionPoints.add(connection.getSourceNode());
        connectionPoints.add(connection.getTargetNode());
    }

    // Start Getters/Setters

    BlockEntity getMachineFromConnector(ConnectionPoint connectionPoint) {
        return connectorMachineMap.get(connectionPoint);
    }

    // End Getters/Setters

    void registerConnectorAndMachine(ConnectionPoint connectionPoint, BlockEntity machine) {
        connectorMachineMap.put(connectionPoint, machine);
    }

    void removeConnector(ConnectionPoint connectionPoint) {
        connectorMachineMap.remove(connectionPoint);
    }

    boolean containsConnectionPoint(ConnectionPoint connectionPoint) {
        return connectionPoints.contains(connectionPoint);
    }

    boolean isValid() {
        throw new NotImplementedException();
    }

    void removeInvalidConnections() {
        connections.removeIf(Network::isConnectionInvalid);
    }

    private static boolean isConnectionInvalid(Connection connection) {
        return !connection.isValid();
    }

    void tick() {
        removeInvalidConnections();
    }
}
