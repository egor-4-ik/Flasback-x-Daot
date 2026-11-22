package dev.daot.flashbackbridge;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DaotFlashbackBridge implements ModInitializer {
    public static final String MOD_ID = "daot_flashback_bridge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Danny's AOT <-> Flashback bridge initialized.");
    }
}
