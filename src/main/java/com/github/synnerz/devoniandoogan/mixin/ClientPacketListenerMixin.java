package com.github.synnerz.devoniandoogan.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devoniandoogan.features.NoRotate;
import com.github.synnerz.devoniandoogan.mixin.accessor.LocalPlayerAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Set;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique
    private static boolean wasSetValue = false;
    @Unique
    private static PositionMoveRotation lastRotation = null;
    @Unique
    private static boolean wasChanged = false;

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;setValuesFromPositionPacket(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;Lnet/minecraft/world/entity/Entity;Z)Z"))
    private void devonianDoogan$onSetValue(ClientboundPlayerPositionPacket clientboundPlayerPositionPacket, CallbackInfo ci) {
        if (!NoRotate.INSTANCE.canNoRotate()) return;
        wasSetValue = true;
    }

    @Inject(
            method = "setValuesFromPositionPacket",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void devonianDoogan$onNewRotation(PositionMoveRotation positionMoveRotation, Set<Relative> set, Entity entity, boolean bl, CallbackInfoReturnable<Boolean> cir, PositionMoveRotation positionMoveRotation2, PositionMoveRotation positionMoveRotation3) {
        if (!wasSetValue || !NoRotate.INSTANCE.canNoRotate()) return;
        lastRotation = positionMoveRotation3;
        wasSetValue = false;
    }

    @WrapOperation(method = "setValuesFromPositionPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setYRot(F)V"))
    private static void devonianDoogan$onSetRotY(Entity instance, float f, Operation<Void> original) {
        if (lastRotation == null || !NoRotate.INSTANCE.canNoRotate()) {
            original.call(instance, f);
        }
    }

    @WrapOperation(method = "setValuesFromPositionPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setXRot(F)V"))
    private static void devonianDoogan$onSetRotX(Entity instance, float f, Operation<Void> original) {
        if (lastRotation == null || !NoRotate.INSTANCE.canNoRotate()) {
            original.call(instance, f);
        }
    }

    @WrapOperation(method = "setValuesFromPositionPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setOldPosAndRot(Lnet/minecraft/world/phys/Vec3;FF)V"))
    private static void devonianDoogan$onSetOldRot(Entity instance, Vec3 vec3, float f, float g, Operation<Void> original) {
        if (lastRotation == null || !NoRotate.INSTANCE.canNoRotate()) {
            original.call(instance, vec3, f, g);
            return;
        }
        var player = Devonian.INSTANCE.getMinecraft().player;
        if (player == null) {
            lastRotation = null;
            return;
        }

        original.call(instance, vec3, player.yRotO, player.xRotO);
    }

    @WrapOperation(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V",
                    ordinal = 1
            )
    )
    private void devonianDoogan$onSendRot(Connection instance, Packet<?> packet, Operation<Void> original) {
        var player = Devonian.INSTANCE.getMinecraft().player;
        if (player == null || lastRotation == null || !NoRotate.INSTANCE.canNoRotate()) {
            original.call(instance, packet);
            return;
        }

        original.call(instance, new ServerboundMovePlayerPacket.PosRot(
                player.getX(),
                player.getY(),
                player.getZ(),
                lastRotation.yRot(),
                lastRotation.xRot(),
                false,
                false
        ));
        wasChanged = true;
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void devonianDoogan$onPostSet(ClientboundPlayerPositionPacket clientboundPlayerPositionPacket, CallbackInfo ci) {
        if (!wasChanged || lastRotation == null || !NoRotate.INSTANCE.canNoRotate()) return;
        var player = Devonian.INSTANCE.getMinecraft().player;
        if (player == null) return;

        LocalPlayerAccessor pl = (LocalPlayerAccessor) player;
        pl.setLastPitchClient(lastRotation.xRot());
        pl.setLastYawClient(lastRotation.yRot());
        wasChanged = false;
        lastRotation = null;
    }
}
