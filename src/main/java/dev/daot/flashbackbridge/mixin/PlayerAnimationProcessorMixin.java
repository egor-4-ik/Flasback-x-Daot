package dev.daot.flashbackbridge.mixin;

import dev.daot.flashbackbridge.client.ReplayPauseController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops PAL's animation clock while Flashback holds a paused replay frame. */
@Pseudo
@Mixin(targets = "com.zigythebird.playeranim.animation.PlayerAnimationProcessor", remap = false)
public abstract class PlayerAnimationProcessorMixin {
    @Inject(method = "handleAnimations", at = @At("HEAD"), cancellable = true, remap = false)
    private void daotFlashbackBridge$freezePalAnimations(
            float tickDelta,
            boolean tick,
            CallbackInfo ci
    ) {
        if (ReplayPauseController.shouldFreezeAnimations()) {
            ci.cancel();
        }
    }
}
