package br.edu.recode;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Fragmentos, torres e pulsos que se deslocam em planos diferentes do fundo. */
final class StageDecor {
    private StageDecor() { }

    static void draw(ShapeRenderer shapes, int width, int height,
                     float journey, float time, float pulse, int phase) {
        float farShift = journey * width * 0.24f;
        float nearShift = journey * width * 0.53f;
        float r = phase == 1 ? 0.13f : phase == 2 ? 0.65f : 0.72f;
        float g = phase == 1 ? 0.78f : phase == 2 ? 0.31f : 0.24f;
        float b = phase == 1 ? 0.92f : 0.94f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 13; i++) {
            float x = wrap(i * width / 9f - farShift, width + 130f) - 65f;
            float y = height * (0.20f + (i % 5) * 0.13f) +
                    (float) Math.sin(time * 0.7f + i) * 8f;
            shapes.setColor(r, g, b, 0.14f + pulse * 0.12f);
            shapes.rect(x, y, 4f, 22f + (i % 3) * 17f);
            shapes.rect(x - 8f, y + 11f, 20f, 3f);
        }
        for (int i = 0; i < 9; i++) {
            float x = wrap(i * width / 6f - nearShift, width + 180f) - 90f;
            float y = height * (0.17f + (i % 4) * 0.18f) +
                    (float) Math.sin(time * 1.2f + i * 2f) * 11f;
            float size = 7f + (i % 3) * 3f;
            shapes.setColor(r, g, b, 0.27f + pulse * 0.20f);
            shapes.triangle(x, y + size, x + size, y, x, y - size);
            shapes.triangle(x, y + size, x - size, y, x, y - size);
            shapes.setColor(1f, 1f, 1f, 0.38f + pulse * 0.26f);
            shapes.circle(x, y, 2.2f);
        }
        shapes.end();
    }

    private static float wrap(float value, float span) {
        float result = value % span;
        return result < 0f ? result + span : result;
    }
}

