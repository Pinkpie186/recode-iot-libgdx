package br.edu.recode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/** Recorta em tempo de execucao as 16 poses de uma folha 4 x 4. */
final class CharacterSpriteSheet implements Disposable {
    private static final int GRID = 4;
    private static final float FRAME_SECONDS = 0.16f;
    private final Texture texture;
    private final TextureRegion[][] frames = new TextureRegion[GRID][GRID];

    CharacterSpriteSheet(String path) {
        texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        int width = texture.getWidth();
        int height = texture.getHeight();
        for (int row = 0; row < GRID; row++) {
            int top = Math.round(row * height / (float) GRID);
            int bottom = Math.round((row + 1) * height / (float) GRID);
            for (int column = 0; column < GRID; column++) {
                int left = Math.round(column * width / (float) GRID);
                int right = Math.round((column + 1) * width / (float) GRID);
                frames[row][column] = new TextureRegion(texture, left, top,
                        right - left, bottom - top);
            }
        }
    }

    void draw(SpriteBatch batch, int animationRow, float animationTime,
              float centerX, float centerY, float size) {
        int frame = (int) (animationTime / FRAME_SECONDS) % GRID;
        batch.draw(frames[animationRow][frame], centerX - size / 2,
                centerY - size / 2, size, size);
    }

    @Override public void dispose() { texture.dispose(); }
}
