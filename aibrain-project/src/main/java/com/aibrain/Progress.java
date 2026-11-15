package com.aibrain;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.io.*;

/** Survival progression: the bot works through stages and remembers how far it got. */
public class Progress {

    public static final String[] NAMES = { "Wood", "Stone", "Coal", "Iron", "Diamond", "Done" };
    public static final int[] NEED = { 10, 24, 8, 10, 3 };

    public static int stage = 0;
    public static int count = 0;

    public static boolean isDone() {
        return stage >= NEED.length;
    }

    public static boolean isTarget(Block b) {
        switch (stage) {
            case 0: return b == Blocks.LOG || b == Blocks.LOG2;
            case 1: return b == Blocks.STONE || b == Blocks.COBBLESTONE;
            case 2: return b == Blocks.COAL_ORE;
            case 3: return b == Blocks.IRON_ORE;
            case 4: return b == Blocks.DIAMOND_ORE;
            default: return false;
        }
    }

    /** Returns true if this block completed the current stage. */
    public static boolean onMined() {
        if (isDone()) return false;
        count++;
        if (count >= NEED[stage]) {
            stage++;
            count = 0;
            return true;
        }
        return false;
    }

    public static String describe() {
        if (isDone()) return "All survival stages complete";
        return "Stage: " + NAMES[stage] + " " + count + "/" + NEED[stage];
    }

    public static void save(File f) {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            out.writeInt(1);
            out.writeInt(stage);
            out.writeInt(count);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(File f) {
        if (!f.exists()) return;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            if (in.readInt() != 1) return;
            stage = in.readInt();
            count = in.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
