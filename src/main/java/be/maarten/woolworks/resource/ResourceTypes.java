package be.maarten.woolworks.resource;

import be.maarten.woolworks.WoolWorks;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class ResourceTypes {
    public static final ResourceKey<Registry<ResourceType>> REGISTRY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(WoolWorks.MODID, "resource_type"));

    private ResourceTypes() {}

    public static boolean enabled(net.minecraft.world.level.Level level, Identifier id) {
        return id != null && level.registryAccess().lookup(REGISTRY)
                .map(registry -> registry.getValue(id)).map(type -> enabled(id, type)).orElse(false);
    }

    public static boolean enabled(Identifier id, ResourceType type) {
        return enabled(id, type, be.maarten.woolworks.machine.MachineConfig.DISABLED_RESOURCES.get().contains(id.toString()));
    }

    public static boolean enabled(Identifier id, ResourceType type, boolean individuallyDisabled) {
        return type.enabled() && type.category().enabled() && !individuallyDisabled
                && (!type.automatic() || be.maarten.woolworks.machine.MachineConfig.ENABLE_AUTOMATIC_MODDED_RESOURCES.get())
                && type.requiredItemTag().map(tag -> net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag))
                .map(items -> items.iterator().hasNext()).orElse(false)).orElse(true);
    }

    public static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, ResourceType.CODEC, ResourceType.CODEC);
        event.dataPackRegistry(GeneratorSettings.REGISTRY, GeneratorSettings.CODEC, GeneratorSettings.CODEC);
    }
}
