package com.aibrain;

import java.io.*;
import java.util.Random;

/**
 * A tiny neural network (8 inputs -> 12 hidden -> 4 outputs) written in pure Java.
 * Learning = simple neuroevolution: after every episode the weights are either kept
 * (if the result was better than the best so far) or restored, and then slightly mutated.
 */
public class Brain {

    public static final int IN = 8;
    public static final int HID = 12;
    public static final int OUT = 4;
    public static final int N = HID * (IN + 1) + OUT * (HID + 1);

    private final Random rnd = new Random();

    public float[] w = new float[N];
    public float[] best = new float[N];
    public int episodes = 0;
    public float bestReward = -1000000f;

    public Brain() {
        for (int i = 0; i < N; i++) {
            w[i] = (float) (rnd.nextGaussian() * 0.5);
        }
        System.arraycopy(w, 0, best, 0, N);
    }

    /** Forward pass: returns OUT values in range -1..1 */
    public float[] think(float[] in) {
        float[] h = new float[HID];
        int p = 0;
        for (int j = 0; j < HID; j++) {
            float s = 0;
            for (int i = 0; i < IN; i++) {
                s += in[i] * w[p++];
            }
            s += w[p++];
            h[j] = (float) Math.tanh(s);
        }
        float[] o = new float[OUT];
        for (int k = 0; k < OUT; k++) {
            float s = 0;
            for (int j = 0; j < HID; j++) {
                s += h[j] * w[p++];
            }
            s += w[p++];
            o[k] = (float) Math.tanh(s);
        }
        return o;
    }

    /** Called when an episode ends; this is where the "learning" happens. */
    public void endEpisode(float reward) {
        episodes++;
        if (reward > bestReward) {
            bestReward = reward;
            System.arraycopy(w, 0, best, 0, N);
        } else {
            System.arraycopy(best, 0, w, 0, N);
            bestReward -= 0.1f; // slowly "forget" lucky results
        }
        float strength = Math.max(0.05f, 0.4f / (1f + episodes * 0.02f));
        for (int i = 0; i < N; i++) {
            if (rnd.nextFloat() < 0.15f) {
                w[i] += (float) rnd.nextGaussian() * strength;
            }
        }
    }

    public void save(File f) {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            out.writeInt(1);
            out.writeInt(N);
            out.writeInt(episodes);
            out.writeFloat(bestReward);
            for (int i = 0; i < N; i++) out.writeFloat(w[i]);
            for (int i = 0; i < N; i++) out.writeFloat(best[i]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load(File f) {
        if (!f.exists()) return;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            int version = in.readInt();
            int n = in.readInt();
            if (version != 1 || n != N) return;
            episodes = in.readInt();
            bestReward = in.readFloat();
            for (int i = 0; i < N; i++) w[i] = in.readFloat();
            for (int i = 0; i < N; i++) best[i] = in.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
