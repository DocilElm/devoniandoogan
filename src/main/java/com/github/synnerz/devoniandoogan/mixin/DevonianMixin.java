package com.github.synnerz.devoniandoogan.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devoniandoogan.DevonianDoogan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Devonian.class)
public class DevonianMixin {
	@Inject(method = "onInitializeClient", at = @At("HEAD"), remap = false)
	private void devonianDoogan$preinit(CallbackInfo ci) {
		DevonianDoogan.INSTANCE.preInit();
	}
}