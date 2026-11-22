package dev.daot.flashbackbridge.mixin;

import dev.daot.flashbackbridge.client.ReplayPauseController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freezes the up-orbit body tilt and avoids partial-tick interpolation while paused. */
@Pseudo
@Mixin(targets = "daot.ODMUpOrbitTilt", remap = false)
public abstract class ODMUpOrbitTiltMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void daotFlashbackBridge$freezeUpOrbitTick(MinecraftClient client, CallbackInfo ci) {
        if (ReplayPauseController.shouldFreezeAnimations()) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "getRenderRotation",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private static float daotFlashbackBridge$holdUpOrbitAtCurrentPose(float tickDelta) {
        return ReplayPauseController.shouldFreezeAnimations() ? 1.0F : tickDelta;
    }
}
