package br.edu.recode;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

final class PedalFrame {
    final long sequence;
    final long deviceMillis;
    final long rotations;
    final long lastCycleMillis;

    private PedalFrame(long sequence, long deviceMillis, long rotations, long lastCycleMillis) {
        this.sequence = sequence;
        this.deviceMillis = deviceMillis;
        this.rotations = rotations;
        this.lastCycleMillis = lastCycleMillis;
    }

    static PedalFrame parse(String line) {
        JsonValue root = new JsonReader().parse(line);
        if (root == null || !root.isObject()) throw new IllegalArgumentException("not an object");
        if (number(root, "v") != 1) throw new IllegalArgumentException("unknown version");
        if (!"pedal".equals(string(root, "mode"))) throw new IllegalArgumentException("wrong mode");
        if (!"ok".equals(string(root, "state"))) throw new IllegalArgumentException("device not ready");
        long sequence = number(root, "seq");
        long ms = number(root, "ms");
        long rotations = number(root, "cycle");
        long lastCycleMs = number(root, "lastCycleMs");
        if (sequence < 0 || ms < 0 || rotations < 0 || lastCycleMs < 0 || lastCycleMs > ms)
            throw new IllegalArgumentException("invalid value");
        return new PedalFrame(sequence, ms, rotations, lastCycleMs);
    }

    private static long number(JsonValue root, String key) {
        JsonValue field = root.get(key);
        if (field == null || !field.isNumber()) throw new IllegalArgumentException("missing number: " + key);
        return field.asLong();
    }

    private static String string(JsonValue root, String key) {
        JsonValue field = root.get(key);
        if (field == null || !field.isString()) throw new IllegalArgumentException("missing string: " + key);
        return field.asString();
    }
}
