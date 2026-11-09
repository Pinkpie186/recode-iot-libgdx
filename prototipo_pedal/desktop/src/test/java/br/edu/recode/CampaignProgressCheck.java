package br.edu.recode;

/** Verificacao sem dependencias externas da progressao da campanha. */
public final class CampaignProgressCheck {
    private CampaignProgressCheck() { }

    public static void main(String[] args) {
        fullVictoryKeepsAllResults();
        phaseThreeRestoresAllHearts();
        irregularTurnsCanCauseDefeat();
        phaseTwoRequiresContinuousRhythm();
        phaseTwoNeverStopsAtSecondTotem();
        phaseTwoBossAttacksEvenWithRegularRhythm();
        interferenceEndsAfterThreeSeconds();
        idleBossAttacksInPhaseTwoAndRespectsRest();
        idleBossAttacksInPhaseThreeAndShieldBlocks();
        phaseThreeRequiresOrderedSequence();
        forearmFramesRequireSensorProtocol();
        sensorRecalibrationStartsFromNewBaseline();
        sensorLossRequiresFreshBaseline();
        signalStatusDoesNotHideSensorError();
        mascotShieldLastsFifteenPlayingSeconds();
        shieldBlocksThenAllowsTheNextHit();
        phaseThreeAttackReachesEloBeforeImpact();
        bossAttackAnimationFollowsTheShot();
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

        for (int turn = 25; turn <= 51; turn++) {
            run.third().acceptTurns(1, turn * 1000L, turn % 2 == 0 ? 1 : -1);
            run.third().update(0.5f, true);
            run.observe();
        }
        require(run.status() == CampaignProgress.Status.VICTORY, "vitoria apos a fase 3");
        require(run.phase() == 3 && run.totalTurns() == 71, "total preservado");
        require(run.turns(1) == 20 && run.turns(2) == 24 && run.turns(3) == 27,
                "resultados de cada fase preservados");
        require(run.objectives(1) == 3 && run.objectives(2) == 3 && run.objectives(3) == 3,
                "objetivos preservados");
        require(run.stablePairs(2) >= 6 && run.stablePairs(3) >= 6,
                "regularidade preservada");
        require(run.totalMovingSeconds() > 30f, "tempo de movimento preservado");
    }

    private static void irregularTurnsCanCauseDefeat() {
        CampaignProgress run = new CampaignProgress();
        run.first().addRotations(20);
        run.observe();
        run.advance();
        long deviceMillis = 1000;
        int observedAttack = 0;
        for (int turn = 1; turn <= 36; turn++) {
            deviceMillis += turn % 2 == 0 ? 300 : 2000;
            run.second().acceptTurns(1, deviceMillis);
            run.observe();
            if (run.attackSequence() > observedAttack && !run.lastAttackBlocked())
                run.resolveHit();
            observedAttack = run.attackSequence();
            if (run.status() == CampaignProgress.Status.DEFEAT) break;
        }
        require(run.status() == CampaignProgress.Status.DEFEAT, "interferencia causa derrota");
        require(run.integrity() == 0 && run.turns(2) >= 24,
                "tres ataques sem protecao encerram a tentativa");
        require(run.turns(1) == 20, "fase anterior mantida na derrota");
    }

    private static void phaseThreeRestoresAllHearts() {
        CampaignProgress run = new CampaignProgress();
        run.first().addRotations(20);
        run.observe();
        run.advance();
        for (int i = 0; i < 2; i++) {
            run.updateBossIdle(0f, true, false);
            run.updateBossIdle(BossIdleClock.PHASE_TWO_SECONDS + 0.1f, true, false);
            run.resolveHit();
        }
        require(run.integrity() == 1, "fase 2 registra vidas perdidas");
        run.second().acceptTurns(36, 1000);
        run.observe();
        require(run.status() == CampaignProgress.Status.BETWEEN_PHASES &&
                run.turns(2) == 36, "resultado da fase 2 permanece salvo");
        run.advance();
        require(run.phase() == 3 && run.integrity() == 3 && run.pendingHits() == 0,
                "fase 3 inicia com tres coracoes e sem dano pendente");
        require(run.turns(2) == 36, "restaurar vidas preserva desempenho anterior");
        run.updateBossIdle(0f, true, false);
        run.updateBossIdle(BossIdleClock.PHASE_THREE_SECONDS + 0.1f, true, false);
        run.resolveHit();
        require(run.integrity() == 2, "ataques da fase 3 ainda reduzem vidas");
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

    private static void phaseTwoNeverStopsAtSecondTotem() {
        PhaseTwoProgress progress = new PhaseTwoProgress();
        long time = 1000;
        for (int turn = 0; turn < 24; turn++) {
            time += turn % 2 == 0 ? 1000 : 1800;
            progress.acceptTurns(1, time);
        }
        require(progress.nodes() == 2 && progress.nodeTurns() == 0,
                "ritmo irregular nao prende ELO no segundo totem");
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

    private static void interferenceEndsAfterThreeSeconds() {
        PhaseTwoProgress second = new PhaseTwoProgress();
        second.acceptTurns(1, 1000);
        second.acceptTurns(1, 2000);
        require(second.interferenceActive(), "fase 2 inicia interferencia no totem");
        second.update(1.5f, false);
        second.togglePause();
        second.update(10f, false);
        require(second.interferenceActive(), "pausa conserva o tempo da interferencia");
        second.togglePause();
        second.update(1.49f, false);
        require(second.interferenceActive(), "interferencia dura ate tres segundos de jogo");
        second.update(0.02f, false);
        require(!second.interferenceActive(), "interferencia da fase 2 termina em tres segundos");
        second.acceptTurns(4, 3000);
        require(!second.interferenceActive(), "interferencia nao reinicia no mesmo totem");
        second.acceptTurns(6, 4000);
        second.acceptTurns(1, 5000);
        second.acceptTurns(1, 6000);
        require(second.interferenceActive(), "proximo totem recebe sua propria janela");

        PhaseThreeProgress third = new PhaseThreeProgress();
        third.acceptTurns(2, 1000);
        require(third.interferenceActive(), "fase 3 inicia interferencia na estacao");
        third.update(3f, false);
        require(!third.interferenceActive(), "interferencia da fase 3 termina em tres segundos");
        third.acceptTurns(1, 2000);
        require(!third.interferenceActive(), "fase 3 nao reinicia na mesma estacao");

        CampaignProgress run = new CampaignProgress();
        run.first().addRotations(20);
        run.observe();
        run.advance();
        run.second().acceptTurns(2, 1000);
        run.observe();
        run.second().update(3.1f, false);
        run.second().acceptTurns(4, 2000);
        run.observe();
        require(run.attackSequence() == 0,
                "interferencia encerrada nao dispara depois de tres segundos");
    }

    private static void idleBossAttacksInPhaseTwoAndRespectsRest() {
        CampaignProgress run = new CampaignProgress();
        run.first().addRotations(20);
        run.observe();
        run.advance();
        run.updateBossIdle(0f, true, false);
        run.updateBossIdle(5.2f, true, false);
        require(run.bossIdleWarning() > 0f && run.attackSequence() == 0,
                "aviso aparece antes do disparo sem giros");
        run.updateBossIdle(1.9f, true, false);
        require(run.attackSequence() == 1 && run.pendingHits() == 1,
                "No Zero ataca ELO parado na fase 2");
        run.resolveHit();
        require(run.integrity() == 2, "disparo parado remove um coracao apos impacto");

        run.updateBossIdle(3f, true, false);
        run.second().acceptTurns(1, 1000L);
        run.observe();
        run.updateBossIdle(BossIdleClock.PHASE_TWO_SECONDS, true, false);
        require(run.attackSequence() == 1, "giro valido reinicia o preparo do chefe");
        run.updateBossIdle(4f, true, false);
        run.updateBossIdle(0f, false, false);
        require(run.bossIdleWarning() == 0f, "pausa ou falta de sinal limpa o aviso");
        run.updateBossIdle(4f, true, false);
        require(run.attackSequence() == 1, "retorno da pausa concede novo intervalo");
    }

    private static void idleBossAttacksInPhaseThreeAndShieldBlocks() {
        CampaignProgress run = new CampaignProgress();
        run.first().addRotations(20);
        run.observe();
        run.advance();
        for (int turn = 1; turn <= 24; turn++) {
            run.second().acceptTurns(1, turn * 1000L);
            run.observe();
        }
        require(run.status() == CampaignProgress.Status.BETWEEN_PHASES,
                "fase 2 concluida antes do teste final");
        run.advance();
        int previousAttacks = run.attackSequence();
        for (int turn = 1; turn <= 5; turn++)
            run.mascotShield().acceptTurns(1, turn * 1000L);
        run.updateBossIdle(0f, true, false);
        run.updateBossIdle(BossIdleClock.PHASE_THREE_SECONDS + 0.1f, true, false);
        require(run.attackSequence() == previousAttacks + 1 &&
                run.lastAttackBlocked() && run.pendingHits() == 0 && run.integrity() == 3,
                "escudo bloqueia ataque do chefe contra ELO parado na fase 3");
        run.mascotShield().update(MascotShield.DURATION_SECONDS, true);
        run.updateBossIdle(0f, true, true);
        run.updateBossIdle(BossIdleClock.PHASE_THREE_SECONDS + 0.1f, true, false);
        require(run.attackSequence() == previousAttacks + 2 && run.pendingHits() == 1,
                "apos expirar o escudo, novo disparo chega a ELO");
        run.resolveHit();
        require(run.integrity() == 2, "fase 3 perde um coracao por impacto real");
    }

    private static void phaseThreeRequiresOrderedSequence() {
        PhaseThreeProgress progress = new PhaseThreeProgress();
        progress.acceptTurns(3, 1000);
        require(progress.precisionTurns() == 0, "lote sem instantes nao prova precisao");
        for (int turn = 1; turn <= 3; turn++)
            progress.acceptTurns(1, turn * 1000L + 1000, turn % 2 == 0 ? 1 : -1);
        require(progress.step() == PhaseThreeProgress.Step.RHYTHM &&
                progress.activeRoute() == 1 && progress.restoredConduits() == 1,
                "terceiro giro muda automaticamente para a linha inferior");
        for (int turn = 5; turn <= 7; turn++) progress.acceptTurns(1, turn * 1000L, 1);
        require(progress.step() == PhaseThreeProgress.Step.STABILITY, "ritmo depois do desvio");
        progress.acceptTurns(1, 8000, -1);
        progress.acceptTurns(1, 8500, 1);
        require(progress.stabilityStreak() == 0, "irregularidade reinicia estabilidade");
        progress.acceptTurns(1, 9000, -1);
        progress.acceptTurns(1, 9500, 1);
        progress.acceptTurns(1, 10000, -1);
        require(progress.shields() == 1 && progress.step() == PhaseThreeProgress.Step.PRECISION &&
                progress.activeRoute() == 1 && progress.restoredConduits() == 2,
                "estabilidade conclui a sequencia e inicia proximo escudo");
        for (int turn = 11; turn <= 13; turn++)
            progress.acceptTurns(1, turn * 1000L, -1);
        require(progress.step() == PhaseThreeProgress.Step.RHYTHM &&
                progress.activeRoute() == 0,
                "segundo conjunto de tres giros muda de volta para a linha superior");
    }

    private static void forearmFramesRequireSensorProtocol() {
        String frame = "{\"v\":2,\"seq\":1,\"ms\":1000,\"mode\":\"forearm\"," +
                "\"movement\":1,\"lastMovementMs\":900,\"direction\":-1,\"state\":\"ok\"}";
        require(PedalFrame.parse(frame).direction == -1, "direcao vem do sensor");
        require(Float.isNaN(PedalFrame.parse(frame).angleDeg),
                "quadro antigo sem inclinacao continua aceito");
        require(PedalFrame.parse(frame.replace("\"state\"", "\"angleDeg\":-31.5,\"state\"")).angleDeg
                == -31.5f, "inclinacao chega ao gesto da estacao");
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

    private static void sensorLossRequiresFreshBaseline() {
        PedalCounter counter = new PedalCounter();
        counter.accept(frame(10, 1000, 4, 900), 1_000_000_000L);
        require(counter.accept(frame(11, 1400, 5, 1300), 1_400_000_000L) == 1,
                "gesto antes da falha recebido");
        counter.invalidateSignal();
        require(counter.accept(frame(20, 2400, 6, 2300), 2_400_000_000L) == 0,
                "primeiro quadro apos perda de sinal estabelece nova base");
        require(counter.accept(frame(21, 2800, 7, 2700), 2_800_000_000L) == 1,
                "gesto seguinte volta a contar");
        String broken = "{\"v\":2,\"seq\":22,\"ms\":2900,\"mode\":\"forearm\"," +
                "\"movement\":7,\"lastMovementMs\":2700,\"direction\":1," +
                "\"state\":\"sensor_error\"}";
        try {
            PedalFrame.parse(broken);
            throw new AssertionError("sensor_error nao pode pontuar");
        } catch (IllegalArgumentException expected) {
            require(expected.getMessage().startsWith("MPU6050 sem sinal"),
                    "falha do sensor deve orientar conferência da montagem");
        }
    }

    private static void signalStatusDoesNotHideSensorError() {
        final long[] last = {1_000_000_000L};
        final String[] error = {""};
        PedalSource source = new PedalSource() {
            @Override public void start() { }
            @Override public PedalFrame poll() { return null; }
            @Override public long lastFrameNanos() { return last[0]; }
            @Override public String error() { return error[0]; }
            @Override public void close() { }
        };
        require(source.signalAvailable(1_100_000_000L), "quadro recente indica sinal ativo");
        error[0] = "MPU6050 sem sinal: confira solda e fios";
        require(!source.signalAvailable(1_100_000_000L),
                "erro do sensor interrompe o jogo imediatamente");
        error[0] = "";
        require(!source.signalAvailable(4_000_000_000L),
                "quadro antigo nao deve indicar conexao ativa");
        last[0] = 4_100_000_000L;
        require(source.signalAvailable(4_200_000_000L),
                "sinal restaurado permite retomar a campanha");
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
        for (int turn = 7; turn <= 18; turn++) {
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

    private static void bossAttackAnimationFollowsTheShot() {
        CombatVisuals visuals = new CombatVisuals();
        require(visuals.attackPoseFrame() == -1, "chefe em repouso antes do ataque");
        visuals.launchHit();
        require(visuals.attackPoseFrame() == 0, "chefe inicia a carga ao disparar");
        visuals.update(0.19f);
        require(visuals.attackPoseFrame() == 1, "energia cresce durante o voo");
        visuals.update(0.19f);
        require(visuals.attackPoseFrame() == 2 && visuals.attackPosePulse() > 0f,
                "quadro de disparo acompanha o projetil");
        visuals.update(0.19f);
        require(visuals.attackPoseFrame() == 3, "chefe recua antes do impacto");
        visuals.update(0.16f);
        require(visuals.attackPoseFrame() == -1 && visuals.hitActive(),
                "animacao termina quando o disparo atinge ELO");
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
