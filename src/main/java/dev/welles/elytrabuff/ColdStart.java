package dev.welles.elytrabuff;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

//? if >=1.21.2 {
/*import net.minecraft.world.entity.LivingEntity;
*///?} else {
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//?}

/**
 * Crouching on the ground with an elytra on charges a cold start: smoke rises from the player's feet after a second,
 * flames and a hiss follow once it is charged. Releasing crouch then throws the player into the air, high enough to
 * open the elytra. Runs on the server only, so it works for players without the client mod as well.
 */
public final class ColdStart {
    private static final int TICKS_PER_SECOND = 20;
    private static final int SMOKE_DELAY = TICKS_PER_SECOND;
    private static final int HISS_INTERVAL = TICKS_PER_SECOND;
    /** Vertical drag of a player in the air, see {@code LivingEntity#travelInAir}. */
    private static final double AIR_DRAG = 0.98;

    /** How many ticks each player has been crouching while able to cold start. */
    private static final Map<ServerPlayer, Integer> CHARGE = new WeakHashMap<>();

    private ColdStart() {}

    public static void tick(ServerPlayer player, BuffConfig config) {
        boolean ready = config.coldStart && canColdStart(player);
        int charge = CHARGE.getOrDefault(player, 0);
        int chargeTicks = Math.max(1, (int) Math.round(config.chargeTime * TICKS_PER_SECOND));

        if (ready && player.isShiftKeyDown()) {
            CHARGE.put(player, ++charge);
            showCharge(player, charge, chargeTicks);
            return;
        }
        CHARGE.remove(player);
        if (ready && charge >= chargeTicks) launch(player, config.launchHeight);
    }

    /** Standing on the ground with an elytra that can fly, not swimming, riding or flying in creative mode. */
    private static boolean canColdStart(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && player.onGround() && !player.isPassenger() && !player.isFallFlying()
            && !player.getAbilities().flying && !player.isInWater() && !player.isInLava() && hasGlider(player);
    }

    private static boolean hasGlider(ServerPlayer player) {
        //? if >=1.21.2 {
        /*for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (LivingEntity.canGlideUsing(player.getItemBySlot(slot), slot)) return true;
        }
        return false;
        *///?} else {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(chest);
        //?}
    }

    private static void showCharge(ServerPlayer player, int charge, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        double x = player.getX();
        double y = player.getY() + 0.1;
        double z = player.getZ();

        if (charge >= Math.min(SMOKE_DELAY, chargeTicks)) {
            // thickens while charging
            int smoke = 1 + (int) (3.0 * Math.min(charge, chargeTicks) / chargeTicks);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, smoke, 0.25, 0.05, 0.25, 0.01);
        }
        if (charge >= chargeTicks) {
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 2, 0.2, 0.02, 0.2, 0.01);
            if ((charge - chargeTicks) % HISS_INTERVAL == 0) {
                level.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
        }
    }

    private static void launch(ServerPlayer player, double height) {
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, launchSpeed(height, player.getGravity()), movement.z);
        // the client moves its player itself, like after knockback it has to be told the new movement
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        ServerLevel level = (ServerLevel) player.level();
        double x = player.getX();
        double y = player.getY() + 0.1;
        double z = player.getZ();
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 20, 0.3, 0.05, 0.3, 0.08);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.3, 0.05, 0.3, 0.05);
        level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** The upward speed that lifts a player by {@code height} blocks, found by simulating the jump. */
    static double launchSpeed(double height, double gravity) {
        // the gravity attribute can be changed, without gravity any speed would be enough
        gravity = Math.max(gravity, 0.001);
        double low = 0.0;
        double high = 10.0;
        for (int i = 0; i < 40; i++) {
            double speed = (low + high) / 2;
            if (peak(speed, gravity) < height) low = speed;
            else high = speed;
        }
        return high;
    }

    /** How high a player rises with the given upward speed: each tick it moves, then gravity and drag apply. */
    private static double peak(double speed, double gravity) {
        double y = 0.0;
        while (speed > 0.0) {
            y += speed;
            speed = (speed - gravity) * AIR_DRAG;
        }
        return y;
    }
}
