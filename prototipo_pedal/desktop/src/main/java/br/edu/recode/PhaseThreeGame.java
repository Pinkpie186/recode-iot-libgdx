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
    private static final Color BACKGROUND = new Color(0.025f, 0.025f, 0.075f, 1f);
    private static final Color PANEL = new Color(0.035f, 0.045f, 0.12f, 0.94f);
    private static final Color TRACK = new Color(0.15f, 0.18f, 0.32f, 1f);
    private static final Color ENERGY = new Color(0.13f, 0.82f, 0.97f, 1f);
    private static final Color RESTORED = new Color(0.30f, 0.91f, 0.68f, 1f);
    private static final Color CORRUPTION = new Color(0.84f, 0.22f, 0.74f, 1f);
    private static final Color MUTED = new Color(0.74f, 0.79f, 0.91f, 1f);

    private final PedalSource source;
    private final boolean simulated;
    private final PedalCounter counter = new PedalCounter();
    private final PhaseThreeProgress progress = new PhaseThreeProgress();
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

    PhaseThreeGame(PedalSource source, boolean simulated) {
        this.source = source;
        this.simulated = simulated;
    }

    @Override public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        background = new ScenarioBackground("scenarios/sistema_central.png");
        elo = new CharacterSpriteSheet("sprites/elo.png");
        mascot = new CharacterSpriteSheet("sprites/mascote.png");
        noZero = new CharacterSpriteSheet("sprites/no_zero.png");
        source.start();
    }

    @Override public void render() {
        long now = System.nanoTime();
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) progress.togglePause();
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            progress.restart();
            energyPulse = shieldPulse = routePulse = 0f;
        }

        PedalFrame frame;
        while ((frame = source.poll()) != null) {
            int turns = counter.accept(frame, now);
            if (turns > 0 && !progress.isPaused() && !progress.isComplete()) {
                int routeBefore = progress.activeRoute();
                int restored = progress.acceptTurns(turns, frame.lastCycleMillis);
                energyPulse = 1f;
                if (restored > 0) shieldPulse = 1f;
                if (!progress.isComplete() && progress.activeRoute() != routeBefore) routePulse = 1f;
            }
        }
        float delta = Gdx.graphics.getDeltaTime();
        boolean connected = source.lastFrameNanos() >= 0 &&
                now - source.lastFrameNanos() <= 2_000_000_000L;
        progress.update(delta, connected && counter.rpm(now) > 0);
        animationTime += delta;
        energyPulse = Math.max(0f, energyPulse - delta * 2.4f);
        shieldPulse = Math.max(0f, shieldPulse - delta * 1.15f);
        routePulse = Math.max(0f, routePulse - delta * 1.15f);

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float left = width * 0.11f;
        float right = width * 0.65f;
        float centerY = height * 0.50f;
        float upperY = centerY + 47f;
        float lowerY = centerY - 47f;
        float segment = (right - left) / PhaseThreeProgress.SHIELD_COUNT;
        float fraction = progress.shields() +
                Math.min(1f, progress.shieldTurns() / (float) PhaseThreeProgress.TURNS_PER_SHIELD);
        float eloX = left + segment * fraction;
        float eloY = progress.activeRoute() == 0 ? upperY : lowerY;

        Gdx.gl.glClearColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        background.draw(batch, width, height);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        drawWorld(width, height, left, right, upperY, lowerY, segment, eloX, eloY);
        drawSpritesAndText(width, height, left, right, upperY, lowerY, segment,
                eloX, eloY, connected, now);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawWorld(int width, int height, float left, float right,
                           float upperY, float lowerY, float segment,
                           float eloX, float eloY) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PANEL);
        shapes.rect(0, height - 105, width, 105);
        shapes.rect(0, 0, width, 109);

        shapes.setColor(TRACK);
        shapes.rect(left, upperY - 6, right - left, 12);
        shapes.rect(left, lowerY - 6, right - left, 12);
        shapes.setColor(ENERGY);
        shapes.rect(left, eloY - 4, Math.max(0f, eloX - left), 8);

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
        if (routePulse > 0f && !progress.isComplete()) {
            shapes.setColor(ENERGY.r, ENERGY.g, ENERGY.b, routePulse * 0.32f);
            shapes.rect(eloX - 24, lowerY, 48, upperY - lowerY);
        }
        if (progress.interferenceActive() && !progress.isPaused()) {
            float x = left + segment * (progress.shields() + 1);
            shapes.setColor(CORRUPTION.r, CORRUPTION.g, CORRUPTION.b, 0.32f);
            shapes.rectLine(width * 0.76f, (upperY + lowerY) / 2f,
                    x + 20, eloY, 5f);
            shapes.rect(x - 23, eloY + 31, 46, 5);
        }

        if (progress.isPaused() || progress.isComplete()) {
            shapes.setColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 0.95f);
            shapes.rect(width * 0.16f, height * 0.26f, width * 0.68f, height * 0.51f);
            shapes.setColor(progress.isComplete() ? RESTORED : ENERGY);
            shapes.rect(width * 0.16f, height * 0.76f, width * 0.68f, 4);
        }
        shapes.end();
    }

    private void drawSpritesAndText(int width, int height, float left, float right,
                                    float upperY, float lowerY, float segment,
                                    float eloX, float eloY, boolean connected, long now) {
        String status = connected ? simulated ? "Dados simulados" : "ESP32 conectado" :
                source.error().isEmpty() ? simulated ? "Iniciando simulacao" :
                        "Aguardando dados do ESP32" : source.error();
        batch.begin();
        if (!progress.isPaused() && !progress.isComplete()) {
            int eloRow = shieldPulse > 0 ? 3 : energyPulse > 0 ? 1 : 0;
            elo.draw(batch, eloRow, animationTime, eloX, eloY + 12, 88);
            mascot.draw(batch, progress.interferenceActive() ? 2 : 0,
                    animationTime, Math.min(right - 22, eloX + 55), eloY + 72, 60);
            int enemyRow = shieldPulse > 0 ? 3 : progress.interferenceActive() ? 1 : 0;
            noZero.draw(batch, enemyRow, animationTime, width * 0.83f,
                    (upperY + lowerY) / 2f + 20, 204);
        }

        font.setColor(Color.WHITE);
        font.getData().setScale(1.6f);
        font.draw(batch, "ReCode  |  Sistema Central", left, height - 37);
        font.getData().setScale(1.1f);
        font.setColor(MUTED);
        font.draw(batch, simulated ? "SIMULACAO DE DESENVOLVIMENTO - sem dispositivo IoT" :
                "Restaure os condutos e estabilize o nucleo", left, height - 71);
        font.draw(batch, "P: pausa   R: reiniciar", width - 235, height - 71);
        font.setColor(Color.WHITE);
        font.draw(batch, "Escudos: " + progress.shields() + " / 3", left, height - 130);
        font.draw(batch, "Condutos: " + progress.restoredConduits() + " / 6",
                right - 170, height - 130);
        if (!progress.isPaused() && !progress.isComplete()) {
            font.setColor(CORRUPTION);
            font.draw(batch, "NO ZERO", width * 0.79f, lowerY - 62);
        }

        font.setColor(MUTED);
        font.draw(batch, status, left, 76);
        font.draw(batch, "Cadencia: " + Math.round(connected ? counter.rpm(now) : 0) + " RPM",
                right - 165, 76);
        if (!progress.isComplete()) {
            font.draw(batch, "Escudo atual: " + Math.min(progress.shieldTurns(), 8)
                    + "/8 voltas; pares regulares: " + Math.min(progress.stablePairs(), 2)
                    + "/2", left, 49);
            font.setColor(progress.interferenceActive() ? CORRUPTION : RESTORED);
            font.draw(batch, progress.routeChanged() ?
                    "No Zero desviou a rota - ELO segue o sinal" :
                    "Rota " + (progress.activeRoute() == 0 ? "superior" : "inferior") + " ativa",
                    left, 25);
        }

        if (progress.isPaused()) {
            font.getData().setScale(2f);
            font.setColor(ENERGY);
            font.draw(batch, "Confronto pausado", width * 0.32f, height * 0.65f);
            font.getData().setScale(1.2f);
            font.setColor(Color.WHITE);
            font.draw(batch, "Voltas durante a pausa nao contam.", width * 0.32f, height * 0.53f);
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
                    + " voltas", width * 0.33f, height * 0.46f);
            font.draw(batch, Math.round(progress.movingSeconds()) +
                    " s com movimento  |  R para jogar novamente", width * 0.29f, height * 0.38f);
        }
        batch.end();
    }

    @Override public void dispose() {
        source.close();
        background.dispose();
        elo.dispose();
        mascot.dispose();
        noZero.dispose();
        font.dispose();
        shapes.dispose();
        batch.dispose();
    }
}
