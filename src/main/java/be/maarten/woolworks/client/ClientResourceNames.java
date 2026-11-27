package be.maarten.woolworks.client;

import be.maarten.woolworks.resource.ResourceType;
import be.maarten.woolworks.resource.ResourceTypes;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import be.maarten.woolworks.registry.ModItems;
import java.util.stream.Stream;

public final class ClientResourceNames {
    private ClientResourceNames() {}

    public static Optional<String> translationKey(ResourceLocation id) {
        var level = Minecraft.getInstance().level;
        if (level == null) return Optional.empty();
        return level.registryAccess().registry(ResourceTypes.REGISTRY)
                .map(registry -> registry.get(id)).map(ResourceType::translationKey);
    }

    public static Stream<ItemStack> infusedWoolVariants() {
        var level = Minecraft.getInstance().level;
        if (level == null) return Stream.of(new ItemStack(ModItems.INFUSED_WOOL.get()));
        return level.registryAccess().registry(ResourceTypes.REGISTRY)
                .map(registry -> registry.keySet().stream().filter(id -> ResourceTypes.enabled(level, id)).map(id -> ModItems.withResource(ModItems.INFUSED_WOOL.get(), id)))
                .orElseGet(() -> Stream.of(new ItemStack(ModItems.INFUSED_WOOL.get())));
    }
}
