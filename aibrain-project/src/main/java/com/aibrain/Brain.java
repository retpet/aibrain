package com.aibrain;

import java.io.*;
import java.util.Random;

/**
 * A tiny neural network (8 inputs -> 12 hidden -> 4 outputs) written in pure Java.
 * It starts with simple "instincts" (turn toward the target, walk, jump over obstacles,
 * attack close enemies, avoid danger) and then improves by neuroevolution:
 * after every episode the weights are kept if the result was better, otherwise restored,
 * and then slightly mutated.
 */
public class Brain {

    public static final int IN = 8;
    public static final int HID = 12;
    public static final int OUT = 4;
    public static final int N = HID * (IN + 1) + OUT * (HID + 1);

    private static final int FILE_VERSION = 2;

    private final Random rnd = new Random();

    public float[] w = new float[N];
    public float[] best = new float[N];
    public int episodes = 0;
    public float bestReward = -1000000f;

    /** index of the weight from input i (or bias if i == IN) to hidden neuron j */
    private static int hid(int j, int i) {
        return j * (IN + 1) + i;
    }

    /** index of the weight from hidden neuron j (or bias if j == HID) to output k */
    private static int out(int k, int j) {
        return HID * (IN + 1) + k * (HID + 1) + j;
    }

    public Brain() {
        for (int i = 0; i < N; i++) {
            w[i] = (float) (rnd.nextGaussian() * 0.05);
        }
        // instincts
        w[hid(0, 1)] = 3f;   w[out(0, 0)] = 1.5f;                       // turn toward the target
        w[hid(1, 0)] = 4f;   w[out(1, 1)] = 1.5f; w[out(1, HID)] = -0.5f; // walk, slow down when close
        w[hid(2, 5)] = 3f;   w[out(2, 2)] = 1.5f;                       // obstacle ahead -> jump
        w[hid(3, IN)] = 2f;  w[hid(3, 2)] = -3f;  w[out(3, 3)] = 1.5f;   // enemy close -> attack
        w[hid(4, 6)] = 4f;   w[out(0, 4)] = 1.0f; w[out(1, 4)] = -2.0f;  // danger -> turn away and stop
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
        float strength = Math.max(0.05f, 0.25f / (1f + episodes * 0.02f));
        for (int i = 0; i < N; i++) {
            if (rnd.nextFloat() < 0.15f) {
                w[i] += (float) rnd.nextGaussian() * strength;
            }
        }
    }

    public void save(File f) {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            out.writeInt(FILE_VERSION);
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
            if (version != FILE_VERSION || n != N) return; // old brains are ignored
            episodes = in.readInt();
            bestReward = in.readFloat();
            for (int i = 0; i < N; i++) w[i] = in.readFloat();
            for (int i = 0; i < N; i++) best[i] = in.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
