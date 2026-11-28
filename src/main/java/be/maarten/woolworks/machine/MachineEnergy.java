package be.maarten.woolworks.machine;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Storage and the generator's per-tick extraction budget share one transaction journal. */
final class MachineEnergy implements EnergyHandler {
    private final int capacity, maxInsert, maxExtract;
    private final BooleanSupplier available;
    private final LongSupplier tick;
    private final Runnable changed;
    private int stored, extracted;
    private long extractionTick = Long.MIN_VALUE;
    private record State(int stored, long tick, int extracted) {}
    private final SnapshotJournal<State> journal = new SnapshotJournal<>() {
        @Override protected State createSnapshot() { return new State(stored, extractionTick, extracted); }
        @Override protected void revertToSnapshot(State state) { stored = state.stored(); extractionTick = state.tick(); extracted = state.extracted(); }
        @Override protected void onRootCommit(State original) { if (stored != original.stored()) changed.run(); }
    };

    MachineEnergy(int capacity, int maxInsert, int maxExtract, BooleanSupplier available, LongSupplier tick, Runnable changed) {
        this.capacity = capacity; this.maxInsert = maxInsert; this.maxExtract = maxExtract;
        this.available = available; this.tick = tick; this.changed = changed;
    }
    @Override public long getAmountAsLong() { return stored; }
    @Override public long getCapacityAsLong() { return capacity; }
    int getEnergyStored() { return stored; }
    int getMaxEnergyStored() { return capacity; }
    void load(net.minecraft.world.level.storage.ValueInput input) {
        if (!input.keySet().contains("Energy")) { setStored(0); return; }
        int value = input.getInt("Energy").orElseThrow(() -> new IllegalStateException("Unreadable saved Energy"));
        if (value < 0 || value > capacity) throw new IllegalStateException("Saved Energy out of range: " + value);
        setStored(value);
    }
    void setStored(int value) {
        if (journal.isInTransaction()) throw new IllegalStateException("Cannot overwrite energy during a transaction");
        stored = Math.clamp(value, 0, capacity);
    }
    int availableOutput() {
        if (!available.getAsBoolean()) return 0;
        int used = extractionTick == tick.getAsLong() ? extracted : 0;
        return Math.min(stored, Math.max(0, maxExtract - used));
    }
    @Override public int insert(int amount, TransactionContext tx) {
        TransferPreconditions.checkNonNegative(amount);
        if (!available.getAsBoolean()) return 0;
        int inserted = Math.min(amount, Math.min(maxInsert, capacity - stored));
        if (inserted > 0) { journal.updateSnapshots(tx); stored += inserted; }
        return inserted;
    }
    @Override public int extract(int amount, TransactionContext tx) {
        TransferPreconditions.checkNonNegative(amount);
        int removed = Math.min(amount, availableOutput());
        if (removed > 0) {
            journal.updateSnapshots(tx);
            long now = tick.getAsLong();
            if (extractionTick != now) { extractionTick = now; extracted = 0; }
            stored -= removed; extracted += removed;
        }
        return removed;
    }
}
