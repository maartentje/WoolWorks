package be.maarten.woolworks.mixin;

import be.maarten.woolworks.machine.SheepGrassEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EatBlockGoal.class)
public abstract class EatBlockGoalMixin {
    @Shadow @Final private Mob mob;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void woolworks$grassBlockEffect(Level level, int type, BlockPos pos, int data) {
        level.levelEvent(type, pos, SheepGrassEffects.shouldMute(level, mob) ? SheepGrassEffects.mark(data) : data);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean woolworks$shortGrassEffect(Level level, BlockPos pos, boolean drop) {
        if (!SheepGrassEffects.shouldMute(level, mob)) return level.destroyBlock(pos, drop);
        return SheepGrassEffects.destroySilently(() -> level.destroyBlock(pos, drop));
    }
}
