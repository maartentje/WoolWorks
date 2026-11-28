package be.maarten.woolworks.client;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.machine.WorkerBlock;
import be.maarten.woolworks.machine.WorkerBlockEntity;
import be.maarten.woolworks.machine.WorkerArea;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = WoolWorks.MODID, value = Dist.CLIENT)
public final class WorkerVisualization {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(WoolWorks.MODID, "pipeline/worker_area"))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();
    private static final RenderType OUTLINE = RenderType.create("woolworks_worker_area",
            RenderSetup.builder(PIPELINE).createRenderSetup());
    private static BlockPos selected;
    private static ResourceKey<Level> dimension;
    private static Level selectedLevel;
    private WorkerVisualization() {}
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { selected = null; selectedLevel = null; dimension = null; }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || event.isCanceled() || be.maarten.woolworks.machine.MuffleInteractions.holdingMuffler(event.getEntity()) || !(WorkerBlockEntity.isShears(event.getEntity().getMainHandItem()) || WorkerBlockEntity.isShears(event.getEntity().getOffhandItem()))
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof WorkerBlock)) return;
        if (event.getPos().equals(selected) && selectedLevel == event.getLevel()) { selected = null; selectedLevel = null; }
        else { selected = event.getPos().immutable(); selectedLevel = event.getLevel(); dimension = event.getLevel().dimension(); }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent.AfterTranslucentParticles event) {
        if (selected == null) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || level != selectedLevel || !level.dimension().equals(dimension) || !level.hasChunkAt(selected)
                || !(level.getBlockEntity(selected) instanceof WorkerBlockEntity worker)) { selected = null; selectedLevel = null; return; }
        var box = WorkerArea.bounds(selected, worker.getBlockState().getValue(WorkerBlock.FACING), worker.area());
        var camera = event.getLevelRenderState().cameraRenderState.pos;
        var pose = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        ShapeRenderer.renderShape(pose, buffers.getBuffer(OUTLINE), Shapes.create(box), 0, 0, 0, 0xA633FF33, 2.0F);
        buffers.endBatch(OUTLINE);
        pose.popPose();
    }
}
