package dev.welles.elytrabuff;

//? if neoforge {
/*import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(ElytraBuff.MOD_ID)
public class ElytraBuffNeoForge {
    public ElytraBuffNeoForge(IEventBus modBus) {
        ElytraBuff.init(FMLPaths.CONFIGDIR.get(), new ElytraBuff.Network() {
            @Override
            public boolean canSend(ServerPlayer player) {
                return player.connection.hasChannel(FlightSettings.TYPE);
            }

            @Override
            public void send(ServerPlayer player, CustomPacketPayload payload) {
                player.connection.send(payload);
            }
        });
        modBus.addListener(ElytraBuffNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> ElytraBuff.onServerTick(event.getServer()));
    }

    // An optional channel: players without the mod can still join and use the cold start, they fly like in vanilla.
    // The handler runs on the main thread and only on clients.
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional()
            .playToClient(FlightSettings.TYPE, FlightSettings.CODEC, (payload, context) -> ElytraBuff.setClientSettings(payload))
            .playToClient(FlapPayload.TYPE, FlapPayload.CODEC, (payload, context) -> WingFlaps.setClientFlapping(payload));
    }
}
*///?}
