package br.edu.recode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/** Teste sem dispositivo: cada toque em F simula uma volta do pedal. */
final class KeyboardPedalSource implements PedalSource {
    private static final long FRAME_NANOS = 100_000_000L;
    private static final long MIN_TURN_MILLIS = 180L;

    private boolean running;
    private boolean keyHeld;
    private long startedNanos;
    private long lastSentNanos = -1;
    private long lastFrameNanos = -1;
    private long sequence;
    private long rotations;
    private long lastCycleMillis;

    @Override public void start() {
        running = true;
        startedNanos = System.nanoTime();
    }

    @Override public PedalFrame poll() {
        if (!running || Gdx.input == null) return null;
        long now = System.nanoTime();
        long elapsedMillis = Math.max(1L, (now - startedNanos) / 1_000_000L);
        boolean pressed = Gdx.input.isKeyPressed(Input.Keys.F);
        boolean newPress = pressed && !keyHeld;
        keyHeld = pressed;
        if (newPress && elapsedMillis - lastCycleMillis >= MIN_TURN_MILLIS) {
            rotations++;
            lastCycleMillis = elapsedMillis;
        }
        if (!newPress && lastSentNanos >= 0 && now - lastSentNanos < FRAME_NANOS)
            return null;
        lastSentNanos = now;
        lastFrameNanos = now;
        return PedalFrame.parse("{\"v\":1,\"seq\":" + sequence++
                + ",\"ms\":" + elapsedMillis
                + ",\"mode\":\"pedal\",\"cycle\":" + rotations
                + ",\"lastCycleMs\":" + lastCycleMillis
                + ",\"state\":\"ok\"}");
    }

    @Override public long lastFrameNanos() { return lastFrameNanos; }
    @Override public String error() { return ""; }
    @Override public void close() { running = false; }
}
