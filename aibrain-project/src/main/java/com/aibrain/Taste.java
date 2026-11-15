package com.aibrain;

import java.io.*;
import java.util.Random;

/** Learns what kind of buildings the player likes (3 kinds x 4 styles) from Like / Dislike feedback. */
public class Taste {

    public static final int TYPES = 3;
    public static final int STYLES = 4;

    private static final float[] pref = new float[TYPES * STYLES];
    private static final Random rnd = new Random();

    public static int lastType = -1;
    public static int lastStyle = -1;

    /** forcedType < 0 means "any kind". Returns {type, style}. */
    public static int[] choose(int forcedType) {
        double[] p = new double[TYPES * STYLES];
        double sum = 0;
        for (int t = 0; t < TYPES; t++) {
            if (forcedType >= 0 && t != forcedType) continue;
            for (int s = 0; s < STYLES; s++) {
                double v = Math.exp(pref[t * STYLES + s]);
                p[t * STYLES + s] = v;
                sum += v;
            }
        }
        double r = rnd.nextDouble() * sum;
        for (int i = 0; i < p.length; i++) {
            r -= p[i];
            if (p[i] > 0 && r <= 0) {
                return new int[] { i / STYLES, i % STYLES };
            }
        }
        return new int[] { forcedType >= 0 ? forcedType : 0, 0 };
    }

    public static boolean feedback(float delta) {
        if (lastType < 0) return false;
        int i = lastType * STYLES + lastStyle;
        pref[i] = Math.max(-4f, Math.min(4f, pref[i] + delta));
        return true;
    }

    public static String favorite() {
        int bi = 0;
        for (int i = 1; i < pref.length; i++) {
            if (pref[i] > pref[bi]) bi = i;
        }
        if (pref[bi] < 0.01f) return "none yet";
        return Structures.NAMES[bi / STYLES] + " (style " + (bi % STYLES + 1) + ")";
    }

    public static void save(File f) {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            out.writeInt(1);
            out.writeInt(pref.length);
            for (float v : pref) out.writeFloat(v);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(File f) {
        if (!f.exists()) return;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            if (in.readInt() != 1 || in.readInt() != pref.length) return;
            for (int i = 0; i < pref.length; i++) pref[i] = in.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
