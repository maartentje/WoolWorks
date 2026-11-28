package be.maarten.woolworks.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.level.Level;

public record WoolExtractionRecipe(Ingredient ingredient, Optional<ItemStackTemplate> output, int duration, int energyPerTick,
        Optional<Identifier> requiredResource, Optional<Identifier> resultTag) implements Recipe<SingleRecipeInput> {
    public WoolExtractionRecipe(Ingredient ingredient, ItemStack result, int duration, int energyPerTick,
            Optional<Identifier> requiredResource, Optional<Identifier> resultTag) {
        this(ingredient, result.isEmpty() ? Optional.empty() : Optional.of(ItemStackTemplate.fromNonEmptyStack(result)), duration, energyPerTick, requiredResource, resultTag);
    }
    public ItemStack result() { return output.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY); }
    private static final java.util.Map<Identifier, ItemStack> TAG_RESULTS = new java.util.concurrent.ConcurrentHashMap<>();
    public static void tagsUpdated(net.neoforged.neoforge.event.TagsUpdatedEvent event) { TAG_RESULTS.clear(); }
    private ItemStack resolvedResult() {
        return resultTag.map(tag -> TAG_RESULTS.computeIfAbsent(tag, id -> {
            var key = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, id);
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key).stream().flatMap(set -> set.stream())
                    .map(holder -> holder.value()).min(java.util.Comparator.comparing(item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()))
                    .map(ItemStack::new).orElse(ItemStack.EMPTY);
        })).orElseGet(this::result);
    }
    @Override public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && MachineRecipeConditions.matches(level, requiredResource, Optional.empty());
    }
    @Override public ItemStack assemble(SingleRecipeInput input) { return resolvedResult().copy(); }
    public ItemStack getResultItem(HolderLookup.Provider registries) { return resolvedResult().copy(); }
    @Override public boolean showNotification() { return true; }
    @Override public String group() { return ""; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.create(ingredient); }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
    @Override public RecipeSerializer<WoolExtractionRecipe> getSerializer() { return ModRecipes.EXTRACTION_SERIALIZER.get(); }
    @Override public RecipeType<WoolExtractionRecipe> getType() { return ModRecipes.WOOL_EXTRACTION.get(); }

    public static final MapCodec<WoolExtractionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(WoolExtractionRecipe::ingredient),
                ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(WoolExtractionRecipe::output),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration").forGetter(WoolExtractionRecipe::duration),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy_per_tick").forGetter(WoolExtractionRecipe::energyPerTick),
                Identifier.CODEC.optionalFieldOf("required_resource").forGetter(WoolExtractionRecipe::requiredResource),
                Identifier.CODEC.optionalFieldOf("result_tag").forGetter(WoolExtractionRecipe::resultTag)
        ).apply(instance, WoolExtractionRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WoolExtractionRecipe> STREAM = StreamCodec.of(
                (buffer, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient());
                    ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).encode(buffer, recipe.output());
                    buffer.writeVarInt(recipe.duration());
                    buffer.writeVarInt(recipe.energyPerTick());
                    RecipeNetwork.writeOptionalId(buffer, recipe.requiredResource());
                    RecipeNetwork.writeOptionalId(buffer, recipe.resultTag());
                }, buffer -> new WoolExtractionRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).decode(buffer), buffer.readVarInt(), buffer.readVarInt(), RecipeNetwork.readOptionalId(buffer), RecipeNetwork.readOptionalId(buffer)));
}
