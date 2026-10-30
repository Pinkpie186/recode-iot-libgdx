package br.edu.recode;

import java.util.concurrent.ConcurrentLinkedQueue;

/** Gera gestos alternados para jogar sem o MPU6050 conectado. */
final class SimulatedPedalSource implements PedalSource {
    private final ConcurrentLinkedQueue<PedalFrame> frames = new ConcurrentLinkedQueue<>();
    private volatile boolean running;
    private volatile long lastFrameNanos = -1;
    private Thread generator;

    @Override public void start() {
        running = true;
        generator = new Thread(this::generateLoop, "recode-forearm-simulator");
        generator.setDaemon(true);
        generator.start();
    }

    @Override public PedalFrame poll() { return frames.poll(); }
    @Override public long lastFrameNanos() { return lastFrameNanos; }
    @Override public String error() { return ""; }

    private void generateLoop() {
        long started = System.nanoTime();
        long sequence = 0;
        while (running) {
            long now = System.nanoTime();
            long elapsedMs = (now - started) / 1_000_000L;
            long rotations = elapsedMs / 1_000L; // Um giro detectado por segundo.
            long lastCycleMs = rotations * 1_000L;
            int direction = rotations == 0 ? 0 : rotations % 2 == 1 ? -1 : 1;
            float angleDeg = rotations == 0 || elapsedMs % 1_000L >= 700L
                    ? 0f : direction * 35f;
            String line = "{\"v\":2,\"seq\":" + sequence++
                    + ",\"ms\":" + elapsedMs
                    + ",\"mode\":\"forearm\",\"movement\":" + rotations
                    + ",\"lastMovementMs\":" + lastCycleMs
                    + ",\"direction\":" + direction
                    + ",\"angleDeg\":" + angleDeg
                    + ",\"state\":\"ok\"}";
            frames.add(PedalFrame.parse(line));
            lastFrameNanos = now;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override public void close() {
        running = false;
        if (generator != null) generator.interrupt();
    }
}
