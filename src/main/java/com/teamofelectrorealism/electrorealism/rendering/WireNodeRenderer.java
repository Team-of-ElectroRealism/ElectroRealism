package com.teamofelectrorealism.electrorealism.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teamofelectrorealism.electrorealism.power.IWireNode;
import com.teamofelectrorealism.electrorealism.power.WireType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class WireNodeRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    public WireNodeRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    private static final float HANG = 0.5f;
    private float time = 0f;

    @Override
    public void render(T blockEntityIn, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        IWireNode blockEntity = (IWireNode) blockEntityIn;

        time += partialTicks;

        for (int i = 0; i < blockEntity.getConnectionPointCount(); i++) {
            if (!blockEntity.hasConnection(i)) continue;
            Vec3 nodeOffset = blockEntity.getConnectionPointOffset(i);
            float nodeOffsetX = ((float) nodeOffset.x());
            float nodeOffsetY = ((float) nodeOffset.y());
            float nodeOffsetZ = ((float) nodeOffset.z());

            IWireNode wireNode = blockEntity.getWireNode(i);
            if (wireNode == null) return;

            Vec3 connectingNodeOffset = wireNode.getConnectionPointOffset(blockEntity.getConnectingConnectionPointIndex(i));
            float connectingNodeOffsetX = ((float) connectingNodeOffset.x());
            float connectingNodeOffsetY = ((float) connectingNodeOffset.y());
            float connectingNodeOffsetZ = ((float) connectingNodeOffset.z());
            BlockPos connectingPos = blockEntity.getConnectorPos(i);

            float relativeOffsetX = connectingPos.getX() - blockEntity.getPos().getX();
            float relativeOffsetY = connectingPos.getY() - blockEntity.getPos().getY();
            float relativeOffsetZ = connectingPos.getZ() - blockEntity.getPos().getZ();
            matrixStackIn.pushPose();

            float offsetDistance = distanceFromZero(relativeOffsetX, relativeOffsetY, relativeOffsetZ);

            matrixStackIn.translate(relativeOffsetX + .5f + connectingNodeOffsetX, relativeOffsetY + .5f + connectingNodeOffsetY, relativeOffsetZ + .5f + connectingNodeOffsetZ);
            wireRender(
                    blockEntityIn,
                    connectingPos,
                    matrixStackIn,
                    bufferIn,
                    -relativeOffsetX - connectingNodeOffsetX + nodeOffsetX,
                    -relativeOffsetY - connectingNodeOffsetY + nodeOffsetY,
                    -relativeOffsetZ - connectingNodeOffsetZ + nodeOffsetZ,
                    blockEntity.getWireType(i),
                    offsetDistance
            );
            matrixStackIn.popPose();
        }
    }



    private static float divf(int a, int b) {
        return (float) a / (float) b;
    }

    private static float hang(float f, float dis) {
        return (float) Math.sin(-f * (float) Math.PI) * (HANG * dis / (float) 16); // use config here todo
    }

    public static float distanceFromZero(float x, float y, float z) {
        return (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    public static void wireRender(BlockEntity tileEntityIn, BlockPos other, PoseStack stack, MultiBufferSource buffer, float x, float y, float z, WireType type, float dis) {

        VertexConsumer ivertexbuilder = buffer.getBuffer(ModRenderType.WIRE);
        Matrix4f matrix4f = stack.last().pose();
        float f = (float) (Mth.fastInvSqrt(x * x + z * z) * 0.025F / 2.0F);
        float o1 = z * f;
        float o2 = x * f;
        BlockPos blockpos1 = tileEntityIn.getBlockPos();
        BlockPos blockpos2 = other;

        int i = tileEntityIn.getLevel().getBrightness(LightLayer.BLOCK, blockpos1);
        int j = tileEntityIn.getLevel().getBrightness(LightLayer.BLOCK, blockpos2);
        int k = tileEntityIn.getLevel().getBrightness(LightLayer.SKY, blockpos1);
        int l = tileEntityIn.getLevel().getBrightness(LightLayer.SKY, blockpos2);
        wirePart(ivertexbuilder, matrix4f, x, y, z, j, i, l, k, 0.025F, 0.025F, o1, o2, type, dis, tileEntityIn.getBlockState(), stack, 0, 1f);
        wirePart(ivertexbuilder, matrix4f, x, y, z, j, i, l, k, 0.025F, 0.0F, o1, o2, type, dis, tileEntityIn.getBlockState(), stack, 1, 1f);
        //light
        //matrix.popPose();
    }

    public static void wirePart(VertexConsumer vertBuilder, Matrix4f matrix, float x, float y, float z, int l1, int l2,
                                int l3, int l4, float a, float b, float o1, float o2, WireType type, float dis, BlockState state, PoseStack stack, int lightOffset, float hangFactor) {
        for (int j = 0; j < 24; ++j) {
            float f = (float) j / 23.0F;
            int k = (int) Mth.lerp(f, (float) l1, (float) l2);
            int l = (int) Mth.lerp(f, (float) l3, (float) l4);
            int light = LightTexture.pack(k, l);

            wireVert(vertBuilder, matrix, light, x, y, z, a, b, 24, j, false, o1, o2, type, dis, state, stack, lightOffset, hangFactor);
            wireVert(vertBuilder, matrix, light, x, y, z, a, b, 24, j + 1, true, o1, o2, type, dis, state, stack, lightOffset+1, hangFactor);

        }
    }

    // giga sketch
    public static void wireVert(VertexConsumer vertBuilder, Matrix4f matrix, int light, float x, float y, float z,
                                float a, float b, int count, int index, boolean sw, float o1, float o2, WireType type, float dis, BlockState state, PoseStack stack, int lightOffset, float hangFactor) {
        int cr = type.getColorRed();
        int cg = type.getColorGreen();
        int cb = type.getColorBlue();
        if (index % 2 == 0) {
            cr *= 0.7F;
            cg *= 0.7F;
            cb *= 0.7F;
        }

        float part = (float) index / (float) count;
        float fx = x * part;
        float fy = (y > 0.0F ? y * part * part : y - y * (1.0F - part) * (1.0F - part)) + (hangFactor*hang(divf(index, count), dis));
        float fz = z * part;

        //System.out.println((fx + o1) +":"+ (fy + n1 - n2) +":"+ (fz - o2));


        if(Math.abs(x) + Math.abs(z) < Math.abs(y)) {
            boolean p = b > 0;
            float c = 0.015f;

            if (!sw) {
                vertBuilder.addVertex(matrix, fx -c, fy, fz + (p?-c:c)).setColor(cr, cg, cb, 255).setLight(light); // might be really wrong
            }

            vertBuilder.addVertex(matrix, fx + c, fy, fz + (p?c:-c)).setColor(cr, cg, cb, 255).setLight(light);
            if (sw) {
                vertBuilder.addVertex(matrix, fx -c, fy, fz + (p?-c:c)).setColor(cr, cg, cb, 255).setLight(light);
            }
        }
        else {
            if (!sw) {
                vertBuilder.addVertex(matrix, fx + o1, fy + a - b, fz - o2).setColor(cr, cg, cb, 255).setLight(light);
            }

            vertBuilder.addVertex(matrix, fx - o1, fy + b, fz + o2).setColor(cr, cg, cb, 255).setLight(light);
            if (sw) {
                vertBuilder.addVertex(matrix, fx + o1, fy + a - b, fz - o2).setColor(cr, cg, cb, 255).setLight(light);
            }
        }
    }
}