package com.teamofelectrorealism.electrorealism.network;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

// https://docs.neoforged.net/docs/1.21.1/datastorage/saveddata
public class NetworkSavedData extends SavedData {
    private static NetworkManager networkManager; // Reference to your NetworkManager

    public NetworkSavedData(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    // Create new instance of saved data
    public static NetworkSavedData create(NetworkManager networkManager) {
        return new NetworkSavedData(networkManager);
    }

    // Load existing instance of saved data
    public static NetworkSavedData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        NetworkSavedData data = NetworkSavedData.create(networkManager);
        // Load saved data
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        // Write data to tag
        return tag;
    }

    public void onNetworkChanged() {
        // Change data in saved data
        // Call set dirty if data changes
        this.setDirty();
    }
}
