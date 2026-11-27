package be.maarten.woolworks.client;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.machine.GeneratorFuels;
import be.maarten.woolworks.recipe.ModRecipes;
import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.resource.ResourceTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Client-only visual placeholders; no stacks are inserted into a menu. */
@EventBusSubscriber(modid = WoolWorks.MODID, value = Dist.CLIENT)
public final class GhostItems {
    private static ClientLevel cachedLevel;
    private static Registry<?> cachedRegistry;
    private static RecipeManager cachedRecipes;
    private static List<ItemStack> generatorWool = List.of(), extractorWool = List.of();
    private GhostItems() {}
    @SubscribeEvent public static void recipesUpdated(RecipesUpdatedEvent event) { clear(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    private static void clear() { cachedLevel = null; cachedRegistry = null; cachedRecipes = null; generatorWool = List.of(); extractorWool = List.of(); }
    public static List<ItemStack> wool(boolean extractor) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) { clear(); return List.of(); }
        var registry = level.registryAccess().registry(ResourceTypes.REGISTRY).orElse(null);
        var recipes = level.getRecipeManager();
        if (cachedLevel != level || cachedRegistry != registry || cachedRecipes != recipes) {
            cachedLevel = level; cachedRegistry = registry; cachedRecipes = recipes;
            List<ItemStack> generator = new ArrayList<>(), extraction = new ArrayList<>();
            if (registry != null) for (var id : registry.keySet().stream().sorted().toList()) {
                if (!be.maarten.woolworks.resource.ResourceTypes.enabled(level, id)) continue;
                ItemStack stack = ModItems.withResource(ModItems.INFUSED_WOOL.get(), id);
                if (GeneratorFuels.find(level, stack).isPresent()) generator.add(stack);
                if (recipes.getRecipeFor(ModRecipes.WOOL_EXTRACTION.get(), new SingleRecipeInput(stack), level).isPresent()) extraction.add(stack);
            }
            generatorWool = List.copyOf(generator); extractorWool = List.copyOf(extraction);
        }
        return extractor ? extractorWool : generatorWool;
    }
    public static void draw(GuiGraphics graphics, Slot slot, int left, int top, List<ItemStack> options) {
        if (slot.hasItem() || options.isEmpty()) return;
        int index = (int)Math.floorMod(System.currentTimeMillis() / 2000L, options.size());
        int x = left + slot.x, y = top + slot.y;
        graphics.renderFakeItem(options.get(index), x, y);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.fill(x, y, x + 16, y + 16, 0x998b8b8b);
        graphics.pose().popPose();
    }
}
