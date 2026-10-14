package br.edu.recode;

import java.util.concurrent.ConcurrentLinkedQueue;

/** Apenas para desenvolvimento: gera o mesmo JSON do ESP32 sem ler um pedal. */
final class SimulatedPedalSource implements PedalSource {
    private final ConcurrentLinkedQueue<PedalFrame> frames = new ConcurrentLinkedQueue<>();
    private volatile boolean running;
    private volatile long lastFrameNanos = -1;
    private Thread generator;

    @Override public void start() {
        running = true;
        generator = new Thread(this::generateLoop, "recode-pedal-simulator");
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
            long rotations = elapsedMs / 1_000L; // Uma volta por segundo: 60 RPM.
            long lastCycleMs = rotations * 1_000L;
            String line = "{\"v\":1,\"seq\":" + sequence++
                    + ",\"ms\":" + elapsedMs
                    + ",\"mode\":\"pedal\",\"cycle\":" + rotations
                    + ",\"lastCycleMs\":" + lastCycleMs
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
