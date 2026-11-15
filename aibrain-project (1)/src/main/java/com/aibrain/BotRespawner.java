package com.aibrain;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = AIBrainMod.MODID)
public class BotRespawner {

    private static class Pending {
        int dim;
        double x, y, z;
        int ticks;
    }

    private static final List<Pending> pending = new ArrayList<Pending>();

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        Entity e = event.getEntityLiving();
        if (e instanceof EntityAIBot && !e.world.isRemote) {
            World world = e.world;
            Pending p = new Pending();
            p.dim = world.provider.getDimension();
            EntityPlayer player = world.getClosestPlayer(e.posX, e.posY, e.posZ, 128.0D, false);
            if (player != null) {
                p.x = player.posX;
                p.y = player.posY;
                p.z = player.posZ;
            } else {
                p.x = e.posX;
                p.y = e.posY;
                p.z = e.posZ;
            }
            p.ticks = 100; // 5 seconds
            pending.add(p);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending.isEmpty()) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (--p.ticks > 0) continue;
            it.remove();
            World world = server.getWorld(p.dim);
            if (world == null) continue;
            EntityAIBot bot = new EntityAIBot(world);
            bot.setLocationAndAngles(p.x + 2, p.y, p.z, 0f, 0f);
            world.spawnEntity(bot);
        }
    }
}
