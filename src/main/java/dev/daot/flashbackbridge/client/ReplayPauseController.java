package dev.daot.flashbackbridge.client;

import dev.daot.flashbackbridge.DaotFlashbackBridge;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Detects a stable paused Flashback replay frame once per client tick.
 *
 * The bridge deliberately uses reflection for Flashback's optional API so the
 * project does not need to bundle the very large Flashback jar as a compile
 * dependency. The names below were checked against Flashback 0.39.10.
 */
public final class ReplayPauseController {
    private static volatile boolean freezeAnimations;
    private static volatile boolean replayPaused;
    private static volatile long frozenWallClockMillis = Long.MIN_VALUE;

    private static volatile FlashbackAccess access;
    private static boolean accessLookupAttempted;
    private static boolean accessWarningLogged;

    private static Object previousReplayServer;
    private static long previousVisualMillis = Long.MIN_VALUE;
    private static boolean previousTickWasPaused;
    private static boolean previousTickWasStable;

    private ReplayPauseController() {
    }

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(ReplayPauseController::onStartClientTick);
        DaotFlashbackBridge.LOGGER.info("Flashback pause animation freeze registered.");
    }

    /** Read exactly once per client tick so all PAL/DAOT hooks share one state. */
    private static void onStartClientTick(MinecraftClient client) {
        if (!FabricLoader.getInstance().isModLoaded("flashback")) {
            reset();
            return;
        }

        try {
            FlashbackAccess api = getAccess();
            if (api == null) {
                reset();
                return;
            }

            Object replayServer = api.getReplayServer.invoke(null);
            if (replayServer == null) {
                reset();
                return;
            }

            boolean paused = api.replayPaused.getBoolean(replayServer);
            boolean fastForwarding = api.fastForwarding.getBoolean(replayServer);
            boolean canRenderStableFrame = (boolean) api.doClientRendering.invoke(replayServer);
            boolean exporting = (boolean) api.isExporting.invoke(null);
            long visualMillis = (long) api.getVisualMillis.invoke(null);

            replayPaused = paused && !exporting;
            boolean stablePausedFrame = paused && !fastForwarding && canRenderStableFrame && !exporting;

            if (!paused || exporting) {
                // While playing/exporting, animation updates must run normally.
                freezeAnimations = false;
                frozenWallClockMillis = Long.MIN_VALUE;
                previousReplayServer = replayServer;
                previousVisualMillis = visualMillis;
                previousTickWasPaused = false;
                previousTickWasStable = false;
                return;
            }

            if (!stablePausedFrame) {
                frozenWallClockMillis = Long.MIN_VALUE;
                // Flashback is seeking, applying a snapshot, or fast-forwarding.
                // Do not freeze this tick; it must be able to build the new pose.
                freezeAnimations = false;
                previousReplayServer = replayServer;
                previousVisualMillis = visualMillis;
                previousTickWasPaused = true;
                previousTickWasStable = false;
                return;
            }

            if (replayServer != previousReplayServer) {
                // First stable tick of a newly opened replay: allow PAL/DAOT to
                // consume the already-loaded initial state once.
                freezeAnimations = false;
            } else if (previousTickWasPaused && !previousTickWasStable) {
                // First stable tick after Flashback finished a seek/snapshot.
                freezeAnimations = false;
            } else if (visualMillis != previousVisualMillis) {
                // The editor moved to a different replay frame while paused.
                freezeAnimations = false;
            } else {
                // Same paused replay frame: keep its last evaluated pose intact.
                if (!freezeAnimations || frozenWallClockMillis == Long.MIN_VALUE) {
                    frozenWallClockMillis = System.currentTimeMillis();
                }
                freezeAnimations = true;
            }

            previousReplayServer = replayServer;
            previousVisualMillis = visualMillis;
            previousTickWasPaused = true;
            previousTickWasStable = true;
        } catch (ReflectiveOperationException | ClassCastException | LinkageError exception) {
            freezeAnimations = false;
            replayPaused = false;
            frozenWallClockMillis = Long.MIN_VALUE;
            if (!accessWarningLogged) {
                accessWarningLogged = true;
                DaotFlashbackBridge.LOGGER.warn(
                        "Could not read Flashback playback state; animation-freeze integration is disabled. " +
                                "This can happen if the Flashback API changed.",
                        exception
                );
            }
        }
    }

    private static FlashbackAccess getAccess() throws ReflectiveOperationException {
        FlashbackAccess current = access;
        if (current != null) {
            return current;
        }

        synchronized (ReplayPauseController.class) {
            if (access != null) {
                return access;
            }
            if (accessLookupAttempted) {
                return null;
            }
            accessLookupAttempted = true;

            ClassLoader loader = ReplayPauseController.class.getClassLoader();
            Class<?> flashbackClass = Class.forName("com.moulberry.flashback.Flashback", false, loader);
            Class<?> replayServerClass = Class.forName(
                    "com.moulberry.flashback.playback.ReplayServer", false, loader
            );

            Method getReplayServer = flashbackClass.getMethod("getReplayServer");
            Method isExporting = flashbackClass.getMethod("isExporting");
            Method getVisualMillis = flashbackClass.getMethod("getVisualMillis");
            Method doClientRendering = replayServerClass.getMethod("doClientRendering");

            Field replayPaused = replayServerClass.getField("replayPaused");
            Field fastForwarding = replayServerClass.getField("fastForwarding");

            access = new FlashbackAccess(
                    getReplayServer,
                    isExporting,
                    getVisualMillis,
                    doClientRendering,
                    replayPaused,
                    fastForwarding
            );
            return access;
        }
    }

    private static void reset() {
        freezeAnimations = false;
        replayPaused = false;
        frozenWallClockMillis = Long.MIN_VALUE;
        previousReplayServer = null;
        previousVisualMillis = Long.MIN_VALUE;
        previousTickWasPaused = false;
        previousTickWasStable = false;
    }

    public static boolean shouldFreezeAnimations() {
        return freezeAnimations;
    }

    /** True while Flashback is paused, including ticks where it is applying a seek. */
    public static boolean isReplayPaused() {
        return replayPaused;
    }

    /** A stable wall-clock value for DAOT timers while the replay frame is frozen. */
    public static long animationClockMillis() {
        long frozen = frozenWallClockMillis;
        return freezeAnimations && frozen != Long.MIN_VALUE ? frozen : System.currentTimeMillis();
    }

    private record FlashbackAccess(
            Method getReplayServer,
            Method isExporting,
            Method getVisualMillis,
            Method doClientRendering,
            Field replayPaused,
            Field fastForwarding
    ) {
    }
}
