package br.edu.recode;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Primeira fase demonstravel: cada volta validada aproxima ELO da saida. */
final class ReCodePedalGame extends ApplicationAdapter {
    private static final int TARGET_ROTATIONS = 20; // Meta tecnica, nao prescricao de exercicio.
    private static final int[] REPAIR_AT = {5, 10, 15};
    private static final Color BACKGROUND = new Color(0.035f, 0.055f, 0.11f, 1f);
    private static final Color PANEL = new Color(0.075f, 0.105f, 0.18f, 1f);
    private static final Color TRACK = new Color(0.16f, 0.21f, 0.32f, 1f);
    private static final Color ENERGY = new Color(0.18f, 0.80f, 0.94f, 1f);
    private static final Color REPAIRED = new Color(0.32f, 0.90f, 0.64f, 1f);
    private static final Color CORRUPTED = new Color(0.82f, 0.30f, 0.43f, 1f);
    private static final Color MUTED_TEXT = new Color(0.70f, 0.79f, 0.88f, 1f);

    private final PedalSource source;
    private final boolean simulated;
    private final PedalCounter counter = new PedalCounter();
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapes;
    private ScenarioBackground background;
    private CharacterSpriteSheet eloSprites;
    private CharacterSpriteSheet mascotSprites;
    private int rotations;
    private float energyPulse;
    private float repairPulse;
    private float animationTime;

    ReCodePedalGame(PedalSource source, boolean simulated) {
        this.source = source;
        this.simulated = simulated;
    }

    @Override public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        shapes = new ShapeRenderer();
        background = new ScenarioBackground("scenarios/circuitos_basicos.png");
        eloSprites = new CharacterSpriteSheet("sprites/elo.png");
        mascotSprites = new CharacterSpriteSheet("sprites/mascote.png");
        source.start();
    }

    @Override public void render() {
        long now = System.nanoTime();
        PedalFrame frame;
        while ((frame = source.poll()) != null) {
            int turns = counter.accept(frame, now);
            if (turns > 0 && rotations < TARGET_ROTATIONS) {
                int previous = rotations;
                rotations = Math.min(TARGET_ROTATIONS, rotations + turns);
                energyPulse = 1f;
                for (int threshold : REPAIR_AT) {
                    if (previous < threshold && rotations >= threshold) repairPulse = 1f;
                }
            }
        }
        float deltaTime = Gdx.graphics.getDeltaTime();
        energyPulse = Math.max(0f, energyPulse - deltaTime * 2.5f);
        repairPulse = Math.max(0f, repairPulse - deltaTime * 1.25f);
        animationTime += deltaTime;

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float left = width * 0.11f;
        float right = width * 0.89f;
        float centerY = height * 0.48f;
        float eloX = left + (right - left) * rotations / TARGET_ROTATIONS;

        Gdx.gl.glClearColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        background.draw(batch, width, height);
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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
        for (int i = 0; i < REPAIR_AT.length; i++) {
            float x = left + (right - left) * REPAIR_AT[i] / TARGET_ROTATIONS;
            boolean repaired = rotations >= REPAIR_AT[i];
            shapes.setColor(repaired ? REPAIRED : CORRUPTED);
            shapes.rect(x - 20, y - 20, 40, 40);
            shapes.setColor(BACKGROUND);
            shapes.rect(x - 12, y - 12, 24, 24);
            shapes.setColor(repaired ? REPAIRED : CORRUPTED);
            shapes.rect(x - 5, y - 5, 10, 10);
        }

        shapes.setColor(rotations == TARGET_ROTATIONS ? REPAIRED : TRACK);
        shapes.circle(right, y, 24);
        shapes.setColor(BACKGROUND);
        shapes.circle(right, y, 15);

        // Um halo mostra visualmente o recebimento de uma volta.
        shapes.setColor(ENERGY.r, ENERGY.g, ENERGY.b, 0.20f + energyPulse * 0.20f);
        shapes.circle(eloX, y, 31 + energyPulse * 10);

        if (rotations == TARGET_ROTATIONS) {
            shapes.setColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 0.93f);
            shapes.rect(width * 0.20f, height * 0.30f, width * 0.60f, height * 0.43f);
            shapes.setColor(REPAIRED);
            shapes.rect(width * 0.20f, height * 0.72f, width * 0.60f, 4);
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
        int repaired = 0;
        for (int threshold : REPAIR_AT) if (rotations >= threshold) repaired++;

        batch.begin();
        if (rotations < TARGET_ROTATIONS) {
            int eloRow = repairPulse > 0 ? 3 : energyPulse > 0 ? 1 : 0;
            boolean nearCircuit = false;
            for (int threshold : REPAIR_AT) {
                if (rotations >= threshold - 2 && rotations < threshold) nearCircuit = true;
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
        font.draw(batch, simulated ? "SIMULACAO DE DESENVOLVIMENTO - sem dispositivo IoT"
                : "Pedale para conduzir ELO e reparar os circuitos", left, height - 72);

        font.setColor(Color.WHITE);
        font.draw(batch, "Voltas: " + rotations + " / " + TARGET_ROTATIONS, left, height - 130);
        font.draw(batch, "Circuitos reparados: " + repaired + " / 3", right - 240, height - 130);
        for (int i = 0; i < REPAIR_AT.length; i++) {
            float x = left + (right - left) * REPAIR_AT[i] / TARGET_ROTATIONS;
            font.setColor(rotations >= REPAIR_AT[i] ? REPAIRED : MUTED_TEXT);
            font.draw(batch, "C" + (i + 1), x - 12, y - 42);
        }

        font.setColor(MUTED_TEXT);
        font.draw(batch, status, left, 70);
        if (connected) font.draw(batch, "Cadencia estimada: " +
                Math.round(counter.rpm(now)) + " RPM", right - 250, 70);

        if (rotations == TARGET_ROTATIONS) {
            font.getData().setScale(2f);
            font.setColor(REPAIRED);
            font.draw(batch, "Circuitos Basicos restaurados!", width * 0.24f, height * 0.62f);
            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            font.draw(batch, "ELO recuperou a energia desta area.", width * 0.29f, height * 0.51f);
            font.setColor(MUTED_TEXT);
            font.draw(batch, "Fim da demonstracao da primeira fase", width * 0.31f, height * 0.42f);
        }
        batch.end();
    }

    @Override public void dispose() {
        source.close();
        background.dispose();
        eloSprites.dispose();
        mascotSprites.dispose();
        shapes.dispose();
        font.dispose();
        batch.dispose();
    }
}
