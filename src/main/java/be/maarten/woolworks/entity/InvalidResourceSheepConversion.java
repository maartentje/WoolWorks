package be.maarten.woolworks.entity;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.machine.MachineConfig;
import be.maarten.woolworks.resource.ResourceTypes;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Delay entity replacement until after chunk loading and authoritative server initialization. */
@EventBusSubscriber(modid = WoolWorks.MODID)
public final class InvalidResourceSheepConversion {
    private static final Map<MinecraftServer, List<ResourceSheep>> PENDING = new IdentityHashMap<>();
    private static final java.util.Set<MinecraftServer> READY = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private InvalidResourceSheepConversion() {}

    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ResourceSheep sheep)
            PENDING.computeIfAbsent(level.getServer(), ignored -> new ArrayList<>()).add(sheep);
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) { READY.add(event.getServer()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { READY.remove(event.getServer()); PENDING.remove(event.getServer()); }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var pending = PENDING.get(server);
        if (pending == null || pending.isEmpty() || !READY.contains(server) || !MachineConfig.SPEC.isLoaded()) return;
        PENDING.remove(server);
        for (var sheep : pending) {
            if (sheep.isRemoved() || !sheep.isAddedToLevel() || !(sheep.level() instanceof ServerLevel level)) continue;
            if (level.registryAccess().lookup(ResourceTypes.REGISTRY).isEmpty()) {
                PENDING.computeIfAbsent(server, ignored -> new ArrayList<>()).add(sheep);
            } else if (!ResourceTypes.enabled(level, sheep.resourceId())) {
                convert(level, sheep);
            }
        }
    }

    private static void convert(ServerLevel level, ResourceSheep source) {
        var sheep = EntityType.SHEEP.create(level, EntitySpawnReason.CONVERSION);
        if (sheep == null) return;
        sheep.copyPosition(source);
        sheep.setDeltaMovement(source.getDeltaMovement());
        sheep.setYHeadRot(source.getYHeadRot());
        sheep.setAge(source.getAge());
        sheep.setColor(DyeColor.WHITE);
        sheep.setSheared(source.isSheared());
        sheep.setHealth(Math.min(source.getHealth(), sheep.getMaxHealth()));
        sheep.setCustomName(source.getCustomName());
        sheep.setCustomNameVisible(source.isCustomNameVisible());
        sheep.setNoAi(source.isNoAi());
        sheep.setSilent(source.isSilent());
        sheep.setNoGravity(source.isNoGravity());
        sheep.setInvulnerable(source.isInvulnerable());
        sheep.setGlowingTag(source.hasGlowingTag());
        sheep.setInvisible(source.isInvisible());
        if (source.isPersistenceRequired()) sheep.setPersistenceRequired();
        for (var effect : source.getActiveEffects()) sheep.addEffect(new MobEffectInstance(effect));
        if (!level.addFreshEntity(sheep)) return;
        var holder = source.getLeashHolder();
        if (holder != null) sheep.setLeashedTo(holder, true);
        else sheep.setLeashData(source.getLeashData());
        source.setLeashData(null);
        if (source.isPassenger()) {
            var vehicle = source.getVehicle();
            source.stopRiding();
            sheep.startRiding(vehicle, true, true);
        }
        source.discard();
    }
}
