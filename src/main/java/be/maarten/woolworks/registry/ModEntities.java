package be.maarten.woolworks.registry;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.entity.ResourceSheep;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, WoolWorks.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<ResourceSheep>> RESOURCE_SHEEP = TYPES.register("resource_sheep",
            () -> EntityType.Builder.of(ResourceSheep::new, MobCategory.CREATURE).sized(0.9F, 1.3F)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(WoolWorks.MODID, "resource_sheep"))));

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(RESOURCE_SHEEP.get(), Sheep.createAttributes().build());
    }

    private ModEntities() {}
}
