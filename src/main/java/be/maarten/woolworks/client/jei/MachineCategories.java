package be.maarten.woolworks.client.jei;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.recipe.GeneratorFuelRecipe;
import be.maarten.woolworks.recipe.WoolExtractionRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class MachineCategories {
    public static final RecipeType<RecipeHolder<GeneratorFuelRecipe>> FUEL = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(WoolWorks.MODID, "generator_fuel"));
    public static final RecipeType<RecipeHolder<WoolExtractionRecipe>> EXTRACTION = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(WoolWorks.MODID, "wool_extraction"));
    private MachineCategories() {}

    public static final class Fuel implements IRecipeCategory<RecipeHolder<GeneratorFuelRecipe>> {
        @Override public RecipeType<RecipeHolder<GeneratorFuelRecipe>> getRecipeType() { return FUEL; }
        @Override public Component getTitle() { return Component.translatable("jei.woolworks.generator_fuel"); }
        @Override public IDrawable getIcon() { return null; }
        @Override public int getWidth() { return 150; }
        @Override public int getHeight() { return 55; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<GeneratorFuelRecipe> holder, IFocusGroup focuses) {
            builder.addInputSlot(5, 8).addIngredients(holder.value().ingredient());
        }
        @Override public void draw(RecipeHolder<GeneratorFuelRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            var recipe = holder.value();
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, recipe.burnTicks() + " ticks", 30, 4, 0x404040, false);
            graphics.drawString(font, recipe.energyPerTick() + " FE/t", 30, 17, 0x404040, false);
            graphics.drawString(font, ((long)recipe.burnTicks() * recipe.energyPerTick()) + " FE total", 30, 30, 0x404040, false);
        }
    }

    public static final class Extraction implements IRecipeCategory<RecipeHolder<WoolExtractionRecipe>> {
        @Override public RecipeType<RecipeHolder<WoolExtractionRecipe>> getRecipeType() { return EXTRACTION; }
        @Override public Component getTitle() { return Component.translatable("jei.woolworks.wool_extraction"); }
        @Override public IDrawable getIcon() { return null; }
        @Override public int getWidth() { return 150; }
        @Override public int getHeight() { return 55; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<WoolExtractionRecipe> holder, IFocusGroup focuses) {
            builder.addInputSlot(5, 8).addIngredients(holder.value().ingredient());
            var level = Minecraft.getInstance().level;
            if (level != null) builder.addOutputSlot(125, 8).addItemStack(holder.value().getResultItem(level.registryAccess()).copy());
        }
        @Override public void draw(RecipeHolder<WoolExtractionRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            var recipe = holder.value();
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, recipe.duration() + " ticks", 30, 4, 0x404040, false);
            graphics.drawString(font, recipe.energyPerTick() + " FE/t", 30, 17, 0x404040, false);
        }
    }
}
