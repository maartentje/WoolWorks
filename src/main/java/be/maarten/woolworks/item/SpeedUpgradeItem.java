package be.maarten.woolworks.item;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class SpeedUpgradeItem extends ExtractorUpgradeItem {
    public static final int MAX_UPGRADES = 8;
    public SpeedUpgradeItem(Properties properties) { super(properties); }
    @Override public int maxExtractorUpgrades() { return MAX_UPGRADES; }
    @Override public Stats stats(int count) { return new Stats(2 + count, 1, 1); }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.woolworks.woolly_speed_upgrade.tooltip").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.woolworks.upgrade.max", maxExtractorUpgrades()).withStyle(ChatFormatting.GRAY));
    }
}
