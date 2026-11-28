package be.maarten.woolworks.machine;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MachineConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue CABLE_TRANSFER_RATE;
    public static final ModConfigSpec.IntValue WORKER_INTERVAL, WORKER_REGROWTH_COST, WORKER_SHEAR_COST, WORKER_WHEAT_COST, WORKER_DURABILITY_COST;
    public static final ModConfigSpec.BooleanValue ENABLE_AUTOMATIC_MODDED_RESOURCES;
    public static final ModConfigSpec.BooleanValue ENABLE_METALS, ENABLE_ALLOYS, ENABLE_GEMS_CRYSTALS, ENABLE_MATERIALS, ENABLE_MOB_DROPS, ENABLE_ESSENCES;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DISABLED_RESOURCES;
    static {
        var builder = new ModConfigSpec.Builder();
        ENABLE_AUTOMATIC_MODDED_RESOURCES = builder.comment("Enable Wool Works' predefined tag-dependent optional resources.")
                .define("enableAutomaticModdedResources", true);
        ENABLE_METALS = builder.comment("Enable resources in the Metals category.").define("enableMetals", true);
        ENABLE_ALLOYS = builder.comment("Enable resources in the Alloys category.").define("enableAlloys", true);
        ENABLE_GEMS_CRYSTALS = builder.comment("Enable resources in the Gems & Crystals category.").define("enableGemsCrystals", true);
        ENABLE_MATERIALS = builder.comment("Enable resources in the Materials category.").define("enableMaterials", true);
        ENABLE_MOB_DROPS = builder.comment("Enable resources in the Mob Drops category.").define("enableMobDrops", true);
        ENABLE_ESSENCES = builder.comment("Enable resources in the Essences category.").define("enableEssences", true);
        DISABLED_RESOURCES = builder.comment("Resource IDs to disable, for example woolworks:osmium.")
                .defineListAllowEmpty("disabledResources", java.util.List.of(), value -> value instanceof String s && net.minecraft.resources.Identifier.tryParse(s) != null);
        CABLE_TRANSFER_RATE = builder.comment("Maximum FE transferred through each cable per server tick.")
                .defineInRange("cableTransferRate", 160, 1, Integer.MAX_VALUE);
        WORKER_INTERVAL = builder.defineInRange("woolWorkerOperationInterval", 200, 1, Integer.MAX_VALUE);
        WORKER_REGROWTH_COST = builder.defineInRange("woolWorkerRegrowthEnergyCost", 400, 0, Integer.MAX_VALUE);
        WORKER_SHEAR_COST = builder.defineInRange("woolWorkerShearingEnergyCost", 200, 0, Integer.MAX_VALUE);
        WORKER_WHEAT_COST = builder.defineInRange("woolWorkerWheatCost", 1, 1, 64);
        WORKER_DURABILITY_COST = builder.defineInRange("woolWorkerShearsDurabilityCost", 0, 0, Integer.MAX_VALUE);
        SPEC = builder.build();
    }
    private MachineConfig() {}
}
