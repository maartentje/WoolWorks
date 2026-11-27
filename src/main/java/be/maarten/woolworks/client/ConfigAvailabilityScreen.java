package be.maarten.woolworks.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Server config and datapack registries are not loaded at the main menu. */
final class ConfigAvailabilityScreen extends Screen {
    private final Screen parent;
    private final Screen config;
    ConfigAvailabilityScreen(Screen parent, Screen config) {
        super(Component.translatable("woolworks.resources.title"));
        this.parent = parent;
        this.config = config;
    }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("woolworks.resources.other_settings"), b -> minecraft.setScreen(config))
                .bounds(width / 2 - 100, height / 2 + 20, 200, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose()).bounds(width / 2 - 100, height / 2 + 45, 200, 20).build());
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 55, 0xffffff);
        graphics.drawWordWrap(font, Component.translatable("woolworks.resources.unavailable"), width / 2 - 150, height / 2 - 35, 300, 0xffffff);
    }
}
