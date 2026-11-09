package br.edu.recode;

interface PedalSource extends AutoCloseable {
    void start();
    PedalFrame poll();
    long lastFrameNanos();
    String error();
    default boolean signalAvailable(long nowNanos) {
        long last = lastFrameNanos();
        return error().isEmpty() && last >= 0 && nowNanos - last <= 2_000_000_000L;
    }
    @Override void close();
}
