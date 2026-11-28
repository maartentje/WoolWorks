package be.maarten.woolworks.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.level.Level;

public record GeneratorFuelRecipe(Ingredient ingredient, int burnTicks, int energyPerTick,
        Optional<Identifier> requiredResource, Optional<Identifier> absentResource) implements Recipe<SingleRecipeInput> {
    @Override public boolean matches(SingleRecipeInput input, Level level) {
        var stack = input.item();
        var id = stack.get(be.maarten.woolworks.registry.ModComponents.RESOURCE_TYPE);
        return stack.is(be.maarten.woolworks.registry.ModItems.INFUSED_WOOL.get()) && id != null
                && MachineRecipeConditions.matches(level, Optional.of(id), Optional.empty())
                && ingredient.test(stack) && MachineRecipeConditions.matches(level, requiredResource, absentResource);
    }
    @Override public ItemStack assemble(SingleRecipeInput input) { return ItemStack.EMPTY; }
    @Override public boolean showNotification() { return true; }
    @Override public String group() { return ""; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.create(ingredient); }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
    @Override public RecipeSerializer<GeneratorFuelRecipe> getSerializer() { return ModRecipes.GENERATOR_SERIALIZER.get(); }
    @Override public RecipeType<GeneratorFuelRecipe> getType() { return ModRecipes.GENERATOR_FUEL.get(); }

    public static final MapCodec<GeneratorFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(GeneratorFuelRecipe::ingredient),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("burn_ticks").forGetter(GeneratorFuelRecipe::burnTicks),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy_per_tick").forGetter(GeneratorFuelRecipe::energyPerTick),
                Identifier.CODEC.optionalFieldOf("required_resource").forGetter(GeneratorFuelRecipe::requiredResource),
                Identifier.CODEC.optionalFieldOf("absent_resource").forGetter(GeneratorFuelRecipe::absentResource)
        ).apply(instance, GeneratorFuelRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, GeneratorFuelRecipe> STREAM = StreamCodec.of(
                (buffer, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient());
                    buffer.writeVarInt(recipe.burnTicks());
                    buffer.writeVarInt(recipe.energyPerTick());
                    RecipeNetwork.writeOptionalId(buffer, recipe.requiredResource());
                    RecipeNetwork.writeOptionalId(buffer, recipe.absentResource());
                }, buffer -> new GeneratorFuelRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt(),
                        buffer.readVarInt(), RecipeNetwork.readOptionalId(buffer), RecipeNetwork.readOptionalId(buffer)));
}
