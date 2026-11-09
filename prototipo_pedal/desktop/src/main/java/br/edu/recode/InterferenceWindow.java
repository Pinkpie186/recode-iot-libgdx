package br.edu.recode;

/** Uma interferencia curta por estacao; a pausa congela o tempo restante. */
final class InterferenceWindow {
    static final float DURATION_SECONDS = 3f;

    private float remainingSeconds;
    private boolean triggered;

    void trigger() {
        if (triggered) return;
        triggered = true;
        remainingSeconds = DURATION_SECONDS;
    }

    void update(float seconds, boolean playing) {
        if (playing && remainingSeconds > 0f)
            remainingSeconds = Math.max(0f, remainingSeconds - Math.max(0f, seconds));
    }

    void reset() {
        remainingSeconds = 0f;
        triggered = false;
    }

    boolean active() { return remainingSeconds > 0f; }
    float remainingSeconds() { return remainingSeconds; }
}
