package be.maarten.woolworks.item;

import be.maarten.woolworks.entity.ResourceSheep;
import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.registry.ModEntities;
import be.maarten.woolworks.resource.ResourceTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ResourceFeedItem extends ResourceItem {
    public ResourceFeedItem(Properties properties) { super(properties); }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Sheep sheep) || sheep.getType() != EntityType.SHEEP || sheep.isBaby() || !sheep.isAlive()) return InteractionResult.PASS;
        var id = stack.get(ModComponents.RESOURCE_TYPE);
        if (!ResourceTypes.enabled(sheep.level(), id)) return InteractionResult.PASS;
        if (!(sheep.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        ResourceSheep replacement = ModEntities.RESOURCE_SHEEP.get().create(level, EntitySpawnReason.CONVERSION);
        if (replacement == null) return InteractionResult.FAIL;
        replacement.snapTo(sheep.getX(), sheep.getY(), sheep.getZ(), sheep.getYRot(), sheep.getXRot());
        replacement.setResourceId(id);
        replacement.setSheared(sheep.isSheared());
        replacement.setCustomName(sheep.getCustomName());
        replacement.setCustomNameVisible(sheep.isCustomNameVisible());
        replacement.setHealth(Math.min(sheep.getHealth(), replacement.getMaxHealth()));
        replacement.setNoAi(sheep.isNoAi());
        replacement.setSilent(sheep.isSilent());
        replacement.setPersistenceRequired();
        if (!level.addFreshEntity(replacement)) return InteractionResult.FAIL;
        sheep.discard();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, replacement, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, replacement.getX(), replacement.getY() + 0.8, replacement.getZ(), 15, 0.4, 0.4, 0.4, 0.0);
        return InteractionResult.SUCCESS;
    }
}
