package com.teamofelectrorealism.electrorealism.network;

import com.mojang.brigadier.CommandDispatcher;
import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.machine.generator.AbstractGeneratorBlockEntity;
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
import java.util.stream.Collectors;

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
        NetworkConnectivityChecker connectivityChecker = new NetworkConnectivityChecker();
        NetworkPolarityValidator polarityValidator = new NetworkPolarityValidator();

        // --- Step 1: Build Graph ---
        Map<INetworkMember, List<ConnectionInfo>> adjList = graphBuilder.buildAdjacencyList(members, level);
        source.sendSuccess(() -> Component.literal("Built adjacency list for network " + networkId.toString().substring(0, 8) + "."), false); // Use false to not broadcast

        // --- Step 2: Check Connectivity ---
        boolean isConnected = connectivityChecker.checkConnectivity(members, adjList);
        source.sendSuccess(() -> Component.literal("Connectivity Check: " + isConnected), false);

        // --- Step 3: Check for closed circuit ---
        boolean isClosedCircuit = connectivityChecker.isClosedCircuit(members, adjList);
        source.sendSuccess(() -> Component.literal("Closed Circuit Check: " + isClosedCircuit), false);

        if (!isConnected) {
            source.sendFailure(Component.literal("Validation Failed: Network is not fully connected."));
            return 0; // Stop if not connected
        }

        // --- Step 4: Check Polarity (only if connected) ---
        boolean polarityOk = polarityValidator.validatePolarity(adjList, members, level); // Pass adjList, members, level
        source.sendSuccess(() -> Component.literal("Polarity Check: " + polarityOk), false);

        // --- Final Result ---
        if (isConnected && polarityOk) {
            source.sendSuccess(() -> Component.literal("Network " + networkId.toString().substring(0, 8) + " validation PASSED (Connected & Polarity OK)."), true); // Use true to broadcast final result
        } else {
            source.sendFailure(Component.literal("Network " + networkId.toString().substring(0, 8) + " validation FAILED (Connected: " + isConnected + ", Polarity: " + polarityOk + "). Check logs for details."));
        }

        return 1; // Command executed
    }
}
