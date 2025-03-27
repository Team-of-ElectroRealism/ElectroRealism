package com.teamofelectrorealism.electrorealism.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * This class stores and renders the highlighted networks.
 */
public class HighlightNetworks {
    private static Map<UUID, List<BlockPos>> networkMap = new HashMap<>();

    /**
     * Called by the client-side packet handler to update the network data.
     *
     * @param networksData The new network data to highlight.
     */
    public static void highlightNetwork(Map<UUID, List<BlockPos>> networksData) {
        networkMap = networksData;
    }

    /**
     * Called during the client render phase (e.g. in RenderWorldLastEvent)
     * to draw an outline around each block in the highlighted networks.
     *
     * @param poseStack   The current PoseStack (matrix stack) for rendering.
     * @param partialTick Partial tick time.
     */
    public static void renderHighlights(PoseStack poseStack, MultiBufferSource buffer, DeltaTracker partialTick) {
        if (networkMap == null || networkMap.isEmpty()) {
            System.out.println("No networks to render.");
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        // Iterate over each network in the map
        for (Map.Entry<UUID, List<BlockPos>> entry : networkMap.entrySet()) {
            UUID networkId = entry.getKey();
            List<BlockPos> blockList = entry.getValue();

            if (blockList == null || blockList.isEmpty()) {
                continue;
            }

            // Generate a unique color based on the network id.
            int color = getColorForNetwork(networkId);

            // Render each block in this network.
            for (BlockPos pos : blockList) {
                // Inside your loop over blocks...
                double x = pos.getX() - camPos.x;
                double y = pos.getY() - camPos.y;
                double z = pos.getZ() - camPos.z;

                RenderOutlineUtil.drawOutline(poseStack, buffer, x, y, z, color);
            }
        }
    }

    /**
     * Generates a unique ARGB color based on the network id.
     *
     * @param networkId The unique network id.
     * @return An ARGB color integer.
     */
    private static int getColorForNetwork(UUID networkId) {
        int hash = networkId.hashCode();
        // Ensure fully opaque alpha channel and use the lower 24 bits for color.
        return 0xFF000000 | (hash & 0x00FFFFFF);
    }
}
