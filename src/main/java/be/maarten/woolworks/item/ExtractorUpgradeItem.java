package be.maarten.woolworks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Base for upgrade items with individual extractor stack limits. */
public abstract class ExtractorUpgradeItem extends Item {
    protected ExtractorUpgradeItem(Properties properties) { super(properties); }
    public abstract int maxExtractorUpgrades();
    public abstract Stats stats(int count);
    public record Stats(int halfSpeed, int outputMultiplier, int energyMultiplier) {
        public double speed() { return halfSpeed / 2.0; }
    }
    public static Stats effective(ItemStack stack) {
        if (stack.getItem() instanceof ExtractorUpgradeItem upgrade && !stack.isEmpty())
            return upgrade.stats(Math.clamp(stack.getCount(), 0, upgrade.maxExtractorUpgrades()));
        return new Stats(2, 1, 1);
    }
    public static int limit(ItemStack stack) {
        return stack.getItem() instanceof ExtractorUpgradeItem upgrade ? upgrade.maxExtractorUpgrades() : 0;
    }
}
