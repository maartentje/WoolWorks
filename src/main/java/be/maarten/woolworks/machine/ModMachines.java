package be.maarten.woolworks.machine;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.item.CableItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMachines {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WoolWorks.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WoolWorks.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, WoolWorks.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> GENERATOR_MENU = MENUS.register("wool_generator", () -> new MenuType<>(MachineMenu::generatorClient, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> EXTRACTOR_MENU = MENUS.register("wool_extractor", () -> new MenuType<>(MachineMenu::extractorClient, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<WorkerMenu>> WORKER_MENU = MENUS.register("wool_worker", () -> new MenuType<>(WorkerMenu::client, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredBlock<WorkerBlock> WORKER = BLOCKS.registerBlock("wool_worker", WorkerBlock::new, properties -> properties.mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> WORKER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("wool_worker", WORKER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkerBlockEntity>> WORKER_ENTITY = BLOCK_ENTITIES.register("wool_worker", () -> new BlockEntityType<>(WorkerBlockEntity::new, WORKER.get()));
    public static final DeferredBlock<GeneratorBlock> GENERATOR = BLOCKS.registerBlock("wool_generator", GeneratorBlock::new, properties -> properties.mapColor(MapColor.METAL).strength(3.5F).lightLevel(state -> state.getValue(GeneratorBlock.ACTIVE) ? 13 : 0).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> GENERATOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem("wool_generator", GENERATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR_ENTITY = BLOCK_ENTITIES.register("wool_generator", () -> new BlockEntityType<>(GeneratorBlockEntity::new, GENERATOR.get()));
    public static final DeferredBlock<ExtractorBlock> EXTRACTOR = BLOCKS.registerBlock("wool_extractor", ExtractorBlock::new, properties -> properties.mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> EXTRACTOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem("wool_extractor", EXTRACTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExtractorBlockEntity>> EXTRACTOR_ENTITY = BLOCK_ENTITIES.register("wool_extractor", () -> new BlockEntityType<>(ExtractorBlockEntity::new, EXTRACTOR.get()));
    public static final DeferredBlock<CableBlock> CABLE = BLOCKS.registerBlock("wool_insulated_cable", CableBlock::new, properties -> properties.mapColor(MapColor.WOOL).strength(1.0F).noOcclusion());
    public static final DeferredItem<BlockItem> CABLE_ITEM = ModItems.ITEMS.registerItem("wool_insulated_cable", properties -> new CableItem(CABLE.get(), properties));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE_ENTITY = BLOCK_ENTITIES.register("wool_insulated_cable", () -> new BlockEntityType<>(CableBlockEntity::new, CABLE.get()));

    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, GENERATOR_ENTITY.get(), (entity, side) -> entity.energy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, GENERATOR_ENTITY.get(), (entity, side) -> entity.automation());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, EXTRACTOR_ENTITY.get(), (entity, side) -> entity.energy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, EXTRACTOR_ENTITY.get(), (entity, side) -> entity.automation());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, WORKER_ENTITY.get(), (entity, side) -> entity.energy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, WORKER_ENTITY.get(), (entity, side) -> entity.automation());
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, entity, side) -> CableNetworks.port(level, pos, side), CABLE.get());
    }
    private ModMachines() {}
}
