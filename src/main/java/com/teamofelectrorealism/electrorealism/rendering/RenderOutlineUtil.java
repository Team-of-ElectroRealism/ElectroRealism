package com.teamofelectrorealism.electrorealism.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;

public class RenderOutlineUtil {

    /**
     * Draws an outline around a block at the specified world coordinates.
     *
     * @param poseStack The current PoseStack for rendering.
     * @param buffer    The MultiBufferSource to get a VertexConsumer.
     * @param x         The x coordinate of the block (world space, relative to camera already applied).
     * @param y         The y coordinate of the block.
     * @param z         The z coordinate of the block.
     * @param color     The ARGB color for the outline.
     */
    public static void drawOutline(PoseStack poseStack, MultiBufferSource buffer,
                                        double x, double y, double z, int color) {
        // Create an AABB representing a standard block (1x1x1).
        AABB aabb = new AABB(x, y, z, x + 1, y + 1, z + 1);

        // Extract the color components (normalized to 0.0 - 1.0).
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // Obtain a VertexConsumer for line rendering.
        VertexConsumer builder = buffer.getBuffer(RenderType.lines());

        // Use Minecraft's built-in method to render a line box.
        LevelRenderer.renderLineBox(poseStack, builder, aabb, r, g, b, a);
    }
}
