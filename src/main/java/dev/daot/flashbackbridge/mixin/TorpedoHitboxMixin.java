package dev.daot.flashbackbridge.mixin;

import daot.network.TorpedoStateSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Echoes torpedo-mode state to the player whose local state changed. */
@Mixin(targets = "daot.TorpedoHitbox", remap = false)
public abstract class TorpedoHitboxMixin {
    @Inject(method = "set", at = @At("TAIL"), remap = false)
    private static void daotFlashbackBridge$echoTorpedoState(
            PlayerEntity player,
            boolean small,
            CallbackInfo ci
    ) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(
                    serverPlayer,
                    new TorpedoStateSyncPayload(serverPlayer.getId(), small)
            );
        }
    }
}
