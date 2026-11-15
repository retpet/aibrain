package com.aibrain;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Map;

/** Keeps one brain per mode and stores them in config/aibrain, so they survive world changes. */
public class BrainManager {

    public enum Mode { SURVIVAL, CREATIVE }

    private static File dir;
    private static Mode mode = Mode.SURVIVAL;
    private static final Map<Mode, Brain> brains = new EnumMap<Mode, Brain>(Mode.class);

    public static void init(File configDir) {
        dir = new File(configDir, "aibrain");
        dir.mkdirs();
        File mf = new File(dir, "mode.txt");
        if (mf.exists()) {
            try {
                String s = new String(Files.readAllBytes(mf.toPath()), StandardCharsets.UTF_8).trim();
                mode = Mode.valueOf(s);
            } catch (Exception e) {
                mode = Mode.SURVIVAL;
            }
        }
    }

    public static Mode getMode() {
        return mode;
    }

    public static void setMode(Mode m) {
        mode = m;
        try {
            Files.write(new File(dir, "mode.txt").toPath(), m.name().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Brain getBrain() {
        Brain b = brains.get(mode);
        if (b == null) {
            b = new Brain();
            b.load(fileFor(mode));
            brains.put(mode, b);
        }
        return b;
    }

    public static void save() {
        for (Map.Entry<Mode, Brain> e : brains.entrySet()) {
            e.getValue().save(fileFor(e.getKey()));
        }
    }

    public static void reset() {
        brains.remove(mode);
        File f = fileFor(mode);
        if (f.exists()) f.delete();
    }

    private static File fileFor(Mode m) {
        return new File(dir, "brain_" + m.name().toLowerCase() + ".dat");
    }
}
