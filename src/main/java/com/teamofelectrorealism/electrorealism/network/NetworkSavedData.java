package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

// https://docs.neoforged.net/docs/1.21.1/datastorage/saveddata
public class NetworkSavedData extends SavedData {

    private Set<Network> networks = new HashSet<>();

    public NetworkSavedData() {
    }

    public static final SavedData.Factory<NetworkSavedData> FACTORY = new SavedData.Factory<>(
            NetworkSavedData::create,
            NetworkSavedData::load,
            null);

    // Create new instance of saved data
    public static NetworkSavedData create() {
        return new NetworkSavedData();
    }

    // Load existing instance of saved data
    public static NetworkSavedData load(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, ElectroRealism.MODID + "_networks");
    }

    private static NetworkSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        NetworkSavedData savedData = new NetworkSavedData();
        ListTag networkList = tag.getList("Networks", Tag.TAG_COMPOUND);
//        networkList.forEach(networkTag -> {
//            savedData.networks.add(Network.read((CompoundTag) networkTag));
//        });
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        NetworkManager networkManager = ElectroRealism.NETWORK_MANAGER;
        tag.put("Networks", new CompoundTag());
        return tag;
    }

    public void onNetworkChanged() {
        // Change data in saved data
        // Call set dirty if data changes
        this.setDirty();
    }

    public Set<Network> getNetworks() {
        return networks;
    }
}
