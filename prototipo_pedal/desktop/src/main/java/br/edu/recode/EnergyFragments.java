package br.edu.recode;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Pequenos objetos do cenario recolhidos quando ELO alcanca cada trecho. */
final class EnergyFragments {
    static final int COUNT = 3;
    private int collected;
    private float pulse;

    void update(float journey, float delta) {
        while (collected < COUNT && journey >= (collected + 1) / 4f) {
            collected++;
            pulse = 1f;
        }
        pulse = Math.max(0f, pulse - Math.max(0f, delta) * 1.4f);
    }

    int collected() { return collected; }

    void draw(ShapeRenderer shapes, float left, float right, float y, float time) {
        time = VisualComfort.ambientTime(time);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < COUNT; i++) {
            float x = left + (right - left) * (i + 1) / 4f;
            float bob = (float) Math.sin(time * 3f + i * 1.4f) * 5f;
            boolean taken = i < collected;
            float size = taken ? 5f : 11f;
            shapes.setColor(taken ? 0.38f : 0.95f, taken ? 0.98f : 0.71f,
                    taken ? 0.90f : 1f, taken ? 0.65f : 0.85f);
            shapes.triangle(x, y + bob + size, x + size, y + bob, x, y + bob - size);
            shapes.triangle(x, y + bob + size, x - size, y + bob, x, y + bob - size);
            if (taken && i == collected - 1 && pulse > 0f) {
                shapes.setColor(0.35f, 0.95f, 0.88f, pulse * 0.30f);
                shapes.circle(x, y + bob, 19f + (1f - pulse) * 30f);
            }
        }
        shapes.end();
    }
}

