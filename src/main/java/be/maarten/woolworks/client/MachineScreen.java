package be.maarten.woolworks.client;

import be.maarten.woolworks.machine.MachineMenu;
import be.maarten.woolworks.registry.ModItems;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;

public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final List<ItemStack> upgradeGhosts = List.of(new ItemStack(ModItems.SPEED_UPGRADE.get()), new ItemStack(ModItems.OVERDRIVE_UPGRADE.get()));
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (menu.isExtractor() && hoveredSlot == menu.slots.get(2)) {
            var stats = menu.upgradeStats();
            lines.add(Component.translatable("gui.woolworks.speed", stats.speed()));
            lines.add(Component.translatable("gui.woolworks.output", stats.outputMultiplier()));
        }
        return lines;
    }

    @Override protected void extractBackground(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        graphics.fill(x + 9, y + 17, x + 22, y + 66, 0xFF555555);
        int bar = Math.clamp(menu.energy(), 0, 20000) * 47 / 20000;
        graphics.fill(x + 10, y + 65 - bar, x + 21, y + 65, 0xFFCD3029);
        int progress = Math.clamp(menu.progress(), 0, 1000);
        if (menu.isExtractor()) {
            graphics.fill(x + 82, y + 37, x + 106, y + 45, 0xFF555555);
            graphics.fill(x + 83, y + 38, x + 83 + progress * 22 / 1000, y + 44, 0xFF43B54B);
        } else {
            graphics.fill(x + 96, y + 17, x + 102, y + 45, 0xFF555555);
            graphics.fill(x + 97, y + 44 - progress * 26 / 1000, x + 101, y + 44, 0xFFE59A32);
        }
        for (var slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF555555);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF8B8B8B);
        }
        GhostItems.draw(graphics, menu.slots.get(0), x, y, GhostItems.wool(menu.isExtractor()));
        if (menu.isExtractor()) GhostItems.draw(graphics, menu.slots.get(2), x, y, upgradeGhosts);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= leftPos + 9 && mouseX < leftPos + 22 && mouseY >= topPos + 17 && mouseY < topPos + 66) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(menu.energy() + " / 20000 FE"),
                    menu.isExtractor() ? Component.translatable("gui.woolworks.consumption", menu.rate()) : Component.literal("Generation: " + menu.rate() + " FE/t")), mouseX, mouseY);
        } else if (menu.isExtractor() && hoveredSlot == menu.slots.get(2) && !hoveredSlot.hasItem()) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.woolworks.upgrades"), Component.translatable("gui.woolworks.speed", menu.upgradeStats().speed()), Component.translatable("gui.woolworks.output", menu.upgradeStats().outputMultiplier())), mouseX, mouseY);
        } else if (menu.isExtractor() && menu.oversizedOutput()
                && hoveredSlot == menu.slots.get(1) && !hoveredSlot.hasItem()) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.woolworks.output_too_large")), mouseX, mouseY);
        } else if (hoveredSlot == menu.slots.get(0) && !hoveredSlot.hasItem()) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.woolworks.infused_wool")), mouseX, mouseY);
        } else if ((!menu.isExtractor() && mouseX >= leftPos + 96 && mouseX < leftPos + 102 && mouseY >= topPos + 17 && mouseY < topPos + 45)
                || (menu.isExtractor() && mouseX >= leftPos + 82 && mouseX < leftPos + 106 && mouseY >= topPos + 37 && mouseY < topPos + 45)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(menu.isExtractor() ? "gui.woolworks.extraction_title" : "gui.woolworks.fuel_title"));
            long ticks = menu.remainingTicks();
            if (ticks == 0) lines.add(Component.translatable(menu.isExtractor() ? "gui.woolworks.no_extraction" : "gui.woolworks.no_fuel"));
            else {
                lines.add(Component.translatable("gui.woolworks.time_remaining", String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0)));
                if (!menu.isExtractor()) lines.add(Component.translatable("gui.woolworks.energy_remaining", String.format(java.util.Locale.ROOT, "%,d", ticks * menu.fuelRate())));
                if (!menu.processing()) lines.add(Component.translatable("gui.woolworks.paused"));
            }
            if (menu.isExtractor() && menu.oversizedOutput()) lines.add(Component.translatable("gui.woolworks.output_too_large"));
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }
}
