package be.maarten.woolworks.item;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class OverdriveUpgradeItem extends ExtractorUpgradeItem {
    public static final int MAX_UPGRADES = 2;
    public OverdriveUpgradeItem(Properties properties) { super(properties); }
    @Override public int maxExtractorUpgrades() { return MAX_UPGRADES; }
    @Override public Stats stats(int count) { return count <= 0 ? new Stats(2, 1, 1) : new Stats(6 * count, 1 << count, 1 << count); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.woolworks.overdrive.speed").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.woolworks.overdrive.output").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.woolworks.overdrive.energy").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.woolworks.upgrade.max", maxExtractorUpgrades()).withStyle(ChatFormatting.GRAY));
    }
}
