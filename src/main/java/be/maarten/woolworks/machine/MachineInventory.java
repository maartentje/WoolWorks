package be.maarten.woolworks.machine;

import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Transactional machine storage with stack-oriented internal helpers. */
public abstract class MachineInventory extends ItemStacksResourceHandler {

    protected MachineInventory(int size) { super(size); }
    protected abstract boolean accepts(int slot, ItemStack stack);
    protected int limit(int slot, ItemStack stack) { return stack.isEmpty() ? 64 : Math.min(64, stack.getMaxStackSize()); }
    @Override public boolean isValid(int slot, ItemResource resource) { return !resource.isEmpty() && accepts(slot, resource.toStack()); }
    @Override protected int getCapacity(int slot, ItemResource resource) { return limit(slot, resource.isEmpty() ? ItemStack.EMPTY : resource.toStack()); }
    public int getSlots() { return size(); }
    public ItemStack getStackInSlot(int slot) { return stacks.get(slot).copy(); }
    public void setStackInSlot(int slot, ItemStack stack) { set(slot, ItemResource.of(stack), stack.getCount()); }
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { if (stack.isEmpty()) return ItemStack.EMPTY;
        try (var tx = Transaction.openRoot()) {
            int moved = insert(slot, ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return stack.copyWithCount(stack.getCount() - moved);
        } }
    public ItemStack extractItem(int slot, int amount, boolean simulate) { var resource = getResource(slot);
        if (resource.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        try (var tx = Transaction.openRoot()) {
            int moved = extract(slot, resource, amount, tx);
            if (!simulate) tx.commit();
            return moved == 0 ? ItemStack.EMPTY : resource.toStack(moved);
        } }
    public int getSlotLimit(int slot) { return limit(slot, ItemStack.EMPTY); }
    public boolean isItemValid(int slot, ItemStack stack) { return !stack.isEmpty() && accepts(slot, stack); }

    /** Keep the 1.21.1 sparse representation rather than the dense transfer-handler format. */
    @Override public void serialize(ValueOutput output) {
        output.putInt("Size", size());
        var items = output.list("Items", ItemStackWithSlot.CODEC);
        for (int i = 0; i < size(); i++) {
            var stack = getStackInSlot(i);
            if (!stack.isEmpty()) items.add(new ItemStackWithSlot(i, stack));
        }
    }
    @Override public void deserialize(ValueInput input) {
        List<ItemStackWithSlot> entries = input.keySet().contains("Items")
                ? input.read("Items", ItemStackWithSlot.CODEC.listOf()).orElseThrow(() -> new IllegalStateException("Unreadable machine inventory Items"))
                : List.of();
        var seen = new boolean[size()];
        for (var entry : entries) {
            if (!entry.isValidInContainer(size()) || seen[entry.slot()]) throw new IllegalStateException("Invalid or duplicate machine inventory slot " + entry.slot());
            seen[entry.slot()] = true;
        }
        for (int i = 0; i < size(); i++) setStackInSlot(i, ItemStack.EMPTY);
        for (var entry : entries) setStackInSlot(entry.slot(), entry.stack());
    }

    public ResourceHandler<ItemResource> automation(int exposedSlots, IntPredicate insertion, IntPredicate extraction) {
        var backing = this;
        return new ResourceHandler<>() {
            @Override public int size() { return exposedSlots; }
            @Override public ItemResource getResource(int index) { Objects.checkIndex(index, size()); return backing.getResource(index); }
            @Override public long getAmountAsLong(int index) { Objects.checkIndex(index, size()); return backing.getAmountAsLong(index); }
            @Override public long getCapacityAsLong(int index, ItemResource resource) { Objects.checkIndex(index, size()); return insertion.test(index) ? backing.getCapacityAsLong(index, resource) : 0; }
            @Override public boolean isValid(int index, ItemResource resource) { Objects.checkIndex(index, size()); return insertion.test(index) && backing.isValid(index, resource); }
            @Override public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
                Objects.checkIndex(index, size()); TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
                return insertion.test(index) ? backing.insert(index, resource, amount, tx) : 0;
            }
            @Override public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
                Objects.checkIndex(index, size()); TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
                return extraction.test(index) ? backing.extract(index, resource, amount, tx) : 0;
            }
        };
    }
}
