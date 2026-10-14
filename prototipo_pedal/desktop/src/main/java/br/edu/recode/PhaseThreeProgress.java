package br.edu.recode;

/** Estado ludico do confronto final. Os limites nao sao metas de exercicio. */
final class PhaseThreeProgress {
    static final int SHIELD_COUNT = 3;
    static final int TURNS_PER_SHIELD = 8;
    static final int STABLE_PAIRS_PER_SHIELD = 2;

    private int shields;
    private int shieldTurns;
    private int stablePairs;
    private int stablePairsTotal;
    private int totalTurns;
    private boolean paused;
    private float movingSeconds;
    private long lastTurnMillis = -1;
    private long lastIntervalMillis = -1;

    /** Retorna o numero de escudos restaurados nesta leitura. */
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
            if (steady && i == 0) {
                stablePairs++;
                stablePairsTotal++;
            }
            if (shieldTurns >= TURNS_PER_SHIELD && stablePairs >= STABLE_PAIRS_PER_SHIELD) {
                shields++;
                restored++;
                shieldTurns = 0;
                stablePairs = 0;
                lastTurnMillis = -1;
                lastIntervalMillis = -1;
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
        lastTurnMillis = -1;
        lastIntervalMillis = -1;
    }

    void restart() {
        shields = shieldTurns = stablePairs = stablePairsTotal = totalTurns = 0;
        paused = false;
        movingSeconds = 0f;
        lastTurnMillis = lastIntervalMillis = -1;
    }

    int shields() { return shields; }
    int shieldTurns() { return shieldTurns; }
    int stablePairs() { return stablePairs; }
    int stablePairsTotal() { return stablePairsTotal; }
    int totalTurns() { return totalTurns; }
    boolean isPaused() { return paused; }
    boolean isComplete() { return shields == SHIELD_COUNT; }
    float movingSeconds() { return movingSeconds; }

    /** Apos metade do trecho, No Zero desvia a energia para a outra rota. */
    boolean routeChanged() { return !isComplete() && shieldTurns >= TURNS_PER_SHIELD / 2; }
    int activeRoute() { return (shields + (routeChanged() ? 1 : 0)) % 2; }
    int restoredConduits() {
        return shields * 2 + (routeChanged() ? 1 : 0);
    }
    boolean interferenceActive() {
        return !isComplete() && shieldTurns >= 2 && stablePairs < STABLE_PAIRS_PER_SHIELD;
    }
}
