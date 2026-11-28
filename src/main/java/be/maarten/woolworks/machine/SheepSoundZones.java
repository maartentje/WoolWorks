package be.maarten.woolworks.machine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-side index of loaded workers, partitioned by chunk. */
public final class SheepSoundZones {
    private static final Map<Level, Map<Long, Set<BlockPos>>> LEVELS = new WeakHashMap<>();
    private SheepSoundZones() {}
    private static long chunkKey(int x, int z) { return (x & 0xffffffffL) | ((long) z << 32); }
    public static void add(WorkerBlockEntity worker) {
        Level level = worker.getLevel();
        if (level == null || level.isClientSide()) return;
        BlockPos pos = worker.getBlockPos().immutable();
        LEVELS.computeIfAbsent(level, ignored -> new HashMap<>()).computeIfAbsent(chunkKey(pos.getX() >> 4, pos.getZ() >> 4), ignored -> new HashSet<>()).add(pos);
    }
    public static void remove(WorkerBlockEntity worker) {
        Level level = worker.getLevel();
        var chunks = LEVELS.get(level);
        if (chunks == null) return;
        long key = chunkKey(worker.getBlockPos().getX() >> 4, worker.getBlockPos().getZ() >> 4);
        var positions = chunks.get(key);
        if (positions != null) { positions.remove(worker.getBlockPos()); if (positions.isEmpty()) chunks.remove(key); }
        if (chunks.isEmpty()) LEVELS.remove(level);
    }
    public static boolean isSheepSound(SoundEvent sound) {
        return sound == SoundEvents.SHEEP_AMBIENT || sound == SoundEvents.SHEEP_HURT
                || sound == SoundEvents.SHEEP_DEATH || sound == SoundEvents.SHEEP_STEP;
    }
    public static boolean isMuted(Sheep sheep) {
        Level level = sheep.level();
        var chunks = LEVELS.get(level);
        if (chunks == null) return false;
        BlockPos sheepPos = sheep.blockPosition();
        int cx = sheepPos.getX() >> 4, cz = sheepPos.getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) {
            var positions = chunks.get(chunkKey(x, z));
            if (positions == null) continue;
            for (BlockPos pos : positions) {
                if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof WorkerBlockEntity worker) || !worker.muffleMode().sheepMuted()) continue;
                if (WorkerArea.bounds(pos, worker.getBlockState().getValue(WorkerBlock.FACING), worker.area()).contains(sheep.position())) return true;
            }
        }
        return false;
    }
    public static boolean isMutedAt(Level level, Vec3 position) {
        var chunks = LEVELS.get(level);
        if (chunks == null) return false;
        BlockPos source = BlockPos.containing(position);
        int cx = source.getX() >> 4, cz = source.getZ() >> 4;
        boolean nearby = false;
        for (int x = cx - 1; x <= cx + 1 && !nearby; x++) for (int z = cz - 1; z <= cz + 1; z++) {
            if (chunks.containsKey(chunkKey(x, z))) { nearby = true; break; }
        }
        if (!nearby) return false;
        AABB search = AABB.ofSize(position, .5, .5, .5);
        for (Sheep sheep : level.getEntitiesOfClass(Sheep.class, search)) {
            if (sheep.position().distanceToSqr(position) <= 0.0625 && isMuted(sheep)) return true;
        }
        return false;
    }
}
