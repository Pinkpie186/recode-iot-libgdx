package br.edu.recode;

interface PedalSource extends AutoCloseable {
    void start();
    PedalFrame poll();
    long lastFrameNanos();
    String error();
    @Override void close();
}
