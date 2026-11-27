package be.maarten.woolworks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.resources.ResourceLocation;
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

    private final EnergyStorage energy = new EnergyStorage(CAPACITY, 0, OUTPUT_PER_TICK) {
        @Override public int extractEnergy(int amount, boolean simulate) {
            if (level == null || level.isClientSide || amount <= 0) return 0;
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
    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && level != null && GeneratorFuels.find(level, stack).isPresent();
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return fuel.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot == 0 ? fuel.insertItem(slot, stack, simulate) : stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return fuel.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && fuel.isItemValid(slot, stack); }
    };

    public GeneratorBlockEntity(BlockPos pos, BlockState state) { super(ModMachines.GENERATOR_ENTITY.get(), pos, state, false); }
    public EnergyStorage energy() { return energy; }
    public ItemStackHandler fuel() { return fuel; }
    public ItemStack migratedRemainder() { return migratedRemainder; }
    public IItemHandler automation() { return automation; }
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
        if (level.isClientSide) return;
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
            energy.deserializeNBT(level.registryAccess(), IntTag.valueOf(energy.getEnergyStored() + generationRate));
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

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fuel", fuel.serializeNBT(registries));
        if (!migratedRemainder.isEmpty()) tag.put("MigratedRemainder", migratedRemainder.save(registries));
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("BurnRemaining", remainingBurn);
        tag.putInt("GenerationRate", generationRate);
        tag.putInt("BurnTotal", burnTotal);
        tag.putInt("InactiveTicks", inactiveTicks);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag savedFuel = tag.getCompound("Fuel").copy();
        var old = new ItemStackHandler(2);
        savedFuel.putInt("Size", 2);
        old.deserializeNBT(registries, savedFuel);
        migratedRemainder = ItemStack.parseOptional(registries, tag.getCompound("MigratedRemainder"));
        if (migratedRemainder.isEmpty()) migratedRemainder = old.getStackInSlot(1).copy();
        fuel.setStackInSlot(0, old.getStackInSlot(0).copy());
        energy.deserializeNBT(registries, IntTag.valueOf(Math.clamp(tag.getInt("Energy"), 0, CAPACITY)));
        remainingBurn = Math.max(0, tag.getInt("BurnRemaining"));
        generationRate = Math.max(0, tag.getInt("GenerationRate"));
        burnTotal = Math.max(remainingBurn, tag.getInt("BurnTotal"));
        inactiveTicks = Math.clamp(tag.getInt("InactiveTicks"), 0, 40);
    }
}
