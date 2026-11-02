package dev.welles.elytrabuff;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class ElytraBuffFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ElytraBuff.init(FabricLoader.getInstance().getConfigDir(), new ElytraBuff.Network() {
            @Override
            public boolean canSend(ServerPlayer player) {
                return ServerPlayNetworking.canSend(player, BonusPayload.TYPE);
            }

            @Override
            public void send(ServerPlayer player, CustomPacketPayload payload) {
                ServerPlayNetworking.send(player, payload);
            }
        });

        //? if >=26.1 {
        /*PayloadTypeRegistry.clientboundPlay().register(BonusPayload.TYPE, BonusPayload.CODEC);
        *///?} else {
        PayloadTypeRegistry.playS2C().register(BonusPayload.TYPE, BonusPayload.CODEC);
        //?}
        ServerTickEvents.END_SERVER_TICK.register(ElytraBuff::onServerTick);
    }
}
//?}
