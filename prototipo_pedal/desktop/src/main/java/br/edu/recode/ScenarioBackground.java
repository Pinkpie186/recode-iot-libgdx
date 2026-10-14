package br.edu.recode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/** Desenha o cenario cobrindo a janela sem distorcer sua proporcao. */
final class ScenarioBackground implements Disposable {
    private final Texture texture;

    ScenarioBackground(String path) {
        texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    void draw(SpriteBatch batch, int width, int height) {
        float scale = Math.max(width / (float) texture.getWidth(),
                height / (float) texture.getHeight());
        float drawWidth = texture.getWidth() * scale;
        float drawHeight = texture.getHeight() * scale;
        batch.draw(texture, (width - drawWidth) / 2f,
                (height - drawHeight) / 2f, drawWidth, drawHeight);
    }

    @Override public void dispose() { texture.dispose(); }
}
