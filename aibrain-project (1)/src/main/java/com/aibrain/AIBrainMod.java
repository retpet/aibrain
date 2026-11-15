package com.aibrain;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

@Mod(modid = AIBrainMod.MODID, name = "AI Brain", version = "0.1", acceptedMinecraftVersions = "[1.12.2]")
public class AIBrainMod {

    public static final String MODID = "aibrain";

    @Mod.Instance(MODID)
    public static AIBrainMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // "Memory" of the AI lives in <game folder>/config/aibrain, NOT in the world save.
        BrainManager.init(event.getModConfigurationDirectory());

        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "bot"),
                EntityAIBot.class, MODID + ".bot", 0, this, 64, 1, true);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandAIBrain());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        BrainManager.save();
    }
}
