package be.maarten.woolworks.machine;

import net.neoforged.neoforge.energy.EnergyStorage;

/** Legacy energy backing until the separate energy-transfer migration. */
@SuppressWarnings("removal")
class MachineEnergy extends EnergyStorage {
    MachineEnergy(int capacity, int receive, int extract) { super(capacity, receive, extract); }
    void setStored(int value) { energy = Math.clamp(value, 0, capacity); }
}
