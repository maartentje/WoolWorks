package be.maarten.woolworks.machine;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;

/** Tags only the block-break effect synchronously emitted by a muted sheep eating grass. */
public final class SheepGrassEffects {
    public static final int SILENT_EFFECT = Integer.MIN_VALUE;
    private static final ThreadLocal<Boolean> MUTED_DESTROY = ThreadLocal.withInitial(() -> false);
    private SheepGrassEffects() {}
    public static boolean shouldMute(Level level, Mob mob) {
        return !level.isClientSide && mob instanceof Sheep sheep && SheepSoundZones.isMuted(sheep);
    }
    public static int mark(int blockStateId) { return blockStateId | SILENT_EFFECT; }
    public static int markDuringDestroy(int blockStateId) { return MUTED_DESTROY.get() ? mark(blockStateId) : blockStateId; }
    public static boolean destroySilently(BooleanSupplier operation) {
        boolean previous = MUTED_DESTROY.get();
        MUTED_DESTROY.set(true);
        try { return operation.getAsBoolean(); }
        finally { if (previous) MUTED_DESTROY.set(true); else MUTED_DESTROY.remove(); }
    }
}
