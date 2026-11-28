package be.maarten.woolworks.recipe;

import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.resource.ResourceTypes;
import java.util.stream.Stream;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/** Resolves component-bearing variants from the synchronized resource registry. */
public final class InfusedWoolSlotDisplay implements SlotDisplay {
    public static final InfusedWoolSlotDisplay INSTANCE = new InfusedWoolSlotDisplay();
    private InfusedWoolSlotDisplay() {}

    @Override public Type<InfusedWoolSlotDisplay> type() { return ModRecipes.INFUSED_WOOL_DISPLAY.get(); }

    @Override public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
        if (!(factory instanceof DisplayContentsFactory.ForStacks<T> stacks)) return Stream.empty();
        var registries = context.getOptional(SlotDisplayContext.REGISTRIES);
        if (registries == null) return Stream.empty();
        return registries.lookup(ResourceTypes.REGISTRY).stream().flatMap(registry -> registry.listElements())
                .filter(holder -> ResourceTypes.enabled(holder.key().identifier(), holder.value()))
                .map(holder -> stacks.forStack(ModItems.withResource(ModItems.INFUSED_WOOL.get(), holder.key().identifier())));
    }
}
