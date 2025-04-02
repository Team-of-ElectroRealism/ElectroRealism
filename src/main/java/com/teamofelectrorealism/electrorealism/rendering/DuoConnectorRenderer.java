package com.teamofelectrorealism.electrorealism.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teamofelectrorealism.electrorealism.block.connector.duo.DuoConnectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class DuoConnectorRenderer {

    /**
     * Renders a custom outline box around the connection point (side) of the Duo Connector that the player is targeting.
     *
     * @param poseStack   The current PoseStack for rendering.
     * @param buffer      The MultiBufferSource for vertex output.
     * @param pos         The block position of the duo connector.
     * @param hitResult   The BlockHitResult containing the hit location.
     */
    public static void renderOutline(PoseStack poseStack, MultiBufferSource buffer, BlockPos pos, BlockHitResult hitResult) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Get the block entity at the position and ensure it's a duo connector.
        if (!(mc.level.getBlockEntity(pos) instanceof DuoConnectorBlockEntity duoEntity)) return;

        // Get the camera position for converting world coordinates to render coordinates.
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        // Calculate the block's center (which is used as the origin for connection point offsets).
        Vec3 blockCenter = Vec3.atCenterOf(pos); // equals (pos.x+0.5, pos.y+0.5, pos.z+0.5)

        // Convert the hit location to local coordinates relative to the block center.
        Vec3 hitPos = hitResult.getLocation();
        Vec3 localHit = hitPos.subtract(blockCenter);

        // Retrieve the connection point offsets for both sides.
        Vec3 cp0 = duoEntity.getConnectionPointOffset(0);
        Vec3 cp1 = duoEntity.getConnectionPointOffset(1);

        // Compare distances from the hit point to decide which connection point was targeted.
        double dist0 = localHit.distanceTo(cp0);
        double dist1 = localHit.distanceTo(cp1);
        int selected = (dist0 < dist1) ? 0 : 1;

        // Choose the connection point based on the comparison.
        Vec3 selectedCP = (selected == 0 ? cp0 : cp1);

        // Convert the selected local connection point back to world coordinates.
        Vec3 worldCP = blockCenter.add(selectedCP);

        // Determine the outline box dimensions.
        double boxSize = (double) 4 /16;
        // Calculate the minimum coordinates for the box relative to the camera.
        double minX = worldCP.x - boxSize / 2 - camPos.x;
        double minY = worldCP.y - boxSize / 2 - camPos.y;
        double minZ = worldCP.z - boxSize / 2 - camPos.z;

        // Choose an outline color (here opaque green is used as an example).
        int color = 0xFF00FF00; // ARGB format.

        // Draw the custom outline box using your RenderOutlineUtil.
        RenderOutlineUtil.drawCustomBox(poseStack, buffer, minX, minY, minZ, boxSize, boxSize, boxSize, color);
    }
}
