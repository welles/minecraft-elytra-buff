package dev.welles.elytrabuff;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells a client that a player started charging a cold start, stopped it or launched, so it can raise the wings while
 * charging and beat them down for the launch.
 *
 * @param chargeTicks how long the cold start takes to charge, only used when it starts
 */
public record ColdStartPayload(int entityId, Phase phase, int chargeTicks) implements CustomPacketPayload {
    public static final Type<ColdStartPayload> TYPE = new Type<>(ElytraBuff.id("cold_start"));
    public static final StreamCodec<FriendlyByteBuf, ColdStartPayload> CODEC = StreamCodec.ofMember(ColdStartPayload::write, ColdStartPayload::new);

    private ColdStartPayload(FriendlyByteBuf buf) {
        this(buf.readVarInt(), Phase.values()[buf.readVarInt()], buf.readVarInt());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(phase.ordinal());
        buf.writeVarInt(chargeTicks);
    }

    @Override
    public Type<ColdStartPayload> type() {
        return TYPE;
    }

    public enum Phase {
        CHARGE, CANCEL, LAUNCH
    }
}
