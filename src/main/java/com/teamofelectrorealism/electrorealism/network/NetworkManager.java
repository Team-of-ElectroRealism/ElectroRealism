package com.teamofelectrorealism.electrorealism.network;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

/**
 * All network creations and connection should be made trough this class
 */
public class NetworkManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Set<Network> networks;

    private static NetworkSavedData savedData;

    public NetworkManager() {
        networks = new HashSet<>();
    }

    public static void levelLoaded(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null || server.overworld() != level) return;
        
        networks = new HashSet<>();
        savedData = null;
        loadNetworkData(server);
    }

    private static void loadNetworkData(MinecraftServer server) {
        if (savedData != null) return;
        savedData = NetworkSavedData.load(server);
        networks = savedData.getNetworks();
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

    public Network createNetwork() {
        Network newNetwork = new Network();
        networks.add(newNetwork);

        LOGGER.info("New network created with UUID: {}", newNetwork.getNetworkId());

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

        // Always null :thinkies:
        Network network1 = findNetwork(connectionPoint1);
        LOGGER.info("Found network with UUID: " + network1.getNetworkId());
        Network network2 = findNetwork(connectionPoint2);
        LOGGER.info("Found network with UUID: " + network2.getNetworkId());

        Network network;
        network = mergeNetworks(network1, network2);
        LOGGER.info("Merged network into UUID: " + network.getNetworkId());
        if (network1.getNetworkId() == network2.getNetworkId()) System.out.println("Same network");

        Connection connection = new Connection(connectionPoint1, connectionPoint2);
        network.registerConnection(connection);

        IWireNode wireNode1 = (IWireNode) level.getBlockEntity(connectionPoint1.getPos());
        IWireNode wireNode2 = (IWireNode) level.getBlockEntity(connectionPoint2.getPos());

        if (wireNode1 != null) network.registerConnectorAndMachine(connectionPoint1, wireNode1.getMachine());
        if (wireNode2 != null) network.registerConnectorAndMachine(connectionPoint2, wireNode2.getMachine());

        for (Network networkObject : networks) {
            LOGGER.info("Network: {}", networkObject.getNetworkId());
        }
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

    public void invalidateNetwork(Network network) {
        network.setInvalid();
    }

    public void removeAllNetworks() {
        networks = new HashSet<>();
    }
}
