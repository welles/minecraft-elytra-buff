package dev.welles.elytrabuff;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Settings stored in {@code config/elytrabuff.properties}.
 * The file is re-read automatically whenever it changes on disk.
 */
public final class BuffConfig {
    public static final double MAX_CHARGE_TIME = 30.0;
    public static final double MAX_LAUNCH_HEIGHT = 64.0;
    public static final double MAX_ENERGY_BONUS = 2.0;
    public static final double MAX_FLAP_STRENGTH = 0.5;
    public static final double MAX_FLAP_TIME = 30.0;
    public static final double MAX_FLAP_COOLDOWN = 60.0;

    public boolean coldStart = true;
    public double chargeTime = 3.0;
    public double launchHeight = 10.0;
    public double energyBonus = 0.2;
    public boolean wingFlaps = true;
    public double flapStrength = 0.04;
    public double flapTime = 3.0;
    public double flapCooldown = 5.0;

    private static final String HEADER = """
        Elytra Buff configuration
        Changes are picked up automatically, no restart required.
        All settings are decided by the server (in multiplayer the server's values apply).

        coldStart     true = crouch on the ground with an elytra on to charge a cold start, release crouch to launch
        chargeTime    How long to crouch until the cold start is charged, in seconds. Smoke rises after 1 second,
                      flames and a hiss show that it is charged. 0.5 to 30.
        launchHeight  How high a cold start throws the player, in blocks. 1 to 64.
        energyBonus   Extra speed gained from losing height while flying, as a fraction of the energy that the descent
                      gives in vanilla. 0.2 = 20 % more, enough to keep flying forever by diving and pulling up again.
                      0 = vanilla. 0 to 2. Only applies to players that have the mod installed on their client.
        wingFlaps     true = crouch while flying to flap the wings and speed up, like with a weak firework rocket.
                      Only applies to players that have the mod installed on their client.
        flapStrength  How much speed each tick of flapping adds, in blocks per tick. Less the faster the player already
                      flies, flapping never gets faster than 1.5 blocks per tick (a firework rocket: about 1.7). 0 to 0.5.
        flapTime      How long the wings can flap at once, in seconds. 0.5 to 30.
        flapCooldown  How long tired out wings take to recover before they can flap again, in seconds. Releasing
                      crouch earlier lets them recover a part of that. 0 to 60.""";

    private static Path file;
    private static FileTime loadedAt;
    private static BuffConfig current = new BuffConfig();

    public static synchronized void init(Path configDir) {
        file = configDir.resolve(ElytraBuff.MOD_ID + ".properties");
        reload();
    }

    /** Returns the current settings, re-reading the file if it was modified. */
    public static synchronized BuffConfig get() {
        if (file != null) {
            try {
                FileTime modified = Files.exists(file) ? Files.getLastModifiedTime(file) : null;
                if (modified == null || !modified.equals(loadedAt)) reload();
            } catch (IOException e) {
                ElytraBuff.LOGGER.warn("Could not check config file {}", file, e);
            }
        }
        return current;
    }

    private static void reload() {
        BuffConfig config = new BuffConfig();
        Properties props = new Properties();
        boolean complete = false;

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
                complete = config.read(props);
            } catch (IOException | RuntimeException e) {
                ElytraBuff.LOGGER.error("Could not read config file {}, using defaults", file, e);
                current = config;
                return;
            }
        }

        if (!complete) {
            try {
                Files.createDirectories(file.getParent());
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    config.write(writer);
                }
            } catch (IOException e) {
                ElytraBuff.LOGGER.error("Could not write config file {}", file, e);
            }
        }

        try {
            loadedAt = Files.exists(file) ? Files.getLastModifiedTime(file) : null;
        } catch (IOException e) {
            loadedAt = null;
        }
        current = config;
    }

    /** Applies all valid values present in {@code props}; returns whether every key was present. */
    private boolean read(Properties props) {
        String value = props.getProperty("coldStart");
        if (value != null) coldStart = Boolean.parseBoolean(value.trim());
        chargeTime = readNumber(props, "chargeTime", chargeTime, 0.5, MAX_CHARGE_TIME);
        launchHeight = readNumber(props, "launchHeight", launchHeight, 1.0, MAX_LAUNCH_HEIGHT);
        energyBonus = readNumber(props, "energyBonus", energyBonus, 0.0, MAX_ENERGY_BONUS);
        value = props.getProperty("wingFlaps");
        if (value != null) wingFlaps = Boolean.parseBoolean(value.trim());
        flapStrength = readNumber(props, "flapStrength", flapStrength, 0.0, MAX_FLAP_STRENGTH);
        flapTime = readNumber(props, "flapTime", flapTime, 0.5, MAX_FLAP_TIME);
        flapCooldown = readNumber(props, "flapCooldown", flapCooldown, 0.0, MAX_FLAP_COOLDOWN);
        return props.keySet().containsAll(toProperties().keySet());
    }

    /** Reads a number clamped to {@code min} to {@code max}, keeps {@code fallback} if it is missing or not a number. */
    private static double readNumber(Properties props, String key, double fallback, double min, double max) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isNaN(parsed)) return Math.clamp(parsed, min, max);
        } catch (NumberFormatException ignored) {
        }
        ElytraBuff.LOGGER.warn("Invalid {} '{}' in {}, using {}", key, value.trim(), file, fallback);
        return fallback;
    }

    private void write(Writer writer) throws IOException {
        for (String line : HEADER.split("\n")) writer.write(("# " + line).stripTrailing() + "\n");
        writer.write("\n");
        for (var entry : toProperties().entrySet()) {
            writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
        }
    }

    private Map<String, String> toProperties() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("coldStart", String.valueOf(coldStart));
        props.put("chargeTime", String.valueOf(chargeTime));
        props.put("launchHeight", String.valueOf(launchHeight));
        props.put("energyBonus", String.valueOf(energyBonus));
        props.put("wingFlaps", String.valueOf(wingFlaps));
        props.put("flapStrength", String.valueOf(flapStrength));
        props.put("flapTime", String.valueOf(flapTime));
        props.put("flapCooldown", String.valueOf(flapCooldown));
        return props;
    }
}
