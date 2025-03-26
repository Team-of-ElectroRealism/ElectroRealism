package com.teamofelectrorealism.electrorealism.network;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.rendering.HighlightNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * All network creations and connection should be made trough this class
 */
public class NetworkManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Set<Network> networks;
    private NetworkSavedData savedData;

    public NetworkManager() {
        networks = new HashSet<>();
    }

    public void getNetworkIds() {
        LOGGER.info("List all networks:");
        for (Network network : networks) {
            LOGGER.info("UUID: {}, Object: {}", network.getNetworkId(), network);
        }
    }

    public void levelLoaded(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null || server.overworld() != level) return;
        
        networks = new HashSet<>();
        savedData = null;
        loadNetworkData(server);
    }

    private void loadNetworkData(MinecraftServer server) {
        if (savedData != null) return;
        savedData = NetworkSavedData.load(server);
        networks = savedData.getNetworks();
    }

    public void addNetwork(Network network) {
        networks.add(network);
    }

    public void tick() {
        for (Network network : networks) {
            network.tick();
        }
    }

    public UUID createNetwork() {
        Network network = new Network();
        LOGGER.info("New network with UUID: {}", network.getNetworkId());
        return network.getNetworkId();
    }

    public void registerINetworkMemberInNetwork(UUID networkId, INetworkMember networkMember) {
        Network network = findNetwork(networkId);
        if (network != null) network.registerINetworkMember(networkMember);
    }

    private Network findNetwork(UUID networkId) {
        for (Network network : networks) {
            if (network.getNetworkId() == networkId) return network;
        }
        return null;
    }

    public UUID createOrMergeNetworks(INetworkMember networkMember1, INetworkMember networkMember2) {
        UUID networkId1 = networkMember1.getNetworkId();
        UUID networkId2 = networkMember2.getNetworkId();

        if (networkId1 == null && networkId2 == null) {
            // Neither node has a network, create a new one
            return createNetwork();
        } else if (networkId1 != null && networkId2 == null) {
            // Only node1 has a network, use it
            LOGGER.info("Using network with UUID: {}", networkId1);
            return networkId1;
        } else if (networkId1 == null && networkId2 != null) {
            // Only node2 has a network, use it
            LOGGER.info("Using network with UUID: {}", networkId2);
            return networkId2;
        } else {
            // Both nodes have networks, merge them into network1
            if (networkId1.equals(networkId2)) {
                LOGGER.info("Using network with UUID: {}", networkId1);
                return networkId1;
            }
            return mergeNetworks(networkId1, networkId2);
        }
    }

    private UUID mergeNetworks(UUID networkId1, UUID networkId2) {
        Network network1 = findNetwork(networkId1);
        Network network2 = findNetwork(networkId2);
        UUID networkId;

        if (network1 != null && network2 != null) {
            // Merge network2 into network1
            network1.registerAllINetworkMembers(network2.getNetworkMembers());
            network2.setInvalid();
            networks.remove(network2);
            LOGGER.info("Merged network {} into {}", networkId2, networkId1);
            networkId = networkId1;
        } else if (network1 == null) {
            LOGGER.error("Network1 not found, but should exist");
            networkId = networkId2;
        } else {
            LOGGER.error("Network2 not found, but should exist");
            networkId = networkId1;
        }

        updateNetworkIds(networkId, findNetwork(networkId).getNetworkMembers());
        return networkId;
    }

    private void updateNetworkIds(UUID networkId, Set<INetworkMember> networkMembers) {
        for (INetworkMember networkMember : networkMembers) {
            networkMember.setNetworkId(networkId);
        }
    }

    public void printMembersInNetwork(UUID networkId) {
        Network network = findNetwork(networkId);
        Set<INetworkMember> networkMembers = network.getNetworkMembers();
        if (networkMembers == null) return;
        LOGGER.info("All members in clicked network with UUID: {}", networkId);
        for (INetworkMember networkMember : networkMembers) {
            LOGGER.info("Member: {}", networkMember);
        }
    }

    public Map<UUID, List<BlockPos>> getNetworksData() {
        Map<UUID, List<BlockPos>> data = new HashMap<>();
        for (Network network : networks) {
            if (network.isValid()) {
                List<BlockPos> blockPositions = network.getNetworkMembers().stream()
                        .map(INetworkMember::getPos)
                        .collect(Collectors.toList());
                data.put(network.getNetworkId(), blockPositions);
            }
        }
        return data;
    }

}
