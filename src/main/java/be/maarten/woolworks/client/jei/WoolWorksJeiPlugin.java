package be.maarten.woolworks.client.jei;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.machine.ModMachines;
import be.maarten.woolworks.recipe.MachineRecipeConditions;
import be.maarten.woolworks.client.ClientMachineRecipes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import be.maarten.woolworks.recipe.GeneratorFuelRecipe;
import be.maarten.woolworks.resource.GeneratorSettings;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

@JeiPlugin
public final class WoolWorksJeiPlugin implements IModPlugin {
    @Override public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(WoolWorks.MODID, "jei");
    }

    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> interpreter = new ISubtypeInterpreter<>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(ModComponents.RESOURCE_TYPE);
            }

        };
        registration.registerSubtypeInterpreter(ModItems.RESOURCE_FEED.get(), interpreter);
        registration.registerSubtypeInterpreter(ModItems.INFUSED_WOOL.get(), interpreter);
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MachineCategories.Fuel(), new MachineCategories.Extraction());
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var snapshot = ClientMachineRecipes.snapshot().orElse(null);
        if (snapshot == null) return;
        var fuels = new ArrayList<RecipeHolder<GeneratorFuelRecipe>>();
        int index = 0;
        for (var id : level.registryAccess().lookupOrThrow(be.maarten.woolworks.resource.ResourceTypes.REGISTRY).keySet()) {
            if (!be.maarten.woolworks.resource.ResourceTypes.enabled(level, id)) continue;
            var stack = ModItems.withResource(ModItems.INFUSED_WOOL.get(), id);
            var selected = be.maarten.woolworks.machine.GeneratorFuels.find(level, stack);
            if (selected.isEmpty()) continue;
            var fuel = selected.get();
            var recipe = new GeneratorFuelRecipe(DataComponentIngredient.of(true, stack), fuel.ticks(), fuel.energyPerTick(), Optional.empty(), Optional.empty());
            fuels.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(WoolWorks.MODID, "jei/infused_wool/" + index++)), recipe));
        }
        registration.addRecipes(MachineCategories.FUEL, fuels);
        registration.addRecipes(MachineCategories.EXTRACTION, snapshot.extraction().stream()
                .filter(holder -> MachineRecipeConditions.matches(level, holder.value().requiredResource(), Optional.empty()))
                .filter(holder -> !holder.value().ingredient().hasNoItems())
                .filter(holder -> !holder.value().getResultItem(level.registryAccess()).isEmpty()).toList());
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(MachineCategories.FUEL, ModMachines.GENERATOR_ITEM.get());
        registration.addRecipeCatalysts(MachineCategories.EXTRACTION, ModMachines.EXTRACTOR_ITEM.get());
    }
}
