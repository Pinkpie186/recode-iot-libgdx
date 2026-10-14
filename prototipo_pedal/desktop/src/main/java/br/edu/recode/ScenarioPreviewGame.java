package br.edu.recode;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Visualizador isolado da arte, independente das fases jogaveis. */
final class ScenarioPreviewGame extends ApplicationAdapter {
    private final String path;
    private final String title;
    private ScenarioBackground background;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;

    ScenarioPreviewGame(String path, String title) {
        this.path = path;
        this.title = title;
    }

    @Override public void create() {
        background = new ScenarioBackground(path);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
    }

    @Override public void render() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        Gdx.gl.glClearColor(0.02f, 0.03f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        background.draw(batch, width, height);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.04f, 0.09f, 0.86f);
        shapes.rect(0, height - 105, width, 105);
        shapes.rect(0, 0, width, 75);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.7f);
        font.draw(batch, "ReCode  |  " + title, 55, height - 40);
        font.getData().setScale(1.1f);
        String phase = "Rede Instavel".equals(title) ? "2" : "3";
        font.draw(batch, "PREVIA VISUAL - use -Pphase=" + phase + " para jogar", 55, height - 74);
        font.draw(batch, "Abra a fase jogavel com -Pphase=" + phase + ".", 55, 43);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override public void dispose() {
        background.dispose();
        batch.dispose();
        shapes.dispose();
        font.dispose();
    }
}
