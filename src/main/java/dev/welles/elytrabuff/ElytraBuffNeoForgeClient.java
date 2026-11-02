package dev.welles.elytrabuff;

//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ElytraBuff.MOD_ID, dist = Dist.CLIENT)
public class ElytraBuffNeoForgeClient {
    public ElytraBuffNeoForgeClient() {
        // the next server sends its own bonus, or none if it does not have the mod
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ElytraBuff.setClientBonus(0.0));
    }
}
*///?}
