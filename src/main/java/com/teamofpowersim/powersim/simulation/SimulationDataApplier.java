// com/teamofpowersim/powersim/simulation/SimulationDataApplier.java (new file)
package com.teamofpowersim.powersim.simulation;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.block.IVoltageConsumer;
import com.teamofpowersim.powersim.block.IVoltageProvider;
import com.teamofpowersim.powersim.network.INetworkMember;
import com.teamofpowersim.powersim.network.NetlistBuilder;
import com.teamofpowersim.powersim.power.IWireNode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class SimulationDataApplier {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void apply(Map<String, double[]> simData,
                             NetlistBuilder.BuildResult buildResult,
                             ServerLevel level,
                             @Nullable Consumer<Component> feedbackChannel, // For sending messages back to command source
                             @Nullable Set<INetworkMember> relevantNetworkMembers) { // To ensure BE is part of the correct network

        double[] time = simData.get("time");
        if (time == null || time.length == 0) {
            String msg = "No time data from simulation.";
            LOGGER.warn(msg);
            if (feedbackChannel != null) feedbackChannel.accept(Component.literal("Error: " + msg));
            return;
        }
        int lastTimeIndex = time.length - 1;

        Map<String, BlockPos> instanceMap = buildResult.instanceMap();
        Map<NetlistBuilder.NodeKey, Integer> nodeIdMap = buildResult.nodeIdMap();

        for (Map.Entry<String, BlockPos> instanceEntry : instanceMap.entrySet()) {
            String originalSpiceName = instanceEntry.getKey();
            BlockPos memberPos = instanceEntry.getValue();

            if (!level.isLoaded(memberPos)) { // Optimization: skip if chunk not loaded
                // LOGGER.trace("Skipping application for BE at unloaded chunk: {}", memberPos);
                continue;
            }
            BlockEntity be = level.getBlockEntity(memberPos);

            if (be instanceof ISimulatable simulatableMember && be instanceof INetworkMember currentNetworkMember) {
                // Optional: Validate if this member is indeed part of the network we intended to simulate
                if (relevantNetworkMembers != null && !relevantNetworkMembers.contains(currentNetworkMember)) {
                    // This could happen if the network structure changed between sim request and sim apply,
                    // or if buildResult's instanceMap is somehow out of sync.
                    LOGGER.warn("Skipping apply for {} @ {} as it's not in the relevant member set for this simulation.", originalSpiceName, memberPos);
                    continue;
                }

                double voltageToApply = 0.0;
                double currentToApply = 0.0;
                boolean dataFullyFound = false;
                String lookupSpiceName = originalSpiceName.toLowerCase();

                // --- PASTE THE DETAILED V/I EXTRACTION LOGIC HERE ---
                // (The big if/else if block for IVoltageProvider, IVoltageConsumer, IWireNode)
                // Make sure to use 'feedbackChannel' for messages if it's not null.
                // Example for one branch:
                if (currentNetworkMember instanceof IVoltageProvider provider) {
                    voltageToApply = provider.getVoltage();
                    String currentKey = "@" + lookupSpiceName + "[i]";
                    double[] currentArray = simData.get(currentKey);
                    if (currentArray == null) {
                        String branchCurrentKey = lookupSpiceName + "#branch";
                        currentArray = simData.get(branchCurrentKey);
                        // if (currentArray != null && feedbackChannel == null) LOGGER.debug("Found provider current using {}#branch key: {}", lookupSpiceName, branchCurrentKey);
                    }
                    if (currentArray != null && currentArray.length > lastTimeIndex) {
                        currentToApply = currentArray[lastTimeIndex];
                        dataFullyFound = true;
                    } else {
                        String msg = String.format("Missing current data for Provider %s (orig: %s) (keys tried: @%s[i], %s#branch).", lookupSpiceName, originalSpiceName, lookupSpiceName, lookupSpiceName);
                        LOGGER.warn(msg + " Array: {}, Length: {}, lastTimeIndex: {}", currentArray, (currentArray != null ? currentArray.length: "null"), lastTimeIndex);
                        if (feedbackChannel != null) feedbackChannel.accept(Component.literal("Error: " + msg));
                    }
                } else if (currentNetworkMember instanceof IVoltageConsumer consumer) {
                    // ... (consumer logic, using feedbackChannel for errors) ...
                    String currentKey = "@" + lookupSpiceName + "[i]";
                    double[] currentArray = simData.get(currentKey);
                    boolean currentFound = false;
                    if (currentArray != null && currentArray.length > lastTimeIndex) {
                        currentToApply = currentArray[lastTimeIndex];
                        currentFound = true;
                    } else {
                        String msg = String.format("Missing current data for Consumer %s (orig: %s) (key: @%s[i]).", lookupSpiceName, originalSpiceName, lookupSpiceName);
                        LOGGER.warn(msg + " Array: {}, Length: {}, lastTimeIndex: {}", currentArray, (currentArray != null ? currentArray.length: "null"), lastTimeIndex);
                        if (feedbackChannel != null) feedbackChannel.accept(Component.literal("Error: " + msg));
                    }

                    NetlistBuilder.NodeKey inputNodeKey = new NetlistBuilder.NodeKey(currentNetworkMember, NetlistBuilder.INPUT_TERMINAL);
                    NetlistBuilder.NodeKey outputNodeKey = new NetlistBuilder.NodeKey(currentNetworkMember, NetlistBuilder.OUTPUT_TERMINAL);
                    Integer node1SimNum = nodeIdMap.get(inputNodeKey);
                    Integer node2SimNum = nodeIdMap.get(outputNodeKey);
                    boolean voltageFound = false;
                    if (node1SimNum != null && node2SimNum != null) {
                        String v1Key = "V(" + node1SimNum + ")"; String v2Key = "V(" + node2SimNum + ")";
                        double[] v1Array = simData.get(v1Key); double[] v2Array = simData.get(v2Key);
                        if (v1Array != null && v1Array.length > lastTimeIndex && v2Array != null && v2Array.length > lastTimeIndex) {
                            voltageToApply = v1Array[lastTimeIndex] - v2Array[lastTimeIndex];
                            voltageFound = true;
                        } else {
                            String msg = String.format("Missing voltage data for Consumer %s (orig: %s). Keys: %s, %s.", lookupSpiceName, originalSpiceName, v1Key, v2Key);
                            LOGGER.warn(msg + " v1Array: {}, v2Array: {}, lastTimeIndex: {}", v1Array, v2Array, lastTimeIndex);
                            if (feedbackChannel != null) feedbackChannel.accept(Component.literal("Error: " + msg));
                        }
                    } else {
                        String msg = String.format("Could not find node numbers for Consumer %s (orig: %s) at %s.", lookupSpiceName, originalSpiceName, memberPos.toShortString());
                        LOGGER.warn(msg + " InputKey: {}, OutputKey: {}. Got: N1={}, N2={}", inputNodeKey, outputNodeKey, node1SimNum, node2SimNum);
                        if (feedbackChannel != null) feedbackChannel.accept(Component.literal("Error: " + msg));
                    }
                    if(currentFound && voltageFound) dataFullyFound = true;

                } else if (currentNetworkMember instanceof IWireNode wireNode && originalSpiceName.toUpperCase().startsWith("RW")) {
                    // ... (IWireNode logic, using feedbackChannel for errors) ...
                    boolean currentFound = false;
                    String currentKey = "@" + lookupSpiceName + "[i]";
                    double[] currentArray = simData.get(currentKey);
                    if (currentArray != null && currentArray.length > lastTimeIndex) {
                        currentToApply = currentArray[lastTimeIndex];
                        currentFound = true;
                    } else {
                        String msg = String.format("Missing current data for Wire %s (orig: %s) (key: @%s[i]).", lookupSpiceName, originalSpiceName, lookupSpiceName);
                        LOGGER.warn(msg + " Array: {}, Length: {}, lastTimeIndex: {}", currentArray, (currentArray != null ? currentArray.length : "null"), lastTimeIndex);
                        if (feedbackChannel != null) feedbackChannel.accept(Component.literal("Error: " + msg));
                    }

                    boolean voltageOrPowerFound = false;
                    String powerKey = "@" + lookupSpiceName + "[p]";
                    double[] powerArray = simData.get(powerKey);
                    if (powerArray != null && powerArray.length > lastTimeIndex) {
                        double powerDissipated = powerArray[lastTimeIndex];
                        if (Math.abs(currentToApply) > 1e-9) voltageToApply = powerDissipated / currentToApply;
                        else voltageToApply = 0;
                        voltageOrPowerFound = true;
                        // if (feedbackChannel == null) LOGGER.debug("Wire {} (orig: {}): P={}, I={}, Calculated V_drop={}", lookupSpiceName, originalSpiceName, powerDissipated, currentToApply, voltageToApply);
                    } else {
                        String msg = String.format("Missing power data for Wire %s (orig: %s) (key: @%s[p]). Setting V_drop to 0.", lookupSpiceName, originalSpiceName, lookupSpiceName);
                        LOGGER.warn(msg);
                        // if (feedbackChannel != null) feedbackChannel.accept(Component.literal("Warning: " + msg)); // Optional warning
                        voltageToApply = 0;
                        voltageOrPowerFound = currentFound;
                    }
                    if (currentFound && voltageOrPowerFound) dataFullyFound = true;
                }
                // --- END PASTE ---

                if (dataFullyFound) {
                    simulatableMember.applySimulation(voltageToApply, currentToApply);
                    String logMsg = String.format("Applied to %s (%s@%s): V=%.3f, I=%.3f", currentNetworkMember.getClass().getSimpleName(), originalSpiceName, memberPos.toShortString(), voltageToApply, currentToApply);
                    if (feedbackChannel != null) feedbackChannel.accept(Component.literal(logMsg)); else LOGGER.trace(logMsg);
                } else {
                    // Apply zero power if data wasn't fully found for a component expected to have data
                    simulatableMember.applySimulation(0.0, 0.0);
                    String logMsg = String.format("Could not apply full sim data to %s (%s@%s). Applied 0V/0A. BE Type: %s", currentNetworkMember.getClass().getSimpleName(), originalSpiceName, memberPos.toShortString(), be.getClass().getName());
                    if (feedbackChannel != null) feedbackChannel.accept(Component.literal(logMsg)); else LOGGER.warn(logMsg);
                }
            }
        }
        if (feedbackChannel != null) feedbackChannel.accept(Component.literal("--- FINISHED APPLYING SIMULATION DATA ---"));
    }
}