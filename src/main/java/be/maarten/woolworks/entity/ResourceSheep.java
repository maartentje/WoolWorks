package be.maarten.woolworks.entity;

import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.resource.ResourceType;
import be.maarten.woolworks.resource.ResourceTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.Optional;

public class ResourceSheep extends Sheep {
    private static final EntityDataAccessor<String> RESOURCE = SynchedEntityData.defineId(ResourceSheep.class, EntityDataSerializers.STRING);

    public ResourceSheep(EntityType<? extends Sheep> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RESOURCE, "");
    }

    public Identifier resourceId() { return Identifier.tryParse(entityData.get(RESOURCE)); }
    public void setResourceId(Identifier id) { entityData.set(RESOURCE, id.toString()); }
    public Optional<ResourceType> resourceType() {
        Identifier id = resourceId();
        return id == null ? Optional.empty() : level().registryAccess().lookup(ResourceTypes.REGISTRY).map(reg -> reg.getValue(id));
    }

    @Override protected void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("ResourceType", entityData.get(RESOURCE));
    }

    @Override protected void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        tag.getString("ResourceType").ifPresent(value -> entityData.set(RESOURCE, value));
    }

    @Override protected Component getTypeName() {
        return resourceType().<Component>map(type -> Component.translatable("entity.woolworks.resource_sheep.named", Component.translatable(type.translationKey())))
                .orElseGet(() -> Component.translatable("entity.woolworks.resource_sheep"));
    }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public boolean canMate(Animal other) { return false; }
    @Override public boolean readyForShearing() { return super.readyForShearing() && ResourceTypes.enabled(level(), resourceId()); }

    // DyeItem calls setColor directly. Rendering also ignores the vanilla color.
    @Override public void setColor(DyeColor color) { }

    @Override public void shear(ServerLevel level, SoundSource source, ItemStack tool) {
        if (!readyForShearing()) return;
        level.playSound(null, this, SoundEvents.SHEEP_SHEAR, source, 1.0F, 1.0F);
        setSheared(true);
        Identifier id = resourceId();
        resourceType().ifPresent(type -> {
            int count = type.minYield() + random.nextInt(type.maxYield() - type.minYield() + 1);
            ItemStack stack = ModItems.withResource(ModItems.INFUSED_WOOL.get(), id);
            stack.setCount(count);
            spawnAtLocation(level, stack, 1.0F);
        });
    }
}
