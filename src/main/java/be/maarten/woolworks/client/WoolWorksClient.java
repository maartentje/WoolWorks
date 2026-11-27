package be.maarten.woolworks.client;

import be.maarten.woolworks.WoolWorks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = WoolWorks.MODID, dist = Dist.CLIENT)
public class WoolWorksClient {
    public WoolWorksClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> {
            var screen = new ConfigurationScreen(mod, parent,
                    (root, type, config, title) -> new ResourceConfigurationSection(root, type, config, title));
            return net.minecraft.client.Minecraft.getInstance().level == null ? new ConfigAvailabilityScreen(parent, screen) : screen;
        });
    }
}
