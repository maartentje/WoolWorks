package be.maarten.woolworks.client;

import be.maarten.woolworks.entity.ResourceSheep;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.sheep.BabySheepModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class ResourceSheepRenderer extends AgeableMobRenderer<ResourceSheep, ResourceSheepRenderer.State, SheepModel> {
    private static final Identifier BODY = Identifier.withDefaultNamespace("textures/entity/sheep/sheep.png");
    private static final Identifier BABY_BODY = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_baby.png");
    private static final Identifier FUR = Identifier.fromNamespaceAndPath("woolworks", "textures/entity/sheep/resource_sheep_fleece.png");
    private static final Identifier FUR_OVERLAY = Identifier.fromNamespaceAndPath("woolworks", "textures/entity/sheep/resource_sheep_fleece_overlay.png");

    public static final class State extends SheepRenderState {
        int resourceColor = 0xFFFFFFFF;
    }

    public ResourceSheepRenderer(EntityRendererProvider.Context context) {
        super(context, new SheepModel(context.bakeLayer(ModelLayers.SHEEP)),
                new BabySheepModel(context.bakeLayer(ModelLayers.SHEEP_BABY)), 0.7F);
        var adultFur = new SheepFurModel(context.bakeLayer(ModelLayers.SHEEP_WOOL));
        var babyFur = new SheepFurModel(context.bakeLayer(ModelLayers.SHEEP_BABY_WOOL));
        addLayer(new RenderLayer<State, SheepModel>(this) {
            @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yaw, float pitch) {
                if (state.isSheared) return;
                var fur = state.isBaby ? babyFur : adultFur;
                int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
                if (state.isInvisible) {
                    if (state.appearsGlowing()) collector.submitModel(fur, state, pose, RenderTypes.outline(FUR),
                            light, overlay, 0xFF000000, null, state.outlineColor, null);
                    return;
                }
                coloredCutoutModelCopyLayerRender(fur, FUR, pose, collector, light, state, 0xFFFFFFFF, 0);
                collector.order(1).submitModel(fur, state, pose, RenderTypes.entityCutoutZOffset(FUR_OVERLAY),
                        light, overlay, state.resourceColor, null, state.outlineColor, null);
            }
        });
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(ResourceSheep sheep, State state, float partialTick) {
        super.extractRenderState(sheep, state, partialTick);
        state.headEatAngleScale = sheep.getHeadEatAngleScale(partialTick);
        state.headEatPositionScale = sheep.getHeadEatPositionScale(partialTick);
        state.isSheared = sheep.isSheared();
        state.resourceColor = sheep.resourceType().map(type -> 0xFF000000 | type.color()).orElse(0xFFFFFFFF);
    }

    @Override public Identifier getTextureLocation(State state) { return state.isBaby ? BABY_BODY : BODY; }
}
