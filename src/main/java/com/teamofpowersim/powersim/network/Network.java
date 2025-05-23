package com.teamofpowersim.powersim.network;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.PowerSim;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class Network {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final UUID networkId;
    private boolean isValid;

    private Set<INetworkMember> networkMembers; // runtime
    private Set<BlockPos> memberPositions; // for loading, saving

    private final AtomicBoolean isDirty = new AtomicBoolean(true); // Start dirty to simulate on first load/creation
    private long lastSimulationTime = 0; // To potentially throttle simulations

    private static final String NETWORK_ID_KEY = "networkid";
    private static final String IS_VALID_KEY = "isvalid";
    private static final String MEMBERS_TAG_KEY = "memberpos";

    /**
     * Creates a new, valid Network with a random UUID and no members.
     */
    Network() {
        this(UUID.randomUUID(), true, new HashSet<>());
        LOGGER.debug("Created new Network with ID: {}", this.networkId);
        // Automatically dirty on creation, which will trigger requestSimulation
        if (PowerSim.NETWORK_MANAGER != null) { // Check as NM might not be fully initialized during early static init
            PowerSim.NETWORK_MANAGER.requestSimulation(this);
        }
    }

    /**
     * Creates a new Network with the specified properties.
     *
     * @param id        The UUID of the network.
     * @param valid     Whether the network is valid.
     * @param positions The initial set of member positions.
     */
    private Network(UUID id, boolean valid, Set<BlockPos> positions) {
        this.networkId = id;
        this.isValid = valid;
        this.memberPositions = positions != null ? new HashSet<>(positions) : new HashSet<>();
        this.networkMembers = new HashSet<>();
        // If loaded from NBT, it's also considered dirty initially to ensure simulation
        if (PowerSim.NETWORK_MANAGER != null) {
            PowerSim.NETWORK_MANAGER.requestSimulation(this);
        }
    }

    public void markDirty() {
        boolean previouslyClean = !this.isDirty.getAndSet(true);
        if (previouslyClean && this.isValid) { // Only request if it became dirty AND is valid
            if (PowerSim.NETWORK_MANAGER != null) {
                LOGGER.trace("Network {} marked dirty (was clean & is valid). Requesting simulation.", this.networkId);
                PowerSim.NETWORK_MANAGER.requestSimulation(this);
            } else {
                LOGGER.warn("Network {} marked dirty, but NetworkManager is null. Simulation won't be requested immediately.", this.networkId);
            }
        } else if (!this.isValid) {
            LOGGER.trace("Network {} marked dirty, but is invalid. Simulation not requested.", this.networkId);
        }
        // If it was already dirty, a simulation request is likely pending or running.
    }

    public boolean isDirtyAndReset() {
        return this.isDirty.getAndSet(false);
    }

    public void setLastSimulationTime(long time) {
        this.lastSimulationTime = time;
    }

    public long getLastSimulationTime() {
        return this.lastSimulationTime;
    }

    /**
     * Marks this network as invalid.
     * This will cause it to be removed from the NetworkManager's list of active networks.
     */
    void setInvalid() {
        isValid = false;
        // When a network becomes invalid, it should not be simulated.
        // Its members will either be re-assigned or become part of no network.
        this.isDirty.set(false); // No longer needs simulation
        NetworkManager networkManager = PowerSim.NETWORK_MANAGER;
        if (networkManager != null && networkManager.getSavedData() != null) {
            networkManager.markDataDirty();
        }
    }

    /**
     * Checks if this network is valid.
     * @return True if the network is valid, false otherwise.
     */
    boolean isValid() {
        return isValid;
    }

    /**
     * Gets the unique ID of this network.
     * @return The UUID of this network.
     */
    UUID getNetworkId() {
        return networkId;
    }

    /**
     * Gets the set of runtime members in this network.
     * @return A set of INetworkMember instances.
     */
    Set<INetworkMember> getNetworkMembers() {
        return networkMembers;
    }

    /**
     * Gets the set of BlockPos representing the positions of members in this network.
     * @return A set of BlockPos instances.
     */
    Set<BlockPos> getMemberPositions() {
        return new HashSet<>(memberPositions);
    }

    /**
     * Adds a member position to the network.
     *
     * @param pos The position to add.
     */
    private void addMemberPosition(BlockPos pos) {
        if (pos != null) {
            this.memberPositions.add(pos);
        }
    }

    /**
     * Adds a runtime member to the network.
     *
     * @param member The member to add.
     */
    private void addRuntimeMember(INetworkMember member) {
        if (member != null) {
            this.networkMembers.add(member);
        }
    }

    /**
     * Adds a network member to the network.
     *
     * @param networkMember The member to add.
     */
    void addNetworkMember(INetworkMember networkMember) {
        if (networkMember != null && this.isValid) { // Only operate on valid networks
            boolean addedToRuntime = this.networkMembers.add(networkMember);
            boolean addedToPos = this.memberPositions.add(networkMember.getPos());
            if (addedToRuntime || addedToPos) {
                markDirty(); // This will trigger requestSimulation
            }
        }
    }

    /**
     * Adds all network members to the network.
     *
     * @param networkMembers The members to add.
     */
    void addAllNetworkMembers(Set<INetworkMember> networkMembers) {
        if (!this.isValid) return;
        boolean changed = false;
        for (INetworkMember networkMember : networkMembers) {
            if (networkMember != null) {
                if (this.networkMembers.add(networkMember)) changed = true;
                if (this.memberPositions.add(networkMember.getPos())) changed = true;
            }
        }
        if (changed) {
            markDirty();
        }
    }

    /**
     * Removes a runtime member from the network.
     *
     * @param member The member to remove.
     */
    void removeRuntimeMember(INetworkMember member) {
        if (member != null && this.isValid) {
            if (this.networkMembers.remove(member)) {
                markDirty();
            }
        }
    }

    /**
     * Removes a member position from the network.
     *
     * @param pos The position of the member to remove.
     */
    void removeMemberPosition(BlockPos pos) {
        if (pos != null && this.isValid) {
            if (this.memberPositions.remove(pos)) {
                markDirty();
            }
        }
    }

    /**
     * Writes the essential network data (ID, validity, member positions) to an NBT tag.
     * @return A CompoundTag representing this network.
     */
    public CompoundTag writeToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(NETWORK_ID_KEY, this.networkId);
        tag.putBoolean(IS_VALID_KEY, this.isValid);

        ListTag membersList = new ListTag();
        for (BlockPos pos : this.memberPositions) {
            membersList.add(NbtUtils.writeBlockPos(pos));
        }
        //LOGGER.trace("  Created membersList with {} entries for Network {}", membersList.size(), networkId);
        tag.put(MEMBERS_TAG_KEY, membersList);
        return tag;
    }

    /**
     * Reads network data from an NBT tag and creates a Network instance.
     * <p>
     * This method reads the network's ID, validity status, and member positions from the provided NBT tag.
     * It expects the member positions to be stored as a list of integer arrays, where each array represents
     * a BlockPos with three integers (x, y, z). If the tag does not contain the necessary data or if the
     * data is in an unexpected format, appropriate error or warning messages are logged.
     * </p>
     * <p>
     * The returned Network instance contains the loaded BlockPos of members,
     * but the runtime INetworkMember set is initially empty.
     * @param tag The CompoundTag containing network data.
     * @return A new Network instance with loaded data.
     */
    public static Network readFromNbt(CompoundTag tag) {
        UUID id = tag.contains(NETWORK_ID_KEY) ? tag.getUUID(NETWORK_ID_KEY) : UUID.randomUUID();
        boolean valid = tag.getBoolean(IS_VALID_KEY);
        Set<BlockPos> positions = new HashSet<>();

        //LOGGER.debug("Attempting to read NBT for Network ID: {}", id);

        if (tag.contains(MEMBERS_TAG_KEY)) {
            Tag listTagBase = tag.get(MEMBERS_TAG_KEY);

            if (listTagBase != null && listTagBase.getId() == Tag.TAG_LIST) {
                ListTag membersList = (ListTag) listTagBase;
                byte elementType = membersList.getElementType();
                int listSize = membersList.size();
                //LOGGER.debug("Network {}: Found '{}' list via generic get(). Actual Size: {}, Element Type ID: {}", id, MEMBERS_TAG_KEY, listSize, elementType);

                if (listSize > 0) {
                    if (elementType == Tag.TAG_INT_ARRAY) {
                        for (int i = 0; i < listSize; i++) {
                            Tag elementTag = membersList.get(i);
                            if(elementTag instanceof IntArrayTag intArrayTag) {
                                int[] array = intArrayTag.getAsIntArray();
                                if (array.length == 3) { // Check for X, Y, Z
                                    BlockPos pos = new BlockPos(array[0], array[1], array[2]);
                                    positions.add(pos);
                                    //LOGGER.trace("    Read BlockPos from Int_Array: {}", pos.toShortString());
                                } else {
                                    LOGGER.error("    Int_Array tag at index {} did not have length 3! Length: {}. Cannot create BlockPos.", i, array.length);
                                }
                            } else {
                                LOGGER.error("    Tag at index {} reported Type ID 11 but was not an instance of IntArrayTag!", i);
                            }
                        }
                    } else {
                        LOGGER.error("  >> ERROR: '{}' list contains unsupported element type ID {}. Cannot load positions.", MEMBERS_TAG_KEY, elementType);
                    }
                } else {
                    LOGGER.debug("  List is empty. No positions to load.");
                }

            } else if (listTagBase != null) {
                LOGGER.error("Network {}: Tag '{}' exists but is not a ListTag! Type ID: {}. Cannot load positions.",
                        id, MEMBERS_TAG_KEY, listTagBase.getId());
            } else {
                LOGGER.warn("Network {}: tag.get('{}') returned null?", id, MEMBERS_TAG_KEY);
            }
        } else {
            LOGGER.warn("NBT tag for network {} did not contain member positions list key '{}'", id, MEMBERS_TAG_KEY);
        }

        //LOGGER.debug("Finished reading NBT for network {}. Valid: {}, Final Positions Count: {}", id, valid, positions.size());
        return new Network(id, valid, positions);
    }

    /**
     * Called every tick to update the network.
     * Currently does nothing, but will eventually contain network simulation logic.
     */
    void tick() {
        // Network simulation logic goes here, soon™
    }

    /**
     * Merges the members from another network into this network.
     *
     * @param otherNetwork The network to merge members from.
     */
    void mergeMembersFrom(Network otherNetwork) {
        if (!this.isValid || otherNetwork == null || !otherNetwork.isValid()) return;

        boolean changed = false;
        Set<INetworkMember> otherRuntimeMembers = otherNetwork.getNetworkMembers();
        Set<BlockPos> otherMemberPositions = otherNetwork.getMemberPositions();

        for (INetworkMember member : otherRuntimeMembers) {
            if (this.networkMembers.add(member)) changed = true;
        }
        for (BlockPos pos : otherMemberPositions) {
            if (this.memberPositions.add(pos)) changed = true;
        }

        if (changed) {
            markDirty(); // This network has changed
        }

        UUID targetNetworkId = this.getNetworkId();
        for (INetworkMember member : otherRuntimeMembers) {
            member.setNetworkId(targetNetworkId); // This might also trigger registration logic in NM
        }

        // Do NOT call markDataDirty here, NetworkManager should handle it
    }
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Network network = (Network) object;
        return networkId.equals(network.networkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId);
    }
}
