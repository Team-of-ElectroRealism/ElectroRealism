package com.teamofpowersim.powersim.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.ModBlocks;
import com.teamofpowersim.powersim.item.ModItems;
import com.teamofpowersim.powersim.network.NetworkManager;
import com.teamofpowersim.powersim.rendering.DuoConnectorRenderer;
import com.teamofpowersim.powersim.rendering.HighlightCircuits;
import com.teamofpowersim.powersim.rendering.HighlightNetworks;
import com.teamofpowersim.powersim.rendering.MachineDataOverlayRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;

@EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {
    private static final Minecraft MC = Minecraft.getInstance();

    private ClientEvents() {}

    @SubscribeEvent
    public static void renderNetworkOutline(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        if (MC.player == null) return;

        if (!MC.player.getMainHandItem().getItem().equals(ModItems.TEST_ITEM.get())) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        DeltaTracker partialTick = event.getPartialTick();

        // Get the MultiBufferSource from the Minecraft instance.
        MultiBufferSource buffer = MC.renderBuffers().bufferSource();

        NetworkManager networkManager = PowerSim.NETWORK_MANAGER;

        HighlightNetworks.highlightNetwork(networkManager.getNetworksDataForClient());
        HighlightNetworks.renderHighlights(poseStack, buffer, partialTick);
    }

    @SubscribeEvent
    public static void renderCircuitHighlights(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        if (MC.player == null) return;

        MultiBufferSource buffer = MC.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();

        HighlightCircuits.renderCircuitHighlights(poseStack, buffer);
    }

    @SubscribeEvent
    public static void renderDuoConnectorOutline(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        if (MC.player == null) return;

        // Ensure the hit result exists and is a block hit.
        if (MC.hitResult == null || MC.hitResult.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;

        net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) MC.hitResult;
        BlockPos lookPos = blockHit.getBlockPos();

        // Make sure the level is not null and that the block at lookPos is a DUO_CONNECTOR.
        if (MC.level == null || !MC.level.getBlockState(lookPos).is(ModBlocks.DUO_CONNECTOR.get())) return;

        MultiBufferSource buffer = MC.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();

        DuoConnectorRenderer.renderOutline(poseStack, buffer, lookPos, blockHit);
    }

    @SubscribeEvent
    public static void renderMachineDataOverlay(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        if (MC.player == null) return;

        if (!MC.player.getMainHandItem().getItem().equals(ModItems.MULTIMETER.get())) {
            return;
        }

        MachineDataOverlayRenderer.render(event);
    }
}
