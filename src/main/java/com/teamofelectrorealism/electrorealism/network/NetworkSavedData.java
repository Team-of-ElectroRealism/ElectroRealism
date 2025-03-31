package com.teamofelectrorealism.electrorealism.network;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.ElectroRealism;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.*;

/**
 * Manages the persistent storage of {@link Network} data across server restarts.
 * This class extends {@link SavedData} to handle the loading and saving of network
 * information to and from NBT data. It ensures that network configurations are
 * preserved between game sessions.
 *
 * @see SavedData
 * @see Network
 * @see <a href="https://docs.neoforged.net/docs/1.21.1/datastorage/saveddata">NeoForge SavedData Documentation</a>
 */
public class NetworkSavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();

    private Set<Network> networks = new HashSet<>();

    private static final String NETWORKS_LIST_KEY = "networks";
    /**
     * Private constructor to prevent direct instantiation.
     */
    private NetworkSavedData() {}

    /**
     * Factory for creating and loading {@link NetworkSavedData} instances.
     */
    public static final SavedData.Factory<NetworkSavedData> FACTORY = new SavedData.Factory<>(
            NetworkSavedData::create,
            NetworkSavedData::loadFromNbt,
            null);

    /**
     * Creates a new instance of {@link NetworkSavedData}.
     *
     * @return A new {@link NetworkSavedData} instance.
     */
    public static NetworkSavedData create() {
        LOGGER.info("Creating new NetworkSavedData instance.");
        return new NetworkSavedData();
    }

    /**
     * Loads or creates the {@link NetworkSavedData} instance for the Overworld.
     * If the data does not exist, it creates a new instance.
     *
     * @param server The {@link MinecraftServer} instance.
     * @return The loaded or newly created {@link NetworkSavedData} instance.
     */
    public static NetworkSavedData load(MinecraftServer server) {
        LOGGER.info("Attempting to load or create NetworkSavedData for Overworld.");
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, ElectroRealism.MODID + "_networks");
    }

    /**
     * Loads {@link NetworkSavedData} from NBT data. This method is called by the {@link SavedData.Factory}
     * when loading data from disk. It reads the list of networks from the NBT tag and reconstructs
     * the {@link NetworkSavedData} instance.
     *
     * @param tag      The {@link CompoundTag} containing the saved data.
     * @param provider The {@link HolderLookup.Provider} for resource lookups.
     * @return A {@link NetworkSavedData} instance loaded from the NBT data.
     * @Logs: Exception If there is an error loading a network from the NBT data.
     */
    private static NetworkSavedData loadFromNbt(CompoundTag tag, HolderLookup.Provider provider) {
        LOGGER.info("Loading NetworkSavedData from NBT.");
        NetworkSavedData savedData = new NetworkSavedData();
        Set<Network> loadedNetworks = new HashSet<>();

        if (tag.contains(NETWORKS_LIST_KEY, Tag.TAG_LIST)) {
            ListTag networkList = tag.getList(NETWORKS_LIST_KEY, Tag.TAG_COMPOUND);
            LOGGER.info("Found {} networks in NBT list.", networkList.size());

            for (Tag networkTagGeneric : networkList) {
                if (networkTagGeneric instanceof CompoundTag networkTag) {
                    try {
                        Network loadedNetwork = Network.readFromNbt(networkTag);
                        if (loadedNetwork.isValid()) {
                            loadedNetworks.add(loadedNetwork);
                            LOGGER.debug("Successfully loaded Network {}", loadedNetwork.getNetworkId());
                        } else {
                            LOGGER.debug("Skipping loading invalid Network {}", loadedNetwork.getNetworkId());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to load a Network from NBT tag: {}", networkTag, e);
                    }
                } else {
                    LOGGER.warn("Unexpected tag type in Networks list: {}", networkTagGeneric.getType().getName());
                }
            }
        } else {
            LOGGER.info("No '{}' list tag found in NBT.", NETWORKS_LIST_KEY);
        }

        savedData.networks = loadedNetworks;
        LOGGER.info("Finished loading NetworkSavedData. {} valid networks loaded.", loadedNetworks.size());
        return savedData;
    }

    /**
     * Saves the current {@link Network} data to NBT.
     *
     * @param tag      The {@link CompoundTag} to save the data to.
     * @param provider The {@link HolderLookup.Provider} for resource lookups.
     * @return The {@link CompoundTag} with the saved data.
     * @throws Exception If there is an error writing a network to the NBT data.
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        LOGGER.info("Saving NetworkSavedData to NBT. Saving {} networks.", this.networks.size());
        ListTag networksList = new ListTag();

        for (Network network : this.networks) {
            if (network != null) {
                try {
                    networksList.add(network.writeToNbt());
                    LOGGER.trace("Saved Network {} to NBT list.", network.getNetworkId());
                } catch (Exception e) {
                    LOGGER.error("Failed to write Network {} to NBT.", network.getNetworkId(), e);
                }
            } else {
                LOGGER.warn("Attempted to save a null Network instance.");
            }
        }

        tag.put(NETWORKS_LIST_KEY, networksList);
        LOGGER.info("Finished saving NetworkSavedData.");
        return tag;
    }

    /**
     * Updates the network data with the current networks from the {@link NetworkManager}.
     * Marks the data as dirty if changes are detected.
     *
     * @param currentNetworksFromManager The current set of {@link Network} from the manager.
     */
    public void updateNetworkData(Set<Network> currentNetworksFromManager) {
        int currentInternalSize = this.networks.size();
        int incomingSize = currentNetworksFromManager.size();
        LOGGER.debug("updateNetworkData called. Internal size: {}, Incoming size: {}", currentInternalSize, incomingSize);


        if (!this.networks.equals(currentNetworksFromManager)) {
            this.networks = new HashSet<>(currentNetworksFromManager);
            LOGGER.info("Network data CHANGED. Updating internal set (new size: {}) and marking dirty.", this.networks.size());
            setDirty();
        } else {
            LOGGER.debug("Network data appears unchanged, not marking as dirty.");
        }
    }

    /**
     * Gets a copy of the current set of networks.
     *
     * @return A new {@link Set} containing the current {@link Network} instances.
     */
    Set<Network> getNetworks() {
        return new HashSet<>(this.networks);
    }
}
