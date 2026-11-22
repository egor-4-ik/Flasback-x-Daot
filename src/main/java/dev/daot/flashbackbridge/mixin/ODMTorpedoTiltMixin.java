package dev.daot.flashbackbridge.mixin;

import dev.daot.flashbackbridge.client.ReplayPauseController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freezes DAOT's torpedo tilt and holds the final interpolated orientation. */
@Pseudo
@Mixin(targets = "daot.ODMTorpedoTilt", remap = false)
public abstract class ODMTorpedoTiltMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void daotFlashbackBridge$freezeTorpedoTiltTick(MinecraftClient client, CallbackInfo ci) {
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
    private static float daotFlashbackBridge$holdTorpedoAtCurrentPose(float tickDelta) {
        return ReplayPauseController.shouldFreezeAnimations() ? 1.0F : tickDelta;
    }
}
