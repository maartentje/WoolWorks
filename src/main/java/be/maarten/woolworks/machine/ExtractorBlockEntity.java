package be.maarten.woolworks.machine;

import be.maarten.woolworks.recipe.MachineRecipeLookup;
import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.item.ExtractorUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.ArrayList;
import java.util.List;

public class ExtractorBlockEntity extends MufflableBlockEntity {
    private static final int CAPACITY = 20_000;
    private long halfProgress;
    private String recipeId = "";
    private int recipeDuration;
    private int recipeEnergy;
    private ItemStack recipeInput = ItemStack.EMPTY;
    private ItemStack recipeResult = ItemStack.EMPTY;
    private ItemStack recipeUpgrade = ItemStack.EMPTY;
    private boolean oversizedOutput;
    private boolean processing;

    public long remainingTicks() {
        if (recipeId.isEmpty() || recipeDuration <= 0) return 0;
        long remaining = Math.max(0, 2L * recipeDuration - halfProgress);
        int speed = ExtractorUpgradeItem.effective(inventory.getStackInSlot(2)).halfSpeed();
        return (remaining + speed - 1) / speed;
    }
    private final List<ItemStack> recoveredUpgrades = new ArrayList<>();

    private final EnergyStorage energy = new EnergyStorage(CAPACITY, CAPACITY, 0) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            if (level == null || level.isClientSide || amount <= 0) return 0;
            int received = super.receiveEnergy(amount, simulate);
            if (received > 0 && !simulate) setChanged();
            return received;
        }
    };
    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 2) return ExtractorUpgradeItem.limit(stack) > 0;
            return slot == 0 && level instanceof ServerLevel server && MachineRecipeLookup.serverExtraction(server, stack).isPresent();
        }
        @Override protected int getStackLimit(int slot, ItemStack stack) { return slot == 2 ? Math.min(stack.getMaxStackSize(), ExtractorUpgradeItem.limit(stack)) : super.getStackLimit(slot, stack); }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot == 0 ? inventory.insertItem(0, stack, simulate) : stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return slot == 1 ? inventory.extractItem(1, amount, simulate) : ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && inventory.isItemValid(0, stack); }
    };

    public ExtractorBlockEntity(BlockPos pos, BlockState state) { super(ModMachines.EXTRACTOR_ENTITY.get(), pos, state, false); }
    public EnergyStorage energy() { return energy; }
    public ItemStackHandler inventory() { return inventory; }
    public IItemHandler automation() { return automation; }
    public boolean isProcessing() { return processing; }
    public int progressPermille() { return recipeDuration > 0 && !recipeId.isEmpty() ? (int)Math.min(1000L, halfProgress * 1000L / (2L * recipeDuration)) : 0; }
    public void dropRecovered() {
        if (level == null || level.isClientSide) return;
        for (ItemStack stack : recoveredUpgrades) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        if (!recoveredUpgrades.isEmpty()) { recoveredUpgrades.clear(); setChanged(); }
    }
    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) {
                long effectiveRate = (long)recipeEnergy * ExtractorUpgradeItem.effective(inventory.getStackInSlot(2)).energyMultiplier();
                return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> recipeDuration > 0 ? (int)Math.min(1000L, halfProgress * 1000L / (2L * recipeDuration)) : 0;
                case 2 -> (int)(effectiveRate & 0xffff);
                case 3 -> inventory.getStackInSlot(2).getCount();
                case 4 -> (int)((effectiveRate >>> 16) & 0xffff);
                case 5 -> (int)((effectiveRate >>> 32) & 0xffff);
                case 6 -> oversizedOutput ? 1 : 0;
                case 7 -> (int)(remainingTicks() & 0xffff);
                case 8 -> (int)((remainingTicks() >>> 16) & 0xffff);
                case 9 -> processing ? 1 : 0;
                case 10 -> (int)((remainingTicks() >>> 32) & 0xffff);
                default -> 0;
            }; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 12; }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExtractorBlockEntity entity) {
        if (!level.isClientSide) {
            boolean processing = entity.tickServer(level);
            entity.processing = processing;
            BlockState current = level.getBlockState(pos);
            if (current.is(ModMachines.EXTRACTOR.get()) && current.getValue(ExtractorBlock.ACTIVE) != processing)
                level.setBlock(pos, current.setValue(ExtractorBlock.ACTIVE, processing), 3);
        }
    }
    private void resetProgress() {
        if (halfProgress != 0 || !recipeId.isEmpty()) {
            halfProgress = 0;
            recipeId = "";
            recipeInput = ItemStack.EMPTY;
            recipeResult = ItemStack.EMPTY;
            setChanged();
        }
    }
    private boolean tickServer(Level level) {
        dropRecovered();
        var input = inventory.getStackInSlot(0);
        if (input.isEmpty()) { resetProgress(); recipeEnergy = 0; oversizedOutput = false; return false; }
        var match = MachineRecipeLookup.serverExtraction((ServerLevel) level, input);
        if (match.isEmpty()) { resetProgress(); recipeEnergy = 0; oversizedOutput = false; return false; }
        var holder = match.get();
        var recipe = holder.value();
        var result = recipe.assemble(new SingleRecipeInput(input));
        if (result.isEmpty()) { resetProgress(); recipeEnergy = 0; oversizedOutput = false; return false; }
        ItemStack upgrade = inventory.getStackInSlot(2);
        var stats = ExtractorUpgradeItem.effective(upgrade);
        if (!recipeId.equals(holder.id().identifier().toString()) || recipeDuration != recipe.duration() || recipeEnergy != recipe.energyPerTick()
                || !ItemStack.isSameItemSameComponents(recipeInput, input) || !ItemStack.matches(recipeResult, result)
                || !ItemStack.matches(recipeUpgrade, upgrade)) {
            halfProgress = 0;
            recipeId = holder.id().identifier().toString();
            recipeDuration = recipe.duration();
            recipeEnergy = recipe.energyPerTick();
            recipeInput = input.copyWithCount(1);
            recipeResult = result.copy();
            recipeUpgrade = upgrade.copy();
            setChanged();
        }
        ItemStack output = inventory.getStackInSlot(1);
        long outputCount = (long)result.getCount() * stats.outputMultiplier();
        oversizedOutput = outputCount > Math.min(result.getMaxStackSize(), inventory.getSlotLimit(1));
        if (oversizedOutput) return false;
        if ((!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, result))
                || output.getCount() + outputCount > Math.min(result.getMaxStackSize(), inventory.getSlotLimit(1))) return false;
        long remaining = 2L * recipe.duration() - halfProgress;
        if (remaining <= 0) { resetProgress(); return false; }
        int units = (int)Math.min(remaining, stats.halfSpeed());
        long cost = (long)recipe.energyPerTick() * stats.energyMultiplier();
        if (cost > energy.getEnergyStored()) return false;
        energy.deserializeNBT(level.registryAccess(), IntTag.valueOf(energy.getEnergyStored() - (int)cost));
        halfProgress += units;
        if (halfProgress >= 2L * recipe.duration()) {
            inventory.extractItem(0, 1, false);
            if (output.isEmpty()) inventory.setStackInSlot(1, result.copyWithCount((int)outputCount));
            else { ItemStack combined = output.copy(); combined.grow((int)outputCount); inventory.setStackInSlot(1, combined); }
            MachineSounds.extraction(level, worldPosition, muffleMode());
            resetProgress();
        }
        setChanged();
        return true;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putLong("HalfProgress", halfProgress);
        tag.putString("Recipe", recipeId);
        tag.putInt("RecipeDuration", recipeDuration);
        tag.putInt("RecipeEnergy", recipeEnergy);
        if (!recipeInput.isEmpty()) tag.put("RecipeInput", recipeInput.save(registries));
        if (!recipeResult.isEmpty()) tag.put("RecipeResult", recipeResult.save(registries));
        if (!recipeUpgrade.isEmpty()) tag.put("RecipeUpgrade", recipeUpgrade.save(registries));
        ListTag recovery = new ListTag();
        for (ItemStack stack : recoveredUpgrades) if (!stack.isEmpty()) recovery.add(stack.save(registries));
        if (!recovery.isEmpty()) tag.put("RecoveredUpgrades", recovery);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        recoveredUpgrades.clear();
        ListTag recovery = tag.getList("RecoveredUpgrades", 10);
        for (int i = 0; i < recovery.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, recovery.getCompound(i));
            if (!stack.isEmpty()) recoveredUpgrades.add(stack);
        }
        ItemStack installed = inventory.getStackInSlot(2);
        int limit = ExtractorUpgradeItem.limit(installed);
        if (!installed.isEmpty() && installed.getCount() > limit) {
            recoveredUpgrades.add(installed.copyWithCount(installed.getCount() - limit));
            inventory.setStackInSlot(2, limit == 0 ? ItemStack.EMPTY : installed.copyWithCount(limit));
        }
        energy.deserializeNBT(registries, IntTag.valueOf(Math.clamp(tag.getInt("Energy"), 0, CAPACITY)));
        halfProgress = Math.max(0L, tag.getLong("HalfProgress"));
        recipeId = tag.getString("Recipe");
        recipeDuration = tag.getInt("RecipeDuration");
        recipeEnergy = tag.getInt("RecipeEnergy");
        recipeInput = ItemStack.parseOptional(registries, tag.getCompound("RecipeInput"));
        recipeResult = ItemStack.parseOptional(registries, tag.getCompound("RecipeResult"));
        recipeUpgrade = ItemStack.parseOptional(registries, tag.getCompound("RecipeUpgrade"));
    }
}
