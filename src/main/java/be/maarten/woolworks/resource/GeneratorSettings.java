package be.maarten.woolworks.resource;

import be.maarten.woolworks.WoolWorks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record GeneratorSettings(boolean allowFurnaceFuels) {
    public static final ResourceKey<Registry<GeneratorSettings>> REGISTRY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(WoolWorks.MODID, "generator_settings"));
    private static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath(WoolWorks.MODID, "default");
    public static final Codec<GeneratorSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("allow_furnace_fuels").forGetter(GeneratorSettings::allowFurnaceFuels)
    ).apply(instance, GeneratorSettings::new));

    public static boolean allowsFurnaceFuels(Level level) {
        return level.registryAccess().registry(REGISTRY).map(registry -> registry.get(DEFAULT_ID))
                .map(GeneratorSettings::allowFurnaceFuels).orElse(false);
    }
}
