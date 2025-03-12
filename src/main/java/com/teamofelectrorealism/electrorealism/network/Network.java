package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.power.LocalNode;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Network {
    private Map<LocalNode, BlockEntity> nodeMachineMap = new HashMap<>();
    private List<Connection> connections;
    private List<LocalNode> nodes;

    public Network() {
        this.connections = new ArrayList<Connection>();
        this.nodes = new ArrayList<LocalNode>();
    }

    void registerConnection(Connection connection) {
        connections.add(connection);
        nodes.add(connection.getSourceNode());
        nodes.add(connection.getTargetNode());
    }

    // Start Getters/Setters

    public BlockEntity getMachineFromNode(LocalNode node) {
        return nodeMachineMap.get(node);
    }

    // End Getters/Setters

    public void registerNodeAndMachine(LocalNode node, BlockEntity machine) {
        nodeMachineMap.put(node, machine);
    }

    public void unregisterNode(LocalNode node) {
        nodeMachineMap.remove(node);
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
