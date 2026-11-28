package be.maarten.woolworks.resource;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import be.maarten.woolworks.machine.MachineConfig;

/** Stable categories shared by built-in and datapack resources. */
public enum ResourceCategory implements StringRepresentable {
    METALS("metals"), ALLOYS("alloys"), GEMS_CRYSTALS("gems_crystals"),
    MATERIALS("materials"), MOB_DROPS("mob_drops"), ESSENCES("essences");

    public static final Codec<ResourceCategory> CODEC = StringRepresentable.fromEnum(ResourceCategory::values);
    private final String id;
    ResourceCategory(String id) { this.id = id; }
    @Override public String getSerializedName() { return id; }
    public boolean enabled() {
        return switch (this) {
            case METALS -> MachineConfig.ENABLE_METALS.get();
            case ALLOYS -> MachineConfig.ENABLE_ALLOYS.get();
            case GEMS_CRYSTALS -> MachineConfig.ENABLE_GEMS_CRYSTALS.get();
            case MATERIALS -> MachineConfig.ENABLE_MATERIALS.get();
            case MOB_DROPS -> MachineConfig.ENABLE_MOB_DROPS.get();
            case ESSENCES -> MachineConfig.ENABLE_ESSENCES.get();
        };
    }
}
