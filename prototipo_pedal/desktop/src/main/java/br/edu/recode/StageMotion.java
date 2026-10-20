package br.edu.recode;

/** Suaviza apenas a apresentacao: os objetivos continuam usando o progresso real. */
final class StageMotion {
    private float x = Float.NaN;
    private float journey = Float.NaN;
    private float speed;

    void reset() {
        x = Float.NaN;
        journey = Float.NaN;
        speed = 0f;
    }

    void update(float targetX, float targetJourney, float delta, boolean paused) {
        if (Float.isNaN(x)) x = targetX;
        if (Float.isNaN(journey)) journey = targetJourney;
        if (paused) {
            speed = 0f;
            return;
        }
        float step = Math.max(0f, Math.min(delta, 0.05f));
        float previousX = x;
        x += (targetX - x) * (1f - (float) Math.exp(-10f * step));
        journey += (targetJourney - journey) * (1f - (float) Math.exp(-4f * step));
        if (Math.abs(targetX - x) < 0.2f) x = targetX;
        if (Math.abs(targetJourney - journey) < 0.0002f) journey = targetJourney;
        speed = step > 0f ? Math.min(1f, Math.abs(x - previousX) / (step * 170f)) : 0f;
    }

    float x() { return x; }
    float journey() { return journey; }
    boolean moving() { return speed > 0.08f; }
    float gait(float time) { return moving() ? (float) Math.sin(time * 24f) * 2f * speed : 0f; }
}
