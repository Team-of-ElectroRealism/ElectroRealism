package com.teamofpowersim.powersim.network;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.block.connector.AbstractConnectorBlockEntity;
import com.teamofpowersim.powersim.block.machine.AbstractMachineBlockEntity;
import com.teamofpowersim.powersim.power.ConnectionPoint;
import com.teamofpowersim.powersim.power.WireType;
import com.teamofpowersim.powersim.simulation.ISimulatable;
import com.teamofpowersim.powersim.simulation.NgSpiceSimulator;
import com.teamofpowersim.powersim.simulation.SimulationDataApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import com.teamofpowersim.powersim.power.IWireNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages the creation, loading, saving, and merging of electrical networks.
 */
public class NetworkManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private Set<Network> networks = ConcurrentHashMap.newKeySet(); // Ensure this is initialized
    @Nullable private NetworkSavedData savedData = null;
    @Nullable private MinecraftServer server = null; // Will be set on first tick or level load

    private final Set<UUID> currentlySimulating = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Constructs a new NetworkManager.
     */
    public NetworkManager() {}

    // --- Loading and Saving ---

    /**
     * Clears the runtime state of the NetworkManager, typically called on world unload.
     * This resets the collection of active networks and references to saved data and server.
     */
    public void levelUnloaded() {
        if (!this.networks.isEmpty()) {
            this.networks.clear();
            LOGGER.debug("Cleared runtime networks set.");
        }
        this.currentlySimulating.clear(); // Clear this too
        this.savedData = null;
        this.server = null;
        LOGGER.info("NetworkManager runtime state cleared.");
    }

    /**
     * Called when a level (specifically the Overworld for global data) loads.
     * Initializes or loads the network data from storage.
     * @param level The level that loaded (should be the Overworld).
     */
    public void levelLoaded(LevelAccessor level) {
        // Add initial check from previous response to diagnose potential instance issues
        LOGGER.info("NetworkManager levelLoaded START. Networks field valid: {}", this.networks != null);
        if (this.networks == null) {
            LOGGER.error("CRITICAL: NetworkManager instance or networks field is NULL at the start of levelLoaded! Aborting.");
            return;
        }
        // --- End initial check ---

        if (!(level instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
            LOGGER.warn("NetworkManager levelLoaded called with non-ServerLevel or null server.");
            return;
        }

        if (this.server == null || this.server != serverLevel.getServer()) {
            this.server = serverLevel.getServer();
            LOGGER.info("NetworkManager initialized/updated for server instance.");
        } else if (this.savedData != null && this.server == serverLevel.getServer()){
            LOGGER.debug("NetworkManager already initialized for this server and data likely loaded.");
            if (serverLevel != this.server.overworld() || (this.savedData != null && this.savedData.getNetworks().isEmpty())) { // check if savedData is null before calling getNetworks
                resolveMembersInLevel(serverLevel);
            }
            return;
        }

        if (serverLevel == this.server.overworld()) {
            LOGGER.info("Loading network data from Overworld...");
            loadSavedNetworkData(this.server); // This creates Network objects
            resolveMembersInLevel(serverLevel); // This adds members to them
            // By this point, Network constructors and addNetworkMember calls should have
            // invoked requestSimulation for all loaded, dirty networks.
        } else {
            LOGGER.info("NetworkManager levelLoaded for dimension {}, resolving members if networks previously loaded.", serverLevel.dimension().location());
            if (this.savedData != null && !this.networks.isEmpty()) {
                resolveMembersInLevel(serverLevel);
            } else {
                LOGGER.warn("Networks not loaded yet (Overworld not loaded, or savedData is null?), cannot resolve members for dimension {}.", serverLevel.dimension().location());
            }
        }
    }

    /**
     * Loads the core network data (IDs and positions) from the SavedData.
     * Populates the 'networks' set with Network objects containing only positions initially.
     */
    // Use the version of loadSavedNetworkData WITHOUT the extra `this.networks = new HashSet<>();` line inside try/catch
    private void loadSavedNetworkData(MinecraftServer server) {
        // Initial check for final field (should always pass after construction)
        if (this.networks == null) {
            LOGGER.error("CRITICAL: NetworkManager.networks is NULL at the start of loadSavedNetworkData! Aborting load.");
            this.savedData = null;
            return;
        }
        if (this.savedData != null) {
            LOGGER.warn("Attempting to load network saved data when it's already loaded.");
            return;
        }
        try {
            LOGGER.debug("Loading NetworkSavedData...");
            this.savedData = NetworkSavedData.load(server);

            if (this.savedData == null) {
                throw new IllegalStateException("NetworkSavedData.load returned null or failed without exception.");
            }

            Set<Network> loadedNetworks = this.savedData.getNetworks();

            LOGGER.debug("Clearing current runtime networks before loading from save.");
            // ** CRUCIAL: Use clear(), DO NOT REASSIGN `this.networks` **
            this.networks.clear(); // Clear the existing set

            if (loadedNetworks != null) {
                this.networks.addAll(loadedNetworks); // Add loaded structures to the existing set
                LOGGER.info("NetworkManager loaded {} networks structure from NetworkSavedData.", this.networks.size());
            } else {
                LOGGER.warn("NetworkSavedData.getNetworks() returned null. Runtime networks remain empty.");
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load NetworkSavedData!", e);
            if (this.networks != null) { // Should be non-null
                this.networks.clear();
            } else {
                LOGGER.error("CRITICAL: NetworkManager.networks became null during catch block processing!");
            }
            this.savedData = null;
        }
    }


    private void resolveMembersInLevel(ServerLevel level) {
        if (this.networks.isEmpty()) {
            LOGGER.debug("No networks loaded, skipping member resolution for level {}.", level.dimension().location());
            return;
        }
        LOGGER.info("Attempting to resolve members in level {} for {} loaded networks...", level.dimension().location(), this.networks.size());

        int resolvedCount = 0;
        int potentialPositionsInLevel = 0; // Count positions relevant to *this* level if needed
        Set<UUID> networksToRemove = new HashSet<>();

        for (Network network : this.networks) {
            if (!network.isValid()) continue;

            Set<BlockPos> positions = network.getMemberPositions(); // Get saved positions
            boolean networkHadPositions = !positions.isEmpty();

            for (BlockPos pos : positions) {
                // Check if the position is within the bounds of the provided level
                // This simple check might not be enough for cross-dimension networks if supported
                // but essential if networks are dimension-specific or primarily in one dimension
                if (!level.isInWorldBounds(pos)){
                    continue; // Skip positions not in this level
                }
                potentialPositionsInLevel++;

                if (level.isLoaded(pos)) { // Check if chunk is loaded
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof INetworkMember member) {
                        // Call the simplified setNetworkId. It handles self-registration.
                        member.setNetworkId(network.getNetworkId());
                        // Also add to runtime list here to ensure network object is populated during load phase
                        network.addNetworkMember(member); // Ensure Network object knows about loaded member
                        resolvedCount++;
                    } else {
                        // Log potentially stale position data
                        //LOGGER.debug("No INetworkMember found at saved position {} for network {}. Block removed?", pos.toShortString(), network.getNetworkId());
                        // Consider removing the position from the network if this persists? network.removeMemberPosition(pos); markDataDirty();
                    }
                } else {
                    // Chunk not loaded. Member will register via its onLoad when chunk loads.
                }
            }
            // Check if network ended up empty after trying to resolve members *in this specific level*
            if (network.getNetworkMembers().isEmpty() && networkHadPositions && potentialPositionsInLevel > 0) {
                // This network had saved positions relevant to this level, but none resolved.
                // Don't remove it yet, members might be in unloaded chunks or other dimensions.
                LOGGER.debug("Network {} has saved positions in level {} but no members resolved in loaded chunks.", network.getNetworkId(), level.dimension().location());
            } else if (network.getMemberPositions().isEmpty()) { // If it has NO saved positions at all
                LOGGER.info("Network {} has no saved positions. Marking for removal.", network.getNetworkId());
                networksToRemove.add(network.getNetworkId());
            }
        }

        // Remove networks that definitively have no saved positions
        networksToRemove.forEach(id -> {
            Network netToRemove = findNetwork(id);
            if (netToRemove != null) {
                LOGGER.info("Removing network {} because it has no member positions.", id);
                netToRemove.setInvalid();
                this.networks.remove(netToRemove);
            }
        });


        LOGGER.info("Finished member resolution attempt for level {}. Resolved {} members for {} potential positions in loaded chunks.", level.dimension().location(), resolvedCount, potentialPositionsInLevel);
        if (!networksToRemove.isEmpty()) {
            markDataDirty();
        }
    }

    /**
     * Marks the NetworkSavedData as dirty, triggering a save on the next world save event.
     */
    public void markDataDirty() {
        if (this.savedData != null) {
            // Pass a snapshot of the current networks set for thread safety
            this.savedData.updateNetworkData(new HashSet<>(this.networks));
            LOGGER.trace("Notified NetworkSavedData to check for updates.");
        } else {
            LOGGER.warn("Attempted to mark data dirty, but NetworkSavedData instance is null.");
        }
    }

    // --- Network Management ---

    /**
     * Adds a network to the manager's runtime set.
     * Should be called by the Network constructor.
     * @param network The network to add.
     */
    void addNetwork(Network network) {
        if (network != null) {
            boolean added = networks.add(network);
            if(added) {
                LOGGER.debug("Added Network {} to NetworkManager.", network.getNetworkId());
                markDataDirty();
            }
        }
    }

    /**
     * Creates a new network and adds it to the manager's runtime set.
     * @return The newly created network.
     */
    private Network createNetwork() {
        Network network = new Network();
        addNetwork(network);
        return network;
    }

    /**
     * Registers a runtime INetworkMember instance with its corresponding Network object's runtime list.
     * This should primarily be called by the member itself via its setNetworkId method.
     * @param networkId The ID of the network the member belongs to.
     * @param networkMember The member instance.
     */
    public void registerINetworkMemberInNetwork(UUID networkId, INetworkMember networkMember) {
        if (networkId == null || networkMember == null) {
            LOGGER.warn("Attempted registration with null networkId or member.");
            return;
        }
        Network network = findNetwork(networkId);
        if (network != null) {
            network.addNetworkMember(networkMember); // Add to the Network's internal list
            // Don't call member.setNetworkId here - assume it's already correct or being handled by the caller.
            LOGGER.trace("Confirmed runtime registration of member at {} to network {}", networkMember.getPos().toShortString(), networkId);
        } else {
            // This can happen legitimately during world load if the Network object hasn't been processed yet,
            // but the BlockEntity loaded first. The loading logic should reconcile this.
            LOGGER.debug("Network {} not found during registration attempt for member at {}. Network might not be loaded yet.", networkId, networkMember.getPos().toShortString());
        }
    }

    @Nullable
    Network findNetwork(UUID networkId) {
        if (networkId == null) return null;
        for (Network network : networks) {
            if (network.getNetworkId().equals(networkId)) {
                return network;
            }
        }
        return null;
    }

    @Nullable
    Network findNetwork(INetworkMember networkMember) {
        return networkMember != null ? findNetwork(networkMember.getNetworkId()) : null;
    }

    /**
     * Central logic for connecting two members. Determines the resulting network,
     * updates IDs on ALL affected members (including machines attached to connectors),
     * and ensures registration.
     * @param networkMember1 First member involved in the connection (e.g., a connector).
     * @param networkMember2 Second member involved in the connection (e.g., another connector).
     * @return The UUID of the resulting network, or null on failure.
     */
    public UUID createOrMergeNetworks(INetworkMember networkMember1, INetworkMember networkMember2) {
        if (networkMember1 == null || networkMember2 == null) {
            LOGGER.error("createOrMergeNetworks called with null member!");
            return null;
        }

        UUID id1 = networkMember1.getNetworkId();
        UUID id2 = networkMember2.getNetworkId();
        Network network1 = findNetwork(id1);
        Network network2 = findNetwork(id2);

        Network resultingNetwork = null;
        UUID resultingNetworkId = null;
        Set<INetworkMember> membersToUpdateId = new HashSet<>();
        membersToUpdateId.add(networkMember1);
        membersToUpdateId.add(networkMember2);

        // --- Check for Machines attached to the connecting members ---
        INetworkMember machine1 = null;
        INetworkMember machine2 = null;
        if (networkMember1 instanceof AbstractConnectorBlockEntity connector1) {
            machine1 = connector1.findNetworkMember();
            if (machine1 != null) {
                LOGGER.trace("Connection involves member1 (connector at {}), found attached machine at {}", networkMember1.getPos().toShortString(), machine1.getPos().toShortString());
                membersToUpdateId.add(machine1);
                if(machine1.getNetworkId() != null && !Objects.equals(id1, machine1.getNetworkId())){
                    LOGGER.warn("Connector {} (ID: {}) attached machine {} (ID: {}) has different ID during connection!", networkMember1.getPos().toShortString(), id1, machine1.getPos().toShortString(), machine1.getNetworkId());
                    id1 = machine1.getNetworkId();
                    network1 = findNetwork(id1);
                } else if (id1 == null && machine1.getNetworkId() != null){
                    id1 = machine1.getNetworkId();
                    network1 = findNetwork(id1);
                    LOGGER.trace("Connector {} adopting network ID {} from attached machine {}", networkMember1.getPos().toShortString(), id1, machine1.getPos().toShortString());
                }
            }
        }
        if (networkMember2 instanceof AbstractConnectorBlockEntity connector2) {
            machine2 = connector2.findNetworkMember();
            if (machine2 != null) {
                LOGGER.trace("Connection involves member2 (connector at {}), found attached machine at {}", networkMember2.getPos().toShortString(), machine2.getPos().toShortString());
                membersToUpdateId.add(machine2);
                if(machine2.getNetworkId() != null && !Objects.equals(id2, machine2.getNetworkId())){
                    LOGGER.warn("Connector {} (ID: {}) attached machine {} (ID: {}) has different ID during connection!", networkMember2.getPos().toShortString(), id2, machine2.getPos().toShortString(), machine2.getNetworkId());
                    id2 = machine2.getNetworkId();
                    network2 = findNetwork(id2);
                } else if (id2 == null && machine2.getNetworkId() != null){
                    id2 = machine2.getNetworkId();
                    network2 = findNetwork(id2);
                    LOGGER.trace("Connector {} adopting network ID {} from attached machine {}", networkMember2.getPos().toShortString(), id2, machine2.getPos().toShortString());
                }
            }
        }
        network1 = findNetwork(id1); // Re-fetch in case ID changed
        network2 = findNetwork(id2);
        // --- End Machine Check ---

        // --- Determine Resulting Network ---
        if (network1 == null && network2 == null) {
            LOGGER.debug("Connecting involved members: Creating new network.");
            resultingNetwork = createNetwork(); // Use private create method
            resultingNetworkId = resultingNetwork.getNetworkId();
        } else if (network1 != null && network2 == null) {
            LOGGER.debug("Connecting involved members: Using existing network {}", id1);
            resultingNetwork = network1;
            resultingNetworkId = id1;
        } else if (network1 == null && network2 != null) {
            LOGGER.debug("Connecting involved members: Using existing network {}", id2);
            resultingNetwork = network2;
            resultingNetworkId = id2;
        } else { // Both potentially have networks
            if(network1 == null || network2 == null){
                LOGGER.error("Network object missing for ID {} or {}! Aborting merge.", id1, id2);
                return null;
            }
            else if (id1.equals(id2)) {
                LOGGER.debug("Connecting involved members: Already in same network {}.", id1);
                resultingNetwork = network1;
                resultingNetworkId = id1;
                membersToUpdateId.clear(); // No ID changes needed
                membersToUpdateId.add(networkMember1); // Still ensure registration of connecting members
                membersToUpdateId.add(networkMember2);
                if(machine1 != null) membersToUpdateId.add(machine1);
                if(machine2 != null) membersToUpdateId.add(machine2);
            } else {
                LOGGER.info("Merging network {} into {} for connection.", id2, id1);
                resultingNetwork = network1;
                resultingNetworkId = id1;
                membersToUpdateId.addAll(network2.getNetworkMembers()); // Add members from old network
                resultingNetwork.addAllNetworkMembers(network2.getNetworkMembers()); // Add positions and runtime refs
                network2.setInvalid();
                this.networks.remove(network2);
                LOGGER.debug("  Removed network {}", id2);
            }
        }

        // --- Finalization ---
        if (resultingNetwork != null && resultingNetworkId != null) {
            LOGGER.debug("Setting final network ID {} for {} involved members...", resultingNetworkId, membersToUpdateId.size());
            for (INetworkMember member : membersToUpdateId) {
                member.setNetworkId(resultingNetworkId); // Triggers self-registration
                resultingNetwork.addNetworkMember(member); // Ensure tracked in resulting network object
            }
            markDataDirty();
        } else {
            LOGGER.error("Resulting network or ID was null after createOrMerge! Cannot finalize connection.");
            return null;
        }
        return resultingNetworkId;
    }

    /**
     * Removes a member from its network. Ensures the member's ID is cleared.
     * Triggers network splitting checks.
     * @param networkMember The member to remove.
     */
    public void removeNetworkMember(INetworkMember networkMember) {
        if (networkMember == null) return;

        UUID networkId = networkMember.getNetworkId();
        Network network = findNetwork(networkId); // Find network using member's current ID

        if (network == null) {
            // This can happen if the network was already removed or if the member's ID was out of sync
            LOGGER.warn("Tried to remove member at {}, but its network ({}) was not found in the manager.", networkMember.getPos().toShortString(), networkId);
            // Still clear the member's ID just in case
            networkMember.setNetworkId(null);
            return;
        }

        LOGGER.debug("Removing member {} from Network {}", networkMember.getPos().toShortString(), network.getNetworkId());

        // Clean up wire connections if the member is a wire node
        // (Keep existing logic from your original removeNetworkMember method for this part)
        cleanupWireConnections(networkMember); // Encapsulate the wire removal logic


        // Remove from the Network object's tracking
        network.removeRuntimeMember(networkMember); // Remove from runtime Set
        network.removeMemberPosition(networkMember.getPos()); // Remove from saved BlockPos Set

        // Clear the ID on the BlockEntity itself
        networkMember.setNetworkId(null);

        // Check if the network is now empty or needs splitting
        if (network.getNetworkMembers().isEmpty() && network.getMemberPositions().isEmpty()) {
            LOGGER.info("Network {} is empty after removal. Removing network.", network.getNetworkId());
            network.setInvalid();
            this.networks.remove(network);
            // No need to split an empty network
        } else {
            LOGGER.debug("Checking if network {} needs splitting after member removal.", network.getNetworkId());
            splitNetworkIfDisconnected(network); // Check remaining members for connectivity
        }

        markDataDirty();
    }

    /**
     * Public method called when a connection between two members is removed
     * (e.g., by the Spool item), without the members themselves being removed.
     * This triggers a check to see if the network needs to be split.
     *
     * @param member1 The first member involved in the removed connection.
     * @param member2 The second member involved in the removed connection.
     */
    public void checkNetworkConnectivityAfterConnectionRemoval(@Nullable INetworkMember member1, @Nullable INetworkMember member2) {
        if (member1 == null || member2 == null) {
            LOGGER.warn("checkNetworkConnectivityAfterConnectionRemoval called with null member(s).");
            return;
        }

        Network network1 = findNetwork(member1);
        Network network2 = findNetwork(member2);
        Network networkToCheck = null;

        // ... (existing logic to determine networkToCheck) ...
        if (network1 != null && network1.equals(network2)) {
            networkToCheck = network1;
        } else if (network1 != null) {
            networkToCheck = network1;
            LOGGER.warn("Members {} and {} were in different networks ({}, {}) or one network was null during connection removal check. Checking network {}.",
                    member1.getPos().toShortString(), member2.getPos().toShortString(),
                    network1.getNetworkId(), // network1 is not null here
                    network2 != null ? network2.getNetworkId() : "null",
                    networkToCheck.getNetworkId());
        } else if (network2 != null) {
            networkToCheck = network2; // network1 was null
            LOGGER.warn("Member {} network was null, checking network {} for member {} after connection removal.",
                    member1.getPos().toShortString(), networkToCheck.getNetworkId(), member2.getPos().toShortString());
        }


        if (networkToCheck != null && networkToCheck.isValid()) {
            LOGGER.debug("Checking network {} for splits after connection removal between {} and {}.",
                    networkToCheck.getNetworkId(), member1.getPos().toShortString(), member2.getPos().toShortString());

            boolean splitOccurred = splitNetworkIfDisconnected(networkToCheck);

            if (!splitOccurred && networkToCheck.isValid()) {
                // If the network wasn't split (still the same network object and valid),
                // its internal connectivity has changed (a wire removed between members).
                // It needs re-simulation.
                LOGGER.debug("Network {} was not split but a connection was removed, marking dirty.", networkToCheck.getNetworkId());
                networkToCheck.markDirty(); // This will trigger requestSimulation
            }
            // If a split occurred, new networks are created and marked dirty by their constructors.
            // The original networkToCheck would have been removed/invalidated.
            // markDataDirty() is called by splitNetworkIfDisconnected if a split happens.
            // If no split, the set of networks and their member lists haven't changed, so no specific save is needed for NetworkSavedData here.
        } else {
            LOGGER.warn("Could not find a valid network to check for splits after connection removal between {} and {}.",
                    member1.getPos().toShortString(), member2.getPos().toShortString());
        }
    }

    /**
     * Splits a network into multiple networks if it becomes disconnected.
     * This happens when a member is removed and the network is no longer a single connected graph.
     * @param originalNetwork The network to check for disconnection.
     */
    private boolean splitNetworkIfDisconnected(Network originalNetwork) { // Changed to return boolean
        Set<INetworkMember> allMembers = new HashSet<>(originalNetwork.getNetworkMembers()); // Use a copy
        if (allMembers.size() <= 1 && !allMembers.isEmpty()) { // Optimization: single member cannot be disconnected from itself
            // LOGGER.trace("Network {} has only one or zero members, no split check needed beyond emptiness.", originalNetwork.getNetworkId());
            return false;
        }
        if (allMembers.isEmpty()){ // An empty network doesn't split. It might get removed by caller.
            // LOGGER.trace("Network {} is empty, cannot split.", originalNetwork.getNetworkId());
            return false;
        }


        Set<BlockPos> visited = new HashSet<>();
        List<Set<INetworkMember>> connectedGroups = new ArrayList<>();

        for (INetworkMember member : allMembers) {
            if (visited.contains(member.getPos())) continue;

            Set<INetworkMember> group = new HashSet<>();
            Queue<INetworkMember> queue = new LinkedList<>();
            queue.add(member);
            // visited.add(member.getPos()); // Add to visited when adding to queue to avoid re-processing

            while (!queue.isEmpty()) {
                INetworkMember current = queue.poll();
                BlockPos currentPos = current.getPos();

                // Re-check visited here in case added by parallel path in complex graph, though BFS should handle.
                if (!visited.add(currentPos)) continue;
                group.add(current);

                // Iterate over a snapshot of potential neighbors to avoid CME if allMembers could change
                // (though in this context, allMembers is a local copy from originalNetwork.getNetworkMembers())
                for (INetworkMember potentialNeighbor : allMembers) { // allMembers is already a copy
                    if (!visited.contains(potentialNeighbor.getPos()) && hasConnection(current, potentialNeighbor)) {
                        // if (!queue.contains(potentialNeighbor)) // Queue.contains is O(N), avoid if possible
                        queue.add(potentialNeighbor);
                        // visited.add(potentialNeighbor.getPos()); // Add here too
                    }
                }
            }

            if (!group.isEmpty()) {
                connectedGroups.add(group);
            }
        }

        if (connectedGroups.size() <= 1) {
            // LOGGER.trace("Network {} does not need splitting ({} group(s) found).", originalNetwork.getNetworkId(), connectedGroups.size());
            return false; // No split needed
        }

        LOGGER.info("Splitting network {} into {} separate groups.", originalNetwork.getNetworkId(), connectedGroups.size());

        this.networks.remove(originalNetwork);
        originalNetwork.setInvalid(); // Mark as invalid

        for (Set<INetworkMember> group : connectedGroups) {
            Network newNetwork = createNetwork(); // createNetwork adds to this.networks and its constructor marks it dirty
            for(INetworkMember networkMemberInGroup : group) {
                // newNetwork.addNetworkMember(networkMemberInGroup); // This will markDirty
                networkMemberInGroup.setNetworkId(newNetwork.getNetworkId()); // This updates BE and registers with NM
                // which in turn calls newNetwork.addNetworkMember
            }
            LOGGER.info("  Created new network {} with {} members after split.", newNetwork.getNetworkId(), group.size());
        }

        markDataDirty(); // Network set changed, save required
        return true; // Split occurred
    }

    private void cleanupWireConnections(INetworkMember networkMember) {
        if (!(networkMember instanceof IWireNode wireNodeMember) || this.server == null) {
            return;
        }

        ServerLevel level = this.server.overworld(); // Or get level relevant to the member
        if (level == null) {
            LOGGER.error("Cannot clean wire connections for {}: ServerLevel is null.", networkMember.getPos());
            return;
        }

        LOGGER.trace("Cleaning connections for removed wire node at {}", networkMember.getPos());
        boolean connectionsRemoved = false;
        for (int i = 0; i < wireNodeMember.getConnectionPointCount(); i++) {
            ConnectionPoint connectionPoint = wireNodeMember.getConnectionPoint(i); // Use getter
            if (connectionPoint != null) { // Check if connection exists at this index
                connectionsRemoved = true; // Mark that we found at least one connection
                BlockPos otherPos = connectionPoint.getPos();
                WireType wireType = connectionPoint.getWireType();

                // Notify the other connected node (if it exists)
                BlockEntity otherBE = level.isLoaded(otherPos) ? level.getBlockEntity(otherPos) : null;
                if (otherBE instanceof IWireNode otherNode) {
                    ConnectionPoint otherConnectionPoint = otherNode.getConnectionPointIn(networkMember.getPos());
                    if (otherConnectionPoint != null) {
                        otherNode.removeConnectionPoint(otherConnectionPoint.getConnectionPointIndex(), false); // Don't drop wire twice
                        LOGGER.trace("  Notified node at {} to remove its connection point index {}", otherPos.toShortString(), otherConnectionPoint.getConnectionPointIndex());
                    } else {
                        LOGGER.warn("  Could not find return connection point on node at {} pointing back to {}", otherPos.toShortString(), networkMember.getPos().toShortString());
                    }
                } else {
                    LOGGER.warn("  Could not find IWireNode at connected position {} to notify.", otherPos.toShortString());
                }

                // Drop the wire item
                if (wireType != null && !level.isClientSide()) {
                    Vec3 dropPos = Vec3.atCenterOf(networkMember.getPos()).add(Vec3.atCenterOf(otherPos)).scale(0.5);
                    ItemStack droppedWire = wireType.getSourceDrop();
                    level.addFreshEntity(new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, droppedWire));
                    LOGGER.trace("  Dropped wire item of type {}", wireType.name());
                }

                // Remove the connection from the member being removed *after* processing
                // wireNodeMember.removeConnectionPoint(i, false); // Let the caller handle the final state / removal of the BE itself
            }
        }
        if(connectionsRemoved){
            // Force update on the removed block *before* it's fully gone if needed
            level.sendBlockUpdated(networkMember.getPos(), level.getBlockState(networkMember.getPos()), level.getBlockState(networkMember.getPos()), 3);
        }
    }

    // --- Ticking ---

    /**
     * Ticks all valid networks. Called from a server tick event handler.
     * @see Network#tick()
     */
    public void tick() {
        if (networks.isEmpty()) return;
        // Create a snapshot for iteration if networks can be modified during tick() by other means
        // Set<Network> currentNetworks = new HashSet<>(this.networks);
        for (Network network : networks) { // Iterating directly on ConcurrentHashMap.newKeySet() is generally safe for reads
            if (network.isValid()) {
                network.tick(); // Network.tick() currently does nothing.
            }
        }
    }

    public void onServerTick(MinecraftServer serverInstance) {
        if (this.server == null) {
            this.server = serverInstance;
            if (this.server != null) {
                // LOGGER.info("NetworkManager server instance initialized via onServerTick.");
            }
        }
    }

    // --- Simulation Requesting ---
    public void requestSimulation(Network network) {
        if (this.server == null) {
            LOGGER.error("Server is null, cannot schedule simulation for network {}. Network remains dirty.", network.getNetworkId());
            // Network will remain dirty. If requestSimulation is called again when server is available, it will proceed.
            return;
        }
        if (network == null || !network.isValid()) {
            // LOGGER.trace("Simulation request for null or invalid network {} - ignoring.", network != null ? network.getNetworkId() : "null");
            return;
        }

        // The currentlySimulating.add check prevents flooding the server.execute queue
        // with redundant tasks for the same network if markDirty is called rapidly.
        if (currentlySimulating.add(network.getNetworkId())) {
            LOGGER.debug("Scheduling simulation for network {} (was not already simulating/queued).", network.getNetworkId());
            this.server.execute(() -> {
                // Re-check network validity and dirty status *inside* the server thread execution,
                // as state might have changed between the request and actual execution.
                // Also check if members exist, as an empty network shouldn't simulate.
                if (network.isValid() && !network.getNetworkMembers().isEmpty() && network.isDirtyAndReset()) {
                    // The network was dirty and is now reset. Proceed with simulation.
                    initiateSimulationForNetwork(network); // This method handles removing from currentlySimulating on completion/error
                } else {
                    // Network is no longer dirty, or became invalid/empty.
                    // No simulation needed for this scheduled task.
                    LOGGER.trace("Simulation for network {} cancelled or not needed upon execution (not dirty, invalid, or empty).", network.getNetworkId());
                    currentlySimulating.remove(network.getNetworkId()); // Release the lock
                }
            });
        } else {
            // Another simulation for this network is already running or scheduled via server.execute.
            // The network remains dirty (because isDirtyAndReset() wasn't called for *this* path).
            // If the ongoing/scheduled simulation completes, and the network is *still* dirty
            // (e.g., due to changes that occurred *after* the previous isDirtyAndReset but *before* this call),
            // a new requestSimulation call (triggered by a subsequent markDirty) will eventually pick it up.
            LOGGER.trace("Network {} is dirty, but a simulation is already in progress or scheduled. It remains dirty.", network.getNetworkId());
        }
    }

    // --- Utility and Debug

    /**
     * Prints the IDs of all currently managed networks to the log.
     * @see Network#getNetworkId()
     */
    public void getNetworkIds() {
        LOGGER.info("Listing all managed networks ({} total):", networks.size());
        for (Network network : networks) {
            LOGGER.info("  - UUID: {}, Valid: {}, Runtime Members: {}, Saved Positions: {}",
                    network.getNetworkId(),
                    network.isValid(),
                    network.getNetworkMembers().size(),
                    network.getMemberPositions().size()
            );
        }
    }

    /**
     * Prints details of members within a specific network.
     * @param networkId The UUID of the network to inspect.
     * @see Network
     */
    public void printMembersInNetwork(UUID networkId) {
        Network network = findNetwork(networkId);
        if (network == null) {
            LOGGER.info("Network with UUID {} not found.", networkId);
            return;
        }

        Set<INetworkMember> runtimeMembers = network.getNetworkMembers();
        Set<BlockPos> savedPositions = network.getMemberPositions();

        LOGGER.info("Details for Network UUID: {}", networkId);
        LOGGER.info("  Saved Positions ({}):", savedPositions.size());
        savedPositions.forEach(pos -> LOGGER.info("    - {}", pos.toShortString()));

        LOGGER.info("  Runtime Members ({}):", runtimeMembers.size());
        if (runtimeMembers.isEmpty() && !savedPositions.isEmpty()) {
            LOGGER.info("    (Runtime members may not be loaded yet)");
        }
        runtimeMembers.forEach(member -> LOGGER.info("    - BE at {}", member.getPos().toShortString()));
    }

    /**
     * Gets the network data formatted for client-side highlighting.
     * @see Network
     * @return Map of Network UUID to List of member BlockPos.
     */
    public Map<UUID, List<BlockPos>> getNetworksDataForClient() {
        Map<UUID, List<BlockPos>> data = new HashMap<>();
        for (Network network : networks) {
            if (network.isValid()) {
                List<BlockPos> blockPositions = new ArrayList<>(network.getMemberPositions());
                if (!blockPositions.isEmpty()) {
                    data.put(network.getNetworkId(), blockPositions);
                }
            }
        }
        return data;
    }

    /**
     * Provides access to the SavedData instance
     * @return The NetworkSavedData instance, or null if not loaded.
     * @see NetworkSavedData
     */
    @Nullable
    public NetworkSavedData getSavedData() {
        return savedData;
    }

    /**
     * Checks if there is networkMember1 direct connection between two network members.
     *
     * @param networkMember1 The first network member.
     * @param networkMember2 The second network member.
     * @return True if there is networkMember1 direct connection, false otherwise.
     */
    private boolean hasConnection(INetworkMember networkMember1, INetworkMember networkMember2) {
        if (networkMember1 instanceof AbstractConnectorBlockEntity connectorA && networkMember2 instanceof AbstractMachineBlockEntity machineB) {
            if (connectorA.findNetworkMember() == machineB) return true;
        }
        if (networkMember2 instanceof AbstractConnectorBlockEntity connectorB && networkMember1 instanceof AbstractMachineBlockEntity machineA) {
            if (connectorB.findNetworkMember() == machineA) return true;
        }
        if (networkMember1 instanceof IWireNode wireA && networkMember2 instanceof IWireNode wireB) {
            return wireA.hasConnectionTo(networkMember2.getPos());
        }
        return false;
    }

    private void applyZeroPowerToNetwork(Network network, ServerLevel level, Set<INetworkMember> membersToProcessOriginal) {
        if (network == null || level == null ) return;

        Set<BlockPos> positionsToProcess = new HashSet<>();
        if (membersToProcessOriginal != null) { // If a specific set was given
            membersToProcessOriginal.forEach(m -> positionsToProcess.add(m.getPos()));
        } else { // Fallback to all members of the network
            network.getNetworkMembers().forEach(m -> positionsToProcess.add(m.getPos()));
        }

        for (BlockPos memberPos : positionsToProcess) {
            if (level.isLoaded(memberPos)) {
                BlockEntity be = level.getBlockEntity(memberPos);
                if (be instanceof ISimulatable simulatableMember && be instanceof INetworkMember currentMember) {
                    // Optional: Check if currentMember is still part of 'network'
                    if (network.getNetworkMembers().contains(currentMember)) {
                        simulatableMember.applySimulation(0.0, 0.0);
                    } else {
                        LOGGER.warn("Attempted to apply zero power to BE at {}, but it's no longer in network {}. Type: {}", memberPos, network.getNetworkId(), be.getClass().getSimpleName());
                    }
                }
            }
        }
        LOGGER.debug("Applied zero power to members of network {} (or attempted).", network.getNetworkId());
    }
    // --- Simulation ---
    private void initiateSimulationForNetwork(final Network network) { // network should be effectively final
        if (this.server == null) {
            LOGGER.error("Server instance is null in initiateSimulationForNetwork (should not happen if onServerTick checks). Aborting for {}.", network.getNetworkId());
            currentlySimulating.remove(network.getNetworkId()); // Clean up
            network.markDirty(); // Ensure it's tried again
            return;
        }

        final Set<INetworkMember> membersSnapshot = new HashSet<>(network.getNetworkMembers()); // Use a snapshot
        if (membersSnapshot.isEmpty()) {
            LOGGER.debug("Network {} has no members at simulation initiation. Skipping.", network.getNetworkId());
            currentlySimulating.remove(network.getNetworkId());
            network.setLastSimulationTime(System.currentTimeMillis()); // Mark as "simulated"
            return;
        }

        // Determine ServerLevel robustly
        ServerLevel level = null;
        INetworkMember firstMember = membersSnapshot.iterator().next();
        if (firstMember instanceof BlockEntity be) {
            if (be.getLevel() instanceof ServerLevel memberLevel && memberLevel.getServer() == this.server) {
                level = memberLevel;
            }
        }
        if (level == null) { // Fallback if first member didn't work out
            for (INetworkMember member : membersSnapshot) {
                if (member instanceof BlockEntity be) {
                    if (be.getLevel() instanceof ServerLevel sl && sl.getServer() == this.server) {
                        level = sl; break;
                    }
                }
            }
        }
        if (level == null) level = this.server.overworld(); // Final fallback

        final ServerLevel finalLevel = level;
        final NetlistBuilder.BuildResult buildResult;

        try {
            NetworkGraphBuilder graphBuilder = new NetworkGraphBuilder();
            Map<INetworkMember, List<ConnectionInfo>> adjList = graphBuilder.buildAdjacencyList(membersSnapshot, finalLevel);

            NetworkCircuitChecker circuitChecker = new NetworkCircuitChecker();
            if (!circuitChecker.isClosedCircuit(membersSnapshot, adjList)) {
                LOGGER.info("Network {} is an open circuit. Applying zero power.", network.getNetworkId());
                this.server.execute(() -> applyZeroPowerToNetwork(network, finalLevel, membersSnapshot));
                currentlySimulating.remove(network.getNetworkId());
                network.setLastSimulationTime(System.currentTimeMillis());
                return;
            }
            buildResult = new NetlistBuilder().buildNetlist(adjList);
        } catch (Exception e) {
            LOGGER.error("Failed to build netlist or check circuit for network {}: {}", network.getNetworkId(), e.getMessage(), e);
            currentlySimulating.remove(network.getNetworkId());
            network.markDirty(); // Re-mark for a retry
            return;
        }

        String spiceNetlist = buildResult.netlist();
        final String finalSpiceNetlist = spiceNetlist; // Make effectively final for lambda
        LOGGER.info("Attempting ASYNC simulation (TESTING WITH 'run' COMMAND) for network {}. Full Netlist:\n{}", network.getNetworkId(), finalSpiceNetlist);
        try {
            // TEMPORARILY USE THE DIAGNOSTIC METHOD
            NgSpiceSimulator.instance().simulateAsync(finalSpiceNetlist,
                    simData -> { // OnResult Consumer
                        this.server.execute(() -> {
                            try {
                                if (!network.isValid() || network.getNetworkMembers().isEmpty()) {
                                    LOGGER.info("Network {} became invalid/empty while (TEST 'run') sim was running. Discarding.", network.getNetworkId());
                                    return;
                                }
                                LOGGER.info("Async (TEST 'run') sim for {} COMPLETED successfully. Applying results.", network.getNetworkId());
                                SimulationDataApplier.apply(simData, buildResult, finalLevel, null, membersSnapshot);
                                network.setLastSimulationTime(System.currentTimeMillis());
                            } catch (Exception e) {
                                LOGGER.error("Error applying (TEST 'run') sim data for network {}: {}. Original Netlist:\n{}", network.getNetworkId(), e.getMessage(), finalSpiceNetlist, e);
                                if (network.isValid()) network.markDirty();
                            } finally {
                                currentlySimulating.remove(network.getNetworkId());
                            }
                        });
                    },
                    exception -> { // OnError Consumer
                        this.server.execute(() -> {
                            try {
                                LOGGER.error("Async (TEST 'run') simulation FAILED for network {}: {}. Original Netlist:\n{}", network.getNetworkId(), exception.getMessage(), finalSpiceNetlist, exception);
                                if (network.isValid()) {
                                    applyZeroPowerToNetwork(network, finalLevel, membersSnapshot);
                                }
                                network.setLastSimulationTime(System.currentTimeMillis());
                            } catch (Exception e_handler) {
                                LOGGER.error("Error during (TEST 'run') simulation error handling for network {}: {}", network.getNetworkId(), e_handler.getMessage(), e_handler);
                            } finally {
                                currentlySimulating.remove(network.getNetworkId());
                            }
                        });
                    }
            );
        } catch (IOException e) { // Catch IOException from NgSpiceSimulator.instance()
            LOGGER.error("IOException obtaining/starting (TEST 'run') NgSpiceSimulator for network {}: {}. Netlist:\n{}", network.getNetworkId(), e.getMessage(), finalSpiceNetlist, e);
            currentlySimulating.remove(network.getNetworkId());
            if (network.isValid()) network.markDirty();
        }
    }
}
