package dev.daot.flashbackbridge.mixin;

import daot.network.BladeAnimSyncPayload;
import daot.network.ODMHookSyncPayload;
import daot.network.ODMHookUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Danny's AOT sends these state packets to players tracking the sender, but not
 * to the sender itself. Flashback records clientbound packets, so echo the
 * exact DAOT sync payloads back to the sender as well.
 */
@Mixin(targets = "daot.network.ModNetworking", remap = false)
public abstract class ModNetworkingMixin {
    @Inject(method = "handleODMHookUpdate", at = @At("TAIL"), remap = false)
    private static void daotFlashbackBridge$echoOdmHookState(
            ServerPlayerEntity player,
            ODMHookUpdatePayload update,
            CallbackInfo ci
    ) {
        ODMHookSyncPayload sync = new ODMHookSyncPayload(
                player.getId(),
                update.leftActive(),
                update.leftExtending(),
                update.leftRetracting(),
                update.leftX(),
                update.leftY(),
                update.leftZ(),
                update.leftStartX(),
                update.leftStartY(),
                update.leftStartZ(),
                update.rightActive(),
                update.rightExtending(),
                update.rightRetracting(),
                update.rightX(),
                update.rightY(),
                update.rightZ(),
                update.rightStartX(),
                update.rightStartY(),
                update.rightStartZ(),
                update.isBoosting(),
                update.ropeTilt(),
                update.orbitDir()
        );

        ServerPlayNetworking.send(player, sync);
    }

    @Inject(method = "broadcastBladeAnim", at = @At("TAIL"), remap = false)
    private static void daotFlashbackBridge$echoBladeAnimation(
            ServerPlayerEntity player,
            int action,
            CallbackInfo ci
    ) {
        ServerPlayNetworking.send(player, new BladeAnimSyncPayload(player.getId(), action));
    }
}
