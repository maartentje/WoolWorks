package be.maarten.woolworks.advancement;

import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.resource.ResourceTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class InfusedWoolTrigger extends SimpleCriterionTrigger<InfusedWoolTrigger.Instance> {
    @Override public Codec<Instance> codec() { return Instance.CODEC; }

    public void inventoryChanged(ServerPlayer player, ItemStack stack) {
        var id = stack.get(ModComponents.RESOURCE_TYPE);
        if (stack.is(ModItems.INFUSED_WOOL.get()) && id != null
                && player.registryAccess().lookupOrThrow(ResourceTypes.REGISTRY).containsKey(id)) {
            trigger(player, instance -> true);
        }
    }

    public record Instance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player)
        ).apply(builder, Instance::new));
    }
}
