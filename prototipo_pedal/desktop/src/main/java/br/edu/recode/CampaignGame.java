package br.edu.recode;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Campanha com orientacao entre fases, pausas livres e defesa do mascote. */
final class CampaignGame extends ApplicationAdapter {
    private static final float INTERMISSION_REVEAL_SECONDS = 0.7f;
    private static final float DEFEAT_RESTART_SECONDS = 3f;
    private static final Color SHADE = new Color(0.02f, 0.04f, 0.10f, 0.96f);
    private static final Color SUCCESS = new Color(0.31f, 0.91f, 0.68f, 1f);
    private static final Color DANGER = new Color(0.92f, 0.37f, 0.61f, 1f);
    private static final Color CYAN = new Color(0.24f, 0.86f, 1f, 1f);
    private static final Color MUTED = new Color(0.72f, 0.80f, 0.93f, 1f);
    private static final Color HEART_EMPTY = new Color(0.33f, 0.22f, 0.34f, 1f);
    private static final Color HEART_LIGHT = new Color(1f, 0.64f, 0.78f, 1f);
    private static final Color HEART_HOLLOW = new Color(0.07f, 0.07f, 0.13f, 1f);

    private final PedalSource source;
    private final boolean simulated;
    private CampaignProgress progress = new CampaignProgress();
    private PedalCounter counter = new PedalCounter();
    private CombatVisuals combat = new CombatVisuals();
    private GameAudio audio;
    private ApplicationAdapter active;
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapes;
    private ScenarioBackground menuBackground;
    private CharacterSpriteSheet menuElo;
    private CharacterSpriteSheet menuMascot;
    private CharacterSpriteSheet menuNoZero;
    private float transitionTime;
    private float defeatRestartTime;
    private float menuTime;
    private boolean onTitle = true;
    private boolean briefing;
    private boolean showHelp;
    private boolean pauseMenu;
    private boolean sensorHold;
    private int titleSelection;
    private int pauseSelection;
    private int observedAttackSequence;
    private int observedObjectives;
    private int observedShieldActivations;

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
        audio = new GameAudio();
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
        observedObjectives = progress.objectives(progress.phase());
        audio.phase(progress.phase());
    }

    @Override public void render() {
        float delta = Math.min(0.1f, Math.max(0f, Gdx.graphics.getDeltaTime()));
        menuTime += delta;
        audio.playing(!onTitle && !briefing && !pauseMenu && !sensorHold &&
                progress.status() == CampaignProgress.Status.RUNNING);
        audio.menu(onTitle || pauseMenu);
        if (onTitle) {
            drainMenuFrames();
            handleTitleInput();
            if (onTitle) drawTitle();
            else if (briefing) {
                active.render();
                drawBriefing();
            }
            else {
                active.render();
                drawCampaignHud();
            }
            return;
        }

        if (briefing) {
            active.render();
            drawBriefing();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) returnToTitle();
            else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.justTouched()) {
                audio.confirm();
                briefing = false;
                lockActive(false);
            }
            return;
        }

        updateSensorHold();

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

        if (isFinished() && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            newCampaign();
            active.render();
            drawBriefing();
            return;
        }
        active.render();
        progress.mascotShield().update(delta,
                progress.status() == CampaignProgress.Status.RUNNING && !phasePaused());
        if (!sensorHold && combat.hitActive() && progress.pendingHits() > 0) {
            progress.resolveHit();
            audio.hit();
            if (progress.status() == CampaignProgress.Status.DEFEAT) {
                audio.gameOver();
                lockActive(true);
            }
        }

        if (progress.status() == CampaignProgress.Status.RUNNING) {
            progress.observe();
            int currentObjectives = progress.objectives(progress.phase());
            if (currentObjectives > observedObjectives) audio.repair();
            observedObjectives = currentObjectives;
            int shieldActivations = progress.mascotShield().activations();
            if (shieldActivations > observedShieldActivations) audio.shield();
            observedShieldActivations = shieldActivations;
            progress.updateBossIdle(delta, !sensorHold && !phasePaused(),
                    combat.attackFlying() || combat.hitActive() ||
                    combat.blockedActive() ||
                    progress.attackSequence() != observedAttackSequence);
            setBossCharging(progress.bossIdleWarning() > 0f);
            if (progress.attackSequence() != observedAttackSequence) {
                observedAttackSequence = progress.attackSequence();
                if (progress.lastAttackBlocked()) combat.launchBlocked();
                else combat.launchHit();
            }
            if (progress.status() == CampaignProgress.Status.DEFEAT) lockActive(true);
        }
        if (progress.status() == CampaignProgress.Status.BETWEEN_PHASES) {
            transitionTime += delta;
            if (transitionTime >= INTERMISSION_REVEAL_SECONDS &&
                    !combat.attackFlying() && progress.pendingHits() == 0 &&
                    (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.justTouched())) {
                audio.confirm();
                active.dispose();
                progress.advance();
                transitionTime = 0f;
                briefing = true;
                openPhase();
                lockActive(true);
                active.render();
                drawBriefing();
                return;
            }
        }
        if (progress.status() == CampaignProgress.Status.DEFEAT &&
                !combat.attackFlying() && !combat.hitActive()) {
            defeatRestartTime += delta;
            if (defeatRestartTime >= DEFEAT_RESTART_SECONDS) {
                newCampaign();
                briefing = false;
                lockActive(false);
                active.render();
                drawCampaignHud();
                return;
            }
        }
        drawCampaignHud();
        if (sensorHold) drawSensorHold();
    }

    private void updateSensorHold() {
        if (simulated || active == null || progress.status() != CampaignProgress.Status.RUNNING) {
            sensorHold = false;
            return;
        }
        boolean lost = !source.signalAvailable(System.nanoTime());
        if (lost == sensorHold) return;
        sensorHold = lost;
        if (lost) {
            setPhasePaused(true);
            lockActive(true);
            progress.mascotShield().resetSequence();
            progress.resetBossIdle();
            setBossCharging(false);
        } else if (!pauseMenu) {
            setPhasePaused(false);
            lockActive(false);
        }
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
        int count = active == null ? 4 : 5;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            titleSelection = (titleSelection + 1) % count;
            audio.navigate();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            titleSelection = (titleSelection + count - 1) % count;
            audio.navigate();
        }
        if (Gdx.input.justTouched()) {
            int selected = selectedMenuOption(0.12f, 0.57f, 0.075f, count);
            if (selected >= 0) {
                if (selected != titleSelection) audio.navigate();
                titleSelection = selected;
                activateTitleOption();
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            activateTitleOption();
        }
    }

    private void activateTitleOption() {
        audio.confirm();
        if (active != null) {
            switch (titleSelection) {
                case 0: continueCampaign(); break;
                case 1: newCampaign(); break;
                case 2: showHelp = true; break;
                case 3: VisualComfort.toggle(); break;
                case 4: Gdx.app.exit(); break;
                default: break;
            }
        } else {
            switch (titleSelection) {
                case 0: newCampaign(); break;
                case 1: showHelp = true; break;
                case 2: VisualComfort.toggle(); break;
                case 3: Gdx.app.exit(); break;
                default: break;
            }
        }
    }

    private void handlePauseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            pauseSelection = (pauseSelection + 1) % 4;
            audio.navigate();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            pauseSelection = (pauseSelection + 3) % 4;
            audio.navigate();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            returnToTitle();
            return;
        }
        if (Gdx.input.justTouched()) {
            int selected = selectedMenuOption(0.33f, 0.55f, 0.075f, 4);
            if (selected >= 0) {
                if (selected != pauseSelection) audio.navigate();
                pauseSelection = selected;
                activatePauseOption();
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            activatePauseOption();
        }
    }

    private void activatePauseOption() {
        audio.confirm();
        switch (pauseSelection) {
            case 0: setPauseMenu(false); break;
            case 1: VisualComfort.toggle(); break;
            case 2: returnToTitle(); break;
            case 3: newCampaign(); break;
            default: break;
        }
    }

    private int selectedMenuOption(float xFraction, float topFraction,
                                   float stepFraction, int count) {
        if (!Gdx.input.justTouched()) return -1;
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float x = Gdx.input.getX();
        float y = height - Gdx.input.getY();
        float left = width * xFraction;
        float right = left + width * (xFraction < 0.2f ? 0.38f : 0.34f);
        if (x < left || x > right) return -1;
        for (int i = 0; i < count; i++) {
            float centerY = height * (topFraction - i * stepFraction);
            if (y >= centerY - 22f && y <= centerY + 24f) return i;
        }
        return -1;
    }

    private void setPauseMenu(boolean open) {
        pauseMenu = open;
        pauseSelection = 0;
        if (open) progress.resetBossIdle();
        if (open) setBossCharging(false);
        if (progress.status() == CampaignProgress.Status.RUNNING && active != null) {
            setPhasePaused(open || sensorHold);
            lockActive(open || sensorHold);
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
        sensorHold = false;
        if (progress.status() == CampaignProgress.Status.RUNNING && !briefing)
            setPhasePaused(true);
        progress.mascotShield().resetSequence();
        progress.resetBossIdle();
        setBossCharging(false);
    }

    private void continueCampaign() {
        onTitle = false;
        pauseMenu = false;
        sensorHold = false;
        if (progress.status() == CampaignProgress.Status.RUNNING && !briefing)
            setPhasePaused(false);
    }

    private void newCampaign() {
        if (active != null) active.dispose();
        while (source.poll() != null) { /* descarta dados da tentativa anterior */ }
        progress = new CampaignProgress();
        counter = new PedalCounter();
        combat = new CombatVisuals();
        transitionTime = 0f;
        defeatRestartTime = 0f;
        observedAttackSequence = 0;
        observedObjectives = 0;
        observedShieldActivations = 0;
        onTitle = false;
        briefing = true;
        pauseMenu = false;
        sensorHold = false;
        showHelp = false;
        openPhase();
        lockActive(true);
    }

    private boolean isFinished() {
        return progress.status() == CampaignProgress.Status.VICTORY ||
                progress.status() == CampaignProgress.Status.DEFEAT;
    }

    private void lockActive(boolean locked) {
        if (active instanceof ReCodePedalGame) ((ReCodePedalGame) active).setCampaignLocked(locked);
        else if (active instanceof PhaseTwoGame) ((PhaseTwoGame) active).setCampaignLocked(locked);
        else if (active instanceof PhaseThreeGame) ((PhaseThreeGame) active).setCampaignLocked(locked);
    }

    private void setBossCharging(boolean charging) {
        if (active instanceof PhaseTwoGame) ((PhaseTwoGame) active).setBossCharging(charging);
        else if (active instanceof PhaseThreeGame)
            ((PhaseThreeGame) active).setBossCharging(charging);
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
        menuNoZero.draw(batch, 0, VisualComfort.ambientTime(menuTime),
                width * 0.77f, height * 0.54f,
                Math.min(width * 0.50f, height * 0.73f));
        batch.setColor(Color.WHITE);
        menuElo.draw(batch, 1, VisualComfort.ambientTime(menuTime),
                width * 0.67f, height * 0.24f, 128f);
        menuMascot.draw(batch, 0, VisualComfort.ambientTime(menuTime),
                width * 0.58f, height * 0.37f, 75f);
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        StageDecor.draw(shapes, width, height, 0.45f, menuTime, 0.2f, 1);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(SHADE);
        shapes.rect(width * 0.07f, height * 0.12f,
                width * (showHelp ? 0.86f : 0.49f), height * 0.78f);
        shapes.setColor(CYAN);
        shapes.rect(width * 0.07f, height * 0.89f,
                width * (showHelp ? 0.86f : 0.49f), 5f);
        if (!showHelp) {
            shapes.setColor(CYAN.r, CYAN.g, CYAN.b, 0.20f);
            shapes.rect(width * 0.10f,
                    height * (0.57f - titleSelection * 0.075f) - 20f,
                    width * 0.43f, 35f);
            shapes.setColor(CYAN);
            shapes.rect(width * 0.10f,
                    height * (0.57f - titleSelection * 0.075f) - 20f,
                    4f, 35f);
        }
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
            font.draw(batch, "Uma aventura em tres regioes", width * 0.12f, height * 0.72f);
            font.setColor(MUTED);
            font.draw(batch, source instanceof KeyboardPedalSource
                            ? "Modo teclado: F alterna o giro; setas escolhem o lado"
                            : simulated ? "Modo sem sensor conectado" : "Controle pelo MPU6050",
                    width * 0.12f, height * 0.67f);
            String[] options = active == null
                    ? new String[] {"Iniciar campanha", "Como jogar", comfortOption(), "Sair"}
                    : new String[] {"Continuar campanha", "Nova campanha", "Como jogar",
                    comfortOption(), "Sair"};
            for (int i = 0; i < options.length; i++) {
                font.setColor(i == titleSelection ? CYAN : Color.WHITE);
                font.getData().setScale(i == titleSelection ? 1.43f : 1.22f);
                font.draw(batch, (i == titleSelection ? "> " : "  ") + options[i],
                        width * 0.12f, height * (0.57f - i * 0.075f));
            }
            font.getData().setScale(1f);
            font.setColor(MUTED);
            font.draw(batch, "Setas ou clique: escolher   Enter: confirmar",
                    width * 0.12f, height * 0.16f);
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawHelp(int width, int height) {
        font.setColor(CYAN);
        font.getData().setScale(2f);
        font.draw(batch, "Como jogar", width * 0.12f, height * 0.78f);
        String[] lines = {
                source instanceof KeyboardPedalSource
                        ? "CONTROLE: F ou setas geram giros; solte entre movimentos."
                        : "CONTROLE: gire o antebraco e volte ao centro entre movimentos.",
                "FASE 1: gire no seu ritmo. Conclua 20 movimentos e repare 3 estruturas.",
                "FASE 2: gire regularmente. Cada totem usa 8 giros com 2 pares seguidos.",
                "Se o ritmo variar, continue: o totem conclui em ate 12 movimentos.",
                "FASE 3: faca 3 giros individuais; ELO troca a rota sozinho.",
                "Apoie o braco e gire no seu ritmo, voltando ao centro.",
                "Depois gire: 2 pares regulares e 3 pares estaveis. Repita 3 vezes.",
                "ESCUDO: 3 pares regulares ativam 15 s de protecao do mascote.",
                "VIDAS: 3 coracoes. No Zero ataca se ELO ficar parado por muito tempo."
        };
        font.getData().setScale(1.03f);
        for (int i = 0; i < lines.length; i++) {
            font.setColor(i == 0 || i == 4 || i == 7 ? CYAN : Color.WHITE);
            font.draw(batch, lines[i], width * 0.12f, height * (0.70f - i * 0.055f));
        }
        font.setColor(MUTED);
        font.draw(batch, "Espaco: pausa e opcoes  |  Sensor sem sinal: progresso preservado",
                width * 0.12f, height * 0.21f);
        font.draw(batch, "Enter ou Esc: voltar", width * 0.12f, height * 0.16f);
    }

    private static String comfortOption() {
        return "Efeitos de fundo: " + (VisualComfort.softEffects() ? "suaves" : "vivos");
    }

    private void drawBriefing() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        int phase = progress.phase();
        String[] lines;
        String objective;
        if (phase == 1) {
            objective = "Religue os tres circuitos";
            lines = new String[] {
                    "Gire o antebraco para conduzir ELO pela trilha.",
                    "Volte a posicao central entre os movimentos.",
                    "O ritmo e livre. A pausa preserva seu progresso."
            };
        } else if (phase == 2) {
            objective = "Estabilize os tres nos da rede";
            lines = new String[] {
                    "Gire regularmente: 8 movimentos e 2 pares por totem.",
                    "Se variar o ritmo, continue ate 12 movimentos.",
                    "Pares regulares carregam o escudo do mascote.",
            };
        } else {
            objective = "Restaure o nucleo central";
            lines = new String[] {
                    "Combine giros individuais, ritmo e estabilidade.",
                    "A rota muda automaticamente apos 3 giros individuais.",
                    "O escudo protege ELO por 15 segundos de jogo."
            };
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.01f, 0.02f, 0.06f, 0.87f);
        shapes.rect(0, 0, width, height);
        shapes.setColor(SHADE);
        shapes.rect(width * 0.13f, height * 0.12f, width * 0.74f, height * 0.76f);
        shapes.setColor(CYAN);
        shapes.rect(width * 0.13f, height * 0.87f, width * 0.74f, 5f);
        shapes.rect(width * 0.13f, height * 0.12f, 5f, height * 0.76f);
        shapes.end();

        batch.begin();
        font.setColor(CYAN);
        font.getData().setScale(1.15f);
        font.draw(batch, "CAPITULO " + phase + " DE 3", width * 0.19f, height * 0.79f);
        font.setColor(Color.WHITE);
        font.getData().setScale(2.0f);
        font.draw(batch, phaseName(phase), width * 0.19f, height * 0.70f);
        font.setColor(SUCCESS);
        font.getData().setScale(1.3f);
        font.draw(batch, "MISSAO: " + objective, width * 0.19f, height * 0.61f);
        font.setColor(Color.WHITE);
        font.getData().setScale(1.1f);
        for (int i = 0; i < lines.length; i++)
            font.draw(batch, (i + 1) + ". " + lines[i],
                    width * 0.19f, height * (0.52f - i * 0.085f));
        font.setColor(MUTED);
        font.draw(batch, "Quando estiver pronto: Enter ou toque para iniciar",
                width * 0.19f, height * 0.22f);
        font.draw(batch, "Esc: voltar ao menu", width * 0.19f, height * 0.17f);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
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
        String[] options = {"Continuar", comfortOption(), "Voltar ao menu", "Nova campanha"};
        for (int i = 0; i < options.length; i++) {
            font.setColor(i == pauseSelection ? CYAN : Color.WHITE);
            font.getData().setScale(i == pauseSelection ? 1.43f : 1.2f);
            font.draw(batch, (i == pauseSelection ? "> " : "  ") + options[i],
                    width * 0.33f, height * (0.55f - i * 0.075f));
        }
        font.getData().setScale(1f);
        font.setColor(MUTED);
        font.draw(batch, "Setas ou clique: escolher   Enter: confirmar",
                width * 0.31f, height * 0.26f);
        font.draw(batch, "Espaco: continuar   M: menu", width * 0.36f, height * 0.21f);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawSensorHold() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        String error = source.error();
        boolean calibrating = error.startsWith("Mantenha o antebraco");
        boolean sensorError = error.startsWith("MPU6050 sem sinal");
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.01f, 0.02f, 0.06f, 0.88f);
        shapes.rect(0, 0, width, height);
        shapes.setColor(SHADE);
        shapes.rect(width * 0.16f, height * 0.18f, width * 0.68f, height * 0.64f);
        shapes.setColor(calibrating ? CYAN : DANGER);
        shapes.rect(width * 0.16f, height * 0.81f, width * 0.68f, 5f);
        shapes.end();

        batch.begin();
        font.setColor(calibrating ? CYAN : DANGER);
        font.getData().setScale(1.75f);
        font.draw(batch, calibrating ? "Calibrando o sensor" : "Sinal do sensor interrompido",
                width * 0.22f, height * 0.71f);
        font.getData().setScale(1.15f);
        font.setColor(Color.WHITE);
        font.draw(batch, sensorError ? "Confira solda, alimentacao 3V3 e fios SDA/SCL." :
                calibrating ? "Mantenha o antebraco na posicao central e estavel." :
                "Confira o cabo USB, a porta COM e o Monitor Serial.",
                width * 0.22f, height * 0.56f);
        font.draw(batch, "A campanha retomara quando o sinal estiver pronto.",
                width * 0.22f, height * 0.47f);
        font.setColor(MUTED);
        font.draw(batch, "Progresso e vidas preservados  |  Espaco: opcoes",
                width * 0.22f, height * 0.32f);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawCampaignHud() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float warning = progress.bossIdleWarning();
        float warningX = width * 0.11f;
        float warningY = height * 0.17f;
        float warningWidth = Math.min(310f, width * 0.33f);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (combat.hitActive()) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(DANGER.r, DANGER.g, DANGER.b, combat.hitStrength() * 0.20f);
            shapes.rect(0, 0, width, height);
            shapes.end();
        }
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < CampaignProgress.INITIAL_INTEGRITY; i++) {
            drawHeart(width - 86f + i * 27f, height - 27f,
                    i < progress.integrity());
        }
        if (progress.phase() > 1) {
            MascotShield shield = progress.mascotShield();
            shapes.setColor(0.20f, 0.27f, 0.37f, 1f);
            shapes.rect(width - 148f, height - 95f, 126f, 6f);
            float portion = shield.active()
                    ? shield.remainingSeconds() / MascotShield.DURATION_SECONDS
                    : shield.charge() / (float) MascotShield.REQUIRED_PAIRS;
            shapes.setColor(shield.active() ? CYAN : SUCCESS);
            shapes.rect(width - 148f, height - 95f, 126f * Math.min(1f, portion), 6f);
        }
        if (warning > 0f) {
            shapes.setColor(0.02f, 0.025f, 0.08f, 0.88f);
            shapes.rect(warningX, warningY, warningWidth, 34f);
            shapes.setColor(DANGER);
            shapes.rect(warningX, warningY, warningWidth * warning, 4f);
        }
        shapes.end();
        batch.begin();
        if (warning > 0f) {
            font.getData().setScale(0.95f);
            font.setColor(DANGER);
            font.draw(batch, "NO ZERO PREPARA UM ATAQUE", warningX + 9f, warningY + 23f);
        }
        font.getData().setScale(0.95f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Fase " + progress.phase() + "/3  |  " +
                progress.totalTurns() + " giros", width - 330f, height - 26f);
        font.setColor(MUTED);
        font.draw(batch, "Fragmentos de energia: " + progress.totalFragments() + "/9",
                width - 330f, height - 72f);
        if (progress.phase() > 1) {
            MascotShield shield = progress.mascotShield();
            font.setColor(shield.active() ? CYAN : MUTED);
            String shieldText = shield.active()
                    ? "Escudo do mascote: " + (int) Math.ceil(shield.remainingSeconds()) + " s"
                    : "Mascote: " + shield.charge() + "/" + MascotShield.REQUIRED_PAIRS +
                    " pares regulares";
            font.draw(batch, shieldText, width - 330f, height - 94f);
        }
        if (combat.hitActive() || combat.blockedActive()) {
            font.getData().setScale(1.6f);
            font.setColor(combat.hitActive() ? DANGER : CYAN);
            font.draw(batch, combat.hitActive() ? "ELO ATINGIDO!" : "ESCUDO BLOQUEOU O ATAQUE!",
                    width * 0.31f, height * 0.75f);
        }
        batch.end();

        if (progress.status() == CampaignProgress.Status.BETWEEN_PHASES) {
            drawIntermission(width, height);
        } else if (isFinished() && !combat.attackFlying() && !combat.hitActive()) {
            drawResult(width, height);
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawHeart(float x, float y, boolean filled) {
        shapes.setColor(filled ? DANGER : HEART_EMPTY);
        shapes.circle(x - 5f, y + 3f, 7f);
        shapes.circle(x + 5f, y + 3f, 7f);
        shapes.triangle(x - 12f, y + 1f, x + 12f, y + 1f, x, y - 13f);
        shapes.setColor(filled ? HEART_LIGHT : HEART_HOLLOW);
        shapes.circle(x - 5f, y + 3f, 4f);
        shapes.circle(x + 5f, y + 3f, 4f);
        shapes.triangle(x - 9f, y, x + 9f, y, x, y - 9f);
    }

    private void drawIntermission(int width, int height) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.01f, 0.02f, 0.06f, 0.83f);
        shapes.rect(0, 0, width, height);
        shapes.setColor(SHADE);
        shapes.rect(width * 0.17f, height * 0.17f, width * 0.66f, height * 0.66f);
        shapes.setColor(SUCCESS);
        shapes.rect(width * 0.17f, height * 0.82f, width * 0.66f, 5f);
        shapes.end();

        batch.begin();
        font.setColor(SUCCESS);
        font.getData().setScale(1.15f);
        font.draw(batch, "AREA RESTAURADA", width * 0.23f, height * 0.75f);
        font.setColor(Color.WHITE);
        font.getData().setScale(1.8f);
        font.draw(batch, phaseName(progress.phase()), width * 0.23f, height * 0.66f);
        font.getData().setScale(1.15f);
        font.draw(batch, progress.turns(progress.phase()) + " giros  |  " +
                progress.objectives(progress.phase()) + "/3 objetivos  |  " +
                Math.round(progress.movingSeconds(progress.phase())) + " s em movimento",
                width * 0.23f, height * 0.53f);
        font.setColor(MUTED);
        font.draw(batch, "A campanha espera ate voce estar pronto.",
                width * 0.23f, height * 0.43f);
        font.setColor(CYAN);
        font.draw(batch, "PROXIMA: " + phaseName(progress.phase() + 1),
                width * 0.23f, height * 0.33f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Enter ou toque: continuar   Espaco: opcoes",
                width * 0.23f, height * 0.23f);
        batch.end();
    }

    private void drawResult(int width, int height) {
        boolean victory = progress.status() == CampaignProgress.Status.VICTORY;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.01f, 0.02f, 0.06f, 0.94f);
        shapes.rect(0, 0, width, height);
        shapes.setColor(SHADE);
        shapes.rect(width * 0.12f, height * 0.13f, width * 0.76f, height * 0.74f);
        shapes.setColor(victory ? SUCCESS : DANGER);
        shapes.rect(width * 0.12f, height * 0.86f, width * 0.76f, 5f);
        shapes.end();

        batch.begin();
        font.getData().setScale(1.9f);
        font.setColor(victory ? SUCCESS : DANGER);
        font.draw(batch, victory ? "REDE RESTAURADA!" : "TENTATIVA ENCERRADA",
                width * 0.19f, height * 0.78f);
        for (int phase = 1; phase <= 3; phase++) {
            float y = height * (0.65f - (phase - 1) * 0.11f);
            font.getData().setScale(1.12f);
            font.setColor(Color.WHITE);
            font.draw(batch, phase + "  " + phaseName(phase) + "   " +
                    progress.turns(phase) + " giros   " +
                    progress.objectives(phase) + "/3 objetivos", width * 0.19f, y);
            font.getData().setScale(1.0f);
            font.setColor(MUTED);
            String detail = Math.round(progress.movingSeconds(phase)) + " s em movimento";
            if (phase > 1) detail += "   |   " + progress.stablePairs(phase) + " pares regulares";
            font.draw(batch, detail, width * 0.22f, y - height * 0.045f);
        }
        font.getData().setScale(1.05f);
        font.setColor(victory ? SUCCESS : CYAN);
        font.draw(batch, "TOTAL: " + progress.totalTurns() + " giros   |   " +
                Math.round(progress.totalMovingSeconds()) + " s em movimento",
                width * 0.19f, height * 0.26f);
        font.draw(batch, "Integridade " + progress.integrity() + "/3   |   Fragmentos " +
                progress.totalFragments() + "/9", width * 0.19f, height * 0.22f);
        font.setColor(Color.WHITE);
        if (victory) {
            font.draw(batch, "R: nova campanha   Espaco: opcoes", width * 0.19f,
                    height * 0.175f);
        } else {
            int seconds = (int) Math.ceil(DEFEAT_RESTART_SECONDS - defeatRestartTime);
            font.draw(batch, "Recomecando em " + seconds + " s   |   R: agora   Espaco: opcoes",
                    width * 0.19f, height * 0.175f);
        }
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
        audio.close();
        shapes.dispose();
        font.dispose();
        batch.dispose();
    }
}

