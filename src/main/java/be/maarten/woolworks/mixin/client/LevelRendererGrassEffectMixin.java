package be.maarten.woolworks.mixin.client;

import be.maarten.woolworks.machine.SheepGrassEffects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelEventHandler.class)
public abstract class LevelRendererGrassEffectMixin {
    @Shadow @org.spongepowered.asm.mixin.Final private ClientLevel level;

    @Inject(method = "levelEvent", at = @At("HEAD"), cancellable = true)
    private void woolworks$silentGrassParticles(int type, BlockPos pos, int data, CallbackInfo callback) {
        if (type != 2001 || (data & SheepGrassEffects.SILENT_EFFECT) == 0) return;
        var state = Block.stateById(data & Integer.MAX_VALUE);
        if (!state.is(Blocks.GRASS_BLOCK) && !state.is(Blocks.SHORT_GRASS)) return;
        level.addDestroyBlockEffect(pos, state);
        callback.cancel();
    }
}
