package br.edu.recode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.GL20;
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

    /** A camera percorre a arte ampliada conforme ELO avanca na fase. */
    void draw(SpriteBatch batch, int width, int height, float journey, float time) {
        float scale = Math.max(width / (float) texture.getWidth(),
                height / (float) texture.getHeight()) * 1.28f;
        float drawWidth = texture.getWidth() * scale;
        float drawHeight = texture.getHeight() * scale;
        float position = Math.max(0f, Math.min(1f, journey));
        float x = -(drawWidth - width) * position;
        float y = (height - drawHeight) / 2f + (float) Math.sin(time * 0.65f) * 5f;
        batch.draw(texture, x, y, drawWidth, drawHeight);
    }

    /** Mantem objetos distantes presos a um ponto da arte enquanto a camera avanca. */
    float landmarkX(int width, int height, float journey, float imageFraction) {
        float scale = Math.max(width / (float) texture.getWidth(),
                height / (float) texture.getHeight()) * 1.28f;
        float drawWidth = texture.getWidth() * scale;
        float position = Math.max(0f, Math.min(1f, journey));
        return -(drawWidth - width) * position + drawWidth * imageFraction;
    }

    /** A arquitetura proxima cobre a parte inferior de um inimigo no horizonte. */
    void drawForeground(SpriteBatch batch, int width, int height, float journey,
                        float time, float screenFraction) {
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, 0, Gdx.graphics.getBackBufferWidth(),
                Math.round(Gdx.graphics.getBackBufferHeight() * screenFraction));
        draw(batch, width, height, journey, time);
        batch.flush();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    @Override public void dispose() { texture.dispose(); }
}
