package be.maarten.woolworks.recipe;

import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.registry.ModItems;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** A normal shaped recipe with registry-aware validation of every infused wool input. */
public class ResourceValidatedShaped extends ShapedRecipe {
    public ResourceValidatedShaped(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(null).copy(), recipe.showNotification());
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level)) return false;
        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (!stack.is(ModItems.INFUSED_WOOL.get())) continue;
            var id = stack.get(ModComponents.RESOURCE_TYPE);
            if (id == null || !MachineRecipeConditions.matches(level, Optional.of(id), Optional.empty())) return false;
        }
        return true;
    }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.VALIDATED_SHAPED_SERIALIZER.get(); }
    public static class Serializer implements RecipeSerializer<ResourceValidatedShaped> {
        private static final ShapedRecipe.Serializer INNER = new ShapedRecipe.Serializer();
        private static final MapCodec<ResourceValidatedShaped> CODEC = INNER.codec().xmap(ResourceValidatedShaped::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, ResourceValidatedShaped> STREAM = StreamCodec.of(
                (buffer, recipe) -> INNER.streamCodec().encode(buffer, recipe),
                buffer -> new ResourceValidatedShaped(INNER.streamCodec().decode(buffer)));
        @Override public MapCodec<ResourceValidatedShaped> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ResourceValidatedShaped> streamCodec() { return STREAM; }
    }
}
