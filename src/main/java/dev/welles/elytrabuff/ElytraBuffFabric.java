package dev.welles.elytrabuff;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public class ElytraBuffFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ElytraBuff.init(FabricLoader.getInstance().getConfigDir());
        ServerTickEvents.END_SERVER_TICK.register(ElytraBuff::onServerTick);
    }
}
//?}
