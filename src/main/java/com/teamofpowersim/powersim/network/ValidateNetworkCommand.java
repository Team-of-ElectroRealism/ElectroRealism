package com.teamofpowersim.powersim.network;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.simulation.NgSpiceSimulator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ValidateNetworkCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("validateNetwork")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> {
                            try {
                                return runValidationAt(
                                        context.getSource(),
                                        BlockPosArgument.getBlockPos(context, "pos")
                                );
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                )
        );
    }

    private static int runValidationAt(CommandSourceStack source, BlockPos pos) throws IOException {
        // Locate and validate the network member
        ServerLevel level = source.getLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof INetworkMember networkMember)) {
            source.sendFailure(Component.literal("Error: Block at " + pos.toShortString() + " is not a network member."));
            return 0;
        }

        UUID networkId = networkMember.getNetworkId();
        if (networkId == null) {
            source.sendFailure(Component.literal("Error: Network member at " + pos.toShortString() + " has no network ID assigned."));
            return 0;
        }

        var network = PowerSim.NETWORK_MANAGER.findNetwork(networkId);
        if (network == null || !network.isValid()) {
            source.sendFailure(Component.literal(
                    network == null
                            ? "Error: Network " + networkId.toString().substring(0,8) + " not found."
                            : "Error: Network " + networkId.toString().substring(0,8) + " is invalid."
            ));
            return 0;
        }

        Set<INetworkMember> members = network.getNetworkMembers();
        if (members.isEmpty()) {
            if (!network.getMemberPositions().isEmpty()) {
                source.sendFailure(Component.literal(
                        "Network " + networkId.toString().substring(0,8) + " has saved positions but no runtime members loaded."
                ));
                return 0;
            }
            source.sendSuccess(() -> Component.literal(
                    "Network " + networkId.toString().substring(0,8) + " is empty. Validation skipped (Valid)."
            ), true);
            return 1;
        }

        // Build adjacency list
        var graphBuilder = new NetworkGraphBuilder();
        var adjList = graphBuilder.buildAdjacencyList(members, level);
        source.sendSuccess(() -> Component.literal("Built adjacency list for network "
                + networkId.toString().substring(0,8) + "."), false);

        // Generate netlist
        // create temp CSV file for ngspice output
        File resultsFile = File.createTempFile("er_results", ".csv");
        resultsFile.deleteOnExit();
        final String spiceNetlist;
        try {
            // pass an absolute results path into NetlistBuilder
            spiceNetlist = new NetlistBuilder().buildNetlist(adjList);

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error generating netlist: " + e.getMessage()));
            LOGGER.error("Netlist generation failed for network {}:", networkId, e);
            return 0;
        }

        // Show netlist to player
        source.sendSuccess(() -> Component.literal("--- Generated SPICE Netlist ---"), false);
        for (String line : spiceNetlist.split("\n")) {
            if (!line.isBlank()) {
                source.sendSuccess(() -> Component.literal(line), false);
            }
        }
        source.sendSuccess(() -> Component.literal("--- End Netlist ---"), false);
        LOGGER.info("Generated Netlist for network {}:\n{}", networkId, spiceNetlist);

        // Check for closed circuit
        boolean isClosed = new NetworkCircuitChecker().isClosedCircuit(members, adjList);
        source.sendSuccess(() -> Component.literal("Closed Circuit Check: " + isClosed), false);

        int circuitCount = new NetworkCircuitChecker().getClosedCircuitCount(members, adjList);
        source.sendSuccess(() -> Component.literal(
                circuitCount == 0 ? "No closed circuits found." : "Found " + circuitCount + " closed circuits."
        ), false);

        // If valid, run simulation
        if (isClosed) {
            source.sendSuccess(() -> Component.literal("Simulation starting…"), false);

            new Thread(() -> {
                try {
                    // Run ngspice and capture all vectors in-memory
                    Map<String, double[]> simData = NgSpiceSimulator.instance().simulate(spiceNetlist);

                    // Send the results back to the player on the server thread
                    source.getServer().execute(() -> {
                        source.sendSuccess(() -> Component.literal("--- SPICE RESULTS ---"), false);

                        double[] time = simData.get("time");
                        if (time == null) {
                            source.sendFailure(Component.literal("Error: no time data returned."));
                            return;
                        }

                        // Send up to first 10 timesteps
                        int steps = Math.min(time.length, 10);
                        for (int i = 0; i < steps; i++) {
                            StringBuilder line = new StringBuilder();
                            line.append(String.format("t=%.6g", time[i]));  // time value

                            // append each voltage/current at this timestep
                            for (Map.Entry<String, double[]> entry : simData.entrySet()) {
                                String name = entry.getKey();
                                if ("time".equals(name)) continue;
                                double[] vec = entry.getValue();
                                if (i < vec.length) {
                                    line.append(String.format(", %s=%.6g", name, vec[i]));
                                }
                            }

                            source.sendSuccess(() -> Component.literal(line.toString()), false);
                        }

                        if (time.length > 10) {
                            source.sendSuccess(() -> Component.literal("… (" + (time.length - 10) + " more steps)"), false);
                        }

                        source.sendSuccess(() -> Component.literal("--- END SPICE RESULTS ---"), false);
                    });
                } catch (Exception e) {
                    LOGGER.error("Error running ngspice", e);
                    source.sendFailure(Component.literal("Simulation failed: " + e.getMessage()));
                }
            }, "NgSpice-Thread").start();

        } else {
            source.sendFailure(Component.literal(
                    "Network " + networkId.toString().substring(0,8)
                            + " validation FAILED (Closed Circuit: " + isClosed + ")."
            ));
        }


        return 1;
    }
}
