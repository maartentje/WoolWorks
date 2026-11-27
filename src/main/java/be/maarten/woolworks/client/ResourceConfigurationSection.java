package be.maarten.woolworks.client;

import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Replaces only the resource list editor in NeoForge's standard config screen. */
final class ResourceConfigurationSection extends ConfigurationSectionScreen {
    ResourceConfigurationSection(Screen parent, ModConfig.Type type, ModConfig config, Component title) {
        super(parent, type, config, title);
    }

    @Override protected <T> Element createList(String key, ModConfigSpec.ListValueSpec spec, ModConfigSpec.ConfigValue<List<T>> value) {
        if (!key.equals("disabledResources")) return super.createList(key, spec, value);
        var button = Button.builder(Component.translatable("neoforge.configuration.uitext.sectiontext"), ignored -> {
            var original = value.getRaw().stream().map(Object::toString).toList();
            minecraft.setScreen(new ResourceSelectionScreen(this, original, selected -> {
                @SuppressWarnings("unchecked") var typed = (List<T>)(List<?>)selected;
                value.set(typed);
                onChanged(key);
                context.modSpec().save();
            }));
        }).tooltip(Tooltip.create(getTooltipComponent(key, null))).build();
        return new Element(getTranslationComponent(key), getTooltipComponent(key, null), button, false);
    }
}
