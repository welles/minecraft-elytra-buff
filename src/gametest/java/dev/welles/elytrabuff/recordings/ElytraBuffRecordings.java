package dev.welles.elytrabuff.recordings;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.Heightmap;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Plays scenes of the mod and saves every tick as a frame in {@code recordings/<scene>/} of the run directory, for
 * {@code record-gifs.sh} to turn into GIFs. Not a test: it checks nothing and only runs on request. Every scene is seen
 * from behind in third person, where the elytra is.
 */
public class ElytraBuffRecordings implements FabricClientGameTest {
    private static final int WIDTH = 854;
    private static final int HEIGHT = 480;
    /** A world with hills, trees and a lake shows the movement, the seed keeps it the same in every recording. */
    private static final String SEED = "elytra";
    /** Open plains of the seed, looking south over hills to a lake; found with pictures around the closest plains. */
    private static final int START_X = -21;
    private static final int START_Z = -993;
    /** Ticks to load and render the landscape after a teleport; waiting for all chunks to render never ends. */
    private static final int LOAD_TICKS = 200;
    /** The cold start charges for 3 seconds by default. */
    private static final int CHARGE_TICKS = 60;

    @Override
    public void runTest(ClientGameTestContext context) {
        // before the world starts, so the integrated server sends that far too: the landscape shows the movement
        context.runOnClient(minecraft -> minecraft.options.renderDistance().set(8));
        try (TestSingleplayerContext singleplayer = context.worldBuilder().setUseConsistentSettings(false)
            .adjustSettings(creator -> {
                creator.setSeed(SEED);
                creator.setDifficulty(Difficulty.PEACEFUL);
                creator.setAllowCommands(true);
            }).create()) {
            TestServerContext server = singleplayer.getServer();
            server.runCommand("gamemode survival @a");
            server.runCommand("time set noon");
            server.runCommand("weather clear 100000");
            server.runCommand("item replace entity @a armor.chest with minecraft:elytra");
            // no fall damage, so a landing never ends a scene; hides the effect particles
            server.runCommand("effect give @a minecraft:resistance infinite 255 true");
            context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK));

            coldStart(context, server);
            wingFlaps(context, server);
        }
    }

    /** The height a player stands at on the ground at x, z, generating the chunk if needed. */
    private static int surface(TestServerContext server, int x, int z) {
        return server.computeOnServer(minecraftServer -> {
            var level = minecraftServer.overworld();
            level.getChunk(x >> 4, z >> 4);
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        });
    }

    /**
     * Crouches on the ground until the cold start is charged: the wings rise, the air stirs and feathers drift off.
     * Releasing crouch beats the wings down and throws the player up, where it opens the elytra and glides away.
     */
    private static void coldStart(ClientGameTestContext context, TestServerContext server) {
        int y = surface(server, START_X, START_Z);
        // the camera stands a few blocks behind the player, grass there would fill the whole view
        for (String plant : new String[] {"short_grass", "tall_grass", "fern", "large_fern"}) {
            server.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air replace minecraft:%s",
                START_X - 4, y, START_Z - 8, START_X + 4, y + 1, START_Z + 2, plant));
        }
        server.runCommand(String.format(Locale.ROOT, "tp @a %d.5 %d %d.5 0 0", START_X, y, START_Z));
        context.waitTicks(LOAD_TICKS);

        // already crouching when the recording starts: standing, the camera behind the player gets stuck at the slope
        look(context, 0, 15);
        context.getInput().holdKey(options -> options.keyShift);
        // the player takes a few ticks to crouch
        context.waitTicks(3);
        Recorder recorder = new Recorder(context, "cold-start");
        // charged, then a moment with the feathers drifting off
        recorder.frames(CHARGE_TICKS + 15);
        context.getInput().releaseKey(options -> options.keyShift);
        // up to the top of the throw, where the player stops rising
        for (int i = 0; i < 40; i++) {
            recorder.frame();
            if (i >= 3 && context.computeOnClient(minecraft -> minecraft.player.getDeltaMovement().y <= 0)) break;
        }
        openElytra(context);
        look(context, 0, 20);
        recorder.frames(35);
    }

    /** Glides a moment, then crouches to flap the wings until they tire, and glides on faster. */
    private static void wingFlaps(ClientGameTestContext context, TestServerContext server) {
        // high above the plains, north of the start, flying south over the same landscape; spectators do not fall
        // while it loads
        int z = START_Z - 40;
        server.runCommand("gamemode spectator @a");
        server.runCommand(String.format(Locale.ROOT, "tp @a %d.5 %d %d.5 0 0", START_X, surface(server, START_X, z) + 20, z));
        context.waitTicks(LOAD_TICKS);
        server.runCommand("gamemode survival @a");
        // falling a few ticks, then jumping opens the elytra
        context.waitTicks(4);
        openElytra(context);
        look(context, 0, 10);
        context.waitTicks(20);

        Recorder recorder = new Recorder(context, "wing-flaps");
        recorder.frames(15);
        context.getInput().holdKey(options -> options.keyShift);
        // the wings tire after 3 seconds by default
        recorder.frames(60);
        context.getInput().releaseKey(options -> options.keyShift);
        recorder.frames(20);
    }

    /**
     * Jumps in the air, which opens the elytra. The client only starts gliding when it sees the jump key go down between
     * two ticks, so the key is held for a tick.
     */
    private static void openElytra(ClientGameTestContext context) {
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTick();
        context.getInput().releaseKey(options -> options.keyJump);
    }

    /** Turns the player, without the interpolation from the last rotation. */
    private static void look(ClientGameTestContext context, float yaw, float pitch) {
        context.runOnClient(minecraft -> {
            var player = minecraft.player;
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.yRotO = yaw;
            player.xRotO = pitch;
        });
    }

    /** Saves numbered frames of one scene; taking a screenshot advances the game by one tick. */
    private static final class Recorder {
        private final ClientGameTestContext context;
        private final Path dir;
        private int frame;

        Recorder(ClientGameTestContext context, String scene) {
            this.context = context;
            this.dir = FabricLoader.getInstance().getGameDir().resolve("recordings").resolve(scene);
        }

        void frame() {
            // toasts and chat messages would cover the scene
            context.runOnClient(minecraft -> {
                minecraft.getToastManager().clear();
                minecraft.gui.getChat().clearMessages(false);
            });
            context.takeScreenshot(TestScreenshotOptions.of(String.format(Locale.ROOT, "%04d", frame++))
                .disableCounterPrefix().withSize(WIDTH, HEIGHT).withDestinationDir(dir));
        }

        void frames(int count) {
            for (int i = 0; i < count; i++) frame();
        }
    }
}
