package br.edu.recode;

/** Sequencia ludica do confronto final; os limites nao sao metas de exercicio. */
final class PhaseThreeProgress {
    enum Step { PRECISION, SWITCH, RHYTHM, STABILITY }

    static final int SHIELD_COUNT = 3;
    static final int PRECISION_TURNS = 3;
    static final int SWITCH_TURNS = 1;
    static final int RHYTHM_PAIRS = 2;
    static final int STABILITY_PAIRS = 3;
    static final int MIN_TURNS_PER_SHIELD = 10;

    private int shields;
    private int shieldTurns;
    private int precisionTurns;
    private int switchTurns;
    private int rhythmStreak;
    private int stabilityStreak;
    private int stablePairsTotal;
    private int totalTurns;
    private Step step = Step.PRECISION;
    private boolean paused;
    private float movingSeconds;
    private long lastTurnMillis = -1;
    private long lastIntervalMillis = -1;

    /** A pedalada na chave de rota e uma acao separada da sequencia de precisao. */
    int acceptTurns(int turns, long lastCycleMillis) {
        if (paused || isComplete() || turns <= 0) return 0;
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

        int restored = 0;
        for (int i = 0; i < turns && !isComplete(); i++) {
            shieldTurns++;
            totalTurns++;
            boolean individual = turns == 1;
            if (steady && individual) stablePairsTotal++;

            switch (step) {
                case PRECISION:
                    // Um lote de voltas nao informa o instante de cada movimento.
                    if (individual && ++precisionTurns >= PRECISION_TURNS) {
                        step = Step.SWITCH;
                        switchTurns = 0;
                        rhythmStreak = 0;
                        lastIntervalMillis = -1;
                    }
                    break;
                case SWITCH:
                    if (individual && ++switchTurns >= SWITCH_TURNS) {
                        step = Step.RHYTHM;
                        lastIntervalMillis = -1;
                    }
                    break;
                case RHYTHM:
                    rhythmStreak = steady && individual ? rhythmStreak + 1 : 0;
                    if (rhythmStreak >= RHYTHM_PAIRS) {
                        step = Step.STABILITY;
                        stabilityStreak = 0;
                    }
                    break;
                case STABILITY:
                    stabilityStreak = steady && individual ? stabilityStreak + 1 : 0;
                    if (stabilityStreak >= STABILITY_PAIRS) {
                        shields++;
                        restored++;
                        shieldTurns = precisionTurns = switchTurns = rhythmStreak = stabilityStreak = 0;
                        step = Step.PRECISION;
                        lastTurnMillis = lastIntervalMillis = -1;
                    }
                    break;
                default:
                    throw new IllegalStateException("Etapa desconhecida");
            }
        }
        return restored;
    }

    void update(float seconds, boolean movingWithSignal) {
        if (!paused && !isComplete() && totalTurns > 0 && movingWithSignal)
            movingSeconds += Math.max(0f, seconds);
    }

    void togglePause() {
        if (isComplete()) return;
        paused = !paused;
        lastTurnMillis = lastIntervalMillis = -1;
        rhythmStreak = stabilityStreak = 0;
    }

    void restart() {
        shields = shieldTurns = precisionTurns = switchTurns = rhythmStreak = stabilityStreak = 0;
        stablePairsTotal = totalTurns = 0;
        step = Step.PRECISION;
        paused = false;
        movingSeconds = 0f;
        lastTurnMillis = lastIntervalMillis = -1;
    }

    int shields() { return shields; }
    int shieldTurns() { return shieldTurns; }
    int precisionTurns() { return precisionTurns; }
    int switchTurns() { return switchTurns; }
    int rhythmStreak() { return rhythmStreak; }
    int stabilityStreak() { return stabilityStreak; }
    int stablePairsTotal() { return stablePairsTotal; }
    int totalTurns() { return totalTurns; }
    Step step() { return step; }
    boolean isPaused() { return paused; }
    boolean isComplete() { return shields == SHIELD_COUNT; }
    float movingSeconds() { return movingSeconds; }

    /** A rota muda apenas quando o jogador aciona a chave com outra pedalada. */
    boolean routeChanged() { return !isComplete() &&
            (step == Step.RHYTHM || step == Step.STABILITY); }
    int activeRoute() { return (shields + (routeChanged() ? 1 : 0)) % 2; }
    int restoredConduits() { return shields * 2 + (routeChanged() ? 1 : 0); }
    boolean interferenceActive() {
        return !isComplete() && shieldTurns >= 2 &&
                (step != Step.STABILITY || stabilityStreak < STABILITY_PAIRS);
    }
}

