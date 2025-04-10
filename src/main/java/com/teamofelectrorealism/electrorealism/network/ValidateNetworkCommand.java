package com.teamofelectrorealism.electrorealism.network;

import com.mojang.brigadier.CommandDispatcher;
import com.teamofelectrorealism.electrorealism.ElectroRealism;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ValidateNetworkCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("validateNetwork")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> runValidationAt(context.getSource(), BlockPosArgument.getBlockPos(context, "pos")))
                )
        );
    }

    private static int runValidationAt(CommandSourceStack source, BlockPos pos) {
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

        Network network = ElectroRealism.NETWORK_MANAGER.findNetwork(networkId);
        if (network == null) {
            source.sendFailure(Component.literal("Error: Network " + networkId.toString().substring(0, 8) + " not found in manager."));
            return 0;
        }
        if (!network.isValid()){
            source.sendFailure(Component.literal("Error: Network " + networkId.toString().substring(0, 8) + " is marked as invalid."));
            return 0;
        }

        Set<INetworkMember> members = network.getNetworkMembers();
        if (members.isEmpty() && !network.getMemberPositions().isEmpty()){
            source.sendFailure(Component.literal("Network " + networkId.toString().substring(0, 8) + " has saved positions but no runtime members loaded. Cannot validate now."));
            return 0;
        }
        if (members.isEmpty() && network.getMemberPositions().isEmpty()){
            source.sendSuccess(() -> Component.literal("Network " + networkId.toString().substring(0, 8) + " is empty. Validation skipped (Valid)."), true);
            return 1;
        }

        // --- Create instances of the new classes ---
        NetworkGraphBuilder graphBuilder = new NetworkGraphBuilder();
        NetworkCircuitChecker circuitChecker = new NetworkCircuitChecker();
        NetlistBuilder netlistBuilder = new NetlistBuilder();

        // --- Step 1: Build Graph ---
        Map<INetworkMember, List<ConnectionInfo>> adjList = graphBuilder.buildAdjacencyList(members, level);
        source.sendSuccess(() -> Component.literal("Built adjacency list for network " + networkId.toString().substring(0, 8) + "."), false);

        // *** ADD Netlist Generation HERE ***
        try {
            String netlistString = netlistBuilder.buildNetlist(adjList, level); // Call the build method
            // Output the netlist to the player who ran the command
            source.sendSuccess(() -> Component.literal("--- Generated SPICE Netlist ---"), false);
            // Split the string into lines to send as separate messages if it's long
            for (String line : netlistString.split("\n")) {
                source.sendSuccess(() -> Component.literal(line), false);
            }
            source.sendSuccess(() -> Component.literal("--- End Netlist ---"), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error generating netlist: " + e.getMessage()));
        }
        // *** END Netlist Generation ***

        // --- Step 2: Check for closed circuit ---
        boolean isClosedCircuit = circuitChecker.isClosedCircuit(members, adjList);
        source.sendSuccess(() -> Component.literal("Closed Circuit Check: " + isClosedCircuit), false);

        // --- Step 3: Report the number of closed circuits found using the new function ---
        int circuitCount = circuitChecker.getClosedCircuitCount(members, adjList);
        if (circuitCount == 0) {
            source.sendSuccess(() -> Component.literal("No closed circuits found."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Found " + circuitCount + " closed circuits."), false);
        }

        // --- Final Result ---
        if (isClosedCircuit) {
            source.sendSuccess(() -> Component.literal("Network " + networkId.toString().substring(0, 8)
                    + " validation PASSED (Closed Circuit: " + isClosedCircuit + ")."), true);
        } else {
            source.sendFailure(Component.literal("Network " + networkId.toString().substring(0, 8)
                    + " validation FAILED (Closed Circuit: " + isClosedCircuit + ")."));
        }

        return 1; // Command executed
    }
}
