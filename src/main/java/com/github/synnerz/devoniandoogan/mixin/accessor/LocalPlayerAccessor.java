package com.github.synnerz.devoniandoogan.mixin.accessor;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {
    @Accessor("yRotLast")
    void setLastYawClient(float value);

    @Accessor("xRotLast")
    void setLastPitchClient(float value);
}
