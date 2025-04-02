package com.teamofelectrorealism.electrorealism.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.ModBlocks;
import com.teamofelectrorealism.electrorealism.item.ModItems;
import com.teamofelectrorealism.electrorealism.network.NetworkManager;
import com.teamofelectrorealism.electrorealism.rendering.DuoConnectorRenderer;
import com.teamofelectrorealism.electrorealism.rendering.HighlightNetworks;
import com.teamofelectrorealism.electrorealism.rendering.RenderOutlineUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void renderNetworkOutline(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!mc.player.getMainHandItem().getItem().equals(ModItems.TEST_ITEM.get())) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        DeltaTracker partialTick = event.getPartialTick();

        // Get the MultiBufferSource from the Minecraft instance.
        MultiBufferSource buffer = mc.renderBuffers().bufferSource();

        NetworkManager networkManager = ElectroRealism.NETWORK_MANAGER;

        HighlightNetworks.highlightNetwork(networkManager.getNetworksDataForClient());
        HighlightNetworks.renderHighlights(poseStack, buffer, partialTick);
    }

    @SubscribeEvent
    public static void renderDuoConnectorOutline(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Ensure the hit result exists and is a block hit.
        if (mc.hitResult == null || mc.hitResult.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;

        net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) mc.hitResult;
        BlockPos lookPos = blockHit.getBlockPos();

        // Make sure the level is not null and that the block at lookPos is a DUO_CONNECTOR.
        if (mc.level == null || !mc.level.getBlockState(lookPos).is(ModBlocks.DUO_CONNECTOR.get())) return;

        MultiBufferSource buffer = mc.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();

        DuoConnectorRenderer.renderOutline(poseStack, buffer, lookPos, blockHit);
    }
}
