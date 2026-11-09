package br.edu.recode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/** Os quatro primeiros quadros mostram o chefe surgindo; os demais, seus ataques. */
final class BossSpriteSheet implements Disposable {
    private final Texture texture;
    private final TextureRegion[][] frames = new TextureRegion[2][4];

    BossSpriteSheet(String path) {
        texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (int row = 0; row < 2; row++) {
            int top = Math.round(row * texture.getHeight() / 2f);
            int bottom = Math.round((row + 1) * texture.getHeight() / 2f);
            for (int column = 0; column < 4; column++) {
                int left = Math.round(column * texture.getWidth() / 4f);
                int right = Math.round((column + 1) * texture.getWidth() / 4f);
                frames[row][column] = new TextureRegion(texture, left, top,
                        right - left, bottom - top);
            }
        }
    }

    void draw(SpriteBatch batch, float reveal, float animationTime, boolean attacking,
              float centerX, float centerY, float size) {
        int row = reveal < 1f ? 0 : 1;
        int frame = reveal < 1f ? Math.min(3, (int) (reveal * 4f)) :
                attacking ? 2 + (int) (animationTime / 0.16f) % 2 :
                        (int) (animationTime / 0.28f) % 2;
        batch.draw(frames[row][frame], centerX - size / 2f,
                centerY - size / 2f, size, size);
    }

    @Override public void dispose() { texture.dispose(); }
}
