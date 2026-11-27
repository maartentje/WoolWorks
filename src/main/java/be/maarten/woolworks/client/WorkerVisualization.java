package be.maarten.woolworks.client;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.machine.WorkerBlock;
import be.maarten.woolworks.machine.WorkerBlockEntity;
import be.maarten.woolworks.machine.WorkerArea;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = WoolWorks.MODID, value = Dist.CLIENT)
public final class WorkerVisualization extends RenderStateShard {
    private static final RenderType OUTLINE = RenderType.create("woolworks_worker_area", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536,
            RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));
    private static BlockPos selected;
    private static ResourceKey<Level> dimension;
    private static Level selectedLevel;
    private WorkerVisualization() { super("woolworks_worker_area", () -> {}, () -> {}); }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { selected = null; selectedLevel = null; dimension = null; }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide || event.isCanceled() || be.maarten.woolworks.machine.MuffleInteractions.holdingMuffler(event.getEntity()) || !(WorkerBlockEntity.isShears(event.getEntity().getMainHandItem()) || WorkerBlockEntity.isShears(event.getEntity().getOffhandItem()))
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof WorkerBlock)) return;
        if (event.getPos().equals(selected) && selectedLevel == event.getLevel()) { selected = null; selectedLevel = null; }
        else { selected = event.getPos().immutable(); selectedLevel = event.getLevel(); dimension = event.getLevel().dimension(); }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || selected == null) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || level != selectedLevel || !level.dimension().equals(dimension) || !level.hasChunkAt(selected)
                || !(level.getBlockEntity(selected) instanceof WorkerBlockEntity worker)) { selected = null; selectedLevel = null; return; }
        var box = WorkerArea.bounds(selected, worker.getBlockState().getValue(WorkerBlock.FACING), worker.area());
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(OUTLINE), box, 0.2f, 1f, 0.2f, 0.65f);
        buffers.endBatch(OUTLINE);
        pose.popPose();
    }
}
