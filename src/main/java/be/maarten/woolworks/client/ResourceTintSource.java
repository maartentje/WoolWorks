package be.maarten.woolworks.client;

import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.resource.ResourceTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record ResourceTintSource() implements ItemTintSource {
    public static final MapCodec<ResourceTintSource> CODEC = MapCodec.unit(new ResourceTintSource());

    @Override public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        var world = level != null ? level : Minecraft.getInstance().level;
        var id = stack.get(ModComponents.RESOURCE_TYPE);
        if (world == null || id == null) return -1;
        return world.registryAccess().lookup(ResourceTypes.REGISTRY)
                .map(registry -> registry.getValue(id)).map(type -> 0xFF000000 | type.color()).orElse(-1);
    }

    @Override public MapCodec<ResourceTintSource> type() { return CODEC; }
}
