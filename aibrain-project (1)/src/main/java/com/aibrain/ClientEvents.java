package com.aibrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = AIBrainMod.MODID)
public class ClientEvents {

    private static KeyBinding menuKey;

    @SubscribeEvent
    public static void registerRenderers(ModelRegistryEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(EntityAIBot.class,
                manager -> new RenderBiped<EntityAIBot>(manager, new ModelPlayer(0.0F, false), 0.5F));

        menuKey = new KeyBinding("AI Brain menu", Keyboard.KEY_J, "AI Brain");
        ClientRegistry.registerKeyBinding(menuKey);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.KeyInputEvent event) {
        if (menuKey != null && menuKey.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiAIMenu());
        }
    }
}
