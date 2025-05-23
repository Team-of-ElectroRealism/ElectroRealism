package com.teamofpowersim.powersim.network;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.simulation.ISimulatable;
import com.teamofpowersim.powersim.simulation.NgSpiceSimulator;
import com.teamofpowersim.powersim.simulation.SimulationDataApplier; // IMPORT THIS

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

public class ValidateNetworkCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("validateNetwork")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> {
                            ServerLevel level = context.getSource().getLevel();
                            BlockPos posArg = BlockPosArgument.getBlockPos(context, "pos");

                            // buildResultHolder is not strictly needed if we pass the BuildResult directly
                            // to the new thread if simulation runs there.
                            // For simplicity, let's build it before starting the simulation thread.

                            // We need the 'network' object for SimulationDataApplier
                            Network networkForValidation;
                            NetlistBuilder.BuildResult buildResultForValidation;
                            String spiceNetlistForValidation;
                            Set<INetworkMember> membersForValidation;

                            try {
                                // --- Initial Setup ---
                                BlockEntity beCmdTarget = level.getBlockEntity(posArg);
                                if (!(beCmdTarget instanceof INetworkMember networkMember)) {
                                    context.getSource().sendFailure(Component.literal("Error: Block at " + posArg.toShortString() + " is not a network member."));
                                    return 0;
                                }
                                UUID networkId = networkMember.getNetworkId();
                                if (networkId == null) {
                                    context.getSource().sendFailure(Component.literal("Error: Network member at " + posArg.toShortString() + " has no network ID assigned."));
                                    return 0;
                                }
                                networkForValidation = PowerSim.NETWORK_MANAGER.findNetwork(networkId);
                                if (networkForValidation == null || !networkForValidation.isValid()) {
                                    context.getSource().sendFailure(Component.literal(
                                            networkForValidation == null ? "Error: Network " + networkId.toString().substring(0,8) + " not found."
                                                    : "Error: Network " + networkId.toString().substring(0,8) + " is invalid."
                                    ));
                                    return 0;
                                }
                                membersForValidation = networkForValidation.getNetworkMembers();
                                if (membersForValidation.isEmpty()) {
                                    // ... (empty network handling) ...
                                    context.getSource().sendSuccess(() -> Component.literal("Network " + networkId.toString().substring(0,8) + " is empty. Validation skipped."), true);
                                    return 1;
                                }

                                NetworkGraphBuilder graphBuilder = new NetworkGraphBuilder();
                                Map<INetworkMember, List<ConnectionInfo>> adjList = graphBuilder.buildAdjacencyList(new HashSet<>(membersForValidation), level); // Use copy
                                context.getSource().sendSuccess(() -> Component.literal("Built adjacency list for network " + networkId.toString().substring(0,8) + "."), false);

                                buildResultForValidation = new NetlistBuilder().buildNetlist(adjList);
                                spiceNetlistForValidation = buildResultForValidation.netlist();

                            } catch (Exception e) { // Catch any setup errors (IOException for sim is handled later)
                                LOGGER.error("Error during validation setup: ", e);
                                context.getSource().sendFailure(Component.literal("Error during validation setup: " + e.getMessage()));
                                return 0;
                            }

                            // --- Pass final/effectively final variables to the execution ---
                            return runValidationLogic(context.getSource(), level, networkForValidation, membersForValidation, buildResultForValidation, spiceNetlistForValidation);
                        })
                )
        );
    }

    private static int runValidationLogic(CommandSourceStack source,
                                          ServerLevel level,
                                          Network network,
                                          Set<INetworkMember> members, // Already a copy from getNetworkMembers() or made one
                                          NetlistBuilder.BuildResult buildResult,
                                          String spiceNetlist) {
        // Show Netlist
        source.sendSuccess(() -> Component.literal("--- Generated SPICE Netlist ---"), false);
        for (String line : spiceNetlist.split("\n")) {
            if (!line.isBlank()) source.sendSuccess(() -> Component.literal(line), false);
        }
        source.sendSuccess(() -> Component.literal("--- End Netlist ---"), false);
        LOGGER.info("Generated Netlist for network {}:\n{}", network.getNetworkId(), spiceNetlist);

        // Circuit Check
        NetworkGraphBuilder graphBuilder = new NetworkGraphBuilder(); // Rebuild adjList for checker, or pass it
        Map<INetworkMember, List<ConnectionInfo>> adjListForCheck = graphBuilder.buildAdjacencyList(members, level);
        boolean isClosed = new NetworkCircuitChecker().isClosedCircuit(members, adjListForCheck);
        source.sendSuccess(() -> Component.literal("Closed Circuit Check: " + isClosed), false);
        // ... (circuit count if you want)

        if (isClosed) {
            source.sendSuccess(() -> Component.literal("Simulation starting (sync via /validateNetwork)..."), false);

            // The simulation and result application will run in a new thread
            // to avoid blocking the server if ngspice takes time,
            // but the command itself will wait for this thread using simulateSync's internal latch.
            new Thread(() -> {
                try {
                    // 1. Use simulateSync - this blocks THIS thread, not the server thread.
                    Map<String, double[]> simData = NgSpiceSimulator.instance().simulateSync(spiceNetlist);

                    // 2. Use SimulationDataApplier.apply on the main server thread
                    source.getServer().execute(() -> {
                        source.sendSuccess(() -> Component.literal("--- SPICE RESULTS (from /validateNetwork) ---"), false);
                        double[] time = simData.get("time");
                        if (time == null || time.length == 0) {
                            source.sendFailure(Component.literal("Error: no time data returned from simulation."));
                            return;
                        }
                        // Optional: Print raw simData table
                        int steps = Math.min(time.length, 10);
                        for (int i = 0; i < steps; i++) {
                            StringBuilder lineBuilder = new StringBuilder();
                            lineBuilder.append(String.format("t=%.6g", time[i]));
                            for (Map.Entry<String, double[]> entry : simData.entrySet()) {
                                String name = entry.getKey();
                                if ("time".equals(name)) continue;
                                double[] vec = entry.getValue();
                                if (i < vec.length) {
                                    lineBuilder.append(String.format(", %s=%.6g", name, vec[i]));
                                }
                            }
                            final String lineToPrint = lineBuilder.toString();
                            source.sendSuccess(() -> Component.literal(lineToPrint), false);
                        }
                        if (time.length > 10) {
                            source.sendSuccess(() -> Component.literal("… (" + (time.length - 10) + " more steps)"), false);
                        }
                        source.sendSuccess(() -> Component.literal("--- END SPICE RESULTS (from /validateNetwork) ---"), false);


                        source.sendSuccess(() -> Component.literal("--- APPLYING SIMULATION DATA (from /validateNetwork) ---"), false);
                        SimulationDataApplier.apply(
                                simData,
                                buildResult, // The BuildResult obtained earlier
                                level,       // The ServerLevel
                                textComponent -> source.sendSuccess(() -> textComponent, false), // Feedback consumer
                                members      // The set of members for this network
                        );
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("NgSpice sync simulation for /validateNetwork was interrupted", e);
                    source.getServer().execute(() -> source.sendFailure(Component.literal("Simulation interrupted: " + e.getMessage())));
                } catch (Exception e) { // Catch IllegalStateException from simulateSync or other runtime errors
                    LOGGER.error("Error running/processing sync ngspice simulation for /validateNetwork", e);
                    source.getServer().execute(() -> source.sendFailure(Component.literal("Sync simulation/apply failed: " + e.getMessage())));
                }
            }, "ValidateNetwork-SimThread").start();

        } else {
            source.sendFailure(Component.literal(
                    "Network " + network.getNetworkId().toString().substring(0,8)
                            + " validation FAILED (Not a closed circuit)."
            ));
            // Optionally apply zero power here too if it's an open circuit
            source.getServer().execute(() -> {
                if (PowerSim.NETWORK_MANAGER != null) { // Check if manager is available
                    // PowerSim.NETWORK_MANAGER.applyZeroPowerToNetwork(network, level); // If method is public
                    // Or replicate logic:
                    for(INetworkMember m : members) {
                        if (m instanceof ISimulatable sim) sim.applySimulation(0,0);
                    }
                    source.sendSuccess(()->Component.literal("Applied zero power to open circuit."), false);
                }
            });
        }
        return 1; // Command executed
    }
}