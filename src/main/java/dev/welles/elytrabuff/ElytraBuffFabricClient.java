package dev.welles.elytrabuff;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ElytraBuffFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Handlers run on the client thread; registering the receiver lets the server know the client has the mod
        ClientPlayNetworking.registerGlobalReceiver(BonusPayload.TYPE, (payload, context) -> ElytraBuff.setClientBonus(payload.energyBonus()));
        // the next server sends its own bonus, or none if it does not have the mod
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ElytraBuff.setClientBonus(0.0));
    }
}
//?}
