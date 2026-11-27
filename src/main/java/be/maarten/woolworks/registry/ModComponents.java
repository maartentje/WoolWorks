package be.maarten.woolworks.registry;

import be.maarten.woolworks.WoolWorks;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, WoolWorks.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> RESOURCE_TYPE = TYPES.register(
            "resource_type", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    private ModComponents() {}
}
