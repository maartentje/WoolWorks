package be.maarten.woolworks.recipe;

import be.maarten.woolworks.WoolWorks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.crafting.IngredientType;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, WoolWorks.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, WoolWorks.MODID);
    public static final DeferredRegister<IngredientType<?>> INGREDIENTS = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, WoolWorks.MODID);
    public static final DeferredHolder<IngredientType<?>, IngredientType<InfusedWoolIngredient>> INFUSED_WOOL_INGREDIENT = INGREDIENTS.register("infused_wool", () -> new IngredientType<>(InfusedWoolIngredient.CODEC));
    public static final DeferredHolder<RecipeType<?>, RecipeType<GeneratorFuelRecipe>> GENERATOR_FUEL = TYPES.register("generator_fuel", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeType<?>, RecipeType<WoolExtractionRecipe>> WOOL_EXTRACTION = TYPES.register("wool_extraction", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeSerializer<?>, GeneratorFuelRecipe.Serializer> GENERATOR_SERIALIZER = SERIALIZERS.register("generator_fuel", GeneratorFuelRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, WoolExtractionRecipe.Serializer> EXTRACTION_SERIALIZER = SERIALIZERS.register("wool_extraction", WoolExtractionRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, ResourceConditionalCrafting.Serializer> CONDITIONAL_CRAFTING_SERIALIZER = SERIALIZERS.register("resource_conditional_crafting", ResourceConditionalCrafting.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, ResourceValidatedShaped.Serializer> VALIDATED_SHAPED_SERIALIZER = SERIALIZERS.register("resource_validated_shaped", ResourceValidatedShaped.Serializer::new);
    private ModRecipes() {}
}
