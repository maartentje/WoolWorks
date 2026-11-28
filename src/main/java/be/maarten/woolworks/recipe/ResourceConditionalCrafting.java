package be.maarten.woolworks.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import java.util.List;
import net.minecraft.world.level.Level;
import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.registry.ModItems;

public record ResourceConditionalCrafting(ShapelessRecipe recipe, Optional<Identifier> requiredResource,
        Optional<Identifier> absentResource, boolean reusableShears, boolean validateResourceWool) implements CraftingRecipe {
    @Override public boolean matches(CraftingInput input, Level level) {
        if (!MachineRecipeConditions.matches(level, requiredResource, absentResource) || !recipe.matches(input, level)) return false;
        if (validateResourceWool) {
            for (int i = 0; i < input.size(); i++) {
                ItemStack stack = input.getItem(i);
                if (stack.is(ModItems.INFUSED_WOOL.get())) {
                    var id = stack.get(ModComponents.RESOURCE_TYPE);
                    if (id == null || !MachineRecipeConditions.matches(level, Optional.of(id), Optional.empty())) return false;
                }
            }
        }
        return true;
    }
    @Override public ItemStack assemble(CraftingInput input) { return recipe.assemble(input); }
    @Override public PlacementInfo placementInfo() { return recipe.placementInfo(); }
    @Override public List<RecipeDisplay> display() { return recipe.display(); }
    @Override public boolean showNotification() { return recipe.showNotification(); }
    @Override public CraftingBookCategory category() { return recipe.category(); }
    @Override public String group() { return recipe.group(); }
    @Override public RecipeSerializer<ResourceConditionalCrafting> getSerializer() { return ModRecipes.CONDITIONAL_CRAFTING_SERIALIZER.get(); }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(input);
        if (reusableShears) {
            for (int i = 0; i < input.size(); i++) {
                if (input.getItem(i).is(Items.SHEARS)) {
                    ItemStack shears = input.getItem(i).copyWithCount(1);
                    if (shears.isDamageableItem()) {
                        int damage = shears.getDamageValue() + 1;
                        if (damage >= shears.getMaxDamage()) shears = ItemStack.EMPTY;
                        else shears.setDamageValue(damage);
                    }
                    remaining.set(i, shears);
                }
            }
        }
        return remaining;
    }

    public static final MapCodec<ResourceConditionalCrafting> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ShapelessRecipe.MAP_CODEC.codec().fieldOf("recipe").forGetter(ResourceConditionalCrafting::recipe),
                Identifier.CODEC.optionalFieldOf("required_resource").forGetter(ResourceConditionalCrafting::requiredResource),
                Identifier.CODEC.optionalFieldOf("absent_resource").forGetter(ResourceConditionalCrafting::absentResource),
                Codec.BOOL.optionalFieldOf("reusable_shears", false).forGetter(ResourceConditionalCrafting::reusableShears),
                Codec.BOOL.optionalFieldOf("validate_resource_wool", false).forGetter(ResourceConditionalCrafting::validateResourceWool)
        ).apply(instance, ResourceConditionalCrafting::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceConditionalCrafting> STREAM = StreamCodec.of((buffer, value) -> {
            ShapelessRecipe.STREAM_CODEC.encode(buffer, value.recipe());
            RecipeNetwork.writeOptionalId(buffer, value.requiredResource());
            RecipeNetwork.writeOptionalId(buffer, value.absentResource());
            buffer.writeBoolean(value.reusableShears());
            buffer.writeBoolean(value.validateResourceWool());
        }, buffer -> new ResourceConditionalCrafting(ShapelessRecipe.STREAM_CODEC.decode(buffer), RecipeNetwork.readOptionalId(buffer), RecipeNetwork.readOptionalId(buffer), buffer.readBoolean(), buffer.readBoolean()));
}
