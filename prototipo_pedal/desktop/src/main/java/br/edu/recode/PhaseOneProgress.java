package br.edu.recode;

/** Estado da primeira fase, independente da tela e da origem das pedaladas. */
final class PhaseOneProgress {
    static final int TARGET_ROTATIONS = 20; // Apenas para testar a mecanica.
    static final int[] REPAIR_AT = {5, 10, 15};

    private int rotations;
    private boolean paused;
    private float activeSeconds;

    void update(float seconds, boolean movingWithSignal) {
        if (!paused && !isComplete() && rotations > 0 && movingWithSignal)
            activeSeconds += Math.max(0f, seconds);
    }

    int addRotations(int turns) {
        if (paused || isComplete() || turns <= 0) return 0;
        int previous = rotations;
        rotations = Math.min(TARGET_ROTATIONS, rotations + turns);
        return rotations - previous;
    }

    void togglePause() {
        if (!isComplete()) paused = !paused;
    }

    void restart() {
        rotations = 0;
        activeSeconds = 0f;
        paused = false;
    }

    int rotations() { return rotations; }
    boolean isPaused() { return paused; }
    boolean isComplete() { return rotations >= TARGET_ROTATIONS; }
    float activeSeconds() { return activeSeconds; }

    int repairedCircuits() {
        int count = 0;
        for (int threshold : REPAIR_AT) if (rotations >= threshold) count++;
        return count;
    }

    int nextRepairAt() {
        for (int threshold : REPAIR_AT) if (rotations < threshold) return threshold;
        return TARGET_ROTATIONS;
    }
}
