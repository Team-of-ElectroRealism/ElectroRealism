package com.teamofelectrorealism.electrorealism.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teamofelectrorealism.electrorealism.network.INetworkMember;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Utility class to visually highlight closed circuits.
 * Each circuit is rendered with a unique color using RenderOutlineUtil.
 */
public class HighlightCircuits {
    // Map from a circuit identifier (UUID) to the list of block positions forming that circuit.
    private static Map<UUID, List<BlockPos>> circuitMap = new HashMap<>();

    /**
     * Populates the highlight map with closed circuits.
     *
     * @param circuits A list of circuits, where each circuit is a List of INetworkMember.
     */
    public static void highlightCircuits(List<List<INetworkMember>> circuits) {
        circuitMap.clear();
        for (List<INetworkMember> circuit : circuits) {
            // Generate a unique identifier for each circuit.
            UUID circuitId = UUID.randomUUID();
            List<BlockPos> positions = new ArrayList<>();
            for (INetworkMember member : circuit) {
                positions.add(member.getPos());
            }
            circuitMap.put(circuitId, positions);
        }
    }

    /**
     * Renders the highlighted circuits using RenderOutlineUtil.
     *
     * @param poseStack The current PoseStack for rendering.
     * @param buffer    The MultiBufferSource to obtain a VertexConsumer.
     */
    public static void renderCircuitHighlights(PoseStack poseStack, MultiBufferSource buffer) {
        if (circuitMap == null || circuitMap.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        // Iterate over each circuit and render each block position.
        for (Map.Entry<UUID, List<BlockPos>> entry : circuitMap.entrySet()) {
            UUID circuitId = entry.getKey();
            List<BlockPos> blockList = entry.getValue();
            int color = getColorForCircuit(circuitId);
            for (BlockPos pos : blockList) {
                double x = pos.getX() - camPos.x;
                double y = pos.getY() - camPos.y;
                double z = pos.getZ() - camPos.z;
                RenderOutlineUtil.drawBox(poseStack, buffer, x, y, z, color);
            }
        }
    }

    /**
     * Generates a unique ARGB color for the given circuit ID.
     *
     * @param circuitId The unique circuit ID.
     * @return A 32-bit ARGB color.
     */
    private static int getColorForCircuit(UUID circuitId) {
        int hash = circuitId.hashCode();
        // Ensure the color is fully opaque (alpha channel 0xFF) and use the lower 24 bits.
        return 0xFF000000 | (hash & 0x00FFFFFF);
    }
}
