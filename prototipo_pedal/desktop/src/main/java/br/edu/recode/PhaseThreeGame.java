package br.edu.recode;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Sistema Central: ELO repara rotas e estabiliza os escudos de No Zero. */
final class PhaseThreeGame extends ApplicationAdapter {
    private static final float BOSS_REVEAL_SECONDS = 2.2f;
    private static final Color BACKGROUND = new Color(0.025f, 0.025f, 0.075f, 1f);
    private static final Color PANEL = new Color(0.035f, 0.045f, 0.12f, 0.94f);
    private static final Color TRACK = new Color(0.15f, 0.18f, 0.32f, 1f);
    private static final Color ENERGY = new Color(0.13f, 0.82f, 0.97f, 1f);
    private static final Color RESTORED = new Color(0.30f, 0.91f, 0.68f, 1f);
    private static final Color CORRUPTION = new Color(0.84f, 0.22f, 0.74f, 1f);
    private static final Color MUTED = new Color(0.74f, 0.79f, 0.91f, 1f);

    private final PedalSource source;
    private final boolean simulated;
    private final PedalCounter counter;
    private final PhaseThreeProgress progress;
    private final boolean manageSource;
    private final MascotShield shield;
    private final CombatVisuals combat;
    private final EnergyFragments fragments;
    private final StageMotion motion = new StageMotion();
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private ScenarioBackground background;
    private CharacterSpriteSheet elo;
    private CharacterSpriteSheet mascot;
    private CharacterSpriteSheet noZero;
    private float animationTime;
    private float energyPulse;
    private float shieldPulse;
    private float routePulse;
    private float wrongRoutePulse;
    private float bossRevealSeconds;
    private float displayRouteY = Float.NaN;
    private boolean campaignLocked;

    void setCampaignLocked(boolean locked) { campaignLocked = locked; }

    PhaseThreeGame(PedalSource source, boolean simulated) {
        this(source, simulated, new PedalCounter(), new PhaseThreeProgress(), true,
                new MascotShield(), new CombatVisuals(), new EnergyFragments());
    }

    PhaseThreeGame(PedalSource source, boolean simulated, PedalCounter counter,
                   PhaseThreeProgress progress, boolean manageSource) {
        this(source, simulated, counter, progress, manageSource,
                new MascotShield(), new CombatVisuals(), new EnergyFragments());
    }

    PhaseThreeGame(PedalSource source, boolean simulated, PedalCounter counter,
                   PhaseThreeProgress progress, boolean manageSource,
                   MascotShield shield, CombatVisuals combat) {
        this(source, simulated, counter, progress, manageSource,
                shield, combat, new EnergyFragments());
    }

    PhaseThreeGame(PedalSource source, boolean simulated, PedalCounter counter,
                   PhaseThreeProgress progress, boolean manageSource,
                   MascotShield shield, CombatVisuals combat, EnergyFragments fragments) {
        this.source = source;
        this.simulated = simulated;
        this.counter = counter;
        this.progress = progress;
        this.manageSource = manageSource;
        this.shield = shield;
        this.combat = combat;
        this.fragments = fragments;
    }

    @Override public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        background = new ScenarioBackground("scenarios/sistema_central.png");
        elo = new CharacterSpriteSheet("sprites/elo.png");
        mascot = new CharacterSpriteSheet("sprites/mascote.png");
        noZero = new CharacterSpriteSheet("sprites/no_zero.png");
        if (manageSource) source.start();
    }

    @Override public void render() {
        long now = System.nanoTime();
        if (manageSource && !campaignLocked && (Gdx.input.isKeyJustPressed(Input.Keys.P) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) progress.togglePause();
        if (manageSource && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            progress.restart();
            energyPulse = shieldPulse = routePulse = wrongRoutePulse = 0f;
            displayRouteY = Float.NaN;
            motion.reset();
            bossRevealSeconds = 0f;
        }

        PedalFrame frame;
        while ((frame = source.poll()) != null) {
            int turns = counter.accept(frame, now);
            if (turns > 0 && !campaignLocked && !progress.isPaused() && !progress.isComplete()) {
                int routeBefore = progress.activeRoute();
                boolean atSwitch = progress.step() == PhaseThreeProgress.Step.SWITCH;
                int restored = progress.acceptTurns(turns, frame.lastCycleMillis, frame.direction);
                if (shield.acceptTurns(turns, frame.lastCycleMillis)) combat.shieldStarted();
                energyPulse = 1f;
                if (restored > 0) shieldPulse = 1f;
                if (!progress.isComplete() && progress.activeRoute() != routeBefore) routePulse = 1f;
                if (atSwitch && progress.step() == PhaseThreeProgress.Step.SWITCH)
                    wrongRoutePulse = 1.4f;
                else if (progress.activeRoute() != routeBefore) wrongRoutePulse = 0f;
            }
        }
        float delta = Gdx.graphics.getDeltaTime();
        boolean connected = source.lastFrameNanos() >= 0 &&
                now - source.lastFrameNanos() <= 2_000_000_000L;
        if (!campaignLocked) progress.update(delta, connected && counter.rpm(now) > 0);
        if (manageSource) shield.update(delta, !progress.isPaused() && !progress.isComplete());
        if (!progress.isPaused()) combat.update(delta);
        if (!progress.isPaused()) animationTime += delta;
        if (!campaignLocked && !progress.isPaused() && !progress.isComplete())
            bossRevealSeconds = Math.min(BOSS_REVEAL_SECONDS, bossRevealSeconds + delta);
        energyPulse = Math.max(0f, energyPulse - delta * 2.4f);
        shieldPulse = Math.max(0f, shieldPulse - delta * 1.15f);
        routePulse = Math.max(0f, routePulse - delta * 1.15f);
        wrongRoutePulse = Math.max(0f, wrongRoutePulse - delta);

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float left = width * 0.11f;
        float right = width * 0.65f;
        float centerY = height * 0.50f;
        float upperY = centerY + 47f;
        float lowerY = centerY - 47f;
        float segment = (right - left) / PhaseThreeProgress.SHIELD_COUNT;
        float stepFraction = progress.step() == PhaseThreeProgress.Step.PRECISION
                ? progress.precisionTurns() / (float) PhaseThreeProgress.PRECISION_TURNS / 3f
                : progress.step() == PhaseThreeProgress.Step.SWITCH
                ? 1f / 3f
                : progress.step() == PhaseThreeProgress.Step.RHYTHM
                ? (1f + progress.rhythmStreak() / (float) PhaseThreeProgress.RHYTHM_PAIRS) / 3f
                : (2f + progress.stabilityStreak() /
                (float) PhaseThreeProgress.STABILITY_PAIRS) / 3f;
        float fraction = progress.shields() + Math.min(1f, stepFraction);
        float targetX = left + segment * fraction;
        float targetRouteY = progress.activeRoute() == 0 ? upperY : lowerY;
        if (Float.isNaN(displayRouteY)) displayRouteY = targetRouteY;
        if (!progress.isPaused()) displayRouteY += (targetRouteY - displayRouteY) *
                (1f - (float) Math.exp(-9f * Math.min(delta, 0.05f)));
        if (Math.abs(displayRouteY - targetRouteY) < 0.5f) displayRouteY = targetRouteY;
        float eloY = displayRouteY;
        float targetJourney = Math.min(1f, fraction / PhaseThreeProgress.SHIELD_COUNT);
        motion.update(targetX, targetJourney, delta, progress.isPaused());
        float eloX = motion.x();
        float journey = motion.journey();
        // O chefe domina o horizonte e acompanha a camera mais devagar que a trilha.
        float bossX = width * (0.69f - journey * 0.10f);
        float bossSize = Math.min(width * 0.48f, height * 0.68f);
        float portalY = height * 0.24f;
        float reveal = Math.min(1f, bossRevealSeconds / BOSS_REVEAL_SECONDS);
        float rise = reveal * reveal * (3f - 2f * reveal);
        float finalBossY = portalY + bossSize * 0.50f +
                (float) Math.sin(VisualComfort.ambientTime(animationTime) * 0.65f) * 3f;
        float bossY = portalY - bossSize * 0.55f +
                (finalBossY - (portalY - bossSize * 0.55f)) * rise;
        // O disparo parte de um dos focos laterais do chefe, visivel atras da arena.
        float mouthX = bossX + (eloX < bossX ? 1f : -1f) * bossSize * 0.27f;
        float mouthY = bossY + bossSize * 0.15f;
        fragments.update(journey, delta);

        Gdx.gl.glClearColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        background.draw(batch, width, height, journey, animationTime);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        StageDecor.draw(shapes, width, height, journey, animationTime, energyPulse, 3);
        if (!progress.isComplete()) {
            drawCircuitRift(bossX, portalY, bossSize, reveal);
            drawBossBacklight(bossX, bossY, bossSize);
        }
        batch.begin();
        if (!progress.isComplete()) {
            batch.setColor(Color.WHITE);
            int enemyRow = shieldPulse > 0 ? 3 :
                    combat.attackFlying() || combat.hitActive() ||
                    combat.blockedActive() || progress.interferenceActive()
                            ? 1 : 0;
            if (reveal < 1f) {
                int clipY = Math.round(portalY * Gdx.graphics.getBackBufferHeight() / height);
                Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
                Gdx.gl.glScissor(0, clipY, Gdx.graphics.getBackBufferWidth(),
                        Gdx.graphics.getBackBufferHeight() - clipY);
            }
            noZero.draw(batch, enemyRow, animationTime, bossX, bossY, bossSize);
            batch.flush();
            if (reveal < 1f) Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
            batch.setColor(Color.WHITE);
        }
        batch.end();
        fragments.draw(shapes, left, right, centerY + 105f, animationTime);
        drawWorld(width, height, left, right, upperY, lowerY, segment,
                eloX, eloY, mouthX, mouthY);
        drawSpritesAndText(width, height, left, right, upperY, lowerY, segment,
                eloX, eloY, bossX, connected, now);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawCircuitRift(float x, float y, float bossSize, float reveal) {
        float ambient = VisualComfort.ambientTime(animationTime);
        float opening = Math.min(1f, 0.16f + reveal * 2.2f);
        float width = bossSize * 1.24f * opening;
        float height = bossSize * 0.30f * opening;
        float glow = 0.72f + 0.28f * (float) Math.sin(ambient * 4f);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(CORRUPTION.r, CORRUPTION.g, CORRUPTION.b, 0.16f * glow);
        shapes.ellipse(x - width * 0.62f, y - height * 0.72f,
                width * 1.24f, height * 1.44f);
        shapes.setColor(CORRUPTION.r, 0.22f, 0.88f, 0.72f * glow);
        shapes.ellipse(x - width * 0.5f, y - height * 0.5f, width, height);
        shapes.setColor(0.025f, 0.008f, 0.055f, 0.97f);
        shapes.ellipse(x - width * 0.46f, y - height * 0.35f,
                width * 0.92f, height * 0.70f);
        for (int side = -1; side <= 1; side += 2) {
            float edge = x + side * width * 0.46f;
            for (int i = 0; i < 3; i++) {
                float startY = y + (i - 1) * height * 0.20f;
                float tipX = edge + side * (36f + i * 20f) * opening;
                float tipY = startY + (i - 1) * 19f;
                shapes.setColor(CORRUPTION.r, 0.35f, 0.92f, 0.56f * glow);
                shapes.rectLine(edge, startY, tipX, startY, 3f);
                shapes.rectLine(tipX, startY, tipX, tipY, 3f);
                shapes.setColor(ENERGY.r, ENERGY.g, ENERGY.b, 0.78f * glow);
                shapes.circle(tipX, tipY, 3f);
            }
        }
        shapes.end();
    }

    private void drawBossBacklight(float bossX, float bossY, float bossSize) {
        float ambientTime = VisualComfort.ambientTime(animationTime);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float pulse = 0.5f + (float) Math.sin(ambientTime * 2.2f) * 0.5f;
        shapes.setColor(CORRUPTION.r, CORRUPTION.g, CORRUPTION.b, 0.07f + pulse * 0.04f);
        shapes.circle(bossX, bossY + bossSize * 0.06f, bossSize * 0.59f);
        shapes.setColor(CORRUPTION.r, 0.15f, 0.28f, 0.10f + pulse * 0.04f);
        shapes.circle(bossX, bossY + bossSize * 0.03f, bossSize * 0.44f);
        for (int i = 0; i < 8; i++) {
            float drift = (float) Math.sin(ambientTime * 0.5f + i * 1.7f) * 12f;
            float x = bossX + (i - 3.5f) * bossSize * 0.15f + drift;
            float y = bossY - bossSize * 0.30f + (i % 3) * bossSize * 0.08f;
            shapes.setColor(CORRUPTION.r, 0.24f, 0.47f, 0.10f);
            shapes.circle(x, y, bossSize * (0.10f + (i % 2) * 0.03f));
        }
        shapes.end();
    }

    private void drawWorld(int width, int height, float left, float right,
                           float upperY, float lowerY, float segment,
                           float eloX, float eloY, float mouthX, float mouthY) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PANEL);
        shapes.rect(0, height - 105, width, 105);
        shapes.rect(0, 0, width, 109);
        float meterX = width * 0.72f;
        float meterY = height - 52f;
        float meterWidth = width * 0.19f;
        shapes.setColor(TRACK);
        shapes.rect(meterX - 3f, meterY - 3f, meterWidth + 6f, 17f);
        for (int i = 0; i < PhaseThreeProgress.SHIELD_COUNT; i++) {
            shapes.setColor(i < progress.shields() ? RESTORED : CORRUPTION);
            float piece = meterWidth / PhaseThreeProgress.SHIELD_COUNT;
            shapes.rect(meterX + i * piece + 2f, meterY,
                    piece - 4f, 11f);
        }

        shapes.setColor(TRACK);
        shapes.rect(left, upperY - 6, right - left, 12);
        shapes.rect(left, lowerY - 6, right - left, 12);
        shapes.setColor(ENERGY);
        shapes.rect(left, (progress.activeRoute() == 0 ? upperY : lowerY) - 4,
                Math.max(0f, eloX - left), 8);

        if (!progress.isComplete()) drawRouteSwitch(left, segment, upperY, lowerY);

        for (int i = 0; i < PhaseThreeProgress.SHIELD_COUNT; i++) {
            float x = left + segment * (i + 1);
            boolean restored = i < progress.shields();
            for (int route = 0; route < 2; route++) {
                float y = route == 0 ? upperY : lowerY;
                boolean active = i == progress.shields() && route == progress.activeRoute();
                shapes.setColor(restored ? RESTORED : active ? ENERGY : TRACK);
                shapes.circle(x, y, 18);
                shapes.setColor(BACKGROUND);
                shapes.circle(x, y, 11);
                shapes.setColor(restored ? RESTORED : active ? ENERGY : TRACK);
                shapes.circle(x, y, 5);
            }
            if (restored && shieldPulse > 0f && i == progress.shields() - 1) {
                shapes.setColor(RESTORED.r, RESTORED.g, RESTORED.b, shieldPulse * 0.30f);
                shapes.circle(x, (upperY + lowerY) / 2f, 38 + (1f - shieldPulse) * 24);
            }
        }

        shapes.setColor(ENERGY.r, ENERGY.g, ENERGY.b, 0.17f + energyPulse * 0.20f);
        shapes.circle(eloX, eloY, 30 + energyPulse * 11);
        combat.drawProjectile(shapes, mouthX, mouthY,
                eloX, eloY + 12f, animationTime);
        combat.draw(shapes, eloX, eloY + 12f, animationTime, shield);
        if (routePulse > 0f && !progress.isComplete()) {
            shapes.setColor(ENERGY.r, ENERGY.g, ENERGY.b, routePulse * 0.32f);
            shapes.rect(eloX - 24, lowerY, 48, upperY - lowerY);
        }
        if (progress.interferenceActive() && !progress.isPaused()) {
            shapes.setColor(CORRUPTION.r, CORRUPTION.g, CORRUPTION.b, 0.22f);
            for (int i = 1; i < 8; i++) {
                float part = i / 8f;
                shapes.circle(mouthX + (eloX - mouthX) * part,
                        mouthY + (eloY + 12f - mouthY) * part, 2.5f);
            }
            shapes.circle(eloX, eloY + 12f, 7f);
        }

        if (progress.isPaused() || progress.isComplete()) {
            shapes.setColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 0.95f);
            shapes.rect(width * 0.16f, height * 0.26f, width * 0.68f, height * 0.51f);
            shapes.setColor(progress.isComplete() ? RESTORED : ENERGY);
            shapes.rect(width * 0.16f, height * 0.76f, width * 0.68f, 4);
        }
        shapes.end();
    }

    private void drawRouteSwitch(float left, float segment, float upperY, float lowerY) {
        float x = left + segment * (progress.shields() + 1f / 3f);
        boolean waiting = progress.step() == PhaseThreeProgress.Step.SWITCH;
        float pulse = 0.5f + 0.5f * (float) Math.sin(animationTime * 5f);
        shapes.setColor(waiting ? ENERGY : TRACK);
        shapes.rectLine(x, upperY, x, lowerY, waiting ? 9f : 5f);
        shapes.circle(x, upperY, waiting ? 12f + pulse * 4f : 8f);
        shapes.circle(x, lowerY, waiting ? 12f + pulse * 4f : 8f);
        float blockedY = progress.shields() % 2 == 0 ? upperY : lowerY;
        shapes.setColor(CORRUPTION);
        shapes.rectLine(x + 18f, blockedY - 15f, x + 18f, blockedY + 15f, 5f);
        if (waiting) {
            shapes.setColor(ENERGY.r, ENERGY.g, ENERGY.b, 0.18f + pulse * 0.25f);
            shapes.circle(x, blockedY, 23f + pulse * 8f);
        }
    }

    private void drawSpritesAndText(int width, int height, float left, float right,
                                    float upperY, float lowerY, float segment,
                                    float eloX, float eloY, float bossX,
                                    boolean connected, long now) {
        String status = connected ? simulated ? "Dados simulados" : "ESP32 conectado" :
                source.error().isEmpty() ? simulated ? "Iniciando simulacao" :
                        "Aguardando dados do ESP32" : source.error();
        batch.begin();
        if (!progress.isPaused() && !progress.isComplete()) {
            int eloRow = combat.hitActive() ? 2 : shieldPulse > 0 ? 3 : motion.moving() ? 1 : 0;
            if (combat.hitActive()) batch.setColor(1f, 0.45f, 0.62f, 1f);
            elo.draw(batch, eloRow, animationTime, eloX + combat.hitOffset(animationTime),
                    eloY + 12 + motion.gait(animationTime), 88);
            batch.setColor(Color.WHITE);
            mascot.draw(batch, shield.active() ? 2 : progress.interferenceActive() ? 2 : 0,
                    animationTime, Math.min(right - 22, eloX + 55), eloY + 72, 60);
        }

        font.setColor(Color.WHITE);
        font.getData().setScale(1.6f);
        font.draw(batch, "ReCode  |  Sistema Central", left, height - 37);
        font.getData().setScale(1.1f);
        font.setColor(MUTED);
        font.draw(batch, simulated ? "Modo sem sensor conectado" :
                "Restaure os condutos e estabilize o nucleo", left, height - 71);
        font.setColor(CORRUPTION);
        font.draw(batch, "NO ZERO", width * 0.72f, height - 25f);
        font.setColor(MUTED);
        if (manageSource) font.draw(batch, "P: pausa   R: reiniciar", width - 235, height - 71);
        else font.draw(batch, "Espaco: opcoes", width - 210, 25);
        font.setColor(Color.WHITE);
        font.draw(batch, "Escudos: " + progress.shields() + " / 3", left, height - 130);
        font.draw(batch, "Condutos: " + progress.restoredConduits() + " / 6",
                right - 170, height - 130);
        font.setColor(MUTED);
        font.draw(batch, status, left, 76);
        font.draw(batch, "Ritmo: " + Math.round(connected ? counter.rpm(now) : 0) + " giros/min",
                right - 165, 76);
        if (!progress.isComplete()) {
            String sequence = progress.step() == PhaseThreeProgress.Step.PRECISION
                    ? "Precisao: " + progress.precisionTurns() + "/3 giros individuais"
                    : progress.step() == PhaseThreeProgress.Step.SWITCH
                    ? "Chave de rota: gire para " +
                        (progress.requiredDirection() < 0 ? "BAIXO" : "CIMA")
                    : progress.step() == PhaseThreeProgress.Step.RHYTHM
                    ? "Ritmo: " + progress.rhythmStreak() + "/2 pares seguidos"
                    : "Estabilidade: " + progress.stabilityStreak() + "/3 pares seguidos";
            font.draw(batch, "Escudo " + (progress.shields() + 1) + "/3  |  " + sequence,
                    left, 49);
            font.setColor(progress.interferenceActive() ? CORRUPTION : RESTORED);
            font.draw(batch, progress.step() == PhaseThreeProgress.Step.SWITCH ?
                    wrongRoutePulse > 0f ? "Sentido oposto: gire para " +
                            (progress.requiredDirection() < 0 ? "BAIXO" : "CIMA") :
                            "No Zero bloqueou a passagem: gire o antebraco" :
                    progress.routeChanged() ? "Desvio acionado por ELO" :
                    "Rota " + (progress.activeRoute() == 0 ? "superior" : "inferior") + " ativa",
                    left, 25);
        }

        if (progress.isPaused()) {
            font.getData().setScale(2f);
            font.setColor(ENERGY);
            font.draw(batch, "Confronto pausado", width * 0.32f, height * 0.65f);
            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            font.draw(batch, "Giros durante a pausa nao contam.", width * 0.32f, height * 0.53f);
            font.draw(batch, "P ou Espaco para continuar", width * 0.35f, height * 0.43f);
        } else if (progress.isComplete()) {
            font.getData().setScale(1.9f);
            font.setColor(RESTORED);
            font.draw(batch, "Sistema Central restaurado!", width * 0.25f, height * 0.67f);
            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            font.draw(batch, "ELO conteve No Zero e reconectou a rede.", width * 0.28f, height * 0.56f);
            font.setColor(MUTED);
            font.draw(batch, "3 escudos  |  6 condutos  |  " + progress.totalTurns()
                    + " giros", width * 0.33f, height * 0.46f);
            font.draw(batch, Math.round(progress.movingSeconds()) +
                    (manageSource ? " s com movimento  |  R para jogar novamente"
                            : " s com movimento  |  Campanha concluida"), width * 0.29f, height * 0.38f);
        }
        batch.end();
    }

    @Override public void dispose() {
        if (manageSource) source.close();
        background.dispose();
        elo.dispose();
        mascot.dispose();
        noZero.dispose();
        font.dispose();
        shapes.dispose();
        batch.dispose();
    }
}
