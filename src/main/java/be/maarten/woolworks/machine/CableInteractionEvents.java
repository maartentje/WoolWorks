package be.maarten.woolworks.machine;

import be.maarten.woolworks.WoolWorks;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

@EventBusSubscriber(modid = WoolWorks.MODID)
public final class CableInteractionEvents {
    private CableInteractionEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void allowSneakPickup(PlayerInteractEvent.RightClickBlock event) {
        if (!event.isCanceled() && event.getUseBlock() != TriState.FALSE && event.getEntity().isShiftKeyDown()
                && event.getItemStack().is(Items.SHEARS) && event.getLevel().getBlockState(event.getPos()).is(ModMachines.CABLE.get())) {
            event.setUseBlock(TriState.TRUE);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preservePendingEnergy(BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBlockEntity() instanceof CableBlockEntity cable) || cable.pendingEnergy() <= 0) return;
        event.getDrops().stream().map(entity -> entity.getItem()).filter(stack -> stack.is(ModMachines.CABLE_ITEM.get()))
                .findFirst().ifPresent(cable::preservePending);
    }
}
