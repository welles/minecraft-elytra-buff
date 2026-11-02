package dev.welles.elytrabuff;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The server's settings for the parts of the mod that change the flight itself. The client simulates its own elytra
 * flight, so it applies them; a client that never receives them (the server does not have the mod) flies like in vanilla.
 *
 * @param flapTicks    how long the player can flap at once
 * @param cooldownTicks how long the wings take to recover completely
 */
public record FlightSettings(double energyBonus, boolean wingFlaps, double flapStrength, int flapTicks, int cooldownTicks)
    implements CustomPacketPayload {
    public static final Type<FlightSettings> TYPE = new Type<>(ElytraBuff.id("flight_settings"));
    public static final StreamCodec<FriendlyByteBuf, FlightSettings> CODEC = StreamCodec.ofMember(FlightSettings::write, FlightSettings::new);
    /** Vanilla flight, for clients on servers without the mod. */
    public static final FlightSettings VANILLA = new FlightSettings(0.0, false, 0.0, 1, 1);

    private static final int TICKS_PER_SECOND = 20;

    public static FlightSettings of(BuffConfig config) {
        return new FlightSettings(
            config.energyBonus, config.wingFlaps, config.flapStrength,
            Math.max(1, (int) Math.round(config.flapTime * TICKS_PER_SECOND)), Math.max(1, (int) Math.round(config.flapCooldown * TICKS_PER_SECOND))
        );
    }

    private FlightSettings(FriendlyByteBuf buf) {
        this(buf.readDouble(), buf.readBoolean(), buf.readDouble(), buf.readVarInt(), buf.readVarInt());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeDouble(energyBonus);
        buf.writeBoolean(wingFlaps);
        buf.writeDouble(flapStrength);
        buf.writeVarInt(flapTicks);
        buf.writeVarInt(cooldownTicks);
    }

    @Override
    public Type<FlightSettings> type() {
        return TYPE;
    }
}
