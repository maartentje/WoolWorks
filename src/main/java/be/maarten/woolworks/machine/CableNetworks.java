package be.maarten.woolworks.machine;

import be.maarten.woolworks.WoolWorks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Server-side, non-storing network; routes are cached until a topology change. */
@EventBusSubscriber(modid = WoolWorks.MODID)
public final class CableNetworks {
    private static final Map<Level, NetworkState> STATES = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> ROUTING = ThreadLocal.withInitial(() -> false);
    private CableNetworks() {}

    public static void invalidate(Level level) {
        NetworkState state = STATES.get(level);
        if (state != null) state.invalidateRoutes();
    }

    public static void setExtraction(Level level, BlockPos pos, boolean active) {
        NetworkState state = STATES.computeIfAbsent(level, ignored -> new NetworkState());
        if (active) state.active.add(pos.immutable()); else state.active.remove(pos);
    }

    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        NetworkState state = STATES.get(level);
        if (state == null || state.active.isEmpty()) return;
        for (BlockPos pos : List.copyOf(state.active)) {
            if (!level.hasChunkAt(pos)) { state.active.remove(pos); continue; }
            if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) cable.tickExtraction();
            else state.active.remove(pos);
        }
    }

    @SubscribeEvent public static void chunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) invalidate(level);
    }
    @SubscribeEvent public static void chunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) invalidate(level);
    }

    public static EnergyHandler port(Level level, BlockPos pos, Direction side) {
        return new EnergyHandler() {
            @Override public long getAmountAsLong() { return 0; }
            @Override public long getCapacityAsLong() { return isEnabledSource(level, pos, side) ? MachineConfig.CABLE_TRANSFER_RATE.get() : 0; }
            @Override public int extract(int amount, TransactionContext tx) { TransferPreconditions.checkNonNegative(amount); return 0; }
            @Override public int insert(int amount, TransactionContext tx) {
                TransferPreconditions.checkNonNegative(amount);
                if (!isEnabledSource(level, pos, side)) return 0;
                return deliverExtracted(level, pos, side, amount, tx);
            }
        };
    }

    private static boolean isEnabledSource(Level level, BlockPos pos, Direction side) {
        if (side == null || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(ModMachines.CABLE.get())
                || !(level.getBlockEntity(pos) instanceof CableBlockEntity cable) || !cable.isExtracting(side)) return false;
        BlockPos neighbor = pos.relative(side);
        return level.hasChunkAt(neighbor) && !level.getBlockState(neighbor).is(ModMachines.CABLE.get())
                && level.getCapability(Capabilities.Energy.BLOCK, neighbor, side.getOpposite()) != null;
    }

    static int deliverExtracted(Level level, BlockPos pos, Direction side, int amount, TransactionContext parent) {
        TransferPreconditions.checkNonNegative(amount);
        if (level.isClientSide() || amount == 0 || ROUTING.get() || !level.hasChunkAt(pos)
                || !level.getBlockState(pos).is(ModMachines.CABLE.get())) return 0;
        ROUTING.set(true);
        try (var tx = Transaction.open(parent)) {
            int transferred = transfer(level, pos, side, amount, tx);
            if (transferred > 0) tx.commit();
            return transferred;
        } finally { ROUTING.remove(); }
    }

    record Route(BlockPos cable, List<BlockPos> path) {}
    static final class NetworkState {
        final Map<BlockPos, List<Route>> routes = new HashMap<>();
        final Map<BlockPos, Integer> used = new HashMap<>();
        final Set<BlockPos> active = new HashSet<>();
        long tick = Long.MIN_VALUE;
        long topologyVersion;
        int rotation;
        record Budget(long tick, Map<BlockPos, Integer> used, int rotation) {}
        final SnapshotJournal<Budget> journal = new SnapshotJournal<>() {
            @Override protected Budget createSnapshot() { return new Budget(tick, new HashMap<>(used), rotation); }
            @Override protected void revertToSnapshot(Budget b) { tick = b.tick(); used.clear(); used.putAll(b.used()); rotation = b.rotation(); }
        };

        void invalidateRoutes() {
            topologyVersion++;
            routes.clear();
        }
    }

    /** Do not cache or use a topology invalidated while it was being discovered. */
    static List<Route> cachedRoutes(NetworkState state, BlockPos root, java.util.function.Supplier<List<Route>> discover) {
        BlockPos key = root.immutable();
        List<Route> cached = state.routes.get(key);
        if (cached != null) return cached;
        long version = state.topologyVersion;
        List<Route> discovered = discover.get();
        if (version != state.topologyVersion) return null;
        state.routes.put(key, discovered);
        return discovered;
    }

    private static List<Route> routes(Level level, BlockPos root) {
        List<Route> result = new ArrayList<>();
        ArrayDeque<Route> pending = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        pending.add(new Route(root.immutable(), List.of(root.immutable())));
        seen.add(root.immutable());
        while (!pending.isEmpty()) {
            Route route = pending.removeFirst();
            result.add(route);
            for (Direction direction : Direction.values()) {
                BlockPos next = route.cable().relative(direction);
                if (!level.hasChunkAt(next) || !seen.add(next) || !level.getBlockState(next).is(ModMachines.CABLE.get())) continue;
                List<BlockPos> path = new ArrayList<>(route.path());
                path.add(next.immutable());
                pending.addLast(new Route(next, List.copyOf(path)));
            }
        }
        return List.copyOf(result);
    }

    private static List<Route> availableRoutes(BlockPos root, List<Route> topology, Map<BlockPos, Integer> usage, int limit) {
        Set<BlockPos> nodes = new HashSet<>();
        for (Route route : topology) if (usage.getOrDefault(route.cable(), 0) < limit) nodes.add(route.cable());
        if (!nodes.remove(root)) return List.of();
        List<Route> result = new ArrayList<>();
        ArrayDeque<Route> queue = new ArrayDeque<>();
        queue.add(new Route(root, List.of(root)));
        while (!queue.isEmpty()) {
            Route route = queue.removeFirst();
            result.add(route);
            for (Direction direction : Direction.values()) {
                BlockPos next = route.cable().relative(direction);
                if (!nodes.remove(next)) continue;
                List<BlockPos> path = new ArrayList<>(route.path());
                path.add(next);
                queue.addLast(new Route(next, List.copyOf(path)));
            }
        }
        return result;
    }

    private static int transfer(Level level, BlockPos root, Direction side, int requested, TransactionContext tx) {
        NetworkState state = STATES.computeIfAbsent(level, ignored -> new NetworkState());
        if (state.tick != level.getGameTime()) { state.journal.updateSnapshots(tx); state.tick = level.getGameTime(); state.used.clear(); }
        List<Route> routes = cachedRoutes(state, root, () -> routes(level, root));
        if (routes == null) return 0;
        long version = state.topologyVersion;
        for (Route route : routes) {
            if (!level.hasChunkAt(route.cable()) || !level.getBlockState(route.cable()).is(ModMachines.CABLE.get())) {
                state.invalidateRoutes();
                return 0;
            }
        }
        if (version != state.topologyVersion) return 0;
        Map<BlockPos, Integer> usage = state.used;
        int limit = MachineConfig.CABLE_TRANSFER_RATE.get();
        BlockPos source = side == null ? null : root.relative(side);
        EnergyHandler sourceStorage = source != null && level.hasChunkAt(source)
                ? level.getCapability(Capabilities.Energy.BLOCK, source, side.getOpposite()) : null;
        int total = 0;
        List<Route> topology = routes;
        for (int pass = 0; pass <= topology.size() && total < requested; pass++) {
            int before = total;
            routes = availableRoutes(root, topology, usage, limit);
            int start = routes.isEmpty() ? 0 : Math.floorMod(state.rotation, routes.size());
            for (int i = 0; i < routes.size() && total < requested; i++) {
            Route route = routes.get((start + i) % routes.size());
            Direction[] directions = Direction.values();
            for (int d = 0; d < directions.length; d++) {
                if (total >= requested) break;
                Direction direction = directions[Math.floorMod(d + state.rotation, directions.length)];
                BlockPos targetPos = route.cable().relative(direction);
                if (targetPos.equals(source) || !level.hasChunkAt(targetPos) || level.getBlockState(targetPos).is(ModMachines.CABLE.get())) continue;
                if (level.getBlockEntity(route.cable()) instanceof CableBlockEntity cable && cable.isExtracting(direction)) continue;
                var target = level.getCapability(Capabilities.Energy.BLOCK, targetPos, direction.getOpposite());
                if (target == null || target == sourceStorage) continue;
                int allowed = requested - total;
                for (BlockPos segment : route.path()) allowed = Math.min(allowed, limit - usage.getOrDefault(segment, 0));
                if (allowed <= 0) continue;
                int received = target.insert(allowed, tx);
                if (received < 0 || received > allowed) throw new IllegalStateException("Invalid energy insertion result " + received);
                if (received == 0) continue;
                state.journal.updateSnapshots(tx);
                for (BlockPos segment : route.path()) usage.merge(segment, received, Integer::sum);
                total += received;
            }
            }
            if (total == before) break;
        }
        if (version != state.topologyVersion) return 0;
        if (total > 0) { state.journal.updateSnapshots(tx); state.rotation++; }
        return total;
    }
}
