package be.maarten.woolworks.recipe;

import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.registry.ModItems;
import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/** Registry membership is checked by the enclosing recipe, which has access to the level. */
public record InfusedWoolIngredient() implements ICustomIngredient {
    public static final MapCodec<InfusedWoolIngredient> CODEC = MapCodec.unit(InfusedWoolIngredient::new);
    @Override public boolean test(ItemStack stack) { return stack.is(ModItems.INFUSED_WOOL.get()) && stack.has(ModComponents.RESOURCE_TYPE); }
    @Override public Stream<Holder<Item>> items() { return Stream.of(ModItems.INFUSED_WOOL.get().builtInRegistryHolder()); }
    @Override public SlotDisplay display() { return InfusedWoolSlotDisplay.INSTANCE; }
    @Override public boolean isSimple() { return false; }
    @Override public IngredientType<?> getType() { return ModRecipes.INFUSED_WOOL_INGREDIENT.get(); }
}
