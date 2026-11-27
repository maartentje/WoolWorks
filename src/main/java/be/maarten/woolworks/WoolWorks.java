package be.maarten.woolworks;

import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.registry.ModEntities;
import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.resource.ResourceTypes;
import be.maarten.woolworks.recipe.ModRecipes;
import be.maarten.woolworks.machine.ModMachines;
import be.maarten.woolworks.machine.MachineConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(WoolWorks.MODID)
public class WoolWorks {
    public static final String MODID = "woolworks";
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    static {
        TABS.register("woolworks", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.woolworks"))
                .icon(() -> new ItemStack(ModItems.RESOURCE_FEED.get()))
                .displayItems((parameters, output) -> {
                    output.accept(ModMachines.GENERATOR_ITEM.get());
                    output.accept(ModMachines.EXTRACTOR_ITEM.get());
                    output.accept(ModMachines.WORKER_ITEM.get());
                    output.accept(ModMachines.CABLE_ITEM.get());
                    output.accept(ModItems.SPEED_UPGRADE.get());
                    output.accept(ModItems.OVERDRIVE_UPGRADE.get());
                    output.accept(ModItems.MUFFLER.get());
                    parameters.holders().lookup(ResourceTypes.REGISTRY).ifPresent(registry ->
                        registry.listElements().filter(holder -> ResourceTypes.enabled(holder.key().location(), holder.value())).forEach(holder -> {
                            var id = holder.key().location();
                            output.accept(ModItems.withResource(ModItems.RESOURCE_FEED.get(), id));
                            output.accept(ModItems.withResource(ModItems.INFUSED_WOOL.get(), id));
                        }));
                })
                .build());
    }

    public WoolWorks(IEventBus bus, ModContainer container) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(be.maarten.woolworks.recipe.WoolExtractionRecipe::tagsUpdated);
        container.registerConfig(ModConfig.Type.SERVER, MachineConfig.SPEC);
        bus.addListener(ResourceTypes::register);
        bus.addListener(ModEntities::attributes);
        bus.addListener(ModMachines::capabilities);
        ModMachines.BLOCKS.register(bus);
        ModMachines.BLOCK_ENTITIES.register(bus);
        ModMachines.MENUS.register(bus);
        ModComponents.TYPES.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.TYPES.register(bus);
        ModRecipes.TYPES.register(bus);
        ModRecipes.SERIALIZERS.register(bus);
        ModRecipes.INGREDIENTS.register(bus);
        be.maarten.woolworks.advancement.ModCriteria.TYPES.register(bus);
        TABS.register(bus);
    }
}
