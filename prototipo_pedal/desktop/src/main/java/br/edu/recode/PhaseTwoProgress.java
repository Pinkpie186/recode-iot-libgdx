package br.edu.recode;

/** Progresso ludico da Rede Instavel; nao avalia capacidade fisica. */
final class PhaseTwoProgress {
    static final int NODE_COUNT = 3;
    static final int TURNS_PER_NODE = 8; // Parametro provisório do prototipo.
    static final int STABLE_PAIRS_PER_NODE = 2;

    private int nodes;
    private int nodeTurns;
    private int stablePairs;
    private int stablePairsTotal;
    private int totalTurns;
    private boolean paused;
    private float movingSeconds;
    private long lastTurnMillis = -1;
    private long lastIntervalMillis = -1;

    /** Retorna quantos nos foram estabilizados nesta leitura. */
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

        int repaired = 0;
        for (int i = 0; i < turns && !isComplete(); i++) {
            nodeTurns++;
            totalTurns++;
            if (steady && i == 0) {
                stablePairs++;
                stablePairsTotal++;
            } else {
                // O no precisa de uma sequencia continua de movimentos regulares.
                stablePairs = 0;
            }
            if (nodeTurns >= TURNS_PER_NODE && stablePairs >= STABLE_PAIRS_PER_NODE) {
                nodes++;
                repaired++;
                nodeTurns = 0;
                stablePairs = 0;
                lastTurnMillis = -1;
                lastIntervalMillis = -1;
            }
        }
        return repaired;
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
        stablePairs = 0;
    }

    void restart() {
        nodes = nodeTurns = stablePairs = stablePairsTotal = totalTurns = 0;
        paused = false;
        movingSeconds = 0f;
        lastTurnMillis = lastIntervalMillis = -1;
    }

    int nodes() { return nodes; }
    int nodeTurns() { return nodeTurns; }
    int stablePairs() { return stablePairs; }
    int stablePairsTotal() { return stablePairsTotal; }
    int totalTurns() { return totalTurns; }
    boolean isPaused() { return paused; }
    boolean isComplete() { return nodes == NODE_COUNT; }
    boolean interferenceActive() { return !isComplete() && nodeTurns >= 2 && stablePairs < STABLE_PAIRS_PER_NODE; }
    float movingSeconds() { return movingSeconds; }
}
