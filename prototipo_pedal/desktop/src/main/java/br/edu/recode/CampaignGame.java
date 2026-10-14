package br.edu.recode;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Executa as tres fases na mesma janela e na mesma conexao com o pedal. */
final class CampaignGame extends ApplicationAdapter {
    private static final float TRANSITION_SECONDS = 2.5f;
    private static final Color SHADE = new Color(0.02f, 0.04f, 0.10f, 0.96f);
    private static final Color SUCCESS = new Color(0.31f, 0.91f, 0.68f, 1f);
    private static final Color DANGER = new Color(0.92f, 0.37f, 0.61f, 1f);

    private final PedalSource source;
    private final boolean simulated;
    private CampaignProgress progress = new CampaignProgress();
    private PedalCounter counter = new PedalCounter();
    private ApplicationAdapter active;
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapes;
    private float transitionTime;

    CampaignGame(PedalSource source, boolean simulated) {
        this.source = source;
        this.simulated = simulated;
    }

    @Override public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        shapes = new ShapeRenderer();
        source.start();
        openPhase();
    }

    private void openPhase() {
        switch (progress.phase()) {
            case 1:
                active = new ReCodePedalGame(source, simulated, counter, progress.first(), false);
                break;
            case 2:
                active = new PhaseTwoGame(source, simulated, counter, progress.second(), false);
                break;
            case 3:
                active = new PhaseThreeGame(source, simulated, counter, progress.third(), false);
                break;
            default:
                throw new IllegalStateException("Fase desconhecida");
        }
        active.create();
    }

    @Override public void render() {
        if (isFinished() && Gdx.input.isKeyJustPressed(Input.Keys.R)) restart();
        active.render();

        if (progress.status() == CampaignProgress.Status.RUNNING) {
            progress.observe();
            if (progress.status() == CampaignProgress.Status.DEFEAT) lockActive();
        }
        if (progress.status() == CampaignProgress.Status.BETWEEN_PHASES) {
            transitionTime += Math.min(0.1f, Math.max(0f, Gdx.graphics.getDeltaTime()));
            if (transitionTime >= TRANSITION_SECONDS) {
                active.dispose();
                progress.advance();
                transitionTime = 0f;
                openPhase();
            }
        }
        drawCampaignHud();
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

    private void restart() {
        active.dispose();
        while (source.poll() != null) { /* ignora dados da tentativa anterior */ }
        progress = new CampaignProgress();
        counter = new PedalCounter();
        transitionTime = 0f;
        openPhase();
    }

    private void drawCampaignHud() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Fase " + progress.phase() + "/3  |  Integridade " +
                progress.integrity() + "/3  |  Total " + progress.totalTurns() + " voltas",
                width - 380f, height - 38f);
        batch.end();

        if (progress.status() == CampaignProgress.Status.BETWEEN_PHASES) {
            batch.begin();
            font.getData().setScale(1.1f);
            font.setColor(SUCCESS);
            font.draw(batch, "Proxima fase: " + phaseName(progress.phase() + 1),
                    width * 0.34f, height * 0.25f);
            batch.end();
        } else if (isFinished()) {
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
                progress.integrity() + "/3", width * 0.19f, height * 0.27f);
        font.setColor(Color.WHITE);
        font.draw(batch, "R: nova tentativa", width * 0.19f, height * 0.19f);
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
        shapes.dispose();
        font.dispose();
        batch.dispose();
    }
}
