package com.aibrain;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = AIBrainMod.MODID)
public class ClientEvents {

    @SubscribeEvent
    public static void registerRenderers(ModelRegistryEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(EntityAIBot.class,
                manager -> new RenderBiped<EntityAIBot>(manager, new ModelBiped(), 0.5F));
    }
}
