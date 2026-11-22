package dev.daot.flashbackbridge.mixin;

import dev.daot.flashbackbridge.client.ReplayPauseController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops DAOT's per-tick body-yaw correction while the replay frame is frozen. */
@Pseudo
@Mixin(targets = "daot.ODMAnimationHandler", remap = false)
public abstract class ODMAnimationHandlerMixin {
    @Inject(method = "tickDoubleOrbitFacing", at = @At("HEAD"), cancellable = true, remap = false)
    private static void daotFlashbackBridge$freezeDoubleOrbitFacing(MinecraftClient client, CallbackInfo ci) {
        if (ReplayPauseController.shouldFreezeAnimations()) {
            ci.cancel();
        }
    }
}
