package br.edu.recode;

/** Uma tentativa completa: progresso e desempenho continuam entre as fases. */
final class CampaignProgress {
    enum Status { RUNNING, BETWEEN_PHASES, VICTORY, DEFEAT }

    static final int INITIAL_INTEGRITY = 3;
    static final int EXTRA_TURNS_PER_STRIKE = 6;
    static final int PHASE_TWO_OPENING_STRIKE = 6;

    private final PhaseOneProgress first = new PhaseOneProgress();
    private final PhaseTwoProgress second = new PhaseTwoProgress();
    private final PhaseThreeProgress third = new PhaseThreeProgress();
    private final MascotShield mascotShield = new MascotShield();
    private final EnergyFragments[] fragments = {
            new EnergyFragments(), new EnergyFragments(), new EnergyFragments()
    };
    private final int[] turns = new int[3];
    private final int[] objectives = new int[3];
    private final int[] stablePairs = new int[3];
    private final float[] movingSeconds = new float[3];
    private int phase = 1;
    private int integrity = INITIAL_INTEGRITY;
    private int observedObjective = -1;
    private int nextStrikeAt = PhaseTwoProgress.TURNS_PER_NODE + EXTRA_TURNS_PER_STRIKE;
    private Status status = Status.RUNNING;
    private int attackSequence;
    private boolean lastAttackBlocked;
    private int pendingHits;

    void observe() {
        if (status != Status.RUNNING) return;
        if (phase == 1) {
            turns[0] = first.rotations();
            objectives[0] = first.repairedCircuits();
            movingSeconds[0] = first.activeSeconds();
            if (first.isComplete()) status = Status.BETWEEN_PHASES;
        } else if (phase == 2) {
            turns[1] = second.totalTurns();
            objectives[1] = second.nodes();
            stablePairs[1] = second.stablePairsTotal();
            movingSeconds[1] = second.movingSeconds();
            if (second.isComplete()) {
                status = Status.BETWEEN_PHASES;
            } else {
                observeInterference(second.nodes(), second.nodeTurns(), second.stablePairs(),
                        PhaseTwoProgress.STABLE_PAIRS_PER_NODE, PhaseTwoProgress.TURNS_PER_NODE);
            }
        } else {
            turns[2] = third.totalTurns();
            objectives[2] = third.shields();
            stablePairs[2] = third.stablePairsTotal();
            movingSeconds[2] = third.movingSeconds();
            if (third.isComplete()) {
                status = Status.VICTORY;
            } else {
                observeInterference(third.shields(), third.shieldTurns(), third.stabilityStreak(),
                        PhaseThreeProgress.STABILITY_PAIRS, PhaseThreeProgress.MIN_TURNS_PER_SHIELD);
            }
        }
    }

    private void observeInterference(int objective, int segmentTurns, int pairs,
                                     int requiredPairs, int target) {
        if (objective != observedObjective) {
            observedObjective = objective;
            nextStrikeAt = phase == 2 ? PHASE_TWO_OPENING_STRIKE
                    : target + EXTRA_TURNS_PER_STRIKE;
        }
        // Cada no recebe um disparo demonstravel. Um ritmo ja estabilizado
        // cria uma barreira da rede mesmo se o escudo do mascote expirou.
        if (phase == 2 && nextStrikeAt == PHASE_TWO_OPENING_STRIKE &&
                segmentTurns >= PHASE_TWO_OPENING_STRIKE) {
            registerAttack(pairs >= requiredPairs);
            nextStrikeAt = target + EXTRA_TURNS_PER_STRIKE;
        }
        if (integrity == 0) {
            status = Status.DEFEAT;
            return;
        }
        // Depois da abertura, novos ataques so ocorrem se faltar regularidade.
        // Uma cadencia lenta e regular nunca causa dano; nao existe cronometro.
        if (segmentTurns < target || pairs >= requiredPairs) return;
        while (segmentTurns >= nextStrikeAt && integrity > pendingHits) {
            registerAttack(false);
            nextStrikeAt += EXTRA_TURNS_PER_STRIKE;
        }
        if (integrity == 0) status = Status.DEFEAT;
    }

    private void registerAttack(boolean rhythmBarrier) {
        boolean mascotBlocked = mascotShield.blockAttack();
        lastAttackBlocked = mascotBlocked || rhythmBarrier;
        attackSequence++;
        if (!lastAttackBlocked) pendingHits++;
    }

    void resolveHit() {
        if (pendingHits <= 0 || integrity <= 0) return;
        pendingHits--;
        integrity--;
        if (integrity == 0) status = Status.DEFEAT;
    }

    void advance() {
        if (status != Status.BETWEEN_PHASES) throw new IllegalStateException("Fase ainda em andamento");
        phase++;
        observedObjective = -1;
        status = Status.RUNNING;
    }

    PhaseOneProgress first() { return first; }
    PhaseTwoProgress second() { return second; }
    PhaseThreeProgress third() { return third; }
    MascotShield mascotShield() { return mascotShield; }
    EnergyFragments fragments(int number) { return fragments[number - 1]; }
    int totalFragments() {
        return fragments[0].collected() + fragments[1].collected() + fragments[2].collected();
    }
    int attackSequence() { return attackSequence; }
    int pendingHits() { return pendingHits; }
    boolean lastAttackBlocked() { return lastAttackBlocked; }
    int phase() { return phase; }
    int integrity() { return integrity; }
    Status status() { return status; }
    int turns(int number) { return turns[number - 1]; }
    int objectives(int number) { return objectives[number - 1]; }
    int stablePairs(int number) { return stablePairs[number - 1]; }
    float movingSeconds(int number) { return movingSeconds[number - 1]; }
    int totalTurns() { return turns[0] + turns[1] + turns[2]; }
    float totalMovingSeconds() { return movingSeconds[0] + movingSeconds[1] + movingSeconds[2]; }
}
