package dev.welles.elytrabuff;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class ElytraBuff {
    public static final String MOD_ID = "elytrabuff";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ElytraBuff() {}

    public static void init(Path configDir) {
        BuffConfig.init(configDir);
        LOGGER.info("Elytra Buff loaded");
    }

    public static void onServerTick(MinecraftServer server) {
        BuffConfig config = BuffConfig.get();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ColdStart.tick(player, config);
        }
    }
}
