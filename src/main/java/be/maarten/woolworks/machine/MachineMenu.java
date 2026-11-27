package be.maarten.woolworks.machine;

import be.maarten.woolworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MachineMenu extends AbstractContainerMenu {
    private final boolean extractor;
    private final int machineSlots;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public static MachineMenu generatorClient(int id, Inventory player) {
        return new MachineMenu(id, player, false, new ItemStackHandler(1), new SimpleContainerData(12), ContainerLevelAccess.NULL);
    }
    public static MachineMenu extractorClient(int id, Inventory player) {
        return new MachineMenu(id, player, true, new ItemStackHandler(3), new SimpleContainerData(12), ContainerLevelAccess.NULL);
    }
    public static MachineMenu generator(int id, Inventory player, GeneratorBlockEntity entity) {
        return new MachineMenu(id, player, false, entity.fuel(), entity.menuData(), ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos()));
    }
    public static MachineMenu extractor(int id, Inventory player, ExtractorBlockEntity entity) {
        return new MachineMenu(id, player, true, entity.inventory(), entity.menuData(), ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos()));
    }

    private MachineMenu(int id, Inventory player, boolean extractor, ItemStackHandler items, ContainerData data, ContainerLevelAccess access) {
        super(extractor ? ModMachines.EXTRACTOR_MENU.get() : ModMachines.GENERATOR_MENU.get(), id);
        this.extractor = extractor;
        this.machineSlots = extractor ? 3 : 1;
        this.data = data;
        this.access = access;
        addSlot(new SlotItemHandler(items, 0, extractor ? 56 : 91, extractor ? 35 : 49));
        if (extractor) {
            addSlot(new SlotItemHandler(items, 1, 116, 35) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
            addSlot(new SlotItemHandler(items, 2, 152, 59) {
                @Override public boolean mayPlace(ItemStack stack) { return be.maarten.woolworks.item.ExtractorUpgradeItem.limit(stack) > 0; }
                @Override public int getMaxStackSize(ItemStack stack) { return Math.min(stack.getMaxStackSize(), be.maarten.woolworks.item.ExtractorUpgradeItem.limit(stack)); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public boolean isExtractor() { return extractor; }
    public int energy() { return data.get(0); }
    public int progress() { return data.get(1); }
    public long rate() { return extractor ? (data.get(2) & 0xffffL) | ((data.get(4) & 0xffffL) << 16) | ((data.get(5) & 0xffffL) << 32) : data.get(2); }
    public boolean oversizedOutput() { return extractor && data.get(6) != 0; }
    public be.maarten.woolworks.item.ExtractorUpgradeItem.Stats upgradeStats() {
        return be.maarten.woolworks.item.ExtractorUpgradeItem.effective(extractor ? slots.get(2).getItem() : ItemStack.EMPTY);
    }
    public int upgrades() { return data.get(3); }
    public int remainingBurn() { return (data.get(3) & 0xffff) | ((data.get(4) & 0xffff) << 16); }
    public boolean coalWoolAvailable() { return data.get(5) != 0; }
    public boolean furnaceFuelAvailable() { return data.get(6) != 0; }
    public long remainingTicks() { return extractor ? (data.get(7) & 0xffffL) | ((data.get(8) & 0xffffL) << 16) | ((data.get(10) & 0xffffL) << 32) : Integer.toUnsignedLong(remainingBurn()); }
    public long fuelRate() { return (data.get(7) & 0xffffL) | ((data.get(8) & 0xffffL) << 16); }
    public boolean processing() { return data.get(9) != 0; }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, extractor ? ModMachines.EXTRACTOR.get() : ModMachines.GENERATOR.get()); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (extractor && be.maarten.woolworks.item.ExtractorUpgradeItem.limit(stack) > 0) {
            if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
