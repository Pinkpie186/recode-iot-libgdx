package br.edu.recode;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Marcos da primeira fase: ferragem partida que se recompõe quando ELO a restaura. */
final class RepairStructureVisuals {
    private static final float[] CORRUPT = {0.95f, 0.25f, 0.53f};
    private static final float[] ONLINE = {0.28f, 0.91f, 0.76f};

    private RepairStructureVisuals() { }

    static void draw(ShapeRenderer shapes, int type, float x, float y,
                     float repair, float flash, float time) {
        float assembled = smooth(repair);
        float broken = 1f - assembled;
        float flicker = 0.66f + 0.34f * (float) Math.sin(time * 5f + type * 2.1f);

        // Cada estrutura repousa sobre um suporte largo ligado à trilha.
        shapes.setColor(0.01f, 0.05f, 0.10f, 0.64f);
        shapes.ellipse(x - 55f, y - 32f, 110f, 24f);
        shapes.setColor(0.035f, 0.11f, 0.17f, 1f);
        shapes.rect(x - 47f, y - 25f, 94f, 15f);
        shapes.setColor(0.12f, 0.27f, 0.34f, 1f);
        shapes.rect(x - 44f, y - 22f, 88f, 3f);
        shapes.rect(x - 38f, y - 10f, 13f, 11f);
        shapes.rect(x + 25f, y - 10f, 13f, 11f);
        shapes.setColor(0.03f, 0.08f, 0.13f, 1f);
        shapes.rect(x - 24f, y - 10f, 48f, 13f);

        // Cabos rompidos são recolocados durante a sequência de reparo.
        shapes.setColor(CORRUPT[0], CORRUPT[1], CORRUPT[2], broken * 0.8f);
        shapes.rectLine(x - 44f, y - 2f, x - 26f, y + 10f - 11f * broken, 3f);
        shapes.rectLine(x + 44f, y - 2f, x + 26f, y + 10f - 8f * broken, 3f);
        shapes.setColor(ONLINE[0], ONLINE[1], ONLINE[2], assembled);
        shapes.rectLine(x - 44f, y - 2f, x - 26f, y + 10f, 4f);
        shapes.rectLine(x + 44f, y - 2f, x + 26f, y + 10f, 4f);

        switch (type) {
            case 0: relay(shapes, x, y, assembled, broken); break;
            case 1: bridge(shapes, x, y, assembled, broken); break;
            default: capacitor(shapes, x, y, assembled, broken); break;
        }

        // Energia corrompida oscila no vão; energia restaurada pulsa no núcleo.
        float alpha = broken * flicker;
        shapes.setColor(CORRUPT[0], CORRUPT[1], CORRUPT[2], alpha * 0.23f);
        shapes.circle(x, y + 42f, 19f + flicker * 3f);
        shapes.setColor(CORRUPT[0], CORRUPT[1], CORRUPT[2], alpha);
        shapes.circle(x + 3f * broken, y + 43f, 4f + 2f * flicker);
        if (assembled > 0f) {
            float pulse = 0.74f + 0.26f * (float) Math.sin(time * 3.4f + type);
            shapes.setColor(ONLINE[0], ONLINE[1], ONLINE[2], assembled * 0.22f * pulse);
            shapes.circle(x, y + 43f, 24f);
            shapes.setColor(ONLINE[0], ONLINE[1], ONLINE[2], assembled);
            shapes.circle(x, y + 43f, 8f);
            shapes.setColor(0.91f, 1f, 0.94f, assembled * pulse);
            shapes.circle(x - 2f, y + 45f, 3f);
        }

        // O reparo se propaga para fora como uma onda; peças soltas recolhem ao corpo.
        if (flash > 0f) {
            float spread = 1f - flash;
            shapes.setColor(0.40f, 1f, 0.82f, flash * 0.28f);
            shapes.circle(x, y + 41f, 19f + spread * 57f);
            shapes.setColor(0.03f, 0.08f, 0.13f, flash * 0.7f);
            shapes.circle(x, y + 41f, 15f + spread * 54f);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0 + type * 0.2;
                float px = x + (float) Math.cos(a) * (19f + spread * 42f);
                float py = y + 41f + (float) Math.sin(a) * (19f + spread * 42f);
                shapes.setColor(0.72f, 1f, 0.86f, flash * 0.8f);
                shapes.rect(px - 2f, py - 2f, 4f, 4f);
            }
        }
    }

    private static void relay(ShapeRenderer s, float x, float y, float built, float damage) {
        frameLeg(s, x - 30f, y, built);
        frameLeg(s, x + 21f, y, built);
        // A coroa do relé vem de duas peças que caíram para fora.
        metal(s, x - 34f - 13f * damage, y + 61f + 9f * damage, 31f, 8f);
        metal(s, x + 3f + 13f * damage, y + 61f - 12f * damage, 31f, 8f);
        metal(s, x - 5f, y + 69f + 13f * damage, 10f, 17f);
        s.setColor(0.24f, 0.46f, 0.52f, 1f);
        s.rect(x - 42f - 13f * damage, y + 67f + 9f * damage, 8f, 5f);
        s.rect(x + 34f + 13f * damage, y + 55f - 12f * damage, 8f, 5f);
        s.setColor(ONLINE[0], ONLINE[1], ONLINE[2], built);
        s.rect(x - 2f, y + 86f, 4f, 8f);
        s.circle(x, y + 97f, 5f);
        brokenShard(s, x - 43f, y + 46f, damage);
        brokenShard(s, x + 41f, y + 35f, damage);
    }

    private static void bridge(ShapeRenderer s, float x, float y, float built, float damage) {
        frameLeg(s, x - 40f, y, built);
        frameLeg(s, x + 31f, y, built);
        // A ponte de dados tem dois vãos diagonais e uma travessa partida.
        s.setColor(0.13f, 0.33f, 0.43f, 1f);
        s.rectLine(x - 38f, y + 59f, x - 17f - 8f * damage, y + 77f - 9f * damage, 9f);
        s.rectLine(x + 38f, y + 59f, x + 17f + 8f * damage, y + 77f + 12f * damage, 9f);
        metal(s, x - 18f - 8f * damage, y + 72f - 9f * damage, 17f, 8f);
        metal(s, x + 1f + 8f * damage, y + 72f + 12f * damage, 17f, 8f);
        s.setColor(ONLINE[0], ONLINE[1], ONLINE[2], built);
        s.rect(x - 15f, y + 79f, 30f, 3f);
        s.rect(x - 26f, y + 19f, 4f, 32f);
        s.rect(x + 22f, y + 19f, 4f, 32f);
        brokenShard(s, x - 20f, y + 87f, damage);
        brokenShard(s, x + 18f, y + 91f, damage);
    }

    private static void capacitor(ShapeRenderer s, float x, float y, float built, float damage) {
        frameLeg(s, x - 42f, y, built);
        frameLeg(s, x + 33f, y, built);
        s.setColor(0.045f, 0.14f, 0.21f, 1f);
        s.rect(x - 21f, y + 9f, 42f, 58f);
        s.setColor(0.24f, 0.43f, 0.52f, 1f);
        s.rect(x - 24f - 11f * damage, y + 17f - 7f * damage, 7f, 47f);
        s.rect(x + 17f + 12f * damage, y + 17f + 10f * damage, 7f, 47f);
        metal(s, x - 23f - 11f * damage, y + 64f - 7f * damage, 46f, 8f);
        // Uma bobina de quatro pontas substitui o quadrado do núcleo.
        s.setColor(0.18f, 0.43f, 0.53f, 1f);
        s.triangle(x, y + 20f, x - 17f, y + 43f, x, y + 68f);
        s.triangle(x, y + 20f, x + 17f, y + 43f, x, y + 68f);
        s.setColor(ONLINE[0], ONLINE[1], ONLINE[2], built);
        s.rect(x - 30f, y + 77f, 60f, 3f);
        s.rect(x - 4f, y + 81f, 8f, 12f);
        brokenShard(s, x - 36f, y + 55f, damage);
        brokenShard(s, x + 33f, y + 65f, damage);
    }

    private static void frameLeg(ShapeRenderer s, float x, float y, float built) {
        s.setColor(0.035f, 0.12f, 0.18f, 1f);
        s.rect(x, y + 1f, 9f, 56f);
        s.setColor(0.17f, 0.36f, 0.43f, 1f);
        s.rect(x + 1f, y + 2f, 3f, 54f);
        s.rect(x - 3f, y + 20f, 15f, 5f);
        s.setColor(ONLINE[0], ONLINE[1], ONLINE[2], built * 0.9f);
        s.rect(x + 6f, y + 5f, 2f, 47f);
    }

    private static void metal(ShapeRenderer s, float x, float y, float w, float h) {
        s.setColor(0.025f, 0.11f, 0.17f, 1f);
        s.rect(x, y, w, h);
        s.setColor(0.25f, 0.45f, 0.52f, 1f);
        s.rect(x + 2f, y + h - 3f, w - 4f, 2f);
    }

    private static void brokenShard(ShapeRenderer s, float x, float y, float damage) {
        if (damage <= 0f) return;
        s.setColor(CORRUPT[0], CORRUPT[1], CORRUPT[2], damage * 0.88f);
        s.triangle(x - 3f, y, x + 4f, y + 6f, x + 1f, y - 7f);
        s.rect(x + 6f, y + 10f, 3f, 3f);
    }

    private static float smooth(float v) {
        v = Math.max(0f, Math.min(1f, v));
        return v * v * (3f - 2f * v);
    }
}
