package be.maarten.woolworks.recipe;

import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.registry.ModItems;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A normal shaped recipe with registry-aware validation of every infused wool input. */
public class ResourceValidatedShaped implements CraftingRecipe {
    private final ShapedRecipe recipe;
    public ResourceValidatedShaped(ShapedRecipe recipe) {
        this.recipe = recipe;
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (!recipe.matches(input, level)) return false;
        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (!stack.is(ModItems.INFUSED_WOOL.get())) continue;
            var id = stack.get(ModComponents.RESOURCE_TYPE);
            if (id == null || !MachineRecipeConditions.matches(level, Optional.of(id), Optional.empty())) return false;
        }
        return true;
    }
    @Override public ItemStack assemble(CraftingInput input) { return recipe.assemble(input); }
    @Override public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingInput input) { return recipe.getRemainingItems(input); }
    @Override public CraftingBookCategory category() { return recipe.category(); }
    @Override public String group() { return recipe.group(); }
    @Override public boolean showNotification() { return recipe.showNotification(); }
    @Override public PlacementInfo placementInfo() { return recipe.placementInfo(); }
    @Override public List<RecipeDisplay> display() { return recipe.display(); }
    @Override public RecipeSerializer<ResourceValidatedShaped> getSerializer() { return ModRecipes.VALIDATED_SHAPED_SERIALIZER.get(); }
    public static final MapCodec<ResourceValidatedShaped> CODEC = ShapedRecipe.MAP_CODEC.xmap(ResourceValidatedShaped::new, value -> value.recipe);
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceValidatedShaped> STREAM = StreamCodec.of(
            (buffer, value) -> ShapedRecipe.STREAM_CODEC.encode(buffer, value.recipe),
            buffer -> new ResourceValidatedShaped(ShapedRecipe.STREAM_CODEC.decode(buffer)));
}
