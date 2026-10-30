package br.edu.recode;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Módulos partidos que ligam as duas rotas do Sistema Central. */
final class CoreConduitVisuals {
    private CoreConduitVisuals() { }

    static void draw(ShapeRenderer s, float x, float upperY, float lowerY,
                     float repair, float flash, boolean routeLit, int activeRoute,
                     float charge, float time) {
        float built = smooth(repair);
        float damage = 1f - built;
        float middle = (upperY + lowerY) * 0.5f;
        float flicker = 0.65f + 0.35f * (float) Math.sin(time * 6f + x * 0.03f);

        // Espinha de transmissão com ruptura visível entre as duas rotas.
        s.setColor(0.035f, 0.075f, 0.15f, 1f);
        s.rect(x - 8f, lowerY + 13f, 16f, upperY - lowerY - 26f);
        s.setColor(0.27f, 0.43f, 0.59f, 1f);
        s.rect(x - 8f, middle + 11f + 9f * damage, 3f,
                upperY - middle - 24f - 9f * damage);
        s.rect(x + 5f, lowerY + 13f, 3f,
                middle - lowerY - 24f - 8f * damage);
        s.setColor(0.94f, 0.24f, 0.76f, damage * flicker);
        s.rectLine(x - 5f, middle + 10f + 9f * damage,
                x + 3f, middle + 2f, 3f);
        s.rectLine(x + 5f, middle - 10f - 8f * damage,
                x - 3f, middle - 2f, 3f);
        s.triangle(x - 13f, middle + 3f, x - 7f, middle + 11f,
                x - 5f, middle - 4f);

        // A chave de rota energiza um lado antes de o módulo inteiro ser recuperado.
        for (int route = 0; route < 2; route++) {
            float y = route == 0 ? upperY : lowerY;
            boolean energized = built > 0f || routeLit && route == activeRoute;
            drawSocket(s, x, y, damage, energized ? built > 0f ? built : 0.85f : 0f,
                    flicker, route == 0);
        }

        if (built > 0f) {
            s.setColor(0.31f, 0.95f, 0.75f, built);
            s.rect(x - 3f, lowerY + 13f, 6f, upperY - lowerY - 26f);
            s.setColor(0.82f, 1f, 0.90f, built * 0.6f);
            s.rect(x - 1f, lowerY + 16f, 2f, upperY - lowerY - 32f);
        } else if (routeLit) {
            s.setColor(0.23f, 0.83f, 0.98f, 0.76f);
            float y = activeRoute == 0 ? upperY : lowerY;
            s.rect(x - 2f, Math.min(y, middle), 4f, Math.abs(y - middle) - 13f);
        }

        // Três inscrições de carga tornam a sequência legível sem texto sobre o chefe.
        for (int i = 0; i < 3; i++) {
            boolean charged = built > 0f || i < Math.round(charge * 3f);
            s.setColor(charged ? 0.32f : 0.16f, charged ? 0.90f : 0.28f,
                    charged ? 0.84f : 0.42f, 1f);
            s.rect(x - 12f + i * 9f, middle - 3f, 6f, 6f);
        }

        if (flash > 0f) {
            float spread = 1f - flash;
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4d;
                s.setColor(0.82f, 1f, 0.92f, flash * 0.85f);
                float sparkX = x + (float) Math.cos(angle) * (22f + spread * 35f);
                float sparkY = middle + (float) Math.sin(angle) * (22f + spread * 35f);
                s.triangle(sparkX, sparkY + 3f, sparkX + 3f, sparkY,
                        sparkX, sparkY - 3f);
            }
        }
    }

    private static void drawSocket(ShapeRenderer s, float x, float y,
                                   float damage, float energy, float flicker,
                                   boolean upper) {
        float side = upper ? 1f : -1f;
        s.setColor(0.035f, 0.09f, 0.17f, 1f);
        s.rect(x - 16f, y - 10f, 32f, 20f);
        s.setColor(0.28f, 0.47f, 0.61f, 1f);
        s.triangle(x - 22f, y, x - 12f, y + 12f,
                x - 12f, y - 12f);
        s.triangle(x + 22f, y, x + 12f, y + 12f,
                x + 12f, y - 12f);
        // Coletores quebrados se aproximam da tomada central.
        s.setColor(0.055f, 0.17f, 0.27f, 1f);
        s.rect(x - 12f - 8f * damage, y + 12f * side + 5f * damage,
                11f, 7f);
        s.rect(x + 1f + 8f * damage, y + 12f * side - 5f * damage,
                11f, 7f);
        s.setColor(0.35f, 0.59f, 0.71f, 1f);
        s.rectLine(x - 12f - 8f * damage, y + 12f * side + 8f * damage,
                x - 1f - 8f * damage, y + 12f * side + 8f * damage, 2f);
        s.rectLine(x + 1f + 8f * damage, y + 12f * side - 2f * damage,
                x + 12f + 8f * damage, y + 12f * side - 2f * damage, 2f);
        s.setColor(0.94f, 0.24f, 0.76f, damage * flicker);
        s.rectLine(x - 8f, y + 3f, x - 1f - 5f * damage, y + 8f, 2.5f);
        s.rectLine(x + 8f, y - 3f, x + 1f + 5f * damage, y - 8f, 2.5f);
        diamond(s, x, y, 5f);
        s.setColor(0.32f, 0.95f, 0.78f, energy);
        s.rect(x - 10f, y + 12f * side, 20f, 3f);
        diamond(s, x, y, 7f);
        s.setColor(0.89f, 1f, 0.94f, energy);
        diamond(s, x - 2f, y + 2f, 2.5f);
    }

    private static void diamond(ShapeRenderer s, float x, float y, float radius) {
        s.triangle(x, y + radius, x + radius, y, x, y - radius);
        s.triangle(x, y + radius, x - radius, y, x, y - radius);
    }

    private static float smooth(float v) {
        v = Math.max(0f, Math.min(1f, v));
        return v * v * (3f - 2f * v);
    }
}
