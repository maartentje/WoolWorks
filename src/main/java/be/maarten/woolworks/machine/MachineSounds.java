package be.maarten.woolworks.machine;

import be.maarten.woolworks.WoolWorks;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

@EventBusSubscriber(modid = WoolWorks.MODID)
public final class MachineSounds {
    private static final ThreadLocal<Sheep> SHEARING = new ThreadLocal<>();
    private MachineSounds() {}
    public static void fuel(Level level, BlockPos pos, MuffleMode mode) {
        if (!level.isClientSide && !mode.machineMuted()) level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 1.2F);
    }
    public static void extraction(Level level, BlockPos pos, MuffleMode mode) {
        if (!level.isClientSide && !mode.machineMuted()) level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.15F, 1.3F);
    }
    public static <T> T captureShearing(Sheep sheep, Supplier<T> action) {
        Sheep previous = SHEARING.get();
        SHEARING.set(sheep);
        try { return action.get(); }
        finally { if (previous == null) SHEARING.remove(); else SHEARING.set(previous); }
    }
    public static void shearing(Level level, Sheep sheep, MuffleMode mode) {
        if (!level.isClientSide && !mode.machineMuted()) level.playSound(null, sheep, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
    @SubscribeEvent public static void entitySound(PlayLevelSoundEvent.AtEntity event) {
        if (event.getLevel().isClientSide || event.getSound() == null || !(event.getEntity() instanceof Sheep sheep)) return;
        if (SHEARING.get() == sheep && event.getSound().value() == SoundEvents.SHEEP_SHEAR) { event.setCanceled(true); return; }
        if (event.getSource() == SoundSource.NEUTRAL && SheepSoundZones.isSheepSound(event.getSound().value())
                && SheepSoundZones.isMuted(sheep)) event.setCanceled(true);
    }
    @SubscribeEvent public static void positionalSound(PlayLevelSoundEvent.AtPosition event) {
        if (event.getLevel().isClientSide || event.getSound() == null) return;
        Sheep sheep = SHEARING.get();
        if (sheep != null && event.getSound().value() == SoundEvents.SHEEP_SHEAR
                && event.getPosition().distanceToSqr(sheep.position()) < 0.1) { event.setCanceled(true); return; }
        if (event.getSource() != SoundSource.NEUTRAL || !SheepSoundZones.isSheepSound(event.getSound().value())) return;
        if (SheepSoundZones.isMutedAt(event.getLevel(), event.getPosition())) event.setCanceled(true);
    }
}
