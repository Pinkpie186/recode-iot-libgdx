package br.edu.recode;

/** Mantem No Zero ativo quando ELO permanece sem giros validos. */
final class BossIdleClock {
    static final float PHASE_TWO_SECONDS = 7f;
    static final float PHASE_THREE_SECONDS = 6f;
    private static final float WARNING_SECONDS = 2f;

    private int observedTurns = -1;
    private float idleSeconds;

    boolean update(float seconds, int turns, float attackAfterSeconds,
                   boolean playing, boolean attackBusy) {
        if (turns != observedTurns) {
            observedTurns = turns;
            idleSeconds = 0f;
            return false;
        }
        if (!playing || attackBusy) {
            idleSeconds = 0f;
            return false;
        }
        idleSeconds += Math.max(0f, seconds);
        if (idleSeconds < attackAfterSeconds) return false;
        idleSeconds = 0f;
        return true;
    }

    float warningProgress(float attackAfterSeconds) {
        float warningStart = attackAfterSeconds - WARNING_SECONDS;
        return idleSeconds <= warningStart ? 0f :
                Math.min(1f, (idleSeconds - warningStart) / WARNING_SECONDS);
    }

    void reset() {
        observedTurns = -1;
        idleSeconds = 0f;
    }
}
