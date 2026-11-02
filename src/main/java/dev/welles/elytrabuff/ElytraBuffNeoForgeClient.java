package dev.welles.elytrabuff;

//? if neoforge {
/*import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ElytraBuff.MOD_ID, dist = Dist.CLIENT)
public class ElytraBuffNeoForgeClient {
    public ElytraBuffNeoForgeClient(ModContainer container) {
        // the next server sends its own settings, or none if it does not have the mod
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ElytraBuff.setClientSettings(FlightSettings.VANILLA));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            if (Minecraft.getInstance().player != null) ElytraBuff.onClientTick(Minecraft.getInstance().player);
        });
        // "Config" button in the mod list
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(
            parent, FMLPaths.CONFIGDIR.get().resolve(ElytraBuff.MOD_ID + ".properties"), Component.literal(mod.getModInfo().getDisplayName()),
            BuffConfig.defaults(), BuffConfig.RANGES, BuffConfig.CHOICES
        ));
    }
}
*///?}
