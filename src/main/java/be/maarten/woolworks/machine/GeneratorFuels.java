package be.maarten.woolworks.machine;

import be.maarten.woolworks.recipe.ModRecipes;
import be.maarten.woolworks.resource.GeneratorSettings;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public final class GeneratorFuels {
    public record Fuel(int ticks, int energyPerTick) {}
    private GeneratorFuels() {}

    public static Optional<Fuel> find(Level level, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        var input = new SingleRecipeInput(stack);
        var defaults = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("woolworks", "generator/infused_wool");
        var recipe = level.getRecipeManager().getAllRecipesFor(ModRecipes.GENERATOR_FUEL.get()).stream()
                .filter(holder -> !holder.id().equals(defaults) && holder.value().matches(input, level)).findFirst();
        if (recipe.isEmpty()) recipe = level.getRecipeManager().byKey(defaults)
                .filter(holder -> holder.value() instanceof be.maarten.woolworks.recipe.GeneratorFuelRecipe)
                .map(holder -> new net.minecraft.world.item.crafting.RecipeHolder<>(holder.id(), (be.maarten.woolworks.recipe.GeneratorFuelRecipe) holder.value()))
                .filter(holder -> holder.value().matches(input, level));
        if (recipe.isPresent()) return Optional.of(new Fuel(recipe.get().value().burnTicks(), recipe.get().value().energyPerTick()));
        return Optional.empty();
    }
}
