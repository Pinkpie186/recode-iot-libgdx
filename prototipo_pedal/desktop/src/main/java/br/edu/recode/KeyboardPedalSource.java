package br.edu.recode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/** Sem sensor: F alterna os lados; cima/baixo escolhem o sentido do giro. */
final class KeyboardPedalSource implements PedalSource {
    private static final long FRAME_NANOS = 100_000_000L;
    private static final long MIN_TURN_MILLIS = 250L;

    private boolean running;
    private boolean fHeld;
    private boolean upHeld;
    private boolean downHeld;
    private long startedNanos;
    private long lastSentNanos = -1;
    private long lastFrameNanos = -1;
    private long sequence;
    private long rotations;
    private long lastCycleMillis;
    private int direction;

    @Override public void start() {
        running = true;
        startedNanos = System.nanoTime();
    }

    @Override public PedalFrame poll() {
        if (!running || Gdx.input == null) return null;
        long now = System.nanoTime();
        long elapsedMillis = Math.max(1L, (now - startedNanos) / 1_000_000L);
        boolean f = Gdx.input.isKeyPressed(Input.Keys.F);
        boolean up = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        float angleDeg = down && !up ? -35f : up && !down ? 35f : 0f;
        int requested = down && !downHeld ? -1 : up && !upHeld ? 1 :
                f && !fHeld ? (direction < 0 ? 1 : -1) : 0;
        fHeld = f;
        upHeld = up;
        downHeld = down;
        if (requested != 0 && (rotations == 0 ||
                elapsedMillis - lastCycleMillis >= MIN_TURN_MILLIS)) {
            rotations++;
            lastCycleMillis = elapsedMillis;
            direction = requested;
        }
        if (requested == 0 && lastSentNanos >= 0 && now - lastSentNanos < FRAME_NANOS)
            return null;
        lastSentNanos = now;
        lastFrameNanos = now;
        return PedalFrame.parse("{\"v\":2,\"seq\":" + sequence++
                + ",\"ms\":" + elapsedMillis
                + ",\"mode\":\"forearm\",\"movement\":" + rotations
                + ",\"lastMovementMs\":" + lastCycleMillis
                + ",\"direction\":" + direction
                + ",\"angleDeg\":" + angleDeg
                + ",\"state\":\"ok\"}");
    }

    @Override public long lastFrameNanos() { return lastFrameNanos; }
    @Override public String error() { return ""; }
    @Override public void close() { running = false; }
}
