package dev.welles.elytrabuff;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Crouching while flying with an elytra flaps the wings: like a weak firework rocket it pushes the player along the
 * direction they look, with a wing beat sound. Flapping tires the wings out after a few seconds, then they need a few
 * seconds to recover. Releasing crouch earlier lets them recover from the time flapped so far.
 * <p>
 * The client of the flying player applies the push, since it simulates its own flight. The server runs the same
 * timing to let the other players hear and see the wing beats, and tells their clients to animate the wings.
 */
public final class WingFlaps {
    /** Ticks between two wing beats. */
    private static final int BEAT_INTERVAL = 8;
    /** The push fades out towards this speed, in blocks per tick: flapping never gets as fast as a firework rocket. */
    private static final double MAX_SPEED = 1.5;
    /** How far a wing beat swings the wings up and down, and forward and back, in radians. */
    private static final float BEAT_SWING_Z = 0.7F;
    private static final float BEAT_SWING_X = 0.15F;
    /** The wings never beat closer together than when standing, so they do not cross behind the back in a dive. */
    private static final float MAX_WING_ROT_Z = (float) (-Math.PI / 12);
    /** How much of a wing beat starts or stops in one tick, so the wings ease in and out of beating. */
    private static final float BEAT_EASING = 0.25F;

    private static final Map<ServerPlayer, Wings> SERVER_WINGS = new WeakHashMap<>();
    /** The wings of the client's own player, only used on the client thread. */
    private static final Wings CLIENT_WINGS = new Wings();
    /** Other players the server said are flapping, by entity id, only used on the client thread. */
    private static final Set<Integer> CLIENT_FLAPPING = new HashSet<>();
    /** The wing beat animations of players on the client, only used on the client thread. */
    private static final Map<Player, WingBeat> CLIENT_BEATS = new WeakHashMap<>();

    private WingFlaps() {}

    /**
     * Runs the wings of a player with the client mod on the server, plays the wing beats for everyone else and tells
     * the other clients with the mod whenever the player starts or stops flapping.
     */
    public static void tick(ServerPlayer player, FlightSettings settings) {
        Wings wings = SERVER_WINGS.computeIfAbsent(player, p -> new Wings());
        boolean wasFlapping = wings.flapping;
        if (wings.tick(player, settings) && wings.flapped % BEAT_INTERVAL == 1) beat(player);
        if (wings.flapping != wasFlapping) ElytraBuff.sendToOthers(player, new FlapPayload(player.getId(), wings.flapping));
    }

    /** Runs the wings of the client's own player, plays the wing beats for them and animates all wing beats. */
    public static void tickClient(Player player, FlightSettings settings) {
        if (CLIENT_WINGS.tick(player, settings) && CLIENT_WINGS.flapped % BEAT_INTERVAL == 1) {
            // only the client's own player hears this, the server plays it for everyone else
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.8F, pitch(player));
        }

        for (Player other : player.level().players()) {
            // a lost stop does not leave the wings beating after landing
            boolean flapping = other == player ? CLIENT_WINGS.flapping : CLIENT_FLAPPING.contains(other.getId()) && other.isFallFlying();
            WingBeat beat = CLIENT_BEATS.get(other);
            if (beat == null && flapping) CLIENT_BEATS.put(other, beat = new WingBeat());
            if (beat != null && !beat.tick(flapping)) CLIENT_BEATS.remove(other);
        }
    }

    public static void setClientFlapping(FlapPayload payload) {
        if (payload.flapping()) CLIENT_FLAPPING.add(payload.entityId());
        else CLIENT_FLAPPING.remove(payload.entityId());
    }

    /** The flying player's wings are fresh again on the next server. */
    public static void resetClient() {
        CLIENT_WINGS.stamina = 1.0;
        CLIENT_WINGS.flapped = 0;
        CLIENT_WINGS.flapping = false;
        CLIENT_WINGS.tired = false;
        CLIENT_FLAPPING.clear();
        CLIENT_BEATS.clear();
    }

    /** The forward and backward tilt of the left wing with the wing beat of a flapping player. */
    public static float wingRotX(LivingEntity entity, float partialTick, float rotX) {
        return rotX + BEAT_SWING_X * wingBeat(entity, partialTick);
    }

    /** The spread of the left wing with the wing beat of a flapping player, more negative is further up. */
    public static float wingRotZ(LivingEntity entity, float partialTick, float rotZ) {
        float beat = wingBeat(entity, partialTick);
        return beat == 0.0F ? rotZ : Math.min(rotZ + BEAT_SWING_Z * beat, MAX_WING_ROT_Z);
    }

    /** -1 = wings all the way up, 1 = all the way down, 0 = not beating. */
    private static float wingBeat(LivingEntity entity, float partialTick) {
        WingBeat beat = entity instanceof Player player ? CLIENT_BEATS.get(player) : null;
        if (beat == null) return 0.0F;
        // starts at the top, so the down stroke comes with the sound
        float phase = (beat.ticks + partialTick) / BEAT_INTERVAL * Mth.TWO_PI;
        return -Mth.lerp(partialTick, beat.strengthOld, beat.strength) * Mth.cos(phase);
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

    /** The wing beat animation of a player, eased in when they start flapping and out when they stop. */
    private static final class WingBeat {
        /** Starts at -1, the first tick is the top of the first beat. */
        int ticks = -1;
        float strength;
        float strengthOld;

        /** Returns whether the wings still beat. */
        boolean tick(boolean flapping) {
            ticks++;
            strengthOld = strength;
            strength = flapping ? Math.min(1.0F, strength + BEAT_EASING) : Math.max(0.0F, strength - BEAT_EASING);
            return flapping || strengthOld > 0.0F;
        }
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
