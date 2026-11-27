package be.maarten.woolworks.advancement;

import be.maarten.woolworks.WoolWorks;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCriteria {
    public static final DeferredRegister<CriterionTrigger<?>> TYPES = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES, WoolWorks.MODID);
    public static final java.util.function.Supplier<InfusedWoolTrigger> INFUSED_WOOL = TYPES.register("infused_wool", InfusedWoolTrigger::new);
    private ModCriteria() {}
}
