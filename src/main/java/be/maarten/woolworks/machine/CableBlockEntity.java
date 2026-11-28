package be.maarten.woolworks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

public class CableBlockEntity extends BlockEntity {
    private int extractionMask;
    private int pendingEnergy;
    private int pendingSide;
    public CableBlockEntity(BlockPos pos, BlockState state) { super(ModMachines.CABLE_ENTITY.get(), pos, state); }
    public boolean isExtracting(Direction side) { return (extractionMask & (1 << side.ordinal())) != 0; }
    public int extractionMask() { return extractionMask; }
    public int pendingEnergy() { return pendingEnergy; }
    public void preservePending(ItemStack stack) {
        if (pendingEnergy <= 0) return;
        var data = net.minecraft.world.level.storage.TagValueOutput.createWithoutContext(net.minecraft.util.ProblemReporter.DISCARDING);
        data.putInt("PendingEnergy", pendingEnergy);
        data.putInt("PendingSide", pendingSide);
        BlockItem.setBlockEntityData(stack, ModMachines.CABLE_ENTITY.get(), data);
    }
    public void refreshNetworkRegistration() {
        if (level != null && !level.isClientSide()) CableNetworks.setExtraction(level, worldPosition, extractionMask != 0 || pendingEnergy > 0);
    }
    public void setExtracting(Direction side, boolean enabled) {
        int bit = 1 << side.ordinal();
        int updated = enabled ? extractionMask | bit : extractionMask & ~bit;
        if (updated == extractionMask) return;
        extractionMask = updated;
        setChanged();
        if (level != null && !level.isClientSide()) {
            CableNetworks.setExtraction(level, worldPosition, extractionMask != 0 || pendingEnergy > 0);
            invalidateCapabilities();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) CableNetworks.setExtraction(level, worldPosition, extractionMask != 0 || pendingEnergy > 0);
    }
    @Override public void onChunkUnloaded() {
        if (level != null && !level.isClientSide()) CableNetworks.setExtraction(level, worldPosition, false);
        super.onChunkUnloaded();
    }
    @Override public void setRemoved() {
        if (level != null && !level.isClientSide()) CableNetworks.setExtraction(level, worldPosition, false);
        super.setRemoved();
    }
    @Override protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt("ExtractionMask", extractionMask);
        tag.putInt("PendingEnergy", pendingEnergy);
        tag.putInt("PendingSide", pendingSide);
    }
    @Override protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        extractionMask = tag.getIntOr("ExtractionMask", 0) & 63;
        pendingEnergy = tag.keySet().contains("PendingEnergy") ? tag.getInt("PendingEnergy").orElseThrow(() -> new IllegalStateException("Unreadable PendingEnergy")) : 0;
        pendingSide = tag.keySet().contains("PendingSide") ? tag.getInt("PendingSide").orElseThrow(() -> new IllegalStateException("Unreadable PendingSide")) : 0;
        if (pendingEnergy < 0 || pendingSide < 0 || pendingSide >= Direction.values().length) throw new IllegalStateException("Invalid pending cable energy");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    private final net.neoforged.neoforge.transfer.transaction.SnapshotJournal<Integer> pendingJournal = new net.neoforged.neoforge.transfer.transaction.SnapshotJournal<>() {
        @Override protected Integer createSnapshot() { return pendingEnergy; }
        @Override protected void revertToSnapshot(Integer previous) { pendingEnergy = previous; }
        @Override protected void onRootCommit(Integer previous) { if (pendingEnergy != previous) setChanged(); }
    };

    /** Tick only while a connection is configured or a partial legacy transfer needs retrying. */
    public void tickExtraction() {
        if (level == null || level.isClientSide()) return;
        if (pendingEnergy > 0) {
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                int delivered = CableNetworks.deliverExtracted(level, worldPosition, Direction.values()[pendingSide], pendingEnergy, tx);
                if (delivered > 0) { pendingJournal.updateSnapshots(tx); pendingEnergy -= delivered; tx.commit(); }
            }
            if (pendingEnergy > 0) return;
        }
        for (Direction side : Direction.values()) {
            if (!isExtracting(side)) continue;
            BlockPos neighbor = worldPosition.relative(side);
            if (!level.hasChunkAt(neighbor) || level.getBlockState(neighbor).is(ModMachines.CABLE.get())) continue;
            var source = level.getCapability(Capabilities.Energy.BLOCK, neighbor, side.getOpposite());
            if (source == null) continue;
            int request = MachineConfig.CABLE_TRANSFER_RATE.get();
            // Retry a partially accepted transfer with the exact accepted amount. Each failed
            // attempt rolls back source, destinations and route reservations together.
            try (var outer = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                for (int attempt = 0; attempt < 2 && request > 0; attempt++) {
                    int accepted;
                    try (var inner = net.neoforged.neoforge.transfer.transaction.Transaction.open(outer)) {
                        int removed = source.extract(request, inner);
                        if (removed < 0 || removed > request) throw new IllegalStateException("Invalid energy extraction result " + removed);
                        if (removed == 0) break;
                        accepted = CableNetworks.deliverExtracted(level, worldPosition, side, removed, inner);
                        if (accepted == removed) { inner.commit(); outer.commit(); break; }
                    }
                    request = accepted;
                }
            }
        }
        CableNetworks.setExtraction(level, worldPosition, extractionMask != 0 || pendingEnergy > 0);
    }
}
