package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class
 * All network creations and connection should be made trough this class
 */
public class NetworkManager {
    public static Map<LevelAccessor, NetworkManager> instances = new HashMap<>();
    private List<Network> networks;

    public NetworkManager(LevelAccessor levelAccessor) {
        instances.put(levelAccessor, this);
        networks = new ArrayList<Network>();
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

    public void createConnection(Level level, ConnectionPoint connectionPoint1, ConnectionPoint connectionPoint2) {
        Network network = this.findOrCreateNetwork(connectionPoint1);
        Connection connection = new Connection(connectionPoint1, connectionPoint2);
        network.registerConnection(connection);

        IWireNode wireNode1 = (IWireNode) level.getBlockEntity(connectionPoint1.getPos());
        IWireNode wireNode2 = (IWireNode) level.getBlockEntity(connectionPoint2.getPos());

        if (wireNode1 != null) network.registerConnectorAndMachine(connectionPoint1, wireNode1.getMachine());
        if (wireNode2 != null) network.registerConnectorAndMachine(connectionPoint2, wireNode2.getMachine());
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
