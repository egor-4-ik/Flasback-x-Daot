package dev.daot.flashbackbridge.mixin;

import daot.HookPoint;
import dev.daot.flashbackbridge.client.ReplayPauseController;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;
import daot.PrototypeOdmArms;

/**
 * Stops DAOT's render-frame smoothing from continuously changing the cable bend
 * while Flashback is holding one paused replay frame.
 */
@Pseudo
@Mixin(targets = "daot.HookLineRenderer", remap = false)
public abstract class HookLineRendererMixin {
    @Shadow private static Vec3d smoothedLeftBend;
    @Shadow private static Vec3d smoothedRightBend;

    @Unique private static boolean daotFlashbackBridge$leftBendCaptured;
    @Unique private static boolean daotFlashbackBridge$rightBendCaptured;
    @Unique private static boolean daotFlashbackBridge$tickDeltaCaptured;
    @Unique private static float daotFlashbackBridge$frozenTickDelta;
    @Inject(method = "renderHookLines", at = @At("HEAD"), remap = false)
    private static void daotFlashbackBridge$resetBendCaptureWhenPlaying(CallbackInfo ci) {
        if (!ReplayPauseController.shouldFreezeAnimations()) {
            daotFlashbackBridge$leftBendCaptured = false;
            daotFlashbackBridge$rightBendCaptured = false;
            daotFlashbackBridge$tickDeltaCaptured = false;
        }
    }


    /**
     * DAOT uses the render tick delta to interpolate the player's position and
     * ODM grip anchors. Flashback can keep prev/current entity positions frozen
     * while render tick delta continues changing, making a paused rope appear
     * to attach to several positions. Hold the first delta of the stable frame.
     */
    @Redirect(
            method = "renderHookLines",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderTickCounter;getTickDelta(Z)F"
            )
    )
    private static float daotFlashbackBridge$freezeRenderTickDelta(
            RenderTickCounter tickCounter, boolean ignoreFreeze
    ) {
        if (ReplayPauseController.shouldFreezeAnimations()) {
            return 1.0f;
        }

        return tickCounter.getTickDelta(ignoreFreeze);
    }

    /** Keep DAOT's 500 ms stale-hook timeout from expiring remote cables on pause. */
    @Redirect(
            method = "renderRemotePlayerHooks",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/System;currentTimeMillis()J"
            ),
            remap = false
    )
    private static long daotFlashbackBridge$freezeRemoteHookTimeoutClock() {
        return ReplayPauseController.animationClockMillis();
    }

    @Invoker("smoothBend")
    private static Vec3d daotFlashbackBridge$invokeSmoothBend(Vec3d previous, HookPoint hook) {
        throw new AssertionError();
    }

    @Redirect(
            method = "renderHookLines",
            at = @At(
                    value = "INVOKE",
                    target = "Ldaot/HookLineRenderer;smoothBend(Lnet/minecraft/util/math/Vec3d;Ldaot/HookPoint;)Lnet/minecraft/util/math/Vec3d;",
                    ordinal = 0
            ),
            remap = false
    )
    private static Vec3d daotFlashbackBridge$holdLeftBend(Vec3d previous, HookPoint hook) {
        if (!ReplayPauseController.shouldFreezeAnimations()) {
            daotFlashbackBridge$leftBendCaptured = false;
            return daotFlashbackBridge$invokeSmoothBend(previous, hook);
        }

        if (!daotFlashbackBridge$leftBendCaptured) {
            daotFlashbackBridge$leftBendCaptured = true;
            return daotFlashbackBridge$invokeSmoothBend(previous, hook);
        }
        return smoothedLeftBend;
    }

    @Redirect(
            method = "renderHookLines",
            at = @At(
                    value = "INVOKE",
                    target = "Ldaot/HookLineRenderer;smoothBend(Lnet/minecraft/util/math/Vec3d;Ldaot/HookPoint;)Lnet/minecraft/util/math/Vec3d;",
                    ordinal = 1
            ),
            remap = false
    )
    private static Vec3d daotFlashbackBridge$holdRightBend(Vec3d previous, HookPoint hook) {
        if (!ReplayPauseController.shouldFreezeAnimations()) {
            daotFlashbackBridge$rightBendCaptured = false;
            return daotFlashbackBridge$invokeSmoothBend(previous, hook);
        }

        if (!daotFlashbackBridge$rightBendCaptured) {
            daotFlashbackBridge$rightBendCaptured = true;
            return daotFlashbackBridge$invokeSmoothBend(previous, hook);
        }
        return smoothedRightBend;
    }
}
