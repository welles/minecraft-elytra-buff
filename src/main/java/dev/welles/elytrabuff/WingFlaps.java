package dev.welles.elytrabuff;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Crouching while flying with an elytra flaps the wings: like a weak firework rocket it pushes the player along the
 * direction they look, with a wing beat sound. Flapping tires the wings out after a few seconds, then they need a few
 * seconds to recover. Releasing crouch earlier lets them recover from the time flapped so far.
 * <p>
 * The client of the flying player applies the push, since it simulates its own flight. The server runs the same
 * timing to let the other players hear and see the wing beats.
 */
public final class WingFlaps {
    /** Ticks between two wing beats. */
    private static final int BEAT_INTERVAL = 8;
    /** The push fades out towards this speed, in blocks per tick: flapping never gets as fast as a firework rocket. */
    private static final double MAX_SPEED = 1.5;

    private static final Map<ServerPlayer, Wings> SERVER_WINGS = new WeakHashMap<>();
    /** The wings of the client's own player, only used on the client thread. */
    private static final Wings CLIENT_WINGS = new Wings();

    private WingFlaps() {}

    /** Runs the wings of a player with the client mod on the server, plays the wing beats for everyone else. */
    public static void tick(ServerPlayer player, FlightSettings settings) {
        Wings wings = SERVER_WINGS.computeIfAbsent(player, p -> new Wings());
        if (wings.tick(player, settings) && wings.flapped % BEAT_INTERVAL == 1) beat(player);
    }

    /** Runs the wings of the client's own player and plays the wing beats for them. */
    public static void tickClient(Player player, FlightSettings settings) {
        if (CLIENT_WINGS.tick(player, settings) && CLIENT_WINGS.flapped % BEAT_INTERVAL == 1) {
            // only the client's own player hears this, the server plays it for everyone else
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.8F, pitch(player));
        }
    }

    /** The flying player's wings are fresh again on the next server. */
    public static void resetClient() {
        CLIENT_WINGS.stamina = 1.0;
        CLIENT_WINGS.flapped = 0;
        CLIENT_WINGS.flapping = false;
        CLIENT_WINGS.tired = false;
    }

    private static void beat(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        // excludes the player, their client plays the sound itself
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.8F, pitch(player));
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 4, 0.5, 0.1, 0.5, 0.02);
    }

    private static float pitch(Player player) {
        return 0.9F + player.getRandom().nextFloat() * 0.2F;
    }

    /**
     * Pushes a flapping player along their view, a bit less the closer they get to the top speed of flapping.
     *
     * @param movement the movement vanilla calculated for this elytra flight tick
     */
    public static Vec3 applyFlap(Player player, Vec3 movement, FlightSettings settings) {
        Wings wings = player.level().isClientSide() ? (player.isLocalPlayer() ? CLIENT_WINGS : null) : SERVER_WINGS.get(player);
        if (wings == null || !wings.flapping || settings.flapStrength() <= 0.0) return movement;
        Vec3 look = player.getLookAngle();
        double fade = Math.max(0.0, 1.0 - movement.dot(look) / MAX_SPEED);
        return movement.add(look.scale(settings.flapStrength() * fade));
    }

    private static final class Wings {
        /** 1 = fresh wings, 0 = tired out. */
        double stamina = 1.0;
        /** Ticks flapped without a break, to time the wing beats. */
        int flapped;
        boolean flapping;
        /** Tired out wings can only flap again once they have fully recovered. */
        boolean tired;

        /** Returns whether the player flaps this tick. */
        boolean tick(Player player, FlightSettings settings) {
            flapping = settings.wingFlaps() && !tired && player.isShiftKeyDown() && player.isFallFlying()
                && !player.isInWater() && !player.isInLava();
            if (flapping) {
                flapped++;
                stamina -= 1.0 / settings.flapTicks();
                if (stamina <= 0.0) {
                    stamina = 0.0;
                    tired = true;
                }
            } else {
                flapped = 0;
                stamina = Math.min(1.0, stamina + 1.0 / settings.cooldownTicks());
                if (stamina == 1.0) tired = false;
            }
            return flapping;
        }
    }
}
