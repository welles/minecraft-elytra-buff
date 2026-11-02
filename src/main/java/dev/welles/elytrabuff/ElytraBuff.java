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

    /** The energy bonus last sent to each player; players without the client mod are never in here. */
    private static final Map<ServerPlayer, Double> SENT_BONUS = new WeakHashMap<>();
    /** The energy bonus of the server this client is connected to, 0 on servers without the mod. */
    private static volatile double clientBonus;
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncBonus(player, config.energyBonus);
            ColdStart.tick(player, config);
        }
    }

    /** Sends the energy bonus to players with the client mod after joining and whenever it changes. */
    private static void syncBonus(ServerPlayer player, double bonus) {
        Double sent = SENT_BONUS.get(player);
        if ((sent == null || sent != bonus) && network.canSend(player)) {
            network.send(player, new BonusPayload(bonus));
            SENT_BONUS.put(player, bonus);
        }
    }

    public static void setClientBonus(double bonus) {
        clientBonus = bonus;
    }

    /**
     * The energy bonus for the player's elytra flight. The client simulates its own flight with the bonus the server
     * sent; the server uses the same value, so both agree, and vanilla physics for players without the client mod.
     */
    private static double energyBonus(Player player) {
        return player.level().isClientSide() ? clientBonus : SENT_BONUS.getOrDefault(player, 0.0);
    }

    /**
     * Adds speed to an elytra flight while it loses height. In vanilla, diving turns the height into speed, but drag
     * takes more than climbing back up returns. Falling by {@code -movement.y} blocks releases {@code gravity} times
     * that much energy; the bonus adds that fraction of it as extra kinetic energy, along the direction of flight.
     *
     * @param movement the movement vanilla calculated for this tick
     * @param gravity  the gravity that applied to it, lower with Slow Falling
     */
    public static Vec3 applyEnergyBonus(LivingEntity entity, Vec3 movement, double gravity) {
        if (!(entity instanceof Player player) || movement.y >= 0.0) return movement;
        double bonus = energyBonus(player);
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
