package br.edu.recode;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.Arrays;

/** Rede Instavel: ELO estabiliza nos enquanto No Zero corrompe o sinal. */
final class PhaseTwoGame extends ApplicationAdapter {
    private static final float BOSS_REVEAL_SECONDS = 1.65f;
    private static final Color BACKGROUND = new Color(0.025f, 0.035f, 0.09f, 1f);
    private static final Color PANEL = new Color(0.035f, 0.055f, 0.13f, 0.92f);
    private static final Color TRACK = new Color(0.17f, 0.22f, 0.38f, 1f);
    private static final Color ENERGY = new Color(0.12f, 0.82f, 0.96f, 1f);
    private static final Color RESTORED = new Color(0.31f, 0.91f, 0.68f, 1f);
    private static final Color CORRUPTION = new Color(0.84f, 0.20f, 0.76f, 1f);
    private static final Color MUTED = new Color(0.72f, 0.79f, 0.91f, 1f);
    private static final String[] STRUCTURE_NAMES = {"ANTENA", "REFLETOR", "FILTRO"};
    private static final float[] NAME_OFFSETS = {26f, 34f, 23f};

    private final PedalSource source;
    private final boolean simulated;
    private final PedalCounter counter;
    private final PhaseTwoProgress progress;
    private final boolean manageSource;
    private final MascotShield shield;
    private final CombatVisuals combat;
    private final EnergyFragments fragments;
    private final StageMotion motion = new StageMotion();
    private final float[] nodeBuild = new float[PhaseTwoProgress.NODE_COUNT];
    private final float[] nodeFlash = new float[PhaseTwoProgress.NODE_COUNT];
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private ScenarioBackground background;
    private CharacterSpriteSheet elo;
    private CharacterSpriteSheet mascot;
    private CharacterSpriteSheet noZero;
    private float animationTime;
    private float energyPulse;
    private float nodePulse;
    private float feedbackSeconds;
    private float bossRevealSeconds;
    private String movementFeedback = "";
    private boolean positiveFeedback;
    private boolean campaignLocked;

    void setCampaignLocked(boolean locked) { campaignLocked = locked; }

    PhaseTwoGame(PedalSource source, boolean simulated) {
        this(source, simulated, new PedalCounter(), new PhaseTwoProgress(), true,
                new MascotShield(), new CombatVisuals(), new EnergyFragments());
    }

    PhaseTwoGame(PedalSource source, boolean simulated, PedalCounter counter,
                 PhaseTwoProgress progress, boolean manageSource) {
        this(source, simulated, counter, progress, manageSource,
                new MascotShield(), new CombatVisuals(), new EnergyFragments());
    }

    PhaseTwoGame(PedalSource source, boolean simulated, PedalCounter counter,
                 PhaseTwoProgress progress, boolean manageSource,
                 MascotShield shield, CombatVisuals combat) {
        this(source, simulated, counter, progress, manageSource,
                shield, combat, new EnergyFragments());
    }

    PhaseTwoGame(PedalSource source, boolean simulated, PedalCounter counter,
                 PhaseTwoProgress progress, boolean manageSource,
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
        background = new ScenarioBackground("scenarios/rede_instavel.png");
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
            energyPulse = nodePulse = feedbackSeconds = 0f;
            Arrays.fill(nodeBuild, 0f);
            Arrays.fill(nodeFlash, 0f);
            bossRevealSeconds = 0f;
            movementFeedback = "";
            motion.reset();
        }

        PedalFrame frame;
        while ((frame = source.poll()) != null) {
            int turns = counter.accept(frame, now);
            if (turns > 0 && !campaignLocked && !progress.isPaused() && !progress.isComplete()) {
                int pairsBefore = progress.stablePairs();
                int nodesBefore = progress.nodes();
                int repaired = progress.acceptTurns(turns, frame.lastCycleMillis);
                boolean shieldStarted = shield.acceptTurns(turns, frame.lastCycleMillis);
                if (shieldStarted) combat.shieldStarted();
                energyPulse = 1f;
                if (repaired > 0) {
                    nodePulse = 1f;
                    for (int i = nodesBefore; i < progress.nodes(); i++) nodeFlash[i] = 1f;
                }
                positiveFeedback = repaired > 0 || shieldStarted ||
                        progress.stablePairs() > pairsBefore;
                movementFeedback = repaired > 0 ? "Estrutura estabilizada!"
                        : shieldStarted ? "Escudo ativo por 15 s."
                        : progress.stablePairs() > pairsBefore ? "Ritmo regular! Continue."
                        : pairsBefore > 0 ? "Ritmo mudou. Retome com calma."
                        : "Giro recebido. Siga no seu ritmo.";
                feedbackSeconds = 2.1f;
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
        nodePulse = Math.max(0f, nodePulse - delta * 1.2f);
        if (!progress.isPaused()) feedbackSeconds = Math.max(0f, feedbackSeconds - delta);
        if (!progress.isPaused() && (!campaignLocked || progress.isComplete())) {
            for (int i = 0; i < PhaseTwoProgress.NODE_COUNT; i++) {
                if (i < progress.nodes())
                    nodeBuild[i] = Math.min(1f, nodeBuild[i] + delta * 1.2f);
                nodeFlash[i] = Math.max(0f, nodeFlash[i] - delta * 1.1f);
            }
        }

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float left = width * 0.11f;
        float right = width * 0.72f;
        float trackY = height * 0.45f;
        float segment = (right - left) / PhaseTwoProgress.NODE_COUNT;
        float fraction = progress.nodes() +
                Math.min(1f, progress.nodeTurns() / (float) PhaseTwoProgress.TURNS_PER_NODE);
        float targetX = left + segment * fraction;
        float targetJourney = Math.min(1f, fraction / PhaseTwoProgress.NODE_COUNT);
        motion.update(targetX, targetJourney, delta, progress.isPaused());
        float eloX = motion.x();
        float journey = motion.journey();
        float reveal = Math.min(1f, bossRevealSeconds / BOSS_REVEAL_SECONDS);
        float rise = reveal * reveal * (3f - 2f * reveal);
        float bossX = width * (0.98f - 0.14f * rise);
        float bossY = trackY + 27f + (1f - rise) * 18f;
        float bossSize = 172f * (0.74f + 0.26f * rise);
        fragments.update(journey, delta);

        Gdx.gl.glClearColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        background.draw(batch, width, height, journey, animationTime);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        StageDecor.draw(shapes, width, height, journey, animationTime, energyPulse, 2);
        drawBossEntrance(width, bossX, bossY, reveal);
        fragments.draw(shapes, left, right, trackY + 74f, animationTime);
        drawShapes(width, height, left, right, trackY, segment, eloX);
        drawSpritesAndText(width, height, left, right, trackY, segment, eloX,
                bossX, bossY, bossSize, connected, now);
        // O tiro fica a frente do chefe e de ELO para mostrar o contato real.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        combat.drawProjectile(shapes, bossX - bossSize * 0.17f, bossY + bossSize * 0.08f,
                eloX, trackY + 15f, animationTime);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBossEntrance(int width, float bossX, float bossY, float reveal) {
        float portalX = width * 0.84f;
        float opening = 0.35f + 0.65f * Math.min(1f, reveal * 2f);
        float pulse = 0.5f + 0.5f * (float) Math.sin(animationTime * 8f);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(CORRUPTION.r, CORRUPTION.g, CORRUPTION.b,
                0.10f + (1f - reveal) * 0.16f);
        shapes.circle(portalX, bossY, 104f * opening);
        shapes.setColor(0.025f, 0.008f, 0.065f, 0.96f);
        shapes.circle(portalX, bossY, 76f * opening);
        shapes.setColor(CORRUPTION.r, CORRUPTION.g, CORRUPTION.b,
                0.35f + pulse * 0.20f);
        for (int i = 0; i < 10; i++) {
            double angle = i * Math.PI / 5d + animationTime * 0.35f;
            float radius = 87f * opening;
            float x = portalX + (float) Math.cos(angle) * radius;
            float y = bossY + (float) Math.sin(angle) * radius;
            shapes.rectLine(x, y, x + (float) Math.cos(angle) * (9f + 15f * (1f - reveal)),
                    y + (float) Math.sin(angle) * (9f + 15f * (1f - reveal)), 3f);
        }
        if (reveal < 1f) {
            shapes.setColor(ENERGY.r, ENERGY.g, ENERGY.b, (1f - reveal) * 0.8f);
            for (int i = 0; i < 5; i++) {
                float x = bossX - 30f + i * 19f;
                float y = bossY + (i % 2 == 0 ? 54f : -59f);
                shapes.circle(x, y, 2f + pulse * 3f);
            }
        }
        shapes.end();
    }

    private void drawShapes(int width, int height, float left, float right,
                            float y, float segment, float eloX) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PANEL);
        shapes.rect(0, height - 105, width, 105);
        shapes.rect(0, 0, width, 108);

        shapes.setColor(TRACK);
        shapes.rect(left, y - 6, right - left, 12);
        shapes.setColor(ENERGY);
        shapes.rect(left, y - 4, Math.max(0f, eloX - left), 8);

        for (int i = 0; i < PhaseTwoProgress.NODE_COUNT; i++) {
            float x = left + segment * (i + 1);
            boolean current = i == progress.nodes() && !progress.isComplete();
            float charge = i < progress.nodes() ? 1f : current
                    ? progress.nodeTurns() / (float) PhaseTwoProgress.TURNS_PER_NODE : 0f;
            NetworkStructureVisuals.draw(shapes, i, x, y, nodeBuild[i], nodeFlash[i],
                    charge, current && progress.interferenceActive(), animationTime);
        }

        shapes.setColor(ENERGY.r, ENERGY.g, ENERGY.b, 0.16f + energyPulse * 0.20f);
        shapes.circle(eloX, y, 31 + energyPulse * 11);
        combat.draw(shapes, eloX, y + 15f, animationTime, shield);

        if (progress.interferenceActive() && !progress.isPaused()) {
            float x = left + segment * (progress.nodes() + 1);
            // A interferencia pertence ao no atual; nao simula um tiro que erra ELO.
            float flicker = 0.5f + 0.5f * (float) Math.sin(animationTime * 6f);
            shapes.setColor(CORRUPTION.r, CORRUPTION.g, CORRUPTION.b,
                    0.28f + flicker * 0.24f);
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI / 3d + animationTime * 0.5f;
                shapes.circle(x + (float) Math.cos(angle) * 34f,
                        y + (float) Math.sin(angle) * 34f, 2.5f);
            }
        }

        if (progress.isPaused() || progress.isComplete()) {
            shapes.setColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 0.94f);
            shapes.rect(width * 0.17f, height * 0.27f, width * 0.66f, height * 0.49f);
            shapes.setColor(progress.isComplete() ? RESTORED : ENERGY);
            shapes.rect(width * 0.17f, height * 0.75f, width * 0.66f, 4);
        }
        shapes.end();
    }

    private void drawSpritesAndText(int width, int height, float left, float right,
                                    float y, float segment, float eloX,
                                    float bossX, float bossY, float bossSize,
                                    boolean connected, long now) {
        String status = connected ? simulated ? "Dados simulados" : "ESP32 conectado" :
                source.error().isEmpty() ? simulated ? "Iniciando simulacao" :
                        "Aguardando dados do ESP32" : source.error();
        batch.begin();
        if (!progress.isPaused() && !progress.isComplete()) {
            int eloRow = combat.hitActive() ? 2 : nodePulse > 0 ? 3 : motion.moving() ? 1 : 0;
            if (combat.hitActive()) batch.setColor(1f, 0.45f, 0.62f, 1f);
            elo.draw(batch, eloRow, animationTime, eloX + combat.hitOffset(animationTime),
                    y + 13 + motion.gait(animationTime), 90);
            batch.setColor(Color.WHITE);
            mascot.draw(batch, shield.active() ? 2 : progress.interferenceActive() ? 2 : 0,
                    animationTime, Math.min(right - 25, eloX + 56), y + 84, 63);
            int enemyRow = nodePulse > 0 ? 2 :
                    combat.attackFlying() || combat.hitActive() ||
                    combat.blockedActive() || progress.interferenceActive() ? 1 : 0;
            noZero.draw(batch, enemyRow, animationTime, bossX, bossY, bossSize);
        }

        font.setColor(Color.WHITE);
        font.getData().setScale(1.6f);
        font.draw(batch, "ReCode  |  Rede Instavel", left, height - 37);
        font.getData().setScale(1.1f);
        font.setColor(MUTED);
        font.draw(batch, simulated ? "Modo sem sensor conectado" :
                "Mantenha seu ritmo confortavel para estabilizar a rede", left, height - 71);
        font.setColor(Color.WHITE);
        font.draw(batch, "Estruturas: " + progress.nodes() + " / 3", left, height - 130);
        font.draw(batch, "Giros validos: " + progress.totalTurns(), right - 250, height - 130);
        for (int i = 0; i < PhaseTwoProgress.NODE_COUNT; i++) {
            float x = left + segment * (i + 1);
            font.setColor(i < progress.nodes() ? RESTORED : MUTED);
            font.draw(batch, STRUCTURE_NAMES[i], x - NAME_OFFSETS[i], y - 55);
        }
        if (!progress.isPaused() && !progress.isComplete()) {
            font.setColor(CORRUPTION);
            font.draw(batch, "NO ZERO", width * 0.80f, y - 78);
        }

        font.setColor(MUTED);
        font.draw(batch, status, left, 73);
        font.draw(batch, "Ritmo: " + Math.round(connected ? counter.rpm(now) : 0) + " giros/min",
                right - 165, 73);
        if (!progress.isComplete()) {
            font.draw(batch, "Estrutura atual: " + Math.min(progress.nodeTurns(), PhaseTwoProgress.TURNS_PER_NODE)
                    + "/8 giros; pares seguidos: " + Math.min(progress.stablePairs(), 2)
                    + "/2", left, 47);
            font.setColor(feedbackSeconds > 0f ? positiveFeedback ? RESTORED : MUTED
                    : progress.interferenceActive() ? CORRUPTION : RESTORED);
            font.draw(batch, feedbackSeconds > 0f ? movementFeedback
                    : progress.interferenceActive() ? "No Zero interfere na rede"
                    : "Sinal em recuperacao", left, 24);
            font.setColor(MUTED);
            font.draw(batch, manageSource ? "P/Espaco: pausa   R: reiniciar"
                    : "Espaco: opcoes", right - 276, 24);
        }

        if (progress.isPaused()) {
            font.getData().setScale(2.0f);
            font.setColor(ENERGY);
            font.draw(batch, "Rede pausada", width * 0.37f, height * 0.64f);
            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            font.draw(batch, "Giros durante a pausa nao contam.", width * 0.32f, height * 0.53f);
            font.draw(batch, "P ou Espaco para continuar", width * 0.35f, height * 0.43f);
        } else if (progress.isComplete()) {
            font.getData().setScale(1.9f);
            font.setColor(RESTORED);
            font.draw(batch, "Rede Instavel estabilizada!", width * 0.27f, height * 0.65f);
            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            font.draw(batch, "No Zero recuou para o Sistema Central.", width * 0.29f, height * 0.55f);
            font.setColor(MUTED);
            font.draw(batch, "3 estruturas  |  " + progress.totalTurns() + " giros  |  " +
                    Math.round(progress.movingSeconds()) + " s com movimento",
                    width * 0.27f, height * 0.45f);
            font.draw(batch, manageSource ? "R para jogar novamente"
                    : "Enter: proxima fase quando estiver pronto", width * 0.28f, height * 0.36f);
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
