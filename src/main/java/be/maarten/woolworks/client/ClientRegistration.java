package be.maarten.woolworks.client;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.registry.ModEntities;
import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.machine.ModMachines;
import be.maarten.woolworks.resource.ResourceTypes;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = WoolWorks.MODID, value = Dist.CLIENT)
public final class ClientRegistration {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RESOURCE_SHEEP.get(), ResourceSheepRenderer::new);
        event.registerBlockEntityRenderer(ModMachines.CABLE_ENTITY.get(), CableRenderer::new);
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(ModMachines.GENERATOR_MENU.get(), MachineScreen::new);
        event.register(ModMachines.EXTRACTOR_MENU.get(), MachineScreen::new);
        event.register(ModMachines.WORKER_MENU.get(), WorkerScreen::new);
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(WoolWorks.MODID, "resource"), ResourceTintSource.CODEC);
    }
}
