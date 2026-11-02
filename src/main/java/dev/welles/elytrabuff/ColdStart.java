package dev.welles.elytrabuff;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

//? if >=1.21.2 {
/*import net.minecraft.world.entity.LivingEntity;
*///?} else {
import net.minecraft.world.item.ElytraItem;
//?}
//? if <26.1 {
import net.minecraft.world.item.ItemStack;
//?}

/**
 * Crouching on the ground with an elytra on charges a cold start, like gathering strength for one mighty wing beat:
 * after a second the air starts to stir around the player's feet, once it is charged feathers drift off the wings and
 * they rustle. Releasing crouch then beats the wings down in a gust of wind and feathers, throwing the player into the
 * air high enough to open the elytra. Runs on the server, so it works for players without the client mod as well;
 * clients with the mod also raise the wings while charging and beat them down for the launch.
 */
public final class ColdStart {
    private static final int TICKS_PER_SECOND = 20;
    private static final int WIND_DELAY = TICKS_PER_SECOND;
    private static final int RUSTLE_INTERVAL = TICKS_PER_SECOND;
    private static final int FEATHER_INTERVAL = 4;
    /** Directions the gust of the launch blows out along the ground. */
    private static final int GUST_DIRECTIONS = 16;
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
            if (charge == 1) ElytraBuff.sendToAll(player, new ColdStartPayload(player.getId(), ColdStartPayload.Phase.CHARGE, chargeTicks));
            showCharge(player, charge, chargeTicks);
            return;
        }
        CHARGE.remove(player);
        if (ready && charge >= chargeTicks) launch(player, config.launchHeight);
        else if (charge > 0) ElytraBuff.sendToAll(player, new ColdStartPayload(player.getId(), ColdStartPayload.Phase.CANCEL, 0));
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

        if (charge >= Math.min(WIND_DELAY, chargeTicks)) {
            // stirs more and more while charging: every 4th tick at first, every tick once charged
            int interval = 4 - 3 * Math.min(charge, chargeTicks) / chargeTicks;
            if (charge % interval == 0) level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.4, 0.02, 0.4, 0.02);
        }
        if (charge >= chargeTicks) {
            if ((charge - chargeTicks) % FEATHER_INTERVAL == 0) {
                level.sendParticles(feather(), x, player.getY() + 1.2, z, 1, 0.3, 0.2, 0.3, 0.02);
            }
            if ((charge - chargeTicks) % RUSTLE_INTERVAL == 0) {
                level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 0.8F, 0.8F);
            }
        }
    }

    private static void launch(ServerPlayer player, double height) {
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, launchSpeed(height, player.getGravity()), movement.z);
        // the client moves its player itself, like after knockback it has to be told the new movement
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        ElytraBuff.sendToAll(player, new ColdStartPayload(player.getId(), ColdStartPayload.Phase.LAUNCH, 0));

        ServerLevel level = (ServerLevel) player.level();
        double x = player.getX();
        double y = player.getY() + 0.1;
        double z = player.getZ();
        // a gust blows out along the ground in every direction; with a count of 0 the offset is the particle's direction
        for (int i = 0; i < GUST_DIRECTIONS; i++) {
            double angle = Math.PI * 2.0 * i / GUST_DIRECTIONS;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 0, Math.cos(angle), 0.05, Math.sin(angle), 0.35);
        }
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 8, 0.4, 0.1, 0.4, 0.05);
        level.sendParticles(feather(), x, player.getY() + 1.2, z, 12, 0.4, 0.3, 0.4, 0.15);
        level.playSound(null, x, y, z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, x, y, z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
    }

    /** Bits of feather, like those of a feather item breaking. */
    private static ItemParticleOption feather() {
        //? if >=26.1 {
        /*return new ItemParticleOption(ParticleTypes.ITEM, Items.FEATHER);
        *///?} else {
        return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.FEATHER));
        //?}
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
