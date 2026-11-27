package be.maarten.woolworks.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import be.maarten.woolworks.machine.CableBlockEntity;
import javax.annotation.Nullable;

public class CableItem extends BlockItem {
    public CableItem(Block block, Properties properties) { super(block, properties); }
    @Override protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        boolean changed = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (changed && level.getBlockEntity(pos) instanceof CableBlockEntity cable) cable.refreshNetworkRegistration();
        return changed;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var rate = be.maarten.woolworks.machine.MachineConfig.CABLE_TRANSFER_RATE;
        lines.add(Component.translatable("item.woolworks.wool_insulated_cable.rate", be.maarten.woolworks.machine.MachineConfig.SPEC.isLoaded() ? rate.get() : rate.getDefault()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.woolworks.wool_insulated_cable.extract").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.woolworks.wool_insulated_cable.pickup").withStyle(ChatFormatting.GRAY));
    }
}
