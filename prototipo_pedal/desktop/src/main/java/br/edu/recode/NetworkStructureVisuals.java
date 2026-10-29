package br.edu.recode;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Torres de transmissão partidas da Rede Instável. */
final class NetworkStructureVisuals {
    private NetworkStructureVisuals() { }

    static void draw(ShapeRenderer s, int type, float x, float y, float repair,
                     float flash, float charge, boolean interference, float time) {
        float built = smooth(repair);
        float damage = 1f - built;
        float jitter = 0.65f + 0.35f * (float) Math.sin(time * 7f + type * 2.3f);

        // Chassi e dois mastros sustentam uma antena partida, não um marcador de percurso.
        s.setColor(0.015f, 0.04f, 0.10f, 0.75f);
        s.ellipse(x - 44f, y - 33f, 88f, 19f);
        s.setColor(0.055f, 0.13f, 0.22f, 1f);
        s.rect(x - 35f, y - 25f, 70f, 13f);
        s.setColor(0.29f, 0.51f, 0.63f, 1f);
        s.rect(x - 31f, y - 23f, 62f, 3f);
        for (int side = -1; side <= 1; side += 2) {
            float px = x + side * 25f;
            s.setColor(0.035f, 0.10f, 0.18f, 1f);
            s.rect(px - 4f, y - 13f, 8f, 65f);
            s.setColor(0.30f, 0.56f, 0.67f, 1f);
            s.rect(px - 4f, y - 10f, 3f, 61f);
            s.rectLine(px, y + 10f, x + side * 12f, y + 31f, 4f);
        }

        // As duas metades do cabeçote voltam ao lugar enquanto os fios se soldam.
        float leftX = x - 31f - 9f * damage;
        float rightX = x + 3f + 10f * damage;
        float leftY = y + 52f + 7f * damage;
        float rightY = y + 52f - 10f * damage;
        plate(s, leftX, leftY, 28f, 8f);
        plate(s, rightX, rightY, 28f, 8f);
        s.setColor(0.92f, 0.25f, 0.73f, damage * jitter);
        s.rectLine(x - 14f, y + 42f, x - 2f - 5f * damage,
                y + 38f - 8f * damage, 3f);
        s.rectLine(x + 14f, y + 42f, x + 2f + 6f * damage,
                y + 40f + 7f * damage, 3f);
        s.setColor(0.31f, 0.96f, 0.75f, built);
        s.rectLine(x - 14f, y + 42f, x, y + 43f, 4f);
        s.rectLine(x + 14f, y + 42f, x, y + 43f, 4f);

        // Três silhuetas de equipamento de rede: antena, refletor e filtro.
        if (type == 0) {
            s.setColor(0.30f, 0.53f, 0.65f, 1f);
            s.rect(x - 3f, y + 60f + 13f * damage, 6f, 23f);
            s.triangle(x, y + 92f + 13f * damage,
                    x - 10f, y + 77f + 13f * damage,
                    x + 10f, y + 77f + 13f * damage);
        } else if (type == 1) {
            s.setColor(0.30f, 0.53f, 0.65f, 1f);
            s.triangle(x - 23f - 9f * damage, y + 62f,
                    x - 2f - 9f * damage, y + 78f,
                    x - 6f - 9f * damage, y + 62f);
            s.triangle(x + 23f + 10f * damage, y + 62f,
                    x + 2f + 10f * damage, y + 78f,
                    x + 6f + 10f * damage, y + 62f);
            s.setColor(0.31f, 0.96f, 0.75f, built);
            s.rect(x - 3f, y + 64f, 6f, 20f);
        } else {
            for (int i = -1; i <= 1; i++) {
                float px = x + i * (14f + 5f * damage);
                s.setColor(0.045f, 0.15f, 0.25f, 1f);
                s.rect(px - 5f, y + 63f + Math.abs(i) * 7f * damage, 10f, 17f);
                s.setColor(0.28f, 0.53f, 0.64f, 1f);
                s.rect(px - 4f, y + 77f + Math.abs(i) * 7f * damage, 8f, 2f);
            }
        }

        // Carga parcial aparece no corpo; o núcleo só fica verde após o reparo.
        s.setColor(0.015f, 0.055f, 0.12f, 1f);
        s.circle(x, y + 29f, 16f);
        s.setColor(0.28f, 0.52f, 0.63f, 1f);
        s.triangle(x, y + 46f, x - 15f, y + 29f, x, y + 13f);
        s.triangle(x, y + 46f, x + 15f, y + 29f, x, y + 13f);
        s.setColor(0.025f, 0.07f, 0.15f, 1f);
        s.circle(x, y + 29f, 10f);
        s.setColor(interference ? 0.98f : 0.91f, 0.20f, 0.73f,
                damage * (interference ? jitter : 0.72f));
        s.circle(x + 2f * damage, y + 29f, 6f);
        s.setColor(0.34f, 0.99f, 0.78f, built);
        s.circle(x, y + 29f, 8f);
        s.setColor(0.87f, 1f, 0.94f, built);
        s.circle(x - 2f, y + 31f, 3f);
        for (int i = 0; i < 4; i++) {
            s.setColor(i < Math.round(charge * 4f) ? 0.23f : 0.12f,
                    i < Math.round(charge * 4f) ? 0.86f : 0.25f,
                    i < Math.round(charge * 4f) ? 0.95f : 0.35f, 1f);
            s.rect(x - 17f + i * 9f, y - 10f, 5f, 4f);
        }
        s.setColor(0.96f, 0.24f, 0.75f, damage * jitter);
        s.triangle(x - 40f, y + 39f, x - 35f, y + 47f, x - 34f, y + 35f);
        s.triangle(x + 37f, y + 36f, x + 42f, y + 43f, x + 44f, y + 31f);

        if (flash > 0f) {
            float spread = 1f - flash;
            s.setColor(0.35f, 1f, 0.77f, flash * 0.18f);
            s.circle(x, y + 29f, 22f + spread * 44f);
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI / 3d;
                s.setColor(0.77f, 1f, 0.88f, flash);
                s.circle(x + (float) Math.cos(angle) * (26f + spread * 30f),
                        y + 29f + (float) Math.sin(angle) * (26f + spread * 30f), 2.5f);
            }
        }
    }

    private static void plate(ShapeRenderer s, float x, float y, float w, float h) {
        s.setColor(0.035f, 0.11f, 0.20f, 1f);
        s.rect(x, y, w, h);
        s.setColor(0.33f, 0.57f, 0.68f, 1f);
        s.rect(x + 2f, y + h - 3f, w - 4f, 2f);
    }

    private static float smooth(float v) {
        v = Math.max(0f, Math.min(1f, v));
        return v * v * (3f - 2f * v);
    }
}
