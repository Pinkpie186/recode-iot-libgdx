package br.edu.recode;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

final class PedalFrame {
    final long sequence;
    final long deviceMillis;
    final long rotations;
    final long lastCycleMillis;
    final int direction;

    private PedalFrame(long sequence, long deviceMillis, long rotations, long lastCycleMillis,
                       int direction) {
        this.sequence = sequence;
        this.deviceMillis = deviceMillis;
        this.rotations = rotations;
        this.lastCycleMillis = lastCycleMillis;
        this.direction = direction;
    }

    static PedalFrame parse(String line) {
        JsonValue root = new JsonReader().parse(line);
        if (root == null || !root.isObject()) throw new IllegalArgumentException("not an object");
        if (number(root, "v") != 2) throw new IllegalArgumentException("Firmware do sensor desatualizado");
        if (!"forearm".equals(string(root, "mode")))
            throw new IllegalArgumentException("Controle incompativel: use o MPU6050");
        String state = string(root, "state");
        if ("calibrating".equals(state))
            throw new IllegalArgumentException("Mantenha o antebraco neutro para calibrar");
        if (!"ok".equals(state))
            throw new IllegalArgumentException("MPU6050 nao esta pronto");
        long sequence = number(root, "seq");
        long ms = number(root, "ms");
        long rotations = number(root, "movement");
        long lastCycleMs = number(root, "lastMovementMs");
        long direction = number(root, "direction");
        if (sequence < 0 || ms < 0 || rotations < 0 || lastCycleMs < 0 || lastCycleMs > ms ||
                direction < -1 || direction > 1)
            throw new IllegalArgumentException("invalid value");
        return new PedalFrame(sequence, ms, rotations, lastCycleMs, (int) direction);
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
