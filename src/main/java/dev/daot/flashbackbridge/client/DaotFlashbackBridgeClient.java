package dev.daot.flashbackbridge.client;

import net.fabricmc.api.ClientModInitializer;

public final class DaotFlashbackBridgeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ReplayPauseController.register();
    }
}
