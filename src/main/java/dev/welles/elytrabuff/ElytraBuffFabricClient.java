package dev.welles.elytrabuff;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ElytraBuffFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Handlers run on the client thread; registering the receiver lets the server know the client has the mod
        ClientPlayNetworking.registerGlobalReceiver(FlightSettings.TYPE, (payload, context) -> ElytraBuff.setClientSettings(payload));
        ClientPlayNetworking.registerGlobalReceiver(FlapPayload.TYPE, (payload, context) -> WingAnimation.onFlap(payload));
        ClientPlayNetworking.registerGlobalReceiver(ColdStartPayload.TYPE, (payload, context) -> WingAnimation.onColdStart(payload));
        // the next server sends its own settings, or none if it does not have the mod
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ElytraBuff.setClientSettings(FlightSettings.VANILLA));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) ElytraBuff.onClientTick(client.player);
        });
    }
}
//?}
