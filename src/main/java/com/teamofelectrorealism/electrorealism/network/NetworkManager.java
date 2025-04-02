package com.teamofelectrorealism.electrorealism.network;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.block.connector.AbstractConnectorBlockEntity;
import com.teamofelectrorealism.electrorealism.block.machine.AbstractMachineBlockEntity;
import com.teamofelectrorealism.electrorealism.power.ConnectionPoint;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import com.teamofelectrorealism.electrorealism.power.IWireNode;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Manages the creation, loading, saving, and merging of electrical networks.
 */
public class NetworkManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private Set<Network> networks;
    @Nullable
    private NetworkSavedData savedData = null;
    @Nullable
    private MinecraftServer server = null;

    /**
     * Constructs a new NetworkManager.
     */
    public NetworkManager() {
    }

    // --- Loading and Saving ---

    /**
     * Called when a level (specifically the Overworld for global data) loads.
     * Initializes or loads the network data from storage.
     * @param level The level that loaded (should be the Overworld).
     */
    public void levelLoaded(LevelAccessor level) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
            LOGGER.warn("NetworkManager levelLoaded called with non-ServerLevel or null server.");
            return;
        }
        if (this.server != null && this.server == serverLevel.getServer() && this.savedData != null) {
            LOGGER.debug("NetworkManager already initialized for this server instance.");
            return;
        }

        this.server = serverLevel.getServer();
        LOGGER.info("NetworkManager initializing for server...");

        if (serverLevel == this.server.overworld()) {
            loadNetworkData(this.server);
            loadNetworkData(serverLevel);
        } else {
            LOGGER.debug("NetworkManager levelLoaded called for non-Overworld dimension, skipping load.");
        }
    }

    /**
     * Loads network data from the NetworkSavedData instance.
     * Populates the runtime 'networks' set.
     * @param server The MinecraftServer instance.
     */
    private void loadNetworkData(MinecraftServer server) {
        if (this.savedData != null) {
            LOGGER.warn("Attempting to load network data when savedData is already loaded.");
            return;
        }
        try {
            this.savedData = NetworkSavedData.load(server);
            this.networks = this.savedData.getNetworks();
            LOGGER.info("NetworkManager loaded {} networks from NetworkSavedData.", this.networks.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load NetworkSavedData!", e);
            this.networks = new HashSet<>();
            this.savedData = null;
        }
    }

    /**
     * Loads network data from the NetworkSavedData instance.
     * Populates the runtime 'networks' set.
     * @param level The ServerLevel instance.
     */
    private void loadNetworkData(ServerLevel level) {
        if (this.networks.isEmpty()) {
            LOGGER.info("No networks loaded, skipping member resolution.");
            return;
        }
        LOGGER.info("Attempting to resolve BlockPos to INetworkMember for loaded networks...");
        int resolvedCount = 0;
        int totalPositions = 0;

        for (Network network : this.networks) {
            Set<BlockPos> positions = network.getMemberPositions();
            totalPositions += positions.size();
            for (BlockPos pos : positions) {
                if (level.isLoaded(pos)) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof INetworkMember member) {
                        UUID memberNetworkId = member.getNetworkId();
                        if (memberNetworkId == null || memberNetworkId.equals(network.getNetworkId())) {
                            if (memberNetworkId == null) {
                                member.setNetworkId(network.getNetworkId());
                            }
                            network.addRuntimeMember(member);
                            resolvedCount++;
                        } else {
                            LOGGER.warn("INetworkMember at {} reports network {} but loaded data expects network {}. This might indicate stale data or merge issues.",
                                    pos, memberNetworkId, network.getNetworkId());
                            // Decide how to handle: remove from this network's positions? Force update BE? Log only?
                        }
                    } else {
                        LOGGER.debug("No valid INetworkMember found at loaded position {}. Block might have been removed.", pos);
                        // Consider removing 'pos' from network.getMemberPositions() here
                    }
                } else {
                    LOGGER.trace("Chunk not loaded at {}, skipping member resolution for now.", pos);
                    // Need chunk loading integration for full robustness
                }
            }
        }
        LOGGER.info("Finished member resolution attempt. Resolved {} out of {} total positions.", resolvedCount, totalPositions);
    }

    /**
     * Marks the NetworkSavedData as dirty, triggering a save on the next world save event.
     */
    public void markDataDirty() {
        if (this.savedData != null) {
            this.savedData.updateNetworkData(this.networks);
            LOGGER.debug("Notified NetworkSavedData to check for updates.");
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
     * Creates a new Network instance. The Network constructor handles adding itself
     * to the manager via `addNetwork`.
     * @return The UUID of the newly created network.
     * @see Network#Network()
     */
    public UUID createNetwork() {
        Network network = new Network();
        LOGGER.info("Total networks: {}", networks.size());
        LOGGER.info("Initiated creation of new network with UUID: {}", network.getNetworkId());
        return network.getNetworkId();
    }

    /**
     * Registers a runtime INetworkMember instance with its corresponding Network.
     * Should be called when a member BlockEntity is loaded or placed.
     * @param networkId The ID of the network the member belongs to.
     * @param networkMember The member instance.
     * @see INetworkMember
     */
    public void registerINetworkMemberInNetwork(UUID networkId, INetworkMember networkMember) {
        Network network = findNetwork(networkId);
        if (network != null && networkMember != null) {
            network.addRuntimeMember(networkMember);
            LOGGER.debug("Registered runtime member at {} to network {}", networkMember.getPos(), networkId);
        } else if (networkMember != null) {
            LOGGER.warn("Attempted to register member at {} to non-existent network {}", networkMember.getPos(), networkId);
        }
    }

    /**
     * Finds a Network instance by its INetworkMember in the runtime set.
     * @param networkMember The INetworkMember to search for.
     * @return The found Network, or null if not found.
     */
    @Nullable
    public Network findNetwork(INetworkMember networkMember) {
        return findNetwork(networkMember.getNetworkId());
    }

    /**
     * Finds a Network instance by its UUID in the runtime set.
     * @param networkId The UUID to search for.
     * @return The found Network, or null if not found.
     * @see Network
     */
    @Nullable
    private Network findNetwork(UUID networkId) {
        if (networkId == null) return null;
        return networks.stream()
                .filter(n -> n.getNetworkId().equals(networkId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Determines the correct network ID when connecting two members, creating or merging networks as needed.
     * @param networkMember1 First member being connected.
     * @param networkMember2 Second member being connected.
     * @return The UUID of the resulting network.
     * @see INetworkMember
     */
    public UUID createOrMergeNetworks(INetworkMember networkMember1, INetworkMember networkMember2) {
        UUID networkId1 = networkMember1.getNetworkId();
        UUID networkId2 = networkMember2.getNetworkId();
        UUID resultingNetworkId;

        if (networkId1 == null && networkId2 == null) {
            resultingNetworkId = createNetwork();
            LOGGER.info("Connecting two members with no existing networks. Created new network: {}", resultingNetworkId);

            Network newNetwork = findNetwork(resultingNetworkId);
            if (newNetwork != null) {
                LOGGER.debug("Adding initial members ({}, {}) to new network {}",
                        networkMember1.getPos().toShortString(),
                        networkMember2.getPos().toShortString(),
                        resultingNetworkId);

                newNetwork.addMemberPosition(networkMember1.getPos());
                newNetwork.addMemberPosition(networkMember2.getPos());

                newNetwork.addRuntimeMember(networkMember1);
                newNetwork.addRuntimeMember(networkMember2);

                markDataDirty();
            } else {
                LOGGER.error("Newly created network {} could not be found immediately!", resultingNetworkId);
            }

        } else if (networkId1 != null && networkId2 == null) {
            resultingNetworkId = networkId1;
            LOGGER.info("Connecting member {} without network to existing network: {}", networkMember2.getPos().toShortString(), resultingNetworkId);
            Network network1 = findNetwork(networkId1);
            if (network1 != null) {
                network1.addRuntimeMember(networkMember2);
                markDataDirty();
            } else {
                LOGGER.error("Network {} not found during connection, but member {} reported it!", networkId1, networkMember1.getPos());
            }
        } else if (networkId1 == null && networkId2 != null) {
            // Only node2 has a network, use it (Symmetrical to above)
            resultingNetworkId = networkId2;
            LOGGER.info("Connecting member {} without network to existing network: {}", networkMember1.getPos().toShortString(), resultingNetworkId);
            Network network2 = findNetwork(networkId2);
            if (network2 != null) {
                network2.addRuntimeMember(networkMember1);
                markDataDirty();
            } else {
                LOGGER.error("Network {} not found during connection, but member {} reported it!", networkId2, networkMember2.getPos());
            }
        } else {
            if (networkId1.equals(networkId2)) {
                resultingNetworkId = networkId1;
                LOGGER.debug("Connecting two members already in the same network: {}", resultingNetworkId);
                Network network = findNetwork(resultingNetworkId);
                if (network != null) {
                    network.addRuntimeMember(networkMember1);
                    network.addRuntimeMember(networkMember2);
                }
            } else {
                LOGGER.info("Connecting members from different networks ({} and {}). Merging...", networkId1, networkId2);
                resultingNetworkId = mergeNetworks(networkId1, networkId2);
            }
        }

        if (resultingNetworkId != null) {
            if (!resultingNetworkId.equals(networkMember1.getNetworkId())) {
                networkMember1.setNetworkId(resultingNetworkId);
            }
            if (!resultingNetworkId.equals(networkMember2.getNetworkId())) {
                networkMember2.setNetworkId(resultingNetworkId);
            }
        } else {
            LOGGER.error("Resulting network ID was null after createOrMerge! Member1: {}, Member2: {}", networkMember1.getPos().toShortString(), networkMember2.getPos().toShortString());
        }

        return resultingNetworkId;
    }

    /**
     * Merges network2 into network1. Network1 becomes the dominant network.
     * Updates member IDs and marks data as dirty.
     * @param networkId1 The ID of the network to merge into.
     * @param networkId2 The ID of the network to be merged and removed.
     * @return The UUID of the merged network (networkId1).
     * @see Network
     */
    private UUID mergeNetworks(UUID networkId1, UUID networkId2) {
        Network network1 = findNetwork(networkId1);
        Network network2 = findNetwork(networkId2);

        if (network1 == null && network2 == null) {
            LOGGER.error("Attempted to merge two non-existent networks: {} and {}", networkId1, networkId2);
            return createNetwork();
        } else if (network1 == null) {
            LOGGER.warn("Network {} to merge into not found. Using Network {} as primary.", networkId1, networkId2);
            markDataDirty();
            return networkId2;
        } else if (network2 == null) {
            LOGGER.warn("Network {} to be merged not found. Keeping Network {} as is.", networkId2, networkId1);
            markDataDirty();
            return networkId1;
        }

        LOGGER.info("Merging Network {} into Network {}", networkId2, networkId1);
        network1.mergeMembersFrom(network2);
        network2.setInvalid();
        networks.remove(network2);

        updateNetworkIdsOnBlockEntities(networkId1, network2.getMemberPositions());

        markDataDirty();
        return networkId1;
    }

    /**
     * Helper to update the networkId field on BlockEntities at given positions.
     * Requires access to the level.
     * @param networkId The new network ID to set.
     * @param memberPositions Positions of members whose ID needs updating.
     * @see INetworkMember
     */
    private void updateNetworkIdsOnBlockEntities(UUID networkId, Set<BlockPos> memberPositions) {
        if (server == null) {
            LOGGER.error("Cannot update BE network IDs: Server instance is null.");
            return;
        }
        ServerLevel level = server.overworld();
        if (level == null) {
            LOGGER.error("Cannot update BE network IDs: Overworld instance is null.");
            return;
        }

        for (BlockPos pos : memberPositions) {
            if (level.isLoaded(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof INetworkMember member) {
                    if (!networkId.equals(member.getNetworkId())) {
                        member.setNetworkId(networkId);
                        LOGGER.debug("Updated Network ID for member at {} to {}", pos, networkId);
                    }
                }
            } else {
                LOGGER.trace("Cannot update BE Network ID at {}: Chunk not loaded.", pos);
            }
        }
    }

    /**
     * Removes a connector from a network.
     * Removes the connector from the runtime members and the member positions.
     * @param networkId The ID of the network to remove the connector from.
     * @param connectorBlockEntity The connector to remove.
     * @param pos The position of the connector.
     */
    public void removeConnectorFromNetwork(UUID networkId, AbstractConnectorBlockEntity connectorBlockEntity, BlockPos pos) {
        Network network = findNetwork(networkId);
        if (network == null) {
            LOGGER.error("Tried removing connector, but network was null with UUID: {}", networkId);
            return;
        }
        network.removeRuntimeMember(connectorBlockEntity);
        network.removeMemberPosition(pos);
        markDataDirty();
    }

    /**
     * Removes a member from its network, cleaning up connections and potentially splitting the network.
     * @param networkMember The member to remove.
     * @see INetworkMember
     */
    public void removeNetworkMember(INetworkMember networkMember) {
        Network network = findNetwork(networkMember);
        if (network == null) {
            LOGGER.warn("Tried to remove member at {}, but no matching network found (or member had no network ID).", networkMember.getPos());
            return;
        } else {
            LOGGER.warn("Found member {} in network {} by position lookup.", networkMember.getPos(), network.getNetworkId());
        }

        if (networkMember instanceof IWireNode wireNodeMember && this.server != null) {
            ServerLevel level = this.server.overworld();
            if (level != null) {
                LOGGER.debug("Cleaning connections for removed member {} in network {}", networkMember.getPos(), network.getNetworkId());
                for (int i = 0; i < wireNodeMember.getConnectionPointCount(); i++) {
                    if (wireNodeMember.hasConnection(i)) {
                        ConnectionPoint connectionPoint = wireNodeMember.getConnectionPoint(i);
                        if(connectionPoint == null) continue;

                        BlockPos otherPos = connectionPoint.getPos();
                        WireType wireType = connectionPoint.getWireType();

                        BlockEntity otherBE = level.getBlockEntity(otherPos);
                        if (otherBE instanceof IWireNode otherNode) {
                            ConnectionPoint otherConnectionPoint = otherNode.getConnectionPointIn(networkMember.getPos());
                            if (otherConnectionPoint != null) {
                                int otherNodeIndex = otherConnectionPoint.getConnectionPointIndex();
                                otherNode.removeConnectionPoint(otherNodeIndex, false);
                                LOGGER.trace("  - Notified node at {} to remove connection point index {}", otherPos, otherNodeIndex);
                            } else {
                                LOGGER.warn("  - Could not find return connection point on node at {} pointing to {}", otherPos, networkMember.getPos());
                            }
                        } else {
                            LOGGER.warn("  - Could not find IWireNode at connected position {}", otherPos);
                        }

                        if (wireType != null && !level.isClientSide()) {
                            Vec3 dropPos = Vec3.atCenterOf(networkMember.getPos()).add(Vec3.atCenterOf(otherPos)).scale(0.5);
                            ItemStack droppedWire = wireType.getSourceDrop();
                            level.addFreshEntity(new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, droppedWire));
                            LOGGER.trace("  - Dropped wire item of type {} at midpoint", wireType.name());
                        }

                        wireNodeMember.removeConnectionPoint(i, false);
                        LOGGER.trace("  - Cleared local connection point index {}", i);
                    }
                }
            } else {
                LOGGER.error("Cannot clean connections for member {}: ServerLevel is null.", networkMember.getPos());
            }
        }

        network.removeNetworkMember(networkMember);
        LOGGER.debug("Removed member {} from Network {} internal lists.", networkMember.getPos(), network.getNetworkId());

        if (network.getNetworkMembers().isEmpty()) {
            LOGGER.info("Network {} is empty after removal. Removing network.", network.getNetworkId());
            network.setInvalid();
            networks.remove(network);
            markDataDirty();
        } else {
            LOGGER.debug("Checking if network {} needs splitting after member removal.", network.getNetworkId());
            splitNetworkIfDisconnected(network);
            markDataDirty();
        }
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
        if (network1 != null && network1.equals(network2)) {
            networkToCheck = network1;
        } else if (network1 != null) {
            networkToCheck = network1;
            LOGGER.warn("Members {} and {} were in different networks ({}, {}) or one network was null during connection removal check. Checking network {}.",
                    member1.getPos().toShortString(), member2.getPos().toShortString(),
                    network1 != null ? network1.getNetworkId() : "null",
                    network2 != null ? network2.getNetworkId() : "null",
                    networkToCheck.getNetworkId());
        } else if (network2 != null) {
            networkToCheck = network2;
            LOGGER.warn("Member {} network was null, checking network {} for member {} after connection removal.",
                    member1.getPos().toShortString(), networkToCheck.getNetworkId(), member2.getPos().toShortString());
        }

        if (networkToCheck != null && networkToCheck.isValid()) {
            LOGGER.debug("Checking network {} for splits after connection removal between {} and {}.",
                    networkToCheck.getNetworkId(), member1.getPos().toShortString(), member2.getPos().toShortString());
            splitNetworkIfDisconnected(networkToCheck);
            markDataDirty();
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
    private void splitNetworkIfDisconnected(Network originalNetwork) {
        Set<INetworkMember> allMembers = originalNetwork.getNetworkMembers();
        Set<BlockPos> visited = new HashSet<>();
        List<Set<INetworkMember>> connectedGroups = new ArrayList<>();

        for (INetworkMember member : allMembers) {
            if (visited.contains(member.getPos())) continue;

            Set<INetworkMember> group = new HashSet<>();
            Queue<INetworkMember> queue = new LinkedList<>();
            queue.add(member);

            while (!queue.isEmpty()) {
                INetworkMember current = queue.poll();
                BlockPos currentPos = current.getPos();

                if (!visited.add(currentPos)) continue;
                group.add(current);

                for (INetworkMember potentialNeighbor : allMembers) {
                    if (!visited.contains(potentialNeighbor.getPos()) && hasConnection(current, potentialNeighbor)) {
                        queue.add(potentialNeighbor);
                    }
                }
            }

            if (!group.isEmpty()) {
                connectedGroups.add(group);
            }
        }

        if (connectedGroups.size() <= 1) return;

        networks.remove(originalNetwork);
        originalNetwork.setInvalid();

        for (Set<INetworkMember> group : connectedGroups) {
            Network newNetwork = new Network();
            newNetwork.registerAllNetworkMembers(group);
            updateNetworkIds(newNetwork.getNetworkId(), group);
            networks.add(newNetwork);
            LOGGER.info("Created new network {} with {} members after split", newNetwork.getNetworkId(), group.size());
        }

        if (savedData != null) savedData.setDirty();
    }

    private void updateNetworkIds(UUID networkId, Set<INetworkMember> members) {
        for (INetworkMember member : members) {
            member.setNetworkId(networkId);
        }
    }

    // --- Ticking ---

    /**
     * Ticks all valid networks. Called from a server tick event handler.
     * @see Network#tick()
     */
    public void tick() {
        if (networks.isEmpty()) return;
        for (Network network : networks) {
            if (network.isValid()) {
                network.tick();
            }
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
}
