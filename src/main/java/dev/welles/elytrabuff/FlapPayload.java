package dev.welles.elytrabuff;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells a client that another player started or stopped flapping their wings, so it can animate them. */
public record FlapPayload(int entityId, boolean flapping) implements CustomPacketPayload {
    public static final Type<FlapPayload> TYPE = new Type<>(ElytraBuff.id("flap"));
    public static final StreamCodec<FriendlyByteBuf, FlapPayload> CODEC = StreamCodec.ofMember(FlapPayload::write, FlapPayload::new);

    private FlapPayload(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readBoolean());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(flapping);
    }

    @Override
    public Type<FlapPayload> type() {
        return TYPE;
    }
}
