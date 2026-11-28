package be.maarten.woolworks.recipe;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/** Shared selection policy; callers must obtain lists from a complete authoritative snapshot. */
public final class MachineRecipeLookup {
    private static final ResourceKey<Recipe<?>> DEFAULT_FUEL = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("woolworks", "generator/infused_wool"));
    private MachineRecipeLookup() {}

    public static Optional<RecipeHolder<GeneratorFuelRecipe>> fuel(Level level, ItemStack stack, List<RecipeHolder<GeneratorFuelRecipe>> ordered) {
        if (stack.isEmpty()) return Optional.empty();
        var input = new SingleRecipeInput(stack);
        var custom = ordered.stream().filter(holder -> !holder.id().equals(DEFAULT_FUEL) && holder.value().matches(input, level)).findFirst();
        return custom.isPresent() ? custom : ordered.stream().filter(holder -> holder.id().equals(DEFAULT_FUEL) && holder.value().matches(input, level)).findFirst();
    }

    public static Optional<RecipeHolder<WoolExtractionRecipe>> extraction(Level level, ItemStack stack, List<RecipeHolder<WoolExtractionRecipe>> ordered) {
        if (stack.isEmpty()) return Optional.empty();
        var input = new SingleRecipeInput(stack);
        return ordered.stream().filter(holder -> holder.value().matches(input, level)).findFirst();
    }

    public static Optional<RecipeHolder<GeneratorFuelRecipe>> serverFuel(ServerLevel level, ItemStack stack) {
        return fuel(level, stack, List.copyOf(level.recipeAccess().recipeMap().byType(ModRecipes.GENERATOR_FUEL.get())));
    }

    public static Optional<RecipeHolder<WoolExtractionRecipe>> serverExtraction(ServerLevel level, ItemStack stack) {
        return extraction(level, stack, List.copyOf(level.recipeAccess().recipeMap().byType(ModRecipes.WOOL_EXTRACTION.get())));
    }
}
