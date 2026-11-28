package be.maarten.woolworks.client;

import be.maarten.woolworks.machine.CableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

public final class CableRenderer implements BlockEntityRenderer<CableBlockEntity, CableRenderer.State> {
    public CableRenderer(BlockEntityRendererProvider.Context context) {}

    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    public static final class State extends BlockEntityRenderState {
        final EnumSet<Direction> extracting = EnumSet.noneOf(Direction.class);
        final BlockModelRenderState indicator = new BlockModelRenderState();
        boolean squares;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(CableBlockEntity cable, State state, float partialTick, Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(cable, state, partialTick, cameraPosition, breakProgress);
        state.extracting.clear();
        for (Direction side : Direction.values()) if (cable.isExtracting(side)) state.extracting.add(side);
        state.squares = ClientConfig.CABLE_EXTRACTION_INDICATOR_STYLE.get() == ClientConfig.IndicatorStyle.SQUARE;
        if (!state.extracting.isEmpty()) Minecraft.getInstance().getBlockModelResolver()
                .update(state.indicator, Blocks.LIME_WOOL.defaultBlockState(), DISPLAY);
        else state.indicator.clear();
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (Direction side : state.extracting) {
            if (state.squares) {
                renderSquares(side, pose, collector, state);
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
            state.indicator.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    private static void renderSquares(Direction side, PoseStack pose, SubmitNodeCollector collector, State state) {
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
            state.indicator.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
