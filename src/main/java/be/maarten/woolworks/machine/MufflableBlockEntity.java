package be.maarten.woolworks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class MufflableBlockEntity extends BlockEntity {
    private MuffleMode mode = MuffleMode.OFF;
    private final boolean sheepModes;
    protected MufflableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean sheepModes) {
        super(type, pos, state); this.sheepModes = sheepModes;
    }
    public MuffleMode muffleMode() { return mode; }
    public void cycleMuffleMode() {
        if (level == null || level.isClientSide()) return;
        mode = mode.next(sheepModes); setChanged(); onMuffleModeChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    protected void onMuffleModeChanged() {}
    @Override protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag); tag.putInt("MuffleMode", mode.ordinal());
    }
    @Override protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        MuffleMode loaded = MuffleMode.read(tag.getIntOr("MuffleMode", 0), sheepModes);
        if (mode != loaded) { mode = loaded; onMuffleModeChanged(); }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
