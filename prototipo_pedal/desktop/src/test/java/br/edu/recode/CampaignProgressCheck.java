package br.edu.recode;

/** Verificacao sem dependencias externas da progressao da campanha. */
public final class CampaignProgressCheck {
    private CampaignProgressCheck() { }

    public static void main(String[] args) {
        fullVictoryKeepsAllResults();
        irregularTurnsCanCauseDefeatWithoutTimeLimit();
        phaseTwoRequiresContinuousRhythm();
        phaseTwoBossAttacksEvenWithRegularRhythm();
        phaseThreeRequiresOrderedSequence();
        forearmFramesRequireSensorProtocol();
        sensorRecalibrationStartsFromNewBaseline();
        mascotShieldLastsFifteenPlayingSeconds();
        shieldBlocksThenAllowsTheNextHit();
        phaseThreeAttackReachesEloBeforeImpact();
        cameraAndEloFollowMovementWithoutJumping();
        fragmentsReactToEloProgress();
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

        for (int turn = 25; turn <= 54; turn++) {
            int direction = run.third().step() == PhaseThreeProgress.Step.SWITCH
                    ? run.third().requiredDirection() : turn % 2 == 0 ? 1 : -1;
            run.third().acceptTurns(1, turn * 1000L, direction);
            run.third().update(0.5f, true);
            run.observe();
        }
        require(run.status() == CampaignProgress.Status.VICTORY, "vitoria apos a fase 3");
        require(run.phase() == 3 && run.totalTurns() == 74, "total preservado");
        require(run.turns(1) == 20 && run.turns(2) == 24 && run.turns(3) == 30,
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
        int observedAttack = 0;
        for (int turn = 1; turn <= 26; turn++) {
            deviceMillis += turn % 2 == 0 ? 300 : 2000;
            run.second().acceptTurns(1, deviceMillis);
            run.observe();
            if (run.attackSequence() > observedAttack && !run.lastAttackBlocked())
                run.resolveHit();
            observedAttack = run.attackSequence();
        }
        require(run.status() == CampaignProgress.Status.DEFEAT, "interferencia causa derrota");
        require(run.integrity() == 0 && run.turns(2) == 20,
                "tres ataques sem protecao encerram a tentativa");
        require(run.turns(1) == 20, "fase anterior mantida na derrota");
    }

    private static void phaseTwoRequiresContinuousRhythm() {
        PhaseTwoProgress progress = new PhaseTwoProgress();
        long time = 1000;
        for (int turn = 0; turn < 7; turn++) {
            time += turn % 2 == 0 ? 1000 : 1800;
            progress.acceptTurns(1, time);
        }
        require(progress.nodes() == 0 && progress.stablePairs() == 0,
                "pares separados por irregularidade nao estabilizam");
        for (int turn = 0; turn < 4; turn++) {
            time += 1000;
            progress.acceptTurns(1, time);
        }
        require(progress.nodes() == 1, "sequencia continua estabiliza o no");
        progress.acceptTurns(1, time + 1000);
        progress.acceptTurns(1, time + 2000);
        progress.acceptTurns(1, time + 3000);
        require(progress.stablePairs() > 0, "sequencia em andamento antes da pausa");
        progress.togglePause();
        require(progress.stablePairs() == 0, "pausa reinicia sequencia regular");
    }

    private static void phaseTwoBossAttacksEvenWithRegularRhythm() {
        CampaignProgress run = new CampaignProgress();
        run.first().addRotations(20);
        run.observe();
        run.advance();
        for (int turn = 1; turn <= 6; turn++) {
            run.second().acceptTurns(1, turn * 1000L);
            run.observe();
        }
        require(run.attackSequence() == 1 && run.lastAttackBlocked() &&
                run.integrity() == 3 && run.pendingHits() == 0,
                "No Zero dispara no primeiro no e o ritmo regular protege ELO");
    }

    private static void phaseThreeRequiresOrderedSequence() {
        PhaseThreeProgress progress = new PhaseThreeProgress();
        progress.acceptTurns(3, 1000);
        require(progress.precisionTurns() == 0, "lote sem instantes nao prova precisao");
        for (int turn = 1; turn <= 3; turn++)
            progress.acceptTurns(1, turn * 1000L + 1000, turn % 2 == 0 ? 1 : -1);
        require(progress.step() == PhaseThreeProgress.Step.SWITCH &&
                progress.activeRoute() == 0 && progress.restoredConduits() == 0,
                "precisao leva a chave sem trocar de linha");
        progress.acceptTurns(2, 5000, -1);
        require(progress.step() == PhaseThreeProgress.Step.SWITCH &&
                progress.activeRoute() == 0, "giros em lote nao acionam a chave");
        progress.acceptTurns(1, 6000, 1);
        require(progress.step() == PhaseThreeProgress.Step.SWITCH &&
                progress.activeRoute() == 0, "giro para cima nao leva a linha inferior");
        progress.acceptTurns(1, 7000, -1);
        require(progress.step() == PhaseThreeProgress.Step.RHYTHM &&
                progress.activeRoute() == 1 && progress.restoredConduits() == 1,
                "giro para baixo aciona o desvio para a linha inferior");
        for (int turn = 8; turn <= 10; turn++) progress.acceptTurns(1, turn * 1000L, 1);
        require(progress.step() == PhaseThreeProgress.Step.STABILITY, "ritmo depois do desvio");
        progress.acceptTurns(1, 11000, -1);
        progress.acceptTurns(1, 11500, 1);
        require(progress.stabilityStreak() == 0, "irregularidade reinicia estabilidade");
        progress.acceptTurns(1, 12000, -1);
        progress.acceptTurns(1, 12500, 1);
        progress.acceptTurns(1, 13000, -1);
        require(progress.shields() == 1 && progress.step() == PhaseThreeProgress.Step.PRECISION &&
                progress.activeRoute() == 1 && progress.restoredConduits() == 2,
                "estabilidade conclui a sequencia e inicia proximo escudo");
        for (int turn = 14; turn <= 16; turn++)
            progress.acceptTurns(1, turn * 1000L, -1);
        require(progress.step() == PhaseThreeProgress.Step.SWITCH &&
                progress.requiredDirection() == 1 && progress.activeRoute() == 1,
                "segundo desvio pede giro para cima");
        progress.acceptTurns(1, 17000, 1);
        require(progress.activeRoute() == 0, "giro para cima leva a linha superior");
    }

    private static void forearmFramesRequireSensorProtocol() {
        String frame = "{\"v\":2,\"seq\":1,\"ms\":1000,\"mode\":\"forearm\"," +
                "\"movement\":1,\"lastMovementMs\":900,\"direction\":-1,\"state\":\"ok\"}";
        require(PedalFrame.parse(frame).direction == -1, "direcao vem do sensor");
        boolean rejected = false;
        try {
            PedalFrame.parse(frame.replace("\"forearm\"", "\"pedal\""));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "firmware antigo de pedal nao pode controlar o jogo");
    }

    private static void sensorRecalibrationStartsFromNewBaseline() {
        PedalCounter counter = new PedalCounter();
        require(counter.accept(frame(1, 1000, 4, 900), 1_000_000_000L) == 0,
                "primeiro quadro estabelece a base");
        require(counter.accept(frame(2, 1400, 5, 1300), 1_400_000_000L) == 1,
                "giro anterior contado");
        require(counter.accept(frame(3, 1600, 0, 0), 1_600_000_000L) == 0,
                "recalibracao nao cria giro fantasma");
        require(counter.accept(frame(4, 2000, 1, 1950), 2_000_000_000L) == 1,
                "jogo volta a contar apos recalibracao");
    }

    private static PedalFrame frame(long sequence, long ms, long movement, long lastMovementMs) {
        return PedalFrame.parse("{\"v\":2,\"seq\":" + sequence + ",\"ms\":" + ms +
                ",\"mode\":\"forearm\",\"movement\":" + movement +
                ",\"lastMovementMs\":" + lastMovementMs +
                ",\"direction\":1,\"state\":\"ok\"}");
    }

    private static void mascotShieldLastsFifteenPlayingSeconds() {
        MascotShield shield = new MascotShield();
        for (int turn = 1; turn <= 5; turn++) shield.acceptTurns(1, turn * 1000L);
        require(shield.active() && shield.activations() == 1, "ritmo carrega escudo");
        shield.update(7f, true);
        shield.update(30f, false);
        require(shield.active() && shield.remainingSeconds() == 8f,
                "pausa congela a duracao do escudo");
        require(shield.blockAttack() && shield.blockedAttacks() == 1,
                "escudo bloqueia ataque");
        shield.update(8f, true);
        require(!shield.active() && !shield.blockAttack(), "escudo expira apos 15 segundos");
    }

    private static void shieldBlocksThenAllowsTheNextHit() {
        CampaignProgress run = new CampaignProgress();
        run.first().addRotations(20);
        run.observe();
        run.advance();
        for (int turn = 1; turn <= 5; turn++)
            run.mascotShield().acceptTurns(1, turn * 1000L);

        long time = 1000;
        for (int turn = 1; turn <= 6; turn++) {
            time += turn % 2 == 0 ? 300 : 2000;
            run.second().acceptTurns(1, time);
            run.observe();
        }
        require(run.integrity() == 3 && run.attackSequence() == 1 &&
                run.lastAttackBlocked(), "primeiro ataque bloqueado");
        run.mascotShield().update(15f, true);
        for (int turn = 7; turn <= 14; turn++) {
            time += turn % 2 == 0 ? 300 : 2000;
            run.second().acceptTurns(1, time);
            run.observe();
        }
        require(run.integrity() == 3 && run.pendingHits() == 1 &&
                run.attackSequence() == 2 && !run.lastAttackBlocked(),
                "ataque aguarda a animacao chegar a ELO");
        run.resolveHit();
        require(run.integrity() == 2 && run.attackSequence() == 2 &&
                !run.lastAttackBlocked(), "impacto apos expiracao reduz integridade");
    }

    private static void cameraAndEloFollowMovementWithoutJumping() {
        StageMotion motion = new StageMotion();
        motion.update(100f, 0f, 1f / 60f, false);
        motion.update(300f, 1f, 1f / 60f, false);
        require(motion.x() > 100f && motion.x() < 300f &&
                motion.journey() > 0f && motion.journey() < 1f && motion.moving(),
                "ELO e camera acompanham o gesto sem salto visual");
        float pausedX = motion.x();
        float pausedJourney = motion.journey();
        motion.update(300f, 1f, 1f, true);
        require(motion.x() == pausedX && motion.journey() == pausedJourney,
                "pausa congela ELO e camera");
        for (int i = 0; i < 120; i++) motion.update(300f, 1f, 1f / 60f, false);
        require(Math.abs(motion.x() - 300f) < 0.3f &&
                Math.abs(motion.journey() - 1f) < 0.001f,
                "ELO e camera chegam ao destino");
    }

    private static void fragmentsReactToEloProgress() {
        EnergyFragments fragments = new EnergyFragments();
        fragments.update(0.24f, 0f);
        require(fragments.collected() == 0, "fragmento aguarda ELO");
        fragments.update(0.75f, 0f);
        require(fragments.collected() == 3, "ELO recolhe objetos do cenario");
        fragments.update(1f, 1f);
        require(fragments.collected() == 3, "fragmentos nao duplicam");
    }

    private static void phaseThreeAttackReachesEloBeforeImpact() {
        CombatVisuals visuals = new CombatVisuals();
        visuals.launchHit();
        visuals.update(0.40f);
        require(visuals.attackFlying() && !visuals.hitActive() &&
                visuals.attackFlightProgress() > 0.5f,
                "disparo percorre a cena antes de atingir ELO");
        visuals.update(0.34f);
        require(!visuals.attackFlying() && visuals.hitActive() &&
                visuals.impactBeamStrength() > 0f,
                "impacto e raio aparecem juntos no destino");
        visuals.update(0.30f);
        require(visuals.impactBeamStrength() == 0f,
                "raio desaparece depois do impacto");

        visuals.launchBlocked();
        require(visuals.attackBlocked() && visuals.attackFlying(),
                "escudo intercepta o proximo disparo");
        visuals.update(0.74f);
        require(visuals.blockedActive() && !visuals.hitActive(),
                "bloqueio aparece quando o disparo chega ao escudo");
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
