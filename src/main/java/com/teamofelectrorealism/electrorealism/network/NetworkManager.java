package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.LocalNode;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkManager {
    public static Map<LevelAccessor, NetworkManager> instances = new HashMap<>();
    private List<Network> networks;

    public NetworkManager(LevelAccessor level) {
        instances.put(level, this);
        networks = new ArrayList<Network>();
    }

    private Network findOrCreateNetwork(LocalNode node) {
        for (Network network : networks) {
            if (network.containsNode(node)) {
                return network;
            }
        }
        Network newNetwork = new Network();
        networks.add(newNetwork);
        return newNetwork;
    }

    public void createConnection(Level level, LocalNode node1, LocalNode node2) {
        Network network = this.findOrCreateNetwork(node1);
        Connection connection = new Connection(node1, node2);
        network.registerConnection(connection);

        IWireNode wireNode1 = (IWireNode) level.getBlockEntity(node1.getPos());
        IWireNode wireNode2 = (IWireNode) level.getBlockEntity(node2.getPos());

        if (wireNode1 != null) network.registerNodeAndMachine(node1, wireNode1.getMachine());
        if (wireNode2 != null) network.registerNodeAndMachine(node2, wireNode2.getMachine());
    }

    private void removeInvalidNetworks() {
        networks.removeIf(network -> !network.isValid());
    }

    public void tick() {
        for (Network network : networks) {
            network.tick();
        }
        removeInvalidNetworks();
    }
}
