package dev.welles.elytrabuff;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells a client the server's flight energy bonus. The client simulates its own elytra flight, so it applies the bonus;
 * a client that never receives it (the server does not have the mod) flies like in vanilla.
 */
public record BonusPayload(double energyBonus) implements CustomPacketPayload {
    public static final Type<BonusPayload> TYPE = new Type<>(ElytraBuff.id("energy_bonus"));
    public static final StreamCodec<FriendlyByteBuf, BonusPayload> CODEC = StreamCodec.ofMember(BonusPayload::write, BonusPayload::new);

    private BonusPayload(FriendlyByteBuf buf) {
        this(buf.readDouble());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeDouble(energyBonus);
    }

    @Override
    public Type<BonusPayload> type() {
        return TYPE;
    }
}
