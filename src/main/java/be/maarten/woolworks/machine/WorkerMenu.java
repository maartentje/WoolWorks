package be.maarten.woolworks.machine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class WorkerMenu extends AbstractContainerMenu {
    private final WorkerBlockEntity worker;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    public static WorkerMenu client(int id, Inventory player) { return new WorkerMenu(id, player, new ItemStackHandler(20), new SimpleContainerData(8), null, ContainerLevelAccess.NULL); }
    public static WorkerMenu server(int id, Inventory player, WorkerBlockEntity worker) { return new WorkerMenu(id, player, worker.inventory(), worker.data(), worker, ContainerLevelAccess.create(worker.getLevel(), worker.getBlockPos())); }
    private WorkerMenu(int id, Inventory player, net.neoforged.neoforge.items.IItemHandler items, ContainerData data, WorkerBlockEntity worker, ContainerLevelAccess access) {
        super(ModMachines.WORKER_MENU.get(), id);
        this.worker = worker; this.access = access; this.data = data;
        addSlot(new SlotItemHandler(items, 0, 29, 25) { @Override public boolean mayPlace(ItemStack stack) { return WorkerBlockEntity.isShears(stack); } @Override public int getMaxStackSize() { return 1; } });
        addSlot(new SlotItemHandler(items, 1, 29, 61) { @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.WHEAT); } });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 6; col++) addSlot(new SlotItemHandler(items, 2 + row * 6 + col, 61 + col * 18, 25 + row * 18) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 100 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 158));
        addDataSlots(data);
    }
    public int energy() { return (data.get(0) & 65535) | (data.get(1) & 65535) << 16; }
    public int area() { return data.get(2); }
    public int elapsed() { return (data.get(3) & 65535) | (data.get(4) & 65535) << 16; }
    public int interval() { return (data.get(5) & 65535) | (data.get(6) & 65535) << 16; }
    public int status() { return data.get(7); }
    @Override public boolean clickMenuButton(Player player, int id) { if (id != 0 || worker == null || !stillValid(player)) return false; worker.cycleArea(); return true; }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, ModMachines.WORKER.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), result = stack.copy();
        if (index < 20) { if (!moveItemStackTo(stack, 20, slots.size(), true)) return ItemStack.EMPTY; }
        else if (WorkerBlockEntity.isShears(stack)) { if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY; }
        else if (stack.is(Items.WHEAT)) { if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY; }
        else return ItemStack.EMPTY;
        slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.onTake(player, stack); return result;
    }
}
