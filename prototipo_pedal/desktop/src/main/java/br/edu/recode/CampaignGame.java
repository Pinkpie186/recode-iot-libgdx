package br.edu.recode;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Campanha com menu, pausa, camera viva e defesa do mascote. */
final class CampaignGame extends ApplicationAdapter {
    private static final float TRANSITION_SECONDS = 2.5f;
    private static final Color SHADE = new Color(0.02f, 0.04f, 0.10f, 0.96f);
    private static final Color SUCCESS = new Color(0.31f, 0.91f, 0.68f, 1f);
    private static final Color DANGER = new Color(0.92f, 0.37f, 0.61f, 1f);
    private static final Color CYAN = new Color(0.24f, 0.86f, 1f, 1f);
    private static final Color MUTED = new Color(0.72f, 0.80f, 0.93f, 1f);

    private final PedalSource source;
    private final boolean simulated;
    private CampaignProgress progress = new CampaignProgress();
    private PedalCounter counter = new PedalCounter();
    private CombatVisuals combat = new CombatVisuals();
    private ApplicationAdapter active;
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapes;
    private ScenarioBackground menuBackground;
    private CharacterSpriteSheet menuElo;
    private CharacterSpriteSheet menuMascot;
    private CharacterSpriteSheet menuNoZero;
    private float transitionTime;
    private float menuTime;
    private boolean onTitle = true;
    private boolean showHelp;
    private boolean pauseMenu;
    private int titleSelection;
    private int pauseSelection;
    private int observedAttackSequence;

    CampaignGame(PedalSource source, boolean simulated) {
        this.source = source;
        this.simulated = simulated;
    }

    @Override public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        shapes = new ShapeRenderer();
        menuBackground = new ScenarioBackground("scenarios/circuitos_basicos.png");
        menuElo = new CharacterSpriteSheet("sprites/elo.png");
        menuMascot = new CharacterSpriteSheet("sprites/mascote.png");
        menuNoZero = new CharacterSpriteSheet("sprites/no_zero.png");
        source.start();
    }

    private void openPhase() {
        switch (progress.phase()) {
            case 1:
                active = new ReCodePedalGame(source, simulated, counter, progress.first(), false,
                        progress.fragments(1));
                break;
            case 2:
                active = new PhaseTwoGame(source, simulated, counter, progress.second(), false,
                        progress.mascotShield(), combat, progress.fragments(2));
                break;
            case 3:
                active = new PhaseThreeGame(source, simulated, counter, progress.third(), false,
                        progress.mascotShield(), combat, progress.fragments(3));
                break;
            default:
                throw new IllegalStateException("Fase desconhecida");
        }
        active.create();
    }

    @Override public void render() {
        float delta = Math.min(0.1f, Math.max(0f, Gdx.graphics.getDeltaTime()));
        menuTime += delta;
        if (onTitle) {
            drainMenuFrames();
            handleTitleInput();
            if (onTitle) drawTitle();
            else {
                active.render();
                drawCampaignHud();
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.P) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            setPauseMenu(!pauseMenu);
        }
        if (pauseMenu) {
            handlePauseInput();
            if (onTitle) {
                drawTitle();
                return;
            }
            if (pauseMenu) {
                active.render();
                drawCampaignHud();
                drawPauseMenu();
                return;
            }
        }

        if (isFinished() && Gdx.input.isKeyJustPressed(Input.Keys.R)) newCampaign();
        active.render();
        progress.mascotShield().update(delta,
                progress.status() == CampaignProgress.Status.RUNNING && !phasePaused());

        if (progress.status() == CampaignProgress.Status.RUNNING) {
            progress.observe();
            if (progress.attackSequence() != observedAttackSequence) {
                observedAttackSequence = progress.attackSequence();
                if (progress.lastAttackBlocked()) combat.blocked();
                else combat.hit();
            }
            if (progress.status() == CampaignProgress.Status.DEFEAT) lockActive();
        }
        if (progress.status() == CampaignProgress.Status.BETWEEN_PHASES) {
            transitionTime += delta;
            if (transitionTime >= TRANSITION_SECONDS) {
                active.dispose();
                progress.advance();
                transitionTime = 0f;
                openPhase();
            }
        }
        drawCampaignHud();
    }

    private void drainMenuFrames() {
        PedalFrame frame;
        long now = System.nanoTime();
        while ((frame = source.poll()) != null) counter.accept(frame, now);
    }

    private void handleTitleInput() {
        if (showHelp) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) ||
                    Gdx.input.justTouched()) showHelp = false;
            return;
        }
        int count = active == null ? 3 : 4;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) titleSelection = (titleSelection + 1) % count;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) titleSelection = (titleSelection + count - 1) % count;
        if (Gdx.input.justTouched()) {
            int selected = selectedMenuOption(0.12f, 0.54f, count);
            if (selected >= 0) {
                titleSelection = selected;
                activateTitleOption();
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            activateTitleOption();
        }
    }

    private void activateTitleOption() {
        if (active != null) {
            switch (titleSelection) {
                case 0: continueCampaign(); break;
                case 1: newCampaign(); break;
                case 2: showHelp = true; break;
                case 3: Gdx.app.exit(); break;
                default: break;
            }
        } else {
            switch (titleSelection) {
                case 0: newCampaign(); break;
                case 1: showHelp = true; break;
                case 2: Gdx.app.exit(); break;
                default: break;
            }
        }
    }

    private void handlePauseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) pauseSelection = (pauseSelection + 1) % 3;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) pauseSelection = (pauseSelection + 2) % 3;
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            returnToTitle();
            return;
        }
        if (Gdx.input.justTouched()) {
            int selected = selectedMenuOption(0.33f, 0.53f, 3);
            if (selected >= 0) {
                pauseSelection = selected;
                activatePauseOption();
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            activatePauseOption();
        }
    }

    private void activatePauseOption() {
        switch (pauseSelection) {
            case 0: setPauseMenu(false); break;
            case 1: returnToTitle(); break;
            case 2: newCampaign(); break;
            default: break;
        }
    }

    private int selectedMenuOption(float xFraction, float topFraction, int count) {
        if (!Gdx.input.justTouched()) return -1;
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float x = Gdx.input.getX();
        float y = height - Gdx.input.getY();
        float left = width * xFraction;
        float right = left + width * (xFraction < 0.2f ? 0.38f : 0.34f);
        if (x < left || x > right) return -1;
        for (int i = 0; i < count; i++) {
            float centerY = height * (topFraction - i * 0.095f);
            if (y >= centerY - 22f && y <= centerY + 24f) return i;
        }
        return -1;
    }

    private void setPauseMenu(boolean open) {
        pauseMenu = open;
        pauseSelection = 0;
        if (progress.status() == CampaignProgress.Status.RUNNING && active != null) {
            setPhasePaused(open);
            if (open) progress.mascotShield().resetSequence();
        }
    }

    private boolean phasePaused() {
        if (progress.phase() == 1) return progress.first().isPaused();
        if (progress.phase() == 2) return progress.second().isPaused();
        return progress.third().isPaused();
    }

    private void setPhasePaused(boolean paused) {
        if (phasePaused() == paused) return;
        if (progress.phase() == 1) progress.first().togglePause();
        else if (progress.phase() == 2) progress.second().togglePause();
        else progress.third().togglePause();
    }

    private void returnToTitle() {
        onTitle = true;
        pauseMenu = false;
        showHelp = false;
        titleSelection = 0;
        if (progress.status() == CampaignProgress.Status.RUNNING) setPhasePaused(true);
        progress.mascotShield().resetSequence();
    }

    private void continueCampaign() {
        onTitle = false;
        pauseMenu = false;
        if (progress.status() == CampaignProgress.Status.RUNNING) setPhasePaused(false);
    }

    private void newCampaign() {
        if (active != null) active.dispose();
        while (source.poll() != null) { /* descarta dados da tentativa anterior */ }
        progress = new CampaignProgress();
        counter = new PedalCounter();
        combat = new CombatVisuals();
        transitionTime = 0f;
        observedAttackSequence = 0;
        onTitle = false;
        pauseMenu = false;
        showHelp = false;
        openPhase();
    }

    private boolean isFinished() {
        return progress.status() == CampaignProgress.Status.VICTORY ||
                progress.status() == CampaignProgress.Status.DEFEAT;
    }

    private void lockActive() {
        if (active instanceof ReCodePedalGame) ((ReCodePedalGame) active).setCampaignLocked(true);
        else if (active instanceof PhaseTwoGame) ((PhaseTwoGame) active).setCampaignLocked(true);
        else if (active instanceof PhaseThreeGame) ((PhaseThreeGame) active).setCampaignLocked(true);
    }

    private void drawTitle() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        Gdx.gl.glClearColor(0.02f, 0.04f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        menuBackground.draw(batch, width, height,
                0.5f + (float) Math.sin(menuTime * 0.22f) * 0.35f, menuTime);
        batch.setColor(1f, 1f, 1f, 0.52f);
        menuNoZero.draw(batch, 0, menuTime, width * 0.77f, height * 0.54f,
                Math.min(width * 0.50f, height * 0.73f));
        batch.setColor(Color.WHITE);
        menuElo.draw(batch, 1, menuTime, width * 0.67f, height * 0.24f, 128f);
        menuMascot.draw(batch, 0, menuTime, width * 0.58f, height * 0.37f, 75f);
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        StageDecor.draw(shapes, width, height, 0.45f, menuTime, 0.2f, 1);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(SHADE);
        shapes.rect(width * 0.07f, height * 0.12f, width * 0.49f, height * 0.78f);
        shapes.setColor(CYAN);
        shapes.rect(width * 0.07f, height * 0.89f, width * 0.49f, 5f);
        shapes.end();

        batch.begin();
        if (showHelp) {
            drawHelp(width, height);
        } else {
            font.setColor(CYAN);
            font.getData().setScale(2.9f);
            font.draw(batch, "ReCode", width * 0.12f, height * 0.79f);
            font.setColor(Color.WHITE);
            font.getData().setScale(1.15f);
            font.draw(batch, "Reconstrua a rede com ELO", width * 0.12f, height * 0.71f);
            font.setColor(MUTED);
            font.draw(batch, simulated ? "Modo de simulacao do pedal" : "Controle por mini pedal IoT",
                    width * 0.12f, height * 0.66f);
            String[] options = active == null
                    ? new String[] {"Jogar", "Como jogar", "Sair"}
                    : new String[] {"Continuar campanha", "Nova campanha", "Como jogar", "Sair"};
            for (int i = 0; i < options.length; i++) {
                font.setColor(i == titleSelection ? CYAN : Color.WHITE);
                font.getData().setScale(i == titleSelection ? 1.55f : 1.3f);
                font.draw(batch, (i == titleSelection ? "> " : "  ") + options[i],
                        width * 0.12f, height * (0.54f - i * 0.095f));
            }
            font.getData().setScale(1f);
            font.setColor(MUTED);
            font.draw(batch, "Setas ou clique: escolher   Enter: confirmar",
                    width * 0.12f, height * 0.17f);
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawHelp(int width, int height) {
        font.setColor(CYAN);
        font.getData().setScale(2f);
        font.draw(batch, "Como jogar", width * 0.12f, height * 0.78f);
        font.getData().setScale(1.15f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Pedale para conduzir ELO e restaurar os objetos.", width * 0.12f, height * 0.68f);
        font.draw(batch, "Fase 2: mantenha movimentos regulares.", width * 0.12f, height * 0.59f);
        font.draw(batch, "Fase 3: precisao, ritmo e estabilidade.", width * 0.12f, height * 0.50f);
        font.setColor(CYAN);
        font.draw(batch, "3 pares regulares carregam o escudo do mascote.", width * 0.12f, height * 0.39f);
        font.draw(batch, "A protecao dura 15 segundos de jogo.", width * 0.12f, height * 0.32f);
        font.setColor(MUTED);
        font.draw(batch, "Espaco: pausa e opcoes de menu", width * 0.12f, height * 0.23f);
        font.draw(batch, "Enter ou Esc: voltar", width * 0.12f, height * 0.17f);
    }

    private void drawPauseMenu() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.015f, 0.025f, 0.07f, 0.91f);
        shapes.rect(0, 0, width, height);
        shapes.setColor(SHADE);
        shapes.rect(width * 0.27f, height * 0.17f, width * 0.46f, height * 0.66f);
        shapes.setColor(CYAN);
        shapes.rect(width * 0.27f, height * 0.82f, width * 0.46f, 5f);
        shapes.end();

        batch.begin();
        font.setColor(CYAN);
        font.getData().setScale(2.2f);
        font.draw(batch, "Jogo pausado", width * 0.33f, height * 0.72f);
        String[] options = {"Continuar", "Voltar ao menu", "Nova campanha"};
        for (int i = 0; i < options.length; i++) {
            font.setColor(i == pauseSelection ? CYAN : Color.WHITE);
            font.getData().setScale(i == pauseSelection ? 1.55f : 1.3f);
            font.draw(batch, (i == pauseSelection ? "> " : "  ") + options[i],
                    width * 0.33f, height * (0.53f - i * 0.095f));
        }
        font.getData().setScale(1f);
        font.setColor(MUTED);
        font.draw(batch, "Setas ou clique: escolher   Enter: confirmar",
                width * 0.31f, height * 0.23f);
        font.draw(batch, "Espaco: continuar   M: menu", width * 0.36f, height * 0.18f);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawCampaignHud() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (combat.hitActive()) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(DANGER.r, DANGER.g, DANGER.b, combat.hitStrength() * 0.20f);
            shapes.rect(0, 0, width, height);
            shapes.end();
        }
        batch.begin();
        font.getData().setScale(0.95f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Fase " + progress.phase() + "/3  |  Integridade " +
                progress.integrity() + "/3  |  " + progress.totalTurns() + " voltas",
                width - 330f, height - 31f);
        font.setColor(MUTED);
        font.draw(batch, "Fragmentos de energia: " + progress.totalFragments() + "/9",
                width - 330f, height - 53f);
        if (progress.phase() > 1) {
            MascotShield shield = progress.mascotShield();
            font.setColor(shield.active() ? CYAN : MUTED);
            String shieldText = shield.active()
                    ? "Escudo do mascote: " + (int) Math.ceil(shield.remainingSeconds()) + " s"
                    : "Mascote: " + shield.charge() + "/" + MascotShield.REQUIRED_PAIRS +
                    " pares regulares";
            font.draw(batch, shieldText, width - 330f, height - 75f);
        }
        if (combat.hitActive() || combat.blockedActive()) {
            font.getData().setScale(1.6f);
            font.setColor(combat.hitActive() ? DANGER : CYAN);
            font.draw(batch, combat.hitActive() ? "ELO ATINGIDO!" : "ESCUDO BLOQUEOU O ATAQUE!",
                    width * 0.31f, height * 0.75f);
        }
        batch.end();

        if (progress.status() == CampaignProgress.Status.BETWEEN_PHASES) {
            batch.begin();
            font.getData().setScale(1.1f);
            font.setColor(SUCCESS);
            font.draw(batch, "Proxima fase: " + phaseName(progress.phase() + 1),
                    width * 0.34f, height * 0.25f);
            batch.end();
        } else if (isFinished() && !combat.hitActive()) {
            drawResult(width, height);
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawResult(int width, int height) {
        boolean victory = progress.status() == CampaignProgress.Status.VICTORY;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(SHADE);
        shapes.rect(width * 0.12f, height * 0.13f, width * 0.76f, height * 0.74f);
        shapes.setColor(victory ? SUCCESS : DANGER);
        shapes.rect(width * 0.12f, height * 0.86f, width * 0.76f, 5f);
        shapes.end();

        batch.begin();
        font.getData().setScale(2f);
        font.setColor(victory ? SUCCESS : DANGER);
        font.draw(batch, victory ? "VITORIA - rede restaurada" : "DERROTA - No Zero dominou a rede",
                width * 0.19f, height * 0.78f);
        font.getData().setScale(1.15f);
        font.setColor(Color.WHITE);
        for (int phase = 1; phase <= 3; phase++) {
            String result = phaseName(phase) + ": " + progress.turns(phase) + " voltas, " +
                    Math.round(progress.movingSeconds(phase)) + " s em movimento, " +
                    progress.objectives(phase) + "/3 objetivos";
            if (phase > 1) result += ", " + progress.stablePairs(phase) + " pares regulares";
            font.draw(batch, result, width * 0.19f, height * (0.64f - (phase - 1) * 0.11f));
        }
        font.setColor(victory ? SUCCESS : DANGER);
        font.draw(batch, "Total: " + progress.totalTurns() + " voltas  |  " +
                Math.round(progress.totalMovingSeconds()) + " s em movimento  |  Integridade " +
                progress.integrity() + "/3  |  Fragmentos " + progress.totalFragments() + "/9",
                width * 0.19f, height * 0.27f);
        font.setColor(Color.WHITE);
        font.draw(batch, "R: nova campanha   Espaco: opcoes", width * 0.19f, height * 0.19f);
        batch.end();
    }

    private static String phaseName(int phase) {
        switch (phase) {
            case 1: return "Circuitos Basicos";
            case 2: return "Rede Instavel";
            case 3: return "Sistema Central";
            default: throw new IllegalArgumentException("Fase desconhecida");
        }
    }

    @Override public void dispose() {
        if (active != null) active.dispose();
        source.close();
        menuBackground.dispose();
        menuElo.dispose();
        menuMascot.dispose();
        menuNoZero.dispose();
        shapes.dispose();
        font.dispose();
        batch.dispose();
    }
}

