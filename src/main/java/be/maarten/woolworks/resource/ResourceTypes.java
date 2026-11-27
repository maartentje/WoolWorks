package be.maarten.woolworks.resource;

import be.maarten.woolworks.WoolWorks;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class ResourceTypes {
    public static final ResourceKey<Registry<ResourceType>> REGISTRY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(WoolWorks.MODID, "resource_type"));

    private ResourceTypes() {}

    public static boolean enabled(net.minecraft.world.level.Level level, ResourceLocation id) {
        return id != null && level.registryAccess().registry(REGISTRY)
                .map(registry -> registry.get(id)).map(type -> enabled(id, type)).orElse(false);
    }

    public static boolean enabled(ResourceLocation id, ResourceType type) {
        return type.enabled() && !be.maarten.woolworks.machine.MachineConfig.DISABLED_RESOURCES.get().contains(id.toString())
                && (!type.automatic() || be.maarten.woolworks.machine.MachineConfig.ENABLE_AUTOMATIC_MODDED_RESOURCES.get())
                && type.requiredItemTag().map(tag -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag))
                .map(items -> items.iterator().hasNext()).orElse(false)).orElse(true);
    }

    public static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, ResourceType.CODEC, ResourceType.CODEC);
        event.dataPackRegistry(GeneratorSettings.REGISTRY, GeneratorSettings.CODEC, GeneratorSettings.CODEC);
    }
}
