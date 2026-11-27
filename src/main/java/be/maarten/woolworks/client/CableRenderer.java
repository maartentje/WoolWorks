package be.maarten.woolworks.client;

import be.maarten.woolworks.machine.CableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

public final class CableRenderer implements BlockEntityRenderer<CableBlockEntity> {
    public CableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(CableBlockEntity cable, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        for (Direction side : Direction.values()) {
            if (!cable.isExtracting(side)) continue;
            if (ClientConfig.CABLE_EXTRACTION_INDICATOR_STYLE.get() == ClientConfig.IndicatorStyle.SQUARE) {
                renderSquares(side, pose, buffer, light, overlay);
                continue;
            }
            pose.pushPose();
            switch (side) {
                case DOWN -> { pose.translate(0.245, 0.07, 0.245); pose.scale(0.51F, 0.13F, 0.51F); }
                case UP -> { pose.translate(0.245, 0.80, 0.245); pose.scale(0.51F, 0.13F, 0.51F); }
                case NORTH -> { pose.translate(0.245, 0.245, 0.07); pose.scale(0.51F, 0.51F, 0.13F); }
                case SOUTH -> { pose.translate(0.245, 0.245, 0.80); pose.scale(0.51F, 0.51F, 0.13F); }
                case WEST -> { pose.translate(0.07, 0.245, 0.245); pose.scale(0.13F, 0.51F, 0.51F); }
                case EAST -> { pose.translate(0.80, 0.245, 0.245); pose.scale(0.13F, 0.51F, 0.51F); }
            }
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.LIME_WOOL.defaultBlockState(), pose, buffer, light, overlay);
            pose.popPose();
        }
    }

    private static void renderSquares(Direction side, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        double axis = side.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 0.125 : 0.875;
        for (int face = 0; face < 4; face++) {
            double a = face == 0 ? 0.244 : face == 1 ? 0.746 : 0.4375;
            double b = face == 2 ? 0.244 : face == 3 ? 0.746 : 0.4375;
            float sa = face < 2 ? 0.01F : 0.125F;
            float sb = face >= 2 ? 0.01F : 0.125F;
            pose.pushPose();
            switch (side.getAxis()) {
                case X -> { pose.translate(axis - 0.0625, a, b); pose.scale(0.125F, sa, sb); }
                case Y -> { pose.translate(a, axis - 0.0625, b); pose.scale(sa, 0.125F, sb); }
                case Z -> { pose.translate(a, b, axis - 0.0625); pose.scale(sa, sb, 0.125F); }
            }
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.LIME_WOOL.defaultBlockState(), pose, buffer, light, overlay);
            pose.popPose();
        }
    }
}
