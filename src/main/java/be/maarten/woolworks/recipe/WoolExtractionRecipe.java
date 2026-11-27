package be.maarten.woolworks.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record WoolExtractionRecipe(Ingredient ingredient, ItemStack result, int duration, int energyPerTick,
        Optional<ResourceLocation> requiredResource, Optional<ResourceLocation> resultTag) implements Recipe<SingleRecipeInput> {
    private static final java.util.Map<ResourceLocation, ItemStack> TAG_RESULTS = new java.util.concurrent.ConcurrentHashMap<>();
    public static void tagsUpdated(net.neoforged.neoforge.event.TagsUpdatedEvent event) { TAG_RESULTS.clear(); }
    private ItemStack resolvedResult() {
        return resultTag.map(tag -> TAG_RESULTS.computeIfAbsent(tag, id -> {
            var key = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, id);
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(key).stream().flatMap(set -> set.stream())
                    .map(holder -> holder.value()).min(java.util.Comparator.comparing(item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()))
                    .map(ItemStack::new).orElse(ItemStack.EMPTY);
        })).orElse(result);
    }
    @Override public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && MachineRecipeConditions.matches(level, requiredResource, Optional.empty());
    }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return resolvedResult().copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return resolvedResult(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.EXTRACTION_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.WOOL_EXTRACTION.get(); }

    public static final class Serializer implements RecipeSerializer<WoolExtractionRecipe> {
        private static final MapCodec<WoolExtractionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(WoolExtractionRecipe::ingredient),
                ItemStack.STRICT_CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(WoolExtractionRecipe::result),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration").forGetter(WoolExtractionRecipe::duration),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy_per_tick").forGetter(WoolExtractionRecipe::energyPerTick),
                ResourceLocation.CODEC.optionalFieldOf("required_resource").forGetter(WoolExtractionRecipe::requiredResource),
                ResourceLocation.CODEC.optionalFieldOf("result_tag").forGetter(WoolExtractionRecipe::resultTag)
        ).apply(instance, WoolExtractionRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, WoolExtractionRecipe> STREAM = StreamCodec.of(
                (buffer, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.result());
                    buffer.writeVarInt(recipe.duration());
                    buffer.writeVarInt(recipe.energyPerTick());
                    RecipeNetwork.writeOptionalId(buffer, recipe.requiredResource());
                    RecipeNetwork.writeOptionalId(buffer, recipe.resultTag());
                }, buffer -> new WoolExtractionRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readVarInt(), RecipeNetwork.readOptionalId(buffer), RecipeNetwork.readOptionalId(buffer)));
        @Override public MapCodec<WoolExtractionRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, WoolExtractionRecipe> streamCodec() { return STREAM; }
    }
}
