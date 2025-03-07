package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.power.LocalNode;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkManager {
    public static Map<LevelAccessor, NetworkManager> instances = new HashMap<>();
    private List<Network> networks;

    public NetworkManager(LevelAccessor levelAccessor) {
        instances.put(levelAccessor, this);
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

    public void createConnection(LocalNode sourceNode, LocalNode targetNode) {
        Network network = this.findOrCreateNetwork(sourceNode);
        Connection connection = new Connection(sourceNode, targetNode);
        network.addConnection(connection);
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
