package dev.welles.elytrabuff;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Moves the elytra wings of players on the client, on top of the angles vanilla gives them: they beat while a player
 * flaps, rise while a cold start charges and come down in one powerful stroke when it launches. Only used on the client
 * thread. The angles are those of the left wing, the right wing mirrors it.
 */
public final class WingAnimation {
    /** How far a wing beat swings the wings up and down, and forward and back, in radians. */
    private static final float BEAT_SWING_Z = 0.7F;
    private static final float BEAT_SWING_X = 0.15F;
    /** The wings never beat closer together than when standing, so they do not cross behind the back in a dive. */
    private static final float MAX_WING_ROT_Z = (float) (-Math.PI / 12);
    /** How much of a wing beat starts or stops in one tick, so the wings ease in and out of beating. */
    private static final float BEAT_EASING = 0.25F;

    /** The wings when crouching in vanilla, where a cold start begins. More negative z is further up. */
    private static final float CROUCH_ROT_Z = (float) (-Math.PI / 4);
    private static final float CROUCH_ROT_X = (float) (Math.PI * 2.0 / 9.0);
    /** The wings raised high above the back once the cold start is charged. */
    private static final float RAISED_ROT_Z = -2.5F;
    private static final float RAISED_ROT_X = 0.35F;
    /** The wings at the bottom of the launching stroke, swept down and back. */
    private static final float STROKE_ROT_Z = -0.2F;
    private static final float STROKE_ROT_X = 0.9F;
    /** How the charged wings quiver, ready to strike. */
    private static final float QUIVER = 0.04F;
    /** Ticks the launching stroke takes, and ticks the wings then take to settle back into the vanilla angles. */
    private static final int STROKE_TICKS = 3;
    private static final int SETTLE_TICKS = 6;
    /** Ticks the wings take to take on the pose of a charge, and to sink back when it is cancelled. */
    private static final int EASE_TICKS = 4;

    /** Other players the server said are flapping, by entity id. */
    private static final Set<Integer> FLAPPING = new HashSet<>();
    private static final Map<Player, WingBeat> BEATS = new WeakHashMap<>();
    /** By entity id, the server tells about players before the client may have them. */
    private static final Map<Integer, ColdStartPose> COLD_STARTS = new HashMap<>();

    private WingAnimation() {}

    /** Animates the wings of all players around the client's own player, every client tick. */
    static void tick(Player player, boolean flapping) {
        for (Player other : player.level().players()) {
            // a lost stop does not leave the wings beating after landing
            boolean beating = other == player ? flapping : FLAPPING.contains(other.getId()) && other.isFallFlying();
            WingBeat beat = BEATS.get(other);
            if (beat == null && beating) BEATS.put(other, beat = new WingBeat());
            if (beat != null && !beat.tick(beating)) BEATS.remove(other);
        }

        for (Iterator<Map.Entry<Integer, ColdStartPose>> it = COLD_STARTS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, ColdStartPose> entry = it.next();
            // players that went out of sight or landed in water do not keep charging
            Entity entity = player.level().getEntity(entry.getKey());
            if (!(entity instanceof Player other) || other.isInWater() || !entry.getValue().tick()) it.remove();
        }
    }

    public static void onFlap(FlapPayload payload) {
        if (payload.flapping()) FLAPPING.add(payload.entityId());
        else FLAPPING.remove(payload.entityId());
    }

    public static void onColdStart(ColdStartPayload payload) {
        ColdStartPose pose = COLD_STARTS.computeIfAbsent(payload.entityId(), id -> new ColdStartPose());
        switch (payload.phase()) {
            case CHARGE -> pose.charge(payload.chargeTicks());
            case CANCEL -> pose.cancel();
            case LAUNCH -> pose.launch();
        }
    }

    /** No one's wings move on the next server until it says so. */
    static void reset() {
        FLAPPING.clear();
        BEATS.clear();
        COLD_STARTS.clear();
    }

    /** The forward and backward tilt of the left wing. */
    public static float wingRotX(LivingEntity entity, float partialTick, float rotX) {
        ColdStartPose pose = COLD_STARTS.get(entity.getId());
        if (pose != null) rotX = pose.blend(partialTick, rotX, pose.rotXOld, pose.rotX);
        return rotX + BEAT_SWING_X * wingBeat(entity, partialTick);
    }

    /** The spread of the left wing, more negative is further up. */
    public static float wingRotZ(LivingEntity entity, float partialTick, float rotZ) {
        ColdStartPose pose = COLD_STARTS.get(entity.getId());
        if (pose != null) rotZ = pose.blend(partialTick, rotZ, pose.rotZOld, pose.rotZ);
        float beat = wingBeat(entity, partialTick);
        return beat == 0.0F ? rotZ : Math.min(rotZ + BEAT_SWING_Z * beat, MAX_WING_ROT_Z);
    }

    /** -1 = wings all the way up, 1 = all the way down, 0 = not beating. */
    private static float wingBeat(LivingEntity entity, float partialTick) {
        WingBeat beat = entity instanceof Player player ? BEATS.get(player) : null;
        if (beat == null) return 0.0F;
        // starts at the top, so the down stroke comes with the sound
        float phase = (beat.ticks + partialTick) / WingFlaps.BEAT_INTERVAL * Mth.TWO_PI;
        return -Mth.lerp(partialTick, beat.strengthOld, beat.strength) * Mth.cos(phase);
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

    /**
     * The wings of a cold start: they rise from the crouching angles while it charges and quiver once it is charged.
     * The launch beats them down, then they settle back into the vanilla angles. The pose replaces the vanilla angles by
     * its weight, so it starts and ends without a jump.
     */
    private static final class ColdStartPose {
        int chargeTicks = 1;
        /** Ticks charged so far, or -1 when not charging. */
        int charged = -1;
        /** Ticks since the launch, or -1 when not launching. */
        int stroke = -1;
        float rotX = CROUCH_ROT_X;
        float rotZ = CROUCH_ROT_Z;
        float rotXOld = CROUCH_ROT_X;
        float rotZOld = CROUCH_ROT_Z;
        float weight;
        float weightOld;

        void charge(int chargeTicks) {
            this.chargeTicks = Math.max(1, chargeTicks);
            charged = 0;
            stroke = -1;
        }

        void cancel() {
            charged = -1;
        }

        void launch() {
            charged = -1;
            stroke = 0;
        }

        /** Returns whether the pose still moves the wings. */
        boolean tick() {
            rotXOld = rotX;
            rotZOld = rotZ;
            weightOld = weight;
            if (charged >= 0) {
                charged++;
                weight = Math.min(1.0F, weight + 1.0F / EASE_TICKS);
                float progress = Math.min(1.0F, (float) charged / chargeTicks);
                rotX = Mth.lerp(progress, CROUCH_ROT_X, RAISED_ROT_X);
                rotZ = Mth.lerp(progress, CROUCH_ROT_Z, RAISED_ROT_Z);
                if (charged >= chargeTicks) rotZ += charged % 2 == 0 ? QUIVER : -QUIVER;
            } else if (stroke >= 0) {
                stroke++;
                if (stroke <= STROKE_TICKS) {
                    // from wherever the wings are towards the bottom of the stroke, faster at the end
                    float progress = (float) stroke / STROKE_TICKS;
                    progress *= progress;
                    rotX = Mth.lerp(progress, rotXOld, STROKE_ROT_X);
                    rotZ = Mth.lerp(progress, rotZOld, STROKE_ROT_Z);
                } else {
                    weight = Math.max(0.0F, weight - 1.0F / SETTLE_TICKS);
                }
            } else {
                weight = Math.max(0.0F, weight - 1.0F / EASE_TICKS);
            }
            return weight > 0.0F || weightOld > 0.0F;
        }

        float blend(float partialTick, float vanilla, float old, float current) {
            return Mth.lerp(Mth.lerp(partialTick, weightOld, weight), vanilla, Mth.lerp(partialTick, old, current));
        }
    }
}
