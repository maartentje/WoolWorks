package be.maarten.woolworks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
        CompoundTag data = new CompoundTag();
        data.putInt("PendingEnergy", pendingEnergy);
        data.putInt("PendingSide", pendingSide);
        BlockItem.setBlockEntityData(stack, ModMachines.CABLE_ENTITY.get(), data);
    }
    public void refreshNetworkRegistration() {
        if (level != null && !level.isClientSide) CableNetworks.setExtraction(level, worldPosition, extractionMask != 0 || pendingEnergy > 0);
    }
    public void setExtracting(Direction side, boolean enabled) {
        int bit = 1 << side.ordinal();
        int updated = enabled ? extractionMask | bit : extractionMask & ~bit;
        if (updated == extractionMask) return;
        extractionMask = updated;
        setChanged();
        if (level != null && !level.isClientSide) {
            CableNetworks.setExtraction(level, worldPosition, extractionMask != 0 || pendingEnergy > 0);
            invalidateCapabilities();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) CableNetworks.setExtraction(level, worldPosition, extractionMask != 0 || pendingEnergy > 0);
    }
    @Override public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) CableNetworks.setExtraction(level, worldPosition, false);
        super.onChunkUnloaded();
    }
    @Override public void setRemoved() {
        if (level != null && !level.isClientSide) CableNetworks.setExtraction(level, worldPosition, false);
        super.setRemoved();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ExtractionMask", extractionMask);
        tag.putInt("PendingEnergy", pendingEnergy);
        tag.putInt("PendingSide", pendingSide);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        extractionMask = tag.getInt("ExtractionMask") & 63;
        pendingEnergy = Math.max(0, tag.getInt("PendingEnergy"));
        pendingSide = Math.clamp(tag.getInt("PendingSide"), 0, 5);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    /** Tick only while a connection is configured or a partial transfer needs retrying. */
    public void tickExtraction() {
        if (level == null || level.isClientSide) return;
        if (pendingEnergy > 0) {
            Direction origin = Direction.values()[pendingSide];
            int delivered = CableNetworks.deliverExtracted(level, worldPosition, origin, pendingEnergy, false);
            if (delivered > 0) { pendingEnergy -= delivered; setChanged(); }
            if (pendingEnergy > 0) return;
        }
        for (Direction side : Direction.values()) {
            if (!isExtracting(side)) continue;
            BlockPos neighbor = worldPosition.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            if (level.getBlockState(neighbor).is(ModMachines.CABLE.get())) { setExtracting(side, false); continue; }
            var source = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, side.getOpposite());
            if (source == null || !source.canExtract()) { setExtracting(side, false); continue; }
            int limit = MachineConfig.CABLE_TRANSFER_RATE.get();
            int available = Math.clamp(source.extractEnergy(limit, true), 0, limit);
            if (available == 0) continue;
            var port = CableNetworks.port(level, worldPosition, side);
            int requested = port.receiveEnergy(available, true);
            if (requested <= 0) continue;
            int removed = Math.clamp(source.extractEnergy(requested, false), 0, requested);
            if (removed == 0) continue;
            pendingEnergy = removed;
            pendingSide = side.ordinal();
            pendingEnergy -= port.receiveEnergy(pendingEnergy, false);
            setChanged();
            if (pendingEnergy > 0) break;
        }
        CableNetworks.setExtraction(level, worldPosition, extractionMask != 0 || pendingEnergy > 0);
    }
}
