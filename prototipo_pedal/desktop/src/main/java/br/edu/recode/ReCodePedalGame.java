package br.edu.recode;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Circuitos Basicos: cada volta valida energiza a trilha e repara os nos. */
final class ReCodePedalGame extends ApplicationAdapter {
    private static final Color BACKGROUND = new Color(0.035f, 0.055f, 0.11f, 1f);
    private static final Color PANEL = new Color(0.075f, 0.105f, 0.18f, 1f);
    private static final Color TRACK = new Color(0.16f, 0.21f, 0.32f, 1f);
    private static final Color ENERGY = new Color(0.18f, 0.80f, 0.94f, 1f);
    private static final Color REPAIRED = new Color(0.32f, 0.90f, 0.64f, 1f);
    private static final Color CORRUPTED = new Color(0.82f, 0.30f, 0.43f, 1f);
    private static final Color MUTED_TEXT = new Color(0.70f, 0.79f, 0.88f, 1f);

    private final PedalSource source;
    private final boolean simulated;
    private final PedalCounter counter;
    private final PhaseOneProgress progress;
    private final boolean manageSource;
    private final EnergyFragments fragments;
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapes;
    private ScenarioBackground background;
    private CharacterSpriteSheet eloSprites;
    private CharacterSpriteSheet mascotSprites;
    private float energyPulse;
    private float repairPulse;
    private float animationTime;
    private boolean campaignLocked;

    void setCampaignLocked(boolean locked) { campaignLocked = locked; }

    ReCodePedalGame(PedalSource source, boolean simulated) {
        this(source, simulated, new PedalCounter(), new PhaseOneProgress(), true,
                new EnergyFragments());
    }

    ReCodePedalGame(PedalSource source, boolean simulated, PedalCounter counter,
                    PhaseOneProgress progress, boolean manageSource) {
        this(source, simulated, counter, progress, manageSource, new EnergyFragments());
    }

    ReCodePedalGame(PedalSource source, boolean simulated, PedalCounter counter,
                    PhaseOneProgress progress, boolean manageSource,
                    EnergyFragments fragments) {
        this.source = source;
        this.simulated = simulated;
        this.counter = counter;
        this.progress = progress;
        this.manageSource = manageSource;
        this.fragments = fragments;
    }

    @Override public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        shapes = new ShapeRenderer();
        background = new ScenarioBackground("scenarios/circuitos_basicos.png");
        eloSprites = new CharacterSpriteSheet("sprites/elo.png");
        mascotSprites = new CharacterSpriteSheet("sprites/mascote.png");
        if (manageSource) source.start();
    }

    @Override public void render() {
        long now = System.nanoTime();
        // Atalhos de desenvolvimento; o objetivo da fase continua ligado ao pedal.
        if (manageSource && !campaignLocked && (Gdx.input.isKeyJustPressed(Input.Keys.P) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) progress.togglePause();
        if (manageSource && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            progress.restart();
            energyPulse = repairPulse = 0f;
        }
        PedalFrame frame;
        while ((frame = source.poll()) != null) {
            int turns = counter.accept(frame, now);
            if (turns > 0 && !campaignLocked) {
                int previous = progress.rotations();
                int accepted = progress.addRotations(turns);
                if (accepted == 0) continue;
                energyPulse = 1f;
                for (int threshold : PhaseOneProgress.REPAIR_AT) {
                    if (previous < threshold && progress.rotations() >= threshold) repairPulse = 1f;
                }
            }
        }
        float deltaTime = Gdx.graphics.getDeltaTime();
        boolean connected = source.lastFrameNanos() >= 0 &&
                now - source.lastFrameNanos() <= 2_000_000_000L;
        if (!campaignLocked) progress.update(deltaTime, connected && counter.rpm(now) > 0);
        energyPulse = Math.max(0f, energyPulse - deltaTime * 2.5f);
        repairPulse = Math.max(0f, repairPulse - deltaTime * 1.25f);
        animationTime += deltaTime;

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float left = width * 0.11f;
        float right = width * 0.89f;
        float centerY = height * 0.48f;
        float eloX = left + (right - left) * progress.rotations() / PhaseOneProgress.TARGET_ROTATIONS;
        float journey = progress.rotations() / (float) PhaseOneProgress.TARGET_ROTATIONS;
        fragments.update(journey, deltaTime);

        Gdx.gl.glClearColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        background.draw(batch, width, height, journey, animationTime);
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        StageDecor.draw(shapes, width, height, journey, animationTime, energyPulse, 1);
        fragments.draw(shapes, left, right, centerY + 72f, animationTime);
        drawWorld(width, height, left, right, centerY, eloX);
        drawText(width, height, left, right, centerY, eloX, now);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawWorld(int width, int height, float left, float right, float y, float eloX) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PANEL);
        shapes.rect(0, height - 105, width, 105);
        shapes.rect(0, 0, width, 105);

        // A trilha apagada torna-se energizada conforme as voltas chegam.
        shapes.setColor(TRACK);
        shapes.rect(left, y - 9, right - left, 18);
        shapes.setColor(ENERGY);
        shapes.rect(left, y - 5, Math.max(0, eloX - left), 10);

        // Tres bloqueios visuais sao reparados em 5, 10 e 15 voltas.
        for (int i = 0; i < PhaseOneProgress.REPAIR_AT.length; i++) {
            float x = left + (right - left) * PhaseOneProgress.REPAIR_AT[i] / PhaseOneProgress.TARGET_ROTATIONS;
            boolean repaired = progress.rotations() >= PhaseOneProgress.REPAIR_AT[i];
            shapes.setColor(repaired ? REPAIRED : CORRUPTED);
            shapes.rect(x - 20, y - 20, 40, 40);
            shapes.setColor(BACKGROUND);
            shapes.rect(x - 12, y - 12, 24, 24);
            shapes.setColor(repaired ? REPAIRED : CORRUPTED);
            shapes.rect(x - 5, y - 5, 10, 10);
            if (repaired && repairPulse > 0f && progress.rotations() == PhaseOneProgress.REPAIR_AT[i]) {
                shapes.setColor(REPAIRED.r, REPAIRED.g, REPAIRED.b, repairPulse * 0.35f);
                shapes.circle(x, y, 28 + (1f - repairPulse) * 28);
            }
        }

        shapes.setColor(progress.isComplete() ? REPAIRED : TRACK);
        shapes.circle(right, y, 24);
        shapes.setColor(BACKGROUND);
        shapes.circle(right, y, 15);

        // Um halo mostra visualmente o recebimento de uma volta.
        shapes.setColor(ENERGY.r, ENERGY.g, ENERGY.b, 0.20f + energyPulse * 0.20f);
        shapes.circle(eloX, y, 31 + energyPulse * 10);

        if (progress.isComplete() || progress.isPaused()) {
            shapes.setColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 0.93f);
            shapes.rect(width * 0.17f, height * 0.28f, width * 0.66f, height * 0.47f);
            shapes.setColor(progress.isComplete() ? REPAIRED : ENERGY);
            shapes.rect(width * 0.17f, height * 0.74f, width * 0.66f, 4);
        }
        shapes.end();
    }

    private void drawText(int width, int height, float left, float right,
                          float y, float eloX, long now) {
        boolean connected = source.lastFrameNanos() >= 0 &&
                now - source.lastFrameNanos() <= 2_000_000_000L;
        String status = connected ? (simulated ? "Dados simulados" : "ESP32 conectado") :
                source.error().isEmpty()
                        ? (simulated ? "Iniciando simulacao" : "Aguardando dados do ESP32")
                        : source.error();
        batch.begin();
        if (!progress.isComplete()) {
            int eloRow = repairPulse > 0 ? 3 : energyPulse > 0 ? 1 : 0;
            boolean nearCircuit = false;
            for (int threshold : PhaseOneProgress.REPAIR_AT) {
                if (progress.rotations() >= threshold - 2 && progress.rotations() < threshold) nearCircuit = true;
            }
            int mascotRow = nearCircuit ? 2 : energyPulse > 0 ? 1 : 0;
            eloSprites.draw(batch, eloRow, animationTime, eloX, y + 10, 92);
            mascotSprites.draw(batch, mascotRow, animationTime,
                    Math.min(right - 20, eloX + 58), y + 85, 68);
        }
        font.getData().setScale(1.6f);
        font.setColor(Color.WHITE);
        font.draw(batch, "ReCode  |  Circuitos Basicos", left, height - 38);
        font.getData().setScale(1.15f);
        font.setColor(MUTED_TEXT);
        font.draw(batch, simulated ? "Modo sem pedal conectado"
                : "Pedale para conduzir ELO e reparar os circuitos", left, height - 72);

        font.setColor(Color.WHITE);
        font.draw(batch, "Voltas: " + progress.rotations() + " / " + PhaseOneProgress.TARGET_ROTATIONS, left, height - 130);
        font.draw(batch, "Circuitos: " + progress.repairedCircuits() + " / 3", right - 195, height - 130);
        for (int i = 0; i < PhaseOneProgress.REPAIR_AT.length; i++) {
            float x = left + (right - left) * PhaseOneProgress.REPAIR_AT[i] / PhaseOneProgress.TARGET_ROTATIONS;
            font.setColor(progress.rotations() >= PhaseOneProgress.REPAIR_AT[i] ? REPAIRED : MUTED_TEXT);
            font.draw(batch, "C" + (i + 1), x - 12, y - 42);
        }

        font.setColor(MUTED_TEXT);
        font.draw(batch, status, left, 70);
        double rpm = connected ? counter.rpm(now) : 0;
        if (!progress.isComplete()) {
            String rhythm = rpm <= 0 ? "Aguardando movimento" : "Movimento detectado";
            font.draw(batch, rhythm + "  |  " + Math.round(rpm) + " RPM", right - 300, 70);
            font.draw(batch, "Proximo objetivo: " + progress.nextRepairAt() + " voltas", left, 42);
            font.draw(batch, manageSource ? "P/Espaco: pausa   R: reiniciar (desenvolvimento)"
                    : "Espaco: opcoes", right - 390, 42);
        }

        if (progress.isPaused()) {
            font.getData().setScale(2.2f);
            font.setColor(ENERGY);
            font.draw(batch, "Fase pausada", width * 0.37f, height * 0.63f);
            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            font.draw(batch, "Voltas durante a pausa nao contam.", width * 0.32f, height * 0.52f);
            font.draw(batch, "P ou Espaco para continuar", width * 0.35f, height * 0.42f);
        } else if (progress.isComplete()) {
            font.getData().setScale(1.9f);
            font.setColor(REPAIRED);
            font.draw(batch, "Circuitos Basicos restaurados!", width * 0.24f, height * 0.63f);
            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            font.draw(batch, "ELO recuperou a energia desta area.", width * 0.29f, height * 0.54f);
            font.setColor(MUTED_TEXT);
            font.draw(batch, "3 circuitos  |  " + progress.rotations() + " voltas  |  " +
                    Math.round(progress.activeSeconds()) + " s com movimento", width * 0.29f, height * 0.45f);
            font.draw(batch, manageSource ? "R para jogar novamente"
                    : "Proxima fase em instantes", width * 0.38f, height * 0.36f);
        }
        batch.end();
    }

    @Override public void dispose() {
        if (manageSource) source.close();
        background.dispose();
        eloSprites.dispose();
        mascotSprites.dispose();
        shapes.dispose();
        font.dispose();
        batch.dispose();
    }
}
