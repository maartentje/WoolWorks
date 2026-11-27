package be.maarten.woolworks.mixin;

import be.maarten.woolworks.machine.SheepGrassEffects;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Level.class)
public abstract class LevelDestroyEffectMixin {
    @ModifyArg(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"), index = 2)
    private int woolworks$markSheepGrassEffect(int data) { return SheepGrassEffects.markDuringDestroy(data); }
}
