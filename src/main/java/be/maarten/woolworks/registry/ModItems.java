package be.maarten.woolworks.registry;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.item.ResourceFeedItem;
import be.maarten.woolworks.item.ResourceItem;
import be.maarten.woolworks.item.SpeedUpgradeItem;
import be.maarten.woolworks.item.OverdriveUpgradeItem;
import be.maarten.woolworks.item.WoollyMufflerItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WoolWorks.MODID);
    public static final DeferredItem<Item> RESOURCE_FEED = ITEMS.registerItem("resource_feed", ResourceFeedItem::new);
    public static final DeferredItem<Item> INFUSED_WOOL = ITEMS.registerItem("infused_wool", ResourceItem::new);
    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.registerItem("woolly_speed_upgrade", SpeedUpgradeItem::new);
    public static final DeferredItem<Item> OVERDRIVE_UPGRADE = ITEMS.registerItem("woolly_overdrive_upgrade", OverdriveUpgradeItem::new);
    public static final DeferredItem<Item> MUFFLER = ITEMS.registerItem("woolly_muffler", WoollyMufflerItem::new);

    private ModItems() {}

    public static ItemStack withResource(Item item, Identifier resource) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.RESOURCE_TYPE, resource);
        return stack;
    }
}
