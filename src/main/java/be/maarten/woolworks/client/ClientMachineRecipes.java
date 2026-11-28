package be.maarten.woolworks.client;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.recipe.MachineRecipesPayload;
import be.maarten.woolworks.recipe.MachineRecipeLookup;
import be.maarten.woolworks.recipe.GeneratorFuelRecipe;
import be.maarten.woolworks.recipe.WoolExtractionRecipe;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.resources.ResourceKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

/** The server-provided machine recipe snapshot; never used for authoritative processing. */
@EventBusSubscriber(modid = WoolWorks.MODID, value = Dist.CLIENT)
public final class ClientMachineRecipes {
    public record Snapshot(List<RecipeHolder<GeneratorFuelRecipe>> fuels, List<RecipeHolder<WoolExtractionRecipe>> extraction) {
        public Snapshot { fuels = List.copyOf(fuels); extraction = List.copyOf(extraction); }
    }
    private static Snapshot pending;
    private static Snapshot current;
    private ClientMachineRecipes() {}

    public static Optional<Snapshot> snapshot() { return Optional.ofNullable(current); }
    /** Outer empty means not synchronized; inner empty means synchronized but no matching recipe. */
    public static Optional<Optional<RecipeHolder<GeneratorFuelRecipe>>> fuel(net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack stack) {
        return snapshot().map(value -> MachineRecipeLookup.fuel(level, stack, value.fuels()));
    }
    public static Optional<Optional<RecipeHolder<WoolExtractionRecipe>>> extraction(net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack stack) {
        return snapshot().map(value -> MachineRecipeLookup.extraction(level, stack, value.extraction()));
    }

    public static void registerPayload(net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent event) {
        event.register(MachineRecipesPayload.TYPE, (payload, context) -> receivePayload(payload));
    }

    private static void receivePayload(MachineRecipesPayload payload) {
        current = null;
        pending = null;
        var ids = new HashSet<ResourceKey<Recipe<?>>>();
        var fuels = new java.util.ArrayList<RecipeHolder<GeneratorFuelRecipe>>();
        var extraction = new java.util.ArrayList<RecipeHolder<WoolExtractionRecipe>>();
        for (var holder : payload.fuels()) {
            if (!ids.add(holder.id()) || !(holder.value() instanceof GeneratorFuelRecipe recipe)) return;
            fuels.add(new RecipeHolder<>(holder.id(), recipe));
        }
        for (var holder : payload.extraction()) {
            if (!ids.add(holder.id()) || !(holder.value() instanceof WoolExtractionRecipe recipe)) return;
            extraction.add(new RecipeHolder<>(holder.id(), recipe));
        }
        pending = new Snapshot(fuels, extraction);
    }

    @SubscribeEvent public static void received(RecipesReceivedEvent event) {
        if (pending != null) { current = pending; pending = null; }
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { current = null; pending = null; }
}
