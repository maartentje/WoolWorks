package be.maarten.woolworks.entity;

import be.maarten.woolworks.WoolWorks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = WoolWorks.MODID)
public final class SheepInteractions {
    @SubscribeEvent
    public static void preventDye(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof ResourceSheep && event.getItemStack().getItem() instanceof DyeItem) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
