package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;

public class Network {
    private final UUID networkId;

    private Map<ConnectionPoint, BlockEntity> connectorMachineMap = new HashMap<>();
    private Set<Connection> connections;
    private Set<ConnectionPoint> connectionPoints;

    private boolean isValid;

    public Network() {
        this.networkId = UUID.randomUUID();
        this.isValid = true;
        this.connections = new HashSet<>();
        this.connectionPoints = new HashSet<>();
    }

    public Network(UUID networkId) {
        this.networkId = networkId;
        this.isValid = true;
        this.connections = new HashSet<>();
        this.connectionPoints = new HashSet<>();
    }

    void registerConnection(Connection connection) {
        connections.add(connection);
        connectionPoints.add(connection.getConnectionPoint());
        connectionPoints.add(connection.getConnectingConnectionPoint());
    }

    // Start Getters/Setters

    BlockEntity getMachineFromConnector(ConnectionPoint connectionPoint) {
        return connectorMachineMap.get(connectionPoint);
    }

    Set<Connection> getConnections() {
        return connections;
    }

    Map<ConnectionPoint, BlockEntity> getConnectorMachineMap() {
        return connectorMachineMap;
    }

    Set<ConnectionPoint> getConnectionPoints() {
        return connectionPoints;
    }

    public void setInvalid() {
        isValid = false;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    // End Getters/Setters

    void registerConnectorAndMachine(ConnectionPoint connectionPoint, BlockEntity machine) {
        connectorMachineMap.put(connectionPoint, machine);
    }

    void removeConnector(ConnectionPoint connectionPoint) {
        connectorMachineMap.remove(connectionPoint);
    }

    boolean containsConnectionPoint(ConnectionPoint connectionPoint) {
        return getConnectionPoints().contains(connectionPoint);
    }

    boolean isValid() {
        return isValid;
    }

    void removeInvalidConnections() {
        connections.removeIf(Network::isConnectionInvalid);
    }

    private static boolean isConnectionInvalid(Connection connection) {
        return !connection.isValid();
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("network_id", networkId);
        tag.putBoolean("isvalid", isValid);

        return tag;
    }

    public Network read(CompoundTag tag) {
        Network network = new Network(tag.getUUID("network_id"));

        isValid = tag.getBoolean("isvalid");

        return network;
    }

    void tick() {
        removeInvalidConnections();
    }
}
