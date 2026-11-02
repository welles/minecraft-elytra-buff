package dev.welles.elytrabuff;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.WeakHashMap;

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

public final class ElytraBuff {
    public static final String MOD_ID = "elytrabuff";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** The flight settings last sent to each player; players without the client mod are never in here. */
    private static final Map<ServerPlayer, FlightSettings> SENT_SETTINGS = new WeakHashMap<>();
    /** The flight settings of the server this client is connected to, vanilla on servers without the mod. */
    private static volatile FlightSettings clientSettings = FlightSettings.VANILLA;
    /** Loader specific networking. */
    private static Network network;

    private ElytraBuff() {}

    public static void init(Path configDir, Network network) {
        ElytraBuff.network = network;
        BuffConfig.init(configDir);
        LOGGER.info("Elytra Buff loaded");
    }

    //? if >=1.21.11 {
    /*public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
    *///?} else {
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    //?}

    public static void onServerTick(MinecraftServer server) {
        BuffConfig config = BuffConfig.get();
        FlightSettings settings = FlightSettings.of(config);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncSettings(player, settings);
            ColdStart.tick(player, config);
            if (SENT_SETTINGS.containsKey(player)) WingFlaps.tick(player, settings);
        }
    }

    /** Sends the flight settings to players with the client mod after joining and whenever they change. */
    private static void syncSettings(ServerPlayer player, FlightSettings settings) {
        if (!settings.equals(SENT_SETTINGS.get(player)) && network.canSend(player)) {
            network.send(player, settings);
            SENT_SETTINGS.put(player, settings);
        }
    }

    public static void setClientSettings(FlightSettings settings) {
        clientSettings = settings;
        WingFlaps.resetClient();
    }

    /** Runs the wing flaps of the client's own player, every client tick while in a world. */
    public static void onClientTick(Player player) {
        WingFlaps.tickClient(player, clientSettings);
    }

    /**
     * The flight settings for the player's elytra flight. The client simulates its own flight with the settings the
     * server sent; the server uses the same ones, so both agree, and vanilla physics for players without the client mod.
     */
    private static FlightSettings settings(Player player) {
        return player.level().isClientSide() ? clientSettings : SENT_SETTINGS.getOrDefault(player, FlightSettings.VANILLA);
    }

    /** Applies the energy bonus and the wing flaps to the movement of an elytra flight tick. */
    public static Vec3 applyFlightBuffs(LivingEntity entity, Vec3 movement, double gravity) {
        if (!(entity instanceof Player player)) return movement;
        FlightSettings settings = settings(player);
        return WingFlaps.applyFlap(player, applyEnergyBonus(movement, gravity, settings.energyBonus()), settings);
    }

    /**
     * Adds speed to an elytra flight while it loses height. In vanilla, diving turns the height into speed, but drag
     * takes more than climbing back up returns. Falling by {@code -movement.y} blocks releases {@code gravity} times
     * that much energy; the bonus adds that fraction of it as extra kinetic energy, along the direction of flight.
     *
     * @param movement the movement vanilla calculated for this tick
     * @param gravity  the gravity that applied to it, lower with Slow Falling
     */
    private static Vec3 applyEnergyBonus(Vec3 movement, double gravity, double bonus) {
        if (movement.y >= 0.0) return movement;
        double speedSqr = movement.lengthSqr();
        if (bonus <= 0.0 || speedSqr == 0.0) return movement;
        // kinetic energy is speed² / 2 per unit of mass
        double gained = 2.0 * bonus * gravity * -movement.y;
        return movement.scale(Math.sqrt((speedSqr + gained) / speedSqr));
    }

    /** Loader specific: sends payloads to players that have the client mod. */
    public interface Network {
        boolean canSend(ServerPlayer player);

        void send(ServerPlayer player, CustomPacketPayload payload);
    }
}
