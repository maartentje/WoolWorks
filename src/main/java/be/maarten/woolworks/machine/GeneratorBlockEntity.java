package be.maarten.woolworks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import be.maarten.woolworks.recipe.GeneratorFuelRecipe;
import be.maarten.woolworks.recipe.MachineRecipeConditions;
import be.maarten.woolworks.resource.GeneratorSettings;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class GeneratorBlockEntity extends MufflableBlockEntity {
    private static final int CAPACITY = 20_000;
    private static final int OUTPUT_PER_TICK = 100;
    private int remainingBurn;
    private int inactiveTicks;
    private int generationRate;
    private int burnTotal;
    private boolean processing;
    private long lastExtractionTick = Long.MIN_VALUE;
    private int extractedThisTick;

    private final MachineEnergy energy = new MachineEnergy(CAPACITY, 0, OUTPUT_PER_TICK) {
        @Override public int extractEnergy(int amount, boolean simulate) {
            if (level == null || level.isClientSide() || amount <= 0) return 0;
            long tick = level.getGameTime();
            if (lastExtractionTick != tick) {
                lastExtractionTick = tick;
                extractedThisTick = 0;
            }
            int extracted = super.extractEnergy(Math.min(amount, OUTPUT_PER_TICK - extractedThisTick), simulate);
            if (extracted > 0 && !simulate) {
                extractedThisTick += extracted;
                setChanged();
            }
            return extracted;
        }
    };

    private ItemStack migratedRemainder = ItemStack.EMPTY;
    private final MachineInventory fuel = new MachineInventory(1) {
        @Override protected boolean accepts(int slot, ItemStack stack) { return slot == 0 && level != null && GeneratorFuels.find(level, stack).isPresent(); }
        @Override protected void onContentsChanged(int slot, ItemStack previous) { setChanged(); }
    };
    private final net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> automation = fuel.automation(1, slot -> slot == 0, slot -> false);

    public GeneratorBlockEntity(BlockPos pos, BlockState state) { super(ModMachines.GENERATOR_ENTITY.get(), pos, state, false); }
    public EnergyStorage energy() { return energy; }
    public MachineInventory fuel() { return fuel; }
    public ItemStack migratedRemainder() { return migratedRemainder; }
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> automation() { return automation; }
    public int remainingFuelTicks() { return remainingBurn; }
    public int fuelGenerationRate() { return remainingBurn > 0 ? generationRate : 0; }
    public boolean isProcessing() { return processing; }
    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) { return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> remainingBurn > 0 && burnTotal > 0 ? (int)(1000L * (burnTotal - remainingBurn) / burnTotal) : 0;
                case 2 -> remainingBurn > 0 ? Math.min(generationRate, 32767) : 0;
                case 3 -> remainingBurn & 0xffff;
                case 4 -> remainingBurn >>> 16;
                case 7 -> generationRate & 0xffff;
                case 8 -> generationRate >>> 16;
                case 9 -> processing ? 1 : 0;
                default -> 0;
            }; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 12; }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity generator) {
        if (level.isClientSide()) return;
        generator.tickServer(level, pos);
    }

    private void tickServer(Level level, BlockPos pos) {
        if (!migratedRemainder.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), migratedRemainder);
            migratedRemainder = ItemStack.EMPTY;
            setChanged();
        }
        if (remainingBurn <= 0) {
            ItemStack stack = fuel.getStackInSlot(0);
            var candidate = GeneratorFuels.find(level, stack);
            if (candidate.isPresent()) {
                var value = candidate.get();
                if (value.energyPerTick() <= CAPACITY - energy.getEnergyStored()) {
                    remainingBurn = value.ticks();
                    burnTotal = remainingBurn;
                    generationRate = value.energyPerTick();
                    fuel.extractItem(0, 1, false);
                    MachineSounds.fuel(level, pos, muffleMode());
                    setChanged();
                }
            }
        }
        boolean generating = remainingBurn > 0 && generationRate > 0 && generationRate <= CAPACITY - energy.getEnergyStored();
        processing = generating;
        if (generating) {
            // Internal generation bypasses the public receive limit, which is intentionally zero.
            energy.setStored(energy.getEnergyStored() + generationRate);
            remainingBurn--;
            setChanged();
        }
        if (generating) {
            inactiveTicks = 0;
            BlockState current = level.getBlockState(pos);
            if (current.is(ModMachines.GENERATOR.get()) && !current.getValue(GeneratorBlock.ACTIVE))
                level.setBlock(pos, current.setValue(GeneratorBlock.ACTIVE, true), 3);
        } else {
            inactiveTicks++;
            if (inactiveTicks >= 40) {
                inactiveTicks = 40;
                BlockState current = level.getBlockState(pos);
                if (current.is(ModMachines.GENERATOR.get()) && current.getValue(GeneratorBlock.ACTIVE))
                    level.setBlock(pos, current.setValue(GeneratorBlock.ACTIVE, false), 3);
            }
        }
        int remainingOutput = OUTPUT_PER_TICK;
        for (Direction direction : Direction.values()) {
            if (remainingOutput == 0 || energy.getEnergyStored() == 0) break;
            BlockPos targetPos = pos.relative(direction);
            if (!level.hasChunkAt(targetPos)) continue;
            var target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, direction.getOpposite());
            if (target == null || !target.canReceive()) continue;
            int offered = energy.extractEnergy(remainingOutput, true);
            int accepted = target.receiveEnergy(offered, true);
            if (accepted <= 0) continue;
            int transferred = target.receiveEnergy(Math.min(offered, accepted), false);
            if (transferred > 0) {
                energy.extractEnergy(transferred, false);
                remainingOutput -= transferred;
            }
        }
    }

    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        fuel.serialize(out.child("Fuel"));
        if (!migratedRemainder.isEmpty()) out.store("MigratedRemainder", ItemStack.CODEC, migratedRemainder);
        out.putInt("Energy", energy.getEnergyStored());
        out.putInt("BurnRemaining", remainingBurn); out.putInt("GenerationRate", generationRate);
        out.putInt("BurnTotal", burnTotal); out.putInt("InactiveTicks", inactiveTicks);
    }
    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        var old = new MachineInventory(2) { @Override protected boolean accepts(int slot, ItemStack stack) { return true; } };
        if (in.keySet().contains("Fuel")) old.deserialize(in.child("Fuel").orElseThrow(() -> new IllegalStateException("Unreadable Fuel inventory")));
        migratedRemainder = in.keySet().contains("MigratedRemainder") ? in.read("MigratedRemainder", ItemStack.CODEC).orElseThrow(() -> new IllegalStateException("Unreadable MigratedRemainder")) : ItemStack.EMPTY;
        if (migratedRemainder.isEmpty()) migratedRemainder = old.getStackInSlot(1).copy();
        fuel.setStackInSlot(0, old.getStackInSlot(0).copy());
        energy.setStored(in.getIntOr("Energy", 0));
        remainingBurn = Math.max(0, in.getIntOr("BurnRemaining", 0));
        generationRate = Math.max(0, in.getIntOr("GenerationRate", 0));
        burnTotal = Math.max(remainingBurn, in.getIntOr("BurnTotal", 0));
        inactiveTicks = Math.clamp(in.getIntOr("InactiveTicks", 0), 0, 40);
    }
}
