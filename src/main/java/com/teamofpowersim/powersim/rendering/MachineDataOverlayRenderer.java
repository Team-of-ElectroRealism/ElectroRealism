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

    private static final Minecraft MC = Minecraft.getInstance();
    private static final double MAX_DISTANCE_SQR = 64 * 64;   // 64-block cull radius
    private static final float  SCALE            = 0.025f;    // Mojang name-tag scale

    private static long nextChatTick = 0;

    private MachineDataOverlayRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        Level level = MC.level;
        if (level == null) return;

        /* ---------------- block the player is looking at ---------------- */
        HitResult hit = MC.hitResult;
        if (!(hit instanceof BlockHitResult blockHit &&
                hit.getType() == HitResult.Type.BLOCK)) return;
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

            String text = format(machine);

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
                chatOncePerSecond(machine, text);
            }
        }

        buffer.endBatch();   // flush the text draw calls

        LOGGER.debug("Rendered {} machine labels at {}", members.size(), lookPos);
    }

    /* Helper – tweak to taste */
    private static String format(AbstractMachineBlockEntity m) {
        double v = m.getSimVoltage();
        double i = m.getSimCurrent();
        return String.format("%.0f V  %.1f A  %.0f W", v, i, v * i);
    }

    private static void chatOncePerSecond(AbstractMachineBlockEntity machine,
                                          String dataLine) {
        var player = MC.player;
        var level  = MC.level;
        if (player == null || level == null) return;

        long now = level.getGameTime();
        if (now < nextChatTick) return;          // 1-second cooldown

        /* Get the human-readable (translated) block name */
        var name = machine.getBlockState()
                .getBlock()
                .getName()            // Component
                .getString();         // plain text

        player.sendSystemMessage(
                Component.literal("[PowerSim] ")
                        .append(name + ": " + dataLine)
        );
        nextChatTick = now + 20;                // 20 ticks ≈ 1 s
    }
}
