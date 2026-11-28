package be.maarten.woolworks.recipe;

import be.maarten.woolworks.WoolWorks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.crafting.IngredientType;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, WoolWorks.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, WoolWorks.MODID);
    public static final DeferredRegister<IngredientType<?>> INGREDIENTS = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, WoolWorks.MODID);
    public static final DeferredRegister<SlotDisplay.Type<?>> SLOT_DISPLAYS = DeferredRegister.create(Registries.SLOT_DISPLAY, WoolWorks.MODID);
    public static final DeferredHolder<SlotDisplay.Type<?>, SlotDisplay.Type<InfusedWoolSlotDisplay>> INFUSED_WOOL_DISPLAY = SLOT_DISPLAYS.register("infused_wool", () -> new SlotDisplay.Type<>(com.mojang.serialization.MapCodec.unit(InfusedWoolSlotDisplay.INSTANCE), net.minecraft.network.codec.StreamCodec.unit(InfusedWoolSlotDisplay.INSTANCE)));
    public static final DeferredHolder<IngredientType<?>, IngredientType<InfusedWoolIngredient>> INFUSED_WOOL_INGREDIENT = INGREDIENTS.register("infused_wool", () -> new IngredientType<>(InfusedWoolIngredient.CODEC));
    public static final DeferredHolder<RecipeType<?>, RecipeType<GeneratorFuelRecipe>> GENERATOR_FUEL = TYPES.register("generator_fuel", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeType<?>, RecipeType<WoolExtractionRecipe>> WOOL_EXTRACTION = TYPES.register("wool_extraction", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GeneratorFuelRecipe>> GENERATOR_SERIALIZER = SERIALIZERS.register("generator_fuel", () -> new RecipeSerializer<>(GeneratorFuelRecipe.CODEC, GeneratorFuelRecipe.STREAM));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WoolExtractionRecipe>> EXTRACTION_SERIALIZER = SERIALIZERS.register("wool_extraction", () -> new RecipeSerializer<>(WoolExtractionRecipe.CODEC, WoolExtractionRecipe.STREAM));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ResourceConditionalCrafting>> CONDITIONAL_CRAFTING_SERIALIZER = SERIALIZERS.register("resource_conditional_crafting", () -> new RecipeSerializer<>(ResourceConditionalCrafting.CODEC, ResourceConditionalCrafting.STREAM));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ResourceValidatedShaped>> VALIDATED_SHAPED_SERIALIZER = SERIALIZERS.register("resource_validated_shaped", () -> new RecipeSerializer<>(ResourceValidatedShaped.CODEC, ResourceValidatedShaped.STREAM));
    public static void syncRecipes(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        var recipes = event.getPlayerList().getServer().getRecipeManager().recipeMap();
        var snapshot = new MachineRecipesPayload(new java.util.ArrayList<>(recipes.byType(GENERATOR_FUEL.get())),
                new java.util.ArrayList<>(recipes.byType(WOOL_EXTRACTION.get())));
        event.getRelevantPlayers()
                .filter(player -> net.neoforged.neoforge.network.registration.NetworkRegistry.hasChannel(player.connection, MachineRecipesPayload.TYPE.id()))
                .forEach(player -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, snapshot));
    }
    public static void registerPayload(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(MachineRecipesPayload.TYPE, MachineRecipesPayload.STREAM);
    }
    private ModRecipes() {}
}
