package amaryllis.advancements_arranged;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

@Mod(AdvancementsArranged.MODID)
public class AdvancementsArranged {
    public static final String MODID = "advancements_arranged";

    public static final Logger LOGGER = LogUtils.getLogger();

    public AdvancementsArranged(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}
