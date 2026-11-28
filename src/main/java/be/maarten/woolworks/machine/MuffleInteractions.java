package be.maarten.woolworks.machine;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.registry.ModItems;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = WoolWorks.MODID)
public final class MuffleInteractions {
    private MuffleInteractions() {}
    public static boolean holdingMuffler(Player player) {
        return player.getMainHandItem().is(ModItems.MUFFLER.get()) || player.getOffhandItem().is(ModItems.MUFFLER.get());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void allowSneaking(PlayerInteractEvent.RightClickBlock event) {
        if (!event.isCanceled() && event.getUseBlock() != TriState.FALSE && event.getEntity().isSecondaryUseActive()
                && holdingMuffler(event.getEntity()) && event.getLevel().getBlockEntity(event.getPos()) instanceof MufflableBlockEntity)
            event.setUseBlock(TriState.TRUE);
    }
    @Nullable public static InteractionResult use(Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!holdingMuffler(player)) return null;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), player.getMainHandItem().is(ModItems.MUFFLER.get()) ? player.getMainHandItem() : player.getOffhandItem())) return InteractionResult.FAIL;
        if (!level.isClientSide() && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof MufflableBlockEntity machine) {
            if (server.blockActionRestricted(level, pos, server.gameMode.getGameModeForPlayer())) return InteractionResult.FAIL;
            machine.cycleMuffleMode();
            server.sendOverlayMessage(Component.translatable("message.woolworks.muffle." + machine.muffleMode().name().toLowerCase(Locale.ROOT)));
        }
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
    }
}
