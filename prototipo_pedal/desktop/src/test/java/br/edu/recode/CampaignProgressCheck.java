package br.edu.recode;

/** Verificacao sem dependencias externas da progressao da campanha. */
public final class CampaignProgressCheck {
    private CampaignProgressCheck() { }

    public static void main(String[] args) {
        fullVictoryKeepsAllResults();
        irregularTurnsCanCauseDefeatWithoutTimeLimit();
        System.out.println("CampaignProgressCheck: OK");
    }

    private static void fullVictoryKeepsAllResults() {
        CampaignProgress run = new CampaignProgress();
        run.first().addRotations(5);
        run.first().update(12f, true);
        run.first().togglePause();
        require(run.first().addRotations(10) == 0, "pausa nao deve contar voltas");
        run.first().togglePause();
        run.first().addRotations(15);
        run.observe();
        require(run.status() == CampaignProgress.Status.BETWEEN_PHASES, "fase 1 concluida");
        run.advance();

        for (int turn = 1; turn <= 24; turn++) {
            run.second().acceptTurns(1, turn * 1000L);
            run.second().update(0.5f, true);
            run.observe();
        }
        require(run.status() == CampaignProgress.Status.BETWEEN_PHASES, "fase 2 concluida");
        require(run.integrity() == 3, "ritmo regular preserva integridade");
        run.advance();

        for (int turn = 25; turn <= 48; turn++) {
            run.third().acceptTurns(1, turn * 1000L);
            run.third().update(0.5f, true);
            run.observe();
        }
        require(run.status() == CampaignProgress.Status.VICTORY, "vitoria apos a fase 3");
        require(run.phase() == 3 && run.totalTurns() == 68, "total preservado");
        require(run.turns(1) == 20 && run.turns(2) == 24 && run.turns(3) == 24,
                "resultados de cada fase preservados");
        require(run.objectives(1) == 3 && run.objectives(2) == 3 && run.objectives(3) == 3,
                "objetivos preservados");
        require(run.stablePairs(2) >= 6 && run.stablePairs(3) >= 6,
                "regularidade preservada");
        require(run.totalMovingSeconds() > 30f, "tempo de movimento preservado");
    }

    private static void irregularTurnsCanCauseDefeatWithoutTimeLimit() {
        CampaignProgress run = new CampaignProgress();
        run.first().addRotations(20);
        run.observe();
        run.advance();
        run.second().update(500f, false);
        run.observe();
        require(run.integrity() == 3, "tempo parado nao causa derrota");

        long deviceMillis = 1000;
        for (int turn = 1; turn <= 26; turn++) {
            deviceMillis += turn % 2 == 0 ? 300 : 2000;
            run.second().acceptTurns(1, deviceMillis);
            run.observe();
        }
        require(run.status() == CampaignProgress.Status.DEFEAT, "interferencia causa derrota");
        require(run.integrity() == 0 && run.turns(2) == 26, "desempenho mantido na derrota");
        require(run.turns(1) == 20, "fase anterior mantida na derrota");
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
