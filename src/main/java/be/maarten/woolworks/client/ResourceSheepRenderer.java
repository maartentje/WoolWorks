package be.maarten.woolworks.client;

import be.maarten.woolworks.entity.ResourceSheep;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class ResourceSheepRenderer extends MobRenderer<ResourceSheep, SheepModel<ResourceSheep>> {
    private static final ResourceLocation BODY = ResourceLocation.withDefaultNamespace("textures/entity/sheep/sheep.png");
    private static final ResourceLocation FUR = ResourceLocation.fromNamespaceAndPath("woolworks", "textures/entity/sheep/resource_sheep_fleece.png");
    private static final ResourceLocation FUR_OVERLAY = ResourceLocation.fromNamespaceAndPath("woolworks", "textures/entity/sheep/resource_sheep_fleece_overlay.png");

    public ResourceSheepRenderer(EntityRendererProvider.Context context) {
        super(context, new SheepModel<>(context.bakeLayer(ModelLayers.SHEEP)), 0.7F);
        SheepFurModel<ResourceSheep> fur = new SheepFurModel<>(context.bakeLayer(ModelLayers.SHEEP_FUR));
        addLayer(new RenderLayer<>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffer, int light, ResourceSheep sheep,
                    float limbSwing, float limbAmount, float partialTick, float age, float yaw, float pitch) {
                if (sheep.isSheared()) return;
                if (sheep.isInvisible()) {
                    // Match vanilla's outline behavior without revealing invisible fleece.
                    if (Minecraft.getInstance().shouldEntityAppearGlowing(sheep)) {
                        getParentModel().copyPropertiesTo(fur);
                        fur.prepareMobModel(sheep, limbSwing, limbAmount, partialTick);
                        fur.setupAnim(sheep, limbSwing, limbAmount, age, yaw, pitch);
                        VertexConsumer outline = buffer.getBuffer(RenderType.outline(FUR));
                        fur.renderToBuffer(pose, outline, light, LivingEntityRenderer.getOverlayCoords(sheep, 0.0F), 0xFF000000);
                    }
                    return;
                }
                int color = sheep.resourceType().map(type -> 0xFF000000 | type.color()).orElse(0xFFFFFFFF);
                coloredCutoutModelCopyLayerRender(getParentModel(), fur, FUR, pose, buffer, light, sheep,
                        limbSwing, limbAmount, age, yaw, pitch, partialTick, 0xFFFFFFFF);
                // The vanilla z-offset render type draws the second texture on the same animated model
                // without depth fighting; the supplied overlay uses binary transparency.
                VertexConsumer overlay = buffer.getBuffer(RenderType.entityCutoutNoCullZOffset(FUR_OVERLAY));
                fur.renderToBuffer(pose, overlay, light, LivingEntityRenderer.getOverlayCoords(sheep, 0.0F), color);
            }
        });
    }

    @Override public ResourceLocation getTextureLocation(ResourceSheep sheep) { return BODY; }

}
