package be.maarten.woolworks.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class WoollyMufflerItem extends Item {
    public WoollyMufflerItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.woolworks.woolly_muffler.machine").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.woolworks.woolly_muffler.sheep").withStyle(ChatFormatting.GRAY));
    }
}
