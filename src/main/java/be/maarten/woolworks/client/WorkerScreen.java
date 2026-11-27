package be.maarten.woolworks.client;

import be.maarten.woolworks.machine.WorkerMenu;
import be.maarten.woolworks.machine.MachineConfig;
import be.maarten.woolworks.machine.WorkerBlockEntity;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class WorkerScreen extends AbstractContainerScreen<WorkerMenu> {
    private static final List<ItemStack> SHEARS = List.of(new ItemStack(Items.SHEARS));
    private static final List<ItemStack> WHEAT = List.of(new ItemStack(Items.WHEAT));
    private Button area;
    public WorkerScreen(WorkerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 182; inventoryLabelY = 89; }
    @Override protected void init() {
        super.init();
        area = addRenderableWidget(Button.builder(Component.empty(), button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 137, topPos + 4, 32, 18).build());
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xff373737);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xffc6c6c6);
        graphics.fill(x + 9, y + 25, x + 21, y + 79, 0xff555555);
        int cap = WorkerBlockEntity.CAPACITY;
        int bar = (int)(Math.clamp((long)menu.energy(), 0, cap) * 52 / cap);
        graphics.fill(x + 10, y + 78 - bar, x + 20, y + 78, 0xffcd3029);
        graphics.fill(x + 34, y + 44, x + 40, y + 59, 0xff555555);
        int interval = Math.max(1, menu.interval());
        int elapsed = Math.clamp(menu.elapsed(), 0, interval);
        int timer = (int)(13L * elapsed / interval);
        graphics.fill(x + 35, y + 58 - timer, x + 39, y + 58, 0xff43b54b);
        for (var slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xff555555);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xff8b8b8b);
        }
        GhostItems.draw(graphics, menu.slots.get(0), x, y, SHEARS);
        GhostItems.draw(graphics, menu.slots.get(1), x, y, WHEAT);
        area.setMessage(Component.literal(menu.area() + "x" + menu.area()));
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        super.render(graphics, x, y, partial);
        renderTooltip(graphics, x, y);
        if (x >= leftPos + 9 && x < leftPos + 21 && y >= topPos + 25 && y < topPos + 79)
            graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.woolworks.worker.energy", menu.energy(), WorkerBlockEntity.CAPACITY), Component.translatable("gui.woolworks.worker.regrowth", WorkerBlockEntity.regrowthCost()), Component.translatable("gui.woolworks.worker.shearing", WorkerBlockEntity.shearingCost())), x, y);
        else if (x >= leftPos + 34 && x < leftPos + 40 && y >= topPos + 44 && y < topPos + 59) {
            int interval = Math.max(1, menu.interval());
            int elapsed = Math.clamp(menu.elapsed(), 0, interval);
            Component status = switch (menu.status()) {
                case WorkerBlockEntity.INSUFFICIENT_ENERGY -> Component.translatable("gui.woolworks.worker.paused_energy");
                case WorkerBlockEntity.MISSING_INPUTS -> Component.translatable("gui.woolworks.worker.paused_inputs");
                case WorkerBlockEntity.OUTPUT_FULL -> Component.translatable("gui.woolworks.worker.paused_output");
                default -> Component.translatable("gui.woolworks.worker.next_operation", String.format(Locale.ROOT, "%.1f", (interval - elapsed) / 20.0));
            };
            graphics.renderComponentTooltip(font, List.of(status,
                    Component.translatable("gui.woolworks.worker.interval", String.format(Locale.ROOT, "%.1f", interval / 20.0))), x, y);
        }
        else if (hoveredSlot == menu.slots.get(0) && !hoveredSlot.hasItem()) graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.woolworks.worker.shears"), Component.translatable("gui.woolworks.worker.shears_desc")), x, y);
        else if (hoveredSlot == menu.slots.get(1) && !hoveredSlot.hasItem()) graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.woolworks.worker.wheat"), Component.translatable("gui.woolworks.worker.wheat_desc")), x, y);
        else if (area.isHovered()) graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.woolworks.worker.area", menu.area(), menu.area()), Component.translatable("gui.woolworks.worker.cycle"), Component.translatable("gui.woolworks.worker.visualization")), x, y);
    }
}
