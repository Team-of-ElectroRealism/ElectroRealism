package com.teamofpowersim.powersim.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.block.machine.AbstractMachineBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MachineDataOverlayRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static BlockPos lastChattedBlockPos = null;
    private static String lastChattedData = null;

    private static final Minecraft MC = Minecraft.getInstance();
    private static final double MAX_DISTANCE_SQR = 64 * 64;   // 64-block cull radius
    private static final float  SCALE            = 0.025f;    // Mojang name-tag scale

    private MachineDataOverlayRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        Level level = MC.level;
        // If level or player is null, or not looking at a block, reset state.
        if (level == null || MC.player == null) { // Added MC.player == null check for robustness
            lastChattedBlockPos = null;
            lastChattedData = null;
            return;
        }

        HitResult hit = MC.hitResult;
        if (!(hit instanceof BlockHitResult blockHit &&
                hit.getType() == HitResult.Type.BLOCK)) {
            // Player is not looking at a block.
            lastChattedBlockPos = null;
            lastChattedData = null;
            return;
        }
        BlockPos lookPos = blockHit.getBlockPos();

        /* ---------------- find the matching network -------------------- */
        Map<UUID, List<BlockPos>> networks =
                PowerSim.NETWORK_MANAGER.getNetworksDataForClient();

        List<BlockPos> members = null;
        for (List<BlockPos> list : networks.values()) {
            if (list.contains(lookPos)) {            // exact match – great!
                members = list;
                break;
            }
            /* fallback: same chunk-section ⇒ probably same cables */
            if (members == null && list.stream().anyMatch(p ->
                    (p.getX() >> 4) == (lookPos.getX() >> 4) &&
                            (p.getZ() >> 4) == (lookPos.getZ() >> 4)))
                members = list;
        }
        if (members == null) return;

        /* ---------------- set-up common render objects ------------------ */
        Camera                camera     = MC.gameRenderer.getMainCamera();
        var                   camPos     = camera.getPosition();
        PoseStack             poseStack  = event.getPoseStack();
        BufferSource          buffer     = MC.renderBuffers().bufferSource();
        EntityRenderDispatcher dispatcher = MC.getEntityRenderDispatcher();
        Font                  font       = MC.font;

        /* ---------------- render one label for every machine ------------ */
        for (BlockPos pos : members) {
            if (!(level.getBlockEntity(pos) instanceof AbstractMachineBlockEntity machine))
                continue;

            double dx = pos.getX() + 0.5 - camPos.x;
            double dy = pos.getY() + 1.20 - camPos.y;      // a tad above the block
            double dz = pos.getZ() + 0.5 - camPos.z;
            if (dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQR) continue; // distance cull

            String text = getDataLine(machine);

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            poseStack.mulPose(dispatcher.cameraOrientation()); // billboard
            poseStack.scale(-SCALE, -SCALE, SCALE);            // mirror + scale

            float half = font.width(text) / 2f;
            int   light = LevelRenderer.getLightColor(level, pos);

            font.drawInBatch(
                    text, -half, 0,
                    0xFFFFFFFF,          // white
                    false,
                    poseStack.last().pose(),
                    buffer,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    light
            );
            poseStack.popPose();

            if (pos.equals(lookPos)) {           // <= player’s cross-hair target
                chatOnNewData(machine, text);
            }
        }

        buffer.endBatch();   // flush the text draw calls

        LOGGER.debug("Rendered {} machine labels at {}", members.size(), lookPos);
    }

    private static String getDataLine(AbstractMachineBlockEntity m) {
        double v = m.getSimVoltage();
        double i = m.getSimCurrent();
        double p = v * i;

        return String.format("%s  %s  %s",
                formatWithBestPrefix(v, "V"),
                formatWithBestPrefix(i, "A"),
                formatWithBestPrefix(p, "W"));
    }

    private static String formatWithBestPrefix(double value, String unit) {
        // Ordered from smaller to larger
        final String[] prefixes = {"n", "µ", "m", "", "k", "M", "G", "T"};
        final double[] factors  = {1e-9, 1e-6, 1e-3, 1, 1e3, 1e6, 1e9, 1e12};

        double abs = Math.abs(value);
        int idx = prefixes.length - 1;            // default → biggest

        // Pick the largest factor that keeps the number ≥ 1
        for (int i = 0; i < factors.length; i++) {
            if (abs < factors[i] * 1_000) {       // 1 – 999 window
                idx = i;
                break;
            }
        }

        double scaled = value / factors[idx];

        // Choose # decimals: 0 for ≥100, 1 for ≥10, 2 otherwise
        String fmt = scaled >= 100   ? "%.0f %s%s"
                : scaled >= 10    ? "%.1f %s%s"
                :                   "%.2f %s%s";

        return String.format(fmt, scaled, prefixes[idx], unit);
    }

    private static void chatOnNewData(AbstractMachineBlockEntity machine, String dataLine) {
            var player = MC.player;
            // Level is already confirmed not null by the render method's start.
            // Player null check is important.
            if (player == null) {
                return;
            }

            BlockPos currentBlockPos = machine.getBlockPos();
            boolean shouldPrint = false;

            // Condition 1: Player is looking at a new block, or looking at a block for the first time
            // after looking away (lastChattedBlockPos would be null).
            if (!currentBlockPos.equals(lastChattedBlockPos)) {
                shouldPrint = true;
            }
            // Condition 2: Player is looking at the same block, but its data has changed.
            else if (!dataLine.equals(lastChattedData)) {
                shouldPrint = true;
            }

            if (shouldPrint) {
                // Get the human-readable (translated) block name
                var name = machine.getBlockState()
                        .getBlock()
                        .getName()            // Component
                        .getString();         // plain text

                player.sendSystemMessage(
                        Component.literal("[PowerSim] ")
                                .append(name + ": " + dataLine)
                );

                // Update the state with the latest printed block and data
                lastChattedBlockPos = currentBlockPos;
                lastChattedData = dataLine;
            }
        }
}
