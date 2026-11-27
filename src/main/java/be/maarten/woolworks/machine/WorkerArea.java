package be.maarten.woolworks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class WorkerArea {
    private WorkerArea() {}
    public static AABB bounds(BlockPos pos, Direction facing, int size) {
        BlockPos first = pos.relative(facing);
        BlockPos last = pos.relative(facing, size);
        Direction sideways = facing.getClockWise();
        BlockPos a = first.relative(sideways, -(size / 2));
        BlockPos b = last.relative(sideways, size / 2).above();
        return new AABB(Math.min(a.getX(), b.getX()), pos.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, pos.getY() + 2, Math.max(a.getZ(), b.getZ()) + 1);
    }
}
