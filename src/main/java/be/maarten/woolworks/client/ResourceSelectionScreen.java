package be.maarten.woolworks.client;

import be.maarten.woolworks.machine.MachineConfig;
import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.resource.ResourceType;
import be.maarten.woolworks.resource.ResourceTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

/** An isolated edit buffer; unknown and temporarily unavailable IDs are retained. */
final class ResourceSelectionScreen extends Screen {
    private final Screen parent;
    private final Consumer<List<String>> save;
    private final LinkedHashSet<String> disabled;
    private final List<ResourceEntry> resources = new ArrayList<>();
    private EditBox search;
    private ResourceList list;
    private boolean available;
    private int filterMode;

    ResourceSelectionScreen(Screen parent, List<String> disabled, Consumer<List<String>> save) {
        super(Component.translatable("woolworks.resources.title"));
        this.parent = parent;
        this.disabled = new LinkedHashSet<>(disabled);
        this.save = save;
    }

    private boolean canEdit() {
        return minecraft != null && minecraft.level != null && minecraft.hasSingleplayerServer()
                && minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished();
    }

    @Override protected void init() {
        available = canEdit() && minecraft.level.registryAccess().lookup(ResourceTypes.REGISTRY).isPresent();
        resources.clear();
        if (available) {
            minecraft.level.registryAccess().lookupOrThrow(ResourceTypes.REGISTRY).entrySet().forEach(entry -> {
                ResourceType type = entry.getValue();
                boolean hasTag = type.requiredItemTag().map(id -> BuiltInRegistries.ITEM
                        .getTag(TagKey.create(Registries.ITEM, id)).map(tag -> tag.iterator().hasNext()).orElse(false)).orElse(true);
                if (hasTag || !type.automatic()) resources.add(new ResourceEntry(entry.getKey().location(), type, hasTag));
            });
            resources.sort(Comparator.comparing((ResourceEntry entry) -> entry.name().getString().toLowerCase(Locale.ROOT))
                    .thenComparing(entry -> entry.id().toString()));
            search = new EditBox(font, width / 2 - 150, 33, 190, 20, Component.translatable("woolworks.resources.search"));
            search.setHint(Component.translatable("woolworks.resources.search"));
            list = new ResourceList(minecraft, width, height - 112, 57, 25);
            search.setResponder(value -> list.filter(value));
            addRenderableWidget(search);
            addRenderableWidget(Button.builder(filterLabel(), button -> {
                filterMode = (filterMode + 1) % 3;
                button.setMessage(filterLabel());
                list.filter(search.getValue());
            }).bounds(width / 2 + 44, 33, 106, 20).tooltip(Tooltip.create(Component.translatable("woolworks.resources.filter.tooltip"))).build());
            addRenderableWidget(list);
            list.filter("");
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            if (available && canEdit()) save.accept(List.copyOf(disabled));
            onClose();
        }).bounds(width / 2 - 102, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(width / 2 + 2, height - 28, 100, 20).build());
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    private Component filterLabel() { return Component.translatable("woolworks.resources.filter." + (filterMode == 0 ? "all" : filterMode == 1 ? "enabled" : "disabled")); }
    private boolean effective(ResourceEntry entry) { return ResourceTypes.enabled(entry.id(), entry.type(), disabled.contains(entry.id().toString())); }

    @Override public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xffffff);
        if (available) graphics.drawCenteredString(font, Component.translatable("woolworks.resources.restart"), width / 2, height - 48, 0xffcc55);
        else graphics.drawWordWrap(font, Component.translatable("woolworks.resources.unavailable"), width / 2 - 150, 50, 300, 0xffffff);
    }

    private record ResourceEntry(Identifier id, ResourceType type, boolean hasTag) {
        Component name() { return Component.translatable(type.translationKey()); }
    }

    private final class ResourceList extends ObjectSelectionList<ResourceList.Row> {
        ResourceList(Minecraft minecraft, int width, int height, int top, int itemHeight) { super(minecraft, width, height, top, itemHeight); }
        @Override public int getRowWidth() { return 300; }
        void filter(String query) {
            clearEntries();
            String needle = query.toLowerCase(Locale.ROOT);
            resources.stream().filter(entry -> entry.name().getString().toLowerCase(Locale.ROOT).contains(needle))
                    .filter(entry -> filterMode == 0 || effective(entry) == (filterMode == 1))
                    .forEach(entry -> addEntry(new Row(entry)));
            setScrollAmount(0);
        }

        final class Row extends ObjectSelectionList.Entry<Row> {
            private final ResourceEntry resource;
            private final Button toggle;
            Row(ResourceEntry resource) {
                this.resource = resource;
                boolean global = resource.type().automatic() && !MachineConfig.ENABLE_AUTOMATIC_MODDED_RESOURCES.get();
                toggle = Button.builder(label(), button -> {
                    String id = resource.id().toString();
                    if (!disabled.remove(id)) disabled.add(id);
                    button.setMessage(label());
                    if (filterMode != 0) ResourceSelectionScreen.this.list.filter(search.getValue());
                }).size(86, 20).build();
                if (!resource.type().enabled() || !resource.hasTag()) {
                    toggle.active = false;
                    toggle.setTooltip(Tooltip.create(Component.translatable(resource.hasTag() ? "woolworks.resources.restricted" : "woolworks.resources.missing_tag")));
                } else if (!resource.type().category().enabled()) {
                    toggle.active = false;
                    toggle.setTooltip(Tooltip.create(Component.translatable("woolworks.resources.category", Component.translatable("woolworks.category." + resource.type().category().getSerializedName()))));
                } else if (global) {
                    toggle.active = false;
                    toggle.setTooltip(Tooltip.create(Component.translatable("woolworks.resources.global")));
                }
            }
            private Component label() {
                if (!resource.type().enabled() || !resource.hasTag()) return Component.translatable("woolworks.resources.restricted_state");
                if (!resource.type().category().enabled()) return Component.translatable("woolworks.resources.category_state");
                if (resource.type().automatic() && !MachineConfig.ENABLE_AUTOMATIC_MODDED_RESOURCES.get())
                    return Component.translatable("woolworks.resources.global_state");
                return Component.translatable(disabled.contains(resource.id().toString()) ? "woolworks.resources.off" : "woolworks.resources.on");
            }
            @Override public Component getNarration() { return Component.literal(resource.name().getString() + ": ").append(label()); }
            @Override public void render(GuiGraphicsExtractor graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                var stack = ModItems.withResource(ModItems.INFUSED_WOOL.get(), resource.id());
                graphics.renderItem(stack, left + 2, top + 2);
                graphics.drawString(font, font.plainSubstrByWidth(resource.name().getString(), width - 117), left + 23, top + 6, 0xffffff, false);
                toggle.setPosition(left + width - 88, top);
                toggle.render(graphics, mouseX, mouseY, partialTick);
            }
            @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { return toggle.mouseClicked(mouseX, mouseY, button); }
        }
    }
}
