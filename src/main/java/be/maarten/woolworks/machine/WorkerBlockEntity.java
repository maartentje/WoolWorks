package be.maarten.woolworks.machine;

import be.maarten.woolworks.entity.ResourceSheep;
import be.maarten.woolworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class WorkerBlockEntity extends MufflableBlockEntity {
    public static final int CAPACITY = 20_000;
    private int area = 3;
    private int rotation;
    private int operationProgress = -1;
    private record PendingShearing(ItemStack wool, List<ItemStack> drops) {}
    private final Map<UUID, PendingShearing> pendingDrops = new HashMap<>();
    public static final int RUNNING = 0, MISSING_INPUTS = 1, INSUFFICIENT_ENERGY = 2, OUTPUT_FULL = 3;
    private final MachineEnergy energy = new MachineEnergy(CAPACITY, Integer.MAX_VALUE, 0,
            () -> level != null && !level.isClientSide(), () -> level == null ? Long.MIN_VALUE : level.getGameTime(), this::setChanged);
    private final MachineInventory inventory = new MachineInventory(20) {
        @Override protected boolean accepts(int slot, ItemStack stack) { return slot == 0 ? isShears(stack) : slot == 1 && stack.is(Items.WHEAT); }
        @Override protected int limit(int slot, ItemStack stack) { return slot == 0 ? 1 : super.limit(slot, stack); }
        @Override protected void onContentsChanged(int slot, ItemStack previous) { setChanged(); }
    };
    private final net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> automation = inventory.automation(20, slot -> slot < 2, slot -> slot >= 2);

    public WorkerBlockEntity(BlockPos pos, BlockState state) { super(ModMachines.WORKER_ENTITY.get(), pos, state, true); }
    @Override public void onLoad() { super.onLoad(); if (level != null && !level.isClientSide() && muffleMode().sheepMuted()) SheepSoundZones.add(this); }
    @Override protected void onMuffleModeChanged() { if (muffleMode().sheepMuted()) SheepSoundZones.add(this); else SheepSoundZones.remove(this); }
    @Override public void onChunkUnloaded() { SheepSoundZones.remove(this); super.onChunkUnloaded(); }
    @Override public void setRemoved() { SheepSoundZones.remove(this); super.setRemoved(); }
    public static boolean isShears(ItemStack stack) { return stack.canPerformAction(ItemAbilities.SHEARS_HARVEST); }
    public MachineInventory inventory() { return inventory; }
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> automation() { return automation; }
    public net.neoforged.neoforge.transfer.energy.EnergyHandler energy() { return energy; }
    public int area() { return area; }
    public int remainingOperationTicks() { return Math.max(0, MachineConfig.WORKER_INTERVAL.get() - Math.max(0, operationProgress)); }
    public static int regrowthCost() { return Math.min(MachineConfig.WORKER_REGROWTH_COST.get(), CAPACITY); }
    public static int shearingCost() { return Math.min(MachineConfig.WORKER_SHEAR_COST.get(), CAPACITY); }
    public void cycleArea() {
        area = area >= 9 ? 1 : area + 2;
        updateArea();
    }
    private void updateArea() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public ContainerData data() { return new ContainerData() {
        @Override public int get(int index) {
            int interval = MachineConfig.WORKER_INTERVAL.get();
            int elapsed = Math.clamp(operationProgress, 0, interval);
            return switch(index) { case 0 -> energy.getEnergyStored() & 65535; case 1 -> energy.getEnergyStored() >>> 16; case 2 -> area;
                case 3 -> elapsed & 65535; case 4 -> elapsed >>> 16; case 5 -> interval & 65535; case 6 -> interval >>> 16; case 7 -> operationStatus(); default -> 0; };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 8; }
    }; }
    private boolean hasOutputSpace() {
        for (int slot = 2; slot < 20; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < Math.min(inventory.getSlotLimit(slot), stack.getMaxStackSize())) return true;
        }
        return false;
    }
    public int operationStatus() {
        boolean wheat = inventory.getStackInSlot(1).getCount() >= MachineConfig.WORKER_WHEAT_COST.get();
        boolean shears = isShears(inventory.getStackInSlot(0));
        boolean room = hasOutputSpace();
        int stored = energy.getEnergyStored();
        if ((wheat && stored >= regrowthCost()) || (shears && room && stored >= shearingCost())) return RUNNING;
        if (!wheat && !shears) return MISSING_INPUTS;
        if ((wheat && stored < regrowthCost()) || (shears && room && stored < shearingCost())) return INSUFFICIENT_ENERGY;
        return OUTPUT_FULL;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, WorkerBlockEntity worker) {
        int interval = MachineConfig.WORKER_INTERVAL.get();
        if (worker.operationProgress < 0) worker.operationProgress = (int)Math.floorMod(pos.asLong(), (long)interval);
        worker.operationProgress = Math.clamp(worker.operationProgress, 0, interval - 1);
        if (worker.operationStatus() != RUNNING) return;
        worker.operationProgress++;
        if (worker.operationProgress >= interval) {
            worker.operationProgress = 0;
            worker.process((ServerLevel)level);
        }
        worker.setChanged();
    }
    private ItemStack insertOutput(ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (int i = 2; i < 20 && !remainder.isEmpty(); i++) {
            ItemStack existing = inventory.getStackInSlot(i);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remainder)) continue;
            int moved = Math.min(remainder.getCount(), Math.min(64, existing.getMaxStackSize()) - existing.getCount());
            if (moved > 0) { if (!simulate) { ItemStack copy = existing.copy(); copy.grow(moved); inventory.setStackInSlot(i, copy); } remainder.shrink(moved); }
        }
        for (int i = 2; i < 20 && !remainder.isEmpty(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) continue;
            int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
            if (!simulate) inventory.setStackInSlot(i, remainder.copyWithCount(moved));
            remainder.shrink(moved);
        }
        return remainder;
    }
    private boolean canStoreAll(List<ItemStack> drops) {
        ItemStackHandler copy = new ItemStackHandler(18);
        for (int slot = 0; slot < 18; slot++) copy.setStackInSlot(slot, inventory.getStackInSlot(slot + 2).copy());
        for (ItemStack drop : drops) {
            ItemStack left = drop.copy();
            for (int slot = 0; slot < 18 && !left.isEmpty(); slot++) left = copy.insertItem(slot, left, false);
            if (!left.isEmpty()) return false;
        }
        return true;
    }
    private ItemStack minimumWool(Sheep sheep) {
        if (sheep instanceof ResourceSheep resource) {
            if (resource.resourceType().isEmpty()) return ItemStack.EMPTY;
            ItemStack result = ModItems.withResource(ModItems.INFUSED_WOOL.get(), resource.resourceId());
            result.setCount(resource.resourceType().get().minYield());
            return result;
        }
        ItemStack result = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(sheep.getColor().getName() + "_wool")));
        return result;
    }
    private void process(ServerLevel server) {
        int regrow = regrowthCost(), shear = shearingCost();
        int wheat = MachineConfig.WORKER_WHEAT_COST.get();
        boolean canGrow = inventory.getStackInSlot(1).getCount() >= wheat && energy.getEnergyStored() >= regrow;
        boolean canShear = isShears(inventory.getStackInSlot(0)) && energy.getEnergyStored() >= shear;
        if (!canGrow && !canShear) return;
        var bounds = WorkerArea.bounds(worldPosition, getBlockState().getValue(WorkerBlock.FACING), area);
        var sheep = server.getEntitiesOfClass(Sheep.class, bounds,
                animal -> animal.isAlive() && !animal.isBaby() && bounds.contains(animal.position()) && server.shouldTickBlocksAt(animal.blockPosition()));
        if (sheep.isEmpty()) return;
        int start = Math.floorMod(rotation++, sheep.size());
        for (int i = 0; i < sheep.size(); i++) {
            Sheep animal = sheep.get((start + i) % sheep.size());
            if (animal.isSheared() && pendingDrops.remove(animal.getUUID()) != null) setChanged();
            ItemStack estimate = minimumWool(animal);
            boolean room = !estimate.isEmpty() && insertOutput(estimate, true).isEmpty();
            if (animal.isSheared()
                    && inventory.getStackInSlot(1).getCount() >= wheat && energy.getEnergyStored() >= regrow) {
                inventory.extractItem(1, wheat, false);
                energy.setStored(energy.getEnergyStored() - regrow);
                animal.setSheared(false);
                setChanged();
            }
            var tool = inventory.getStackInSlot(0);
            if (!isShears(tool) || energy.getEnergyStored() < shear || !animal.readyForShearing()) continue;
            IShearable target = (IShearable) animal;
            if (!target.isShearable(null, tool, server, animal.blockPosition())) continue;
            List<ItemStack> drops;
            PendingShearing pending = pendingDrops.get(animal.getUUID());
            if (pending != null && !ItemStack.isSameItemSameComponents(pending.wool(), estimate)) {
                // The sheep's colour/resource changed after the result was reserved.
                pendingDrops.remove(animal.getUUID());
                pending = null;
                setChanged();
            }
            if (pending != null) {
                if (!canStoreAll(pending.drops())) continue;
                drops = pending.drops();
                animal.setSheared(true);
                pendingDrops.remove(animal.getUUID());
            } else {
                if (!room) continue;
                drops = MachineSounds.captureShearing(animal, () -> target.onSheared(null, tool, server, animal.blockPosition()));
                if (!animal.isSheared()) continue;
                if (!canStoreAll(drops)) {
                    // Preserve an unexpectedly large/custom hook result without rerolling it.
                    animal.setSheared(false);
                    pendingDrops.put(animal.getUUID(), new PendingShearing(estimate.copyWithCount(1), drops.stream().map(ItemStack::copy).toList()));
                    setChanged();
                    continue;
                }
            }
            energy.setStored(energy.getEnergyStored() - shear);
            MachineSounds.shearing(server, animal, muffleMode());
            for (var drop : drops) insertOutput(drop, false);
            int durability = MachineConfig.WORKER_DURABILITY_COST.get();
            if (durability > 0) { tool.hurtAndBreak(durability, server, null, item -> {}); inventory.setStackInSlot(0, tool); }
            setChanged();
        }
    }
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out); inventory.serialize(out.child("Inventory"));
        out.putInt("Energy", energy.getEnergyStored()); out.putInt("Area", area); out.putInt("Rotation", rotation);
        if (operationProgress >= 0) out.putInt("OperationProgress", operationProgress);
        if (!pendingDrops.isEmpty()) {
            var plans = out.childrenList("PendingShearing");
            pendingDrops.forEach((id, pending) -> {
                var plan = plans.addChild(); plan.store("Sheep", net.minecraft.core.UUIDUtil.CODEC, id);
                plan.store("Wool", ItemStack.CODEC, pending.wool()); plan.store("Drops", ItemStack.CODEC.listOf(), pending.drops());
            });
        }
    }
    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        if (in.keySet().contains("Inventory")) inventory.deserialize(in.child("Inventory").orElseThrow(() -> new IllegalStateException("Unreadable worker inventory")));
        energy.load(in);
        int saved = in.getIntOr("Area", 0); area = saved == 11 ? 9 : saved >= 1 && saved <= 9 && (saved & 1) == 1 ? saved : 3;
        rotation = in.getIntOr("Rotation", 0); operationProgress = in.getInt("OperationProgress").map(v -> Math.max(0, v)).orElse(-1);
        pendingDrops.clear();
        var plans = in.childrenList("PendingShearing");
        if (in.keySet().contains("PendingShearing") && plans.isEmpty()) throw new IllegalStateException("Unreadable pending shearing");
        for (var plan : plans.orElseGet(() -> in.childrenListOrEmpty("PendingShearing"))) {
            var id = plan.read("Sheep", net.minecraft.core.UUIDUtil.CODEC).orElseThrow(() -> new IllegalStateException("Unreadable pending sheep UUID"));
            var wool = plan.read("Wool", ItemStack.CODEC).orElseThrow(() -> new IllegalStateException("Unreadable pending wool"));
            var drops = plan.read("Drops", ItemStack.CODEC.listOf()).orElseThrow(() -> new IllegalStateException("Unreadable pending drops"));
            if (pendingDrops.put(id, new PendingShearing(wool, List.copyOf(drops))) != null) throw new IllegalStateException("Duplicate pending sheep " + id);
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof net.minecraft.server.level.ServerLevel) {
            for (int i = 0; i < inventory().getSlots(); i++) {
                var stack = inventory().getStackInSlot(i);
                if (!stack.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
        }
    }
}
