package dev.daot.flashbackbridge.mixin;

import dev.daot.flashbackbridge.client.ReplayPauseController;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps DAOT's wall-clock-based hook animation timers fixed while Flashback
 * displays a stable paused frame. On resume, the elapsed pause duration is
 * added to the timer origin so the hook continues instead of jumping forward.
 */
@Pseudo
@Mixin(targets = "daot.HookPoint", remap = false)
public abstract class HookPointMixin {
    @Shadow public boolean isExtending;
    @Shadow public boolean isRetracting;
    @Shadow public long shootTime;
    @Shadow public long retractTime;

    @Unique private boolean daotFlashbackBridge$extensionPaused;
    @Unique private long daotFlashbackBridge$extensionPauseStarted;
    @Unique private float daotFlashbackBridge$extensionProgress;

    @Unique private boolean daotFlashbackBridge$retractionPaused;
    @Unique private long daotFlashbackBridge$retractionPauseStarted;
    @Unique private float daotFlashbackBridge$retractionProgress;

    @Inject(method = "getExtensionProgress", at = @At("HEAD"), cancellable = true, remap = false)
    private void daotFlashbackBridge$freezeExtensionProgress(CallbackInfoReturnable<Float> cir) {
        long now = System.currentTimeMillis();

        if (ReplayPauseController.shouldFreezeAnimations()) {
            if (!daotFlashbackBridge$extensionPaused) {
                daotFlashbackBridge$extensionPaused = true;
                daotFlashbackBridge$extensionPauseStarted = now;
                daotFlashbackBridge$extensionProgress = isExtending
                        ? Math.max(0.0F, Math.min(1.0F, (now - shootTime) / 150.0F))
                        : 1.0F;
            }
            cir.setReturnValue(daotFlashbackBridge$extensionProgress);
            return;
        }

        if (daotFlashbackBridge$extensionPaused) {
            if (!ReplayPauseController.isReplayPaused()) {
                shootTime += now - daotFlashbackBridge$extensionPauseStarted;
            }
            daotFlashbackBridge$extensionPaused = false;
        }
    }

    @Inject(method = "getRetractionProgress", at = @At("HEAD"), cancellable = true, remap = false)
    private void daotFlashbackBridge$freezeRetractionProgress(CallbackInfoReturnable<Float> cir) {
        long now = System.currentTimeMillis();

        if (ReplayPauseController.shouldFreezeAnimations()) {
            if (!daotFlashbackBridge$retractionPaused) {
                daotFlashbackBridge$retractionPaused = true;
                daotFlashbackBridge$retractionPauseStarted = now;
                daotFlashbackBridge$retractionProgress = isRetracting
                        ? Math.max(0.0F, Math.min(1.0F, (now - retractTime) / 200.0F))
                        : 0.0F;
            }
            cir.setReturnValue(daotFlashbackBridge$retractionProgress);
            return;
        }

        if (daotFlashbackBridge$retractionPaused) {
            if (!ReplayPauseController.isReplayPaused()) {
                retractTime += now - daotFlashbackBridge$retractionPauseStarted;
            }
            daotFlashbackBridge$retractionPaused = false;
        }
    }
}
