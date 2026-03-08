package com.github.synnerz.devoniandoogan.mixin;

import com.github.synnerz.devoniandoogan.features.AvoidBreakingSecrets;
import com.github.synnerz.devoniandoogan.features.ZeroPingDB;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(
            method = "destroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void devonianDoogan$onPreBlockDestroy(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir, Level level, BlockState blockState, Block block) {
        AvoidBreakingSecrets.INSTANCE.setShouldAvoid(AvoidBreakingSecrets.INSTANCE.avoid(blockPos, blockState, block));
    }

    @WrapOperation(
            method = "destroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private boolean devonianDoogan$onBlockDestroy(Level instance, BlockPos blockPos, BlockState blockState, int i, Operation<Boolean> original) {
        if (AvoidBreakingSecrets.INSTANCE.getShouldAvoid()) return false;
        return original.call(instance, blockPos, blockState, i);
    }

    @Inject(
            method = "startDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/tutorial/Tutorial;onDestroyBlock(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;F)V"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void devonianDoogan$onBlockStartBreak(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir, BlockState blockState) {
        ZeroPingDB.INSTANCE.onBreak(blockPos, blockState, blockState.getBlock());
    }
}
