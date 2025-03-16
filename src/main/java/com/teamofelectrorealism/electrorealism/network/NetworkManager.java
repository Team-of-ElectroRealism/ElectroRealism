package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.*;

/**
 * All network creations and connection should be made trough this class
 */
public class NetworkManager {
    public static Map<LevelAccessor, NetworkManager> instances = new HashMap<>();
    private Set<Network> networks;

    public NetworkManager(LevelAccessor levelAccessor) {
        instances.put(levelAccessor, this);
        networks = new HashSet<>();
    }

    private Network findOrCreateNetwork(ConnectionPoint connectionPoint) {
        Network network = findNetwork(connectionPoint);
        if (network == null) {
            Network newNetwork = createNetwork();
            return newNetwork;
        }
        return network;
    }

    @Nullable
    private Network findNetwork(ConnectionPoint connectionPoint) {
        for (Network network : networks) {
            if (network.containsConnectionPoint(connectionPoint)) {
                return network;
            }
        }
        return null;
    }

    private Network createNetwork() {
        Network newNetwork = new Network();
        networks.add(newNetwork);
        return newNetwork;
    }

    private Network mergeNetworks(Network network1, Network network2) {
        for (Connection connection: network2.getConnections()) {
            network1.registerConnection(connection);
        }
        network2.setInvalid();
        //removeInvalidNetworks();
        networks.remove(network2);
        return network1;
    }

    public void createConnection(Level level, ConnectionPoint connectionPoint1, ConnectionPoint connectionPoint2) {
        if (level.isClientSide()) return;

        Network network1 = findNetwork(connectionPoint1);
        Network network2 = findNetwork(connectionPoint2);

        Network network;
        if (network1 != null && network2 != null && network1 != network2) {
            network = mergeNetworks(network1, network2);
        } else {
            network = this.findOrCreateNetwork(connectionPoint1);
        }

        Connection connection = new Connection(connectionPoint1, connectionPoint2);
        network.registerConnection(connection);

        IWireNode wireNode1 = (IWireNode) level.getBlockEntity(connectionPoint1.getPos());
        IWireNode wireNode2 = (IWireNode) level.getBlockEntity(connectionPoint2.getPos());

        if (wireNode1 != null) network.registerConnectorAndMachine(connectionPoint1, wireNode1.getMachine());
        if (wireNode2 != null) network.registerConnectorAndMachine(connectionPoint2, wireNode2.getMachine());
        System.out.println("Before printNetwork()");
        network.printNetwork();
        System.out.println(networks);
        System.out.println(instances);
        System.out.println("After printNetwork()");
    }
    
    public void removeConnection(Level level, ConnectionPoint connectionPoint1, ConnectionPoint connectionPoint2) {
        Network network = this.findNetwork(connectionPoint1);
        if (network == null) return;
        // todo
    }

    private void removeInvalidNetworks() {
        networks.removeIf(NetworkManager::isNetworkInvalid);
    }

    private static boolean isNetworkInvalid(Network network) {
        return !network.isValid();
    }

    public void tick() {
        for (Network network : networks) {
            network.tick();
        }
        removeInvalidNetworks();
    }
}
