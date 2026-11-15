package com.aibrain;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class CommandAIBrain extends CommandBase {

    @Override
    public String getName() {
        return "aibrain";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aibrain <spawn | mode survival | mode creative | info | reset>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) throw new WrongUsageException(getUsage(sender));

        String sub = args[0].toLowerCase();
        if (sub.equals("spawn")) {
            World world = sender.getEntityWorld();
            // only one bot at a time, so they do not disturb each other's learning
            List<Entity> old = new ArrayList<Entity>();
            for (Entity e : world.loadedEntityList) {
                if (e instanceof EntityAIBot) old.add(e);
            }
            for (Entity e : old) e.setDead();

            Vec3d p = sender.getPositionVector();
            EntityAIBot bot = new EntityAIBot(world);
            bot.setLocationAndAngles(p.x + 2, p.y, p.z, 0f, 0f);
            world.spawnEntity(bot);
            msg(sender, "AI bot spawned. Mode: " + BrainManager.getMode());
        } else if (sub.equals("mode")) {
            if (args.length < 2) throw new WrongUsageException(getUsage(sender));
            if (args[1].equalsIgnoreCase("survival")) {
                BrainManager.setMode(BrainManager.Mode.SURVIVAL);
            } else if (args[1].equalsIgnoreCase("creative")) {
                BrainManager.setMode(BrainManager.Mode.CREATIVE);
            } else {
                throw new WrongUsageException(getUsage(sender));
            }
            msg(sender, "Mode: " + BrainManager.getMode() + " (respawn the bot with /aibrain spawn)");
        } else if (sub.equals("info")) {
            Brain b = BrainManager.getBrain();
            msg(sender, "Mode: " + BrainManager.getMode() + ", episodes: " + b.episodes
                    + ", best reward: " + String.format("%.2f", b.bestReward));
        } else if (sub.equals("reset")) {
            BrainManager.reset();
            msg(sender, "Brain for mode " + BrainManager.getMode() + " was reset.");
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    private void msg(ICommandSender s, String text) {
        s.sendMessage(new TextComponentString(text));
    }
}
