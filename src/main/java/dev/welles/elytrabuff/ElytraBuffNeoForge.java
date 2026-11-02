package dev.welles.elytrabuff;

//? if neoforge {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(ElytraBuff.MOD_ID)
public class ElytraBuffNeoForge {
    public ElytraBuffNeoForge() {
        ElytraBuff.init(FMLPaths.CONFIGDIR.get());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> ElytraBuff.onServerTick(event.getServer()));
    }
}
*///?}
