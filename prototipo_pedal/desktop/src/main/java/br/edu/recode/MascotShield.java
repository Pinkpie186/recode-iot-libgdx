package br.edu.recode;

/** Protecao ludica carregada por uma sequencia de pedaladas regulares. */
final class MascotShield {
    static final int REQUIRED_PAIRS = 3;
    static final float DURATION_SECONDS = 15f;

    private int charge;
    private int activations;
    private int blockedAttacks;
    private long lastTurnMillis = -1;
    private long lastIntervalMillis = -1;
    private float remainingSeconds;

    boolean acceptTurns(int turns, long lastCycleMillis) {
        if (turns <= 0 || active()) return false;
        boolean steady = false;
        if (turns == 1 && lastTurnMillis >= 0 && lastCycleMillis > lastTurnMillis) {
            long interval = lastCycleMillis - lastTurnMillis;
            if (interval >= 180 && interval <= 10_000) {
                steady = lastIntervalMillis > 0 &&
                        interval >= lastIntervalMillis * 0.65 &&
                        interval <= lastIntervalMillis * 1.35;
                lastIntervalMillis = interval;
            } else {
                lastIntervalMillis = -1;
            }
        } else {
            lastIntervalMillis = -1;
        }
        lastTurnMillis = lastCycleMillis;
        charge = steady ? charge + 1 : 0;
        if (charge < REQUIRED_PAIRS) return false;
        charge = 0;
        remainingSeconds = DURATION_SECONDS;
        activations++;
        lastTurnMillis = lastIntervalMillis = -1;
        return true;
    }

    void update(float deltaSeconds, boolean playing) {
        if (!playing || !active()) return;
        remainingSeconds = Math.max(0f, remainingSeconds - Math.max(0f, deltaSeconds));
        if (!active()) resetSequence();
    }

    boolean blockAttack() {
        if (!active()) return false;
        blockedAttacks++;
        return true;
    }

    void resetSequence() {
        charge = 0;
        lastTurnMillis = lastIntervalMillis = -1;
    }

    boolean active() { return remainingSeconds > 0f; }
    float remainingSeconds() { return remainingSeconds; }
    int charge() { return charge; }
    int activations() { return activations; }
    int blockedAttacks() { return blockedAttacks; }
}

