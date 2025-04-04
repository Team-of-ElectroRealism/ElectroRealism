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

import java.util.UUID;

public class ValidateNetworkCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("validateNetwork")
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> runValidationAt(context.getSource(), BlockPosArgument.getBlockPos(context, "pos"))))
        );
    }

    private static int runValidationAt(CommandSourceStack source, BlockPos pos) {
        ServerLevel level = source.getLevel();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof INetworkMember networkMember) {
            UUID networkId = networkMember.getNetworkId();
            if (networkId != null) {
                // Assumes findNetwork is accessible/public in NetworkManager
                Network network = ElectroRealism.NETWORK_MANAGER.findNetwork(networkId);
                if (network != null) {
                    NetworkValidator validator = new NetworkValidator();
                    boolean isValid = validator.validate(network, level); // Pass level

                    source.sendSuccess(() -> Component.literal("Network " + networkId.toString().substring(0, 8) + " validation result: " + isValid), true);
                    return 1; // Success
                } else {
                    source.sendFailure(Component.literal("Error: Network " + networkId.toString().substring(0, 8) + " not found in manager."));
                }
            } else {
                source.sendFailure(Component.literal("Error: Block at " + pos + " has no network ID."));
            }
        } else {
            source.sendFailure(Component.literal("Error: Block at " + pos + " is not a network member."));
        }
        return 0; // Failure
    }
}
