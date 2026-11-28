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

    private final MachineEnergy energy = new MachineEnergy(CAPACITY, CAPACITY, 0,
            () -> level != null && !level.isClientSide(), () -> level == null ? Long.MIN_VALUE : level.getGameTime(), this::setChanged);
    private final MachineInventory inventory = new MachineInventory(3) {
        @Override protected boolean accepts(int slot, ItemStack stack) {
            if (slot == 2) return ExtractorUpgradeItem.limit(stack) > 0;
            return slot == 0 && level instanceof ServerLevel server && MachineRecipeLookup.serverExtraction(server, stack).isPresent();
        }
        @Override protected int limit(int slot, ItemStack stack) { return slot == 2 && !stack.isEmpty() ? Math.min(stack.getMaxStackSize(), ExtractorUpgradeItem.limit(stack)) : super.limit(slot, stack); }
        @Override protected void onContentsChanged(int slot, ItemStack previous) { setChanged(); }
    };
    private final net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> automation = inventory.automation(2, slot -> slot == 0, slot -> slot == 1);

    public ExtractorBlockEntity(BlockPos pos, BlockState state) { super(ModMachines.EXTRACTOR_ENTITY.get(), pos, state, false); }
    public net.neoforged.neoforge.transfer.energy.EnergyHandler energy() { return energy; }
    public MachineInventory inventory() { return inventory; }
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> automation() { return automation; }
    public boolean isProcessing() { return processing; }
    public int progressPermille() { return recipeDuration > 0 && !recipeId.isEmpty() ? (int)Math.min(1000L, halfProgress * 1000L / (2L * recipeDuration)) : 0; }
    public void dropRecovered() {
        if (level == null || level.isClientSide()) return;
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
        if (!level.isClientSide()) {
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
        energy.setStored(energy.getEnergyStored() - (int)cost);
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
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out); inventory.serialize(out.child("Inventory"));
        out.putInt("Energy", energy.getEnergyStored()); out.putLong("HalfProgress", halfProgress);
        out.putString("Recipe", recipeId); out.putInt("RecipeDuration", recipeDuration); out.putInt("RecipeEnergy", recipeEnergy);
        if (!recipeInput.isEmpty()) out.store("RecipeInput", ItemStack.CODEC, recipeInput);
        if (!recipeResult.isEmpty()) out.store("RecipeResult", ItemStack.CODEC, recipeResult);
        if (!recipeUpgrade.isEmpty()) out.store("RecipeUpgrade", ItemStack.CODEC, recipeUpgrade);
        if (!recoveredUpgrades.isEmpty()) out.store("RecoveredUpgrades", ItemStack.CODEC.listOf(), recoveredUpgrades);
    }
    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        if (in.keySet().contains("Inventory")) inventory.deserialize(in.child("Inventory").orElseThrow(() -> new IllegalStateException("Unreadable extractor inventory")));
        recoveredUpgrades.clear();
        if (in.keySet().contains("RecoveredUpgrades")) recoveredUpgrades.addAll(in.read("RecoveredUpgrades", ItemStack.CODEC.listOf()).orElseThrow(() -> new IllegalStateException("Unreadable recovered upgrades")));
        ItemStack installed = inventory.getStackInSlot(2); int limit = ExtractorUpgradeItem.limit(installed);
        if (!installed.isEmpty() && installed.getCount() > limit) {
            recoveredUpgrades.add(installed.copyWithCount(installed.getCount() - limit));
            inventory.setStackInSlot(2, limit == 0 ? ItemStack.EMPTY : installed.copyWithCount(limit));
        }
        energy.load(in); halfProgress = Math.max(0, in.getLongOr("HalfProgress", 0));
        recipeId = in.getStringOr("Recipe", ""); recipeDuration = in.getIntOr("RecipeDuration", 0); recipeEnergy = in.getIntOr("RecipeEnergy", 0);
        recipeInput = readStack(in, "RecipeInput"); recipeResult = readStack(in, "RecipeResult"); recipeUpgrade = readStack(in, "RecipeUpgrade");
    }
    private static ItemStack readStack(net.minecraft.world.level.storage.ValueInput in, String key) {
        return in.keySet().contains(key) ? in.read(key, ItemStack.CODEC).orElseThrow(() -> new IllegalStateException("Unreadable " + key)) : ItemStack.EMPTY;
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof net.minecraft.server.level.ServerLevel) {
            dropRecovered();
            for (int i = 0; i < inventory().getSlots(); i++) {
                var stack = inventory().getStackInSlot(i);
                if (!stack.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
        }
    }
}
