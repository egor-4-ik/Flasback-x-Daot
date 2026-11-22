package dev.daot.flashbackbridge.mixin;

import dev.daot.flashbackbridge.client.ReplayPauseController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Holds remote ODM cable extension/retraction progress on a paused replay frame. */
@Pseudo
@Mixin(targets = "daot.RemoteHookTracker$RemoteHookData", remap = false)
public abstract class RemoteHookDataMixin {
    @Redirect(
            method = {"getExtensionProgress", "getRetractionProgress"},
            at = @At(value = "INVOKE", target = "Ljava/lang/System;currentTimeMillis()J"),
            remap = false
    )
    private static long daotFlashbackBridge$freezeRemoteCableProgressClock() {
        return ReplayPauseController.animationClockMillis();
    }
}
