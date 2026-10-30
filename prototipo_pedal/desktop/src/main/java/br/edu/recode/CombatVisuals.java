package br.edu.recode;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Animacoes compartilhadas do impacto de No Zero e da defesa do mascote. */
final class CombatVisuals {
    private static final Color HIT = new Color(1f, 0.21f, 0.47f, 1f);
    private static final Color GUARD = new Color(0.34f, 0.96f, 1f, 1f);
    private static final float ATTACK_FLIGHT_SECONDS = 0.72f;
    private static final float IMPACT_BEAM_SECONDS = 0.30f;
    private float hitSeconds;
    private float blockedSeconds;
    private float shieldStartSeconds;
    private float attackFlightSeconds;
    private float impactBeamSeconds;
    private boolean attackBlocked;

    void hit() { hitSeconds = 0.9f; }
    void blocked() { blockedSeconds = 0.75f; }
    void shieldStarted() { shieldStartSeconds = 0.8f; }

    void reset() {
        hitSeconds = blockedSeconds = shieldStartSeconds = 0f;
        attackFlightSeconds = impactBeamSeconds = 0f;
        attackBlocked = false;
    }

    void launchHit() { launch(false); }
    void launchBlocked() { launch(true); }

    private void launch(boolean blocked) {
        attackBlocked = blocked;
        attackFlightSeconds = ATTACK_FLIGHT_SECONDS;
        impactBeamSeconds = 0f;
        hitSeconds = blockedSeconds = 0f;
    }

    void update(float delta) {
        hitSeconds = Math.max(0f, hitSeconds - delta);
        blockedSeconds = Math.max(0f, blockedSeconds - delta);
        shieldStartSeconds = Math.max(0f, shieldStartSeconds - delta);
        impactBeamSeconds = Math.max(0f, impactBeamSeconds - delta);
        if (attackFlightSeconds > 0f) {
            attackFlightSeconds = Math.max(0f, attackFlightSeconds - delta);
            if (attackFlightSeconds == 0f) {
                impactBeamSeconds = IMPACT_BEAM_SECONDS;
                if (attackBlocked) blocked();
                else hit();
            }
        }
    }

    boolean hitActive() { return hitSeconds > 0f; }
    float hitStrength() { return hitSeconds / 0.9f; }
    boolean blockedActive() { return blockedSeconds > 0f; }
    float blockedStrength() { return blockedSeconds / 0.75f; }
    boolean attackFlying() { return attackFlightSeconds > 0f; }
    float attackFlightProgress() { return 1f - attackFlightSeconds / ATTACK_FLIGHT_SECONDS; }
    boolean attackBlocked() { return attackBlocked; }
    float impactBeamStrength() { return impactBeamSeconds / IMPACT_BEAM_SECONDS; }
    float hitOffset(float time) {
        return hitActive() && !VisualComfort.softEffects()
                ? (float) Math.sin(time * 72f) * 9f * hitStrength() : 0f;
    }

    /** Orbe em perspectiva: nasce no chefe, cruza a arena e termina em ELO ou no escudo. */
    void drawProjectile(ShapeRenderer shapes, float originX, float originY,
                        float targetX, float targetY, float time) {
        if (!attackFlying() && impactBeamSeconds <= 0f) return;
        float dx = targetX - originX;
        float dy = targetY - originY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float reach = attackBlocked && distance > 0f
                ? Math.max(0f, 1f - 53f / distance) : 1f;
        float endX = originX + dx * reach;
        float endY = originY + dy * reach;
        if (attackFlying()) {
            float progress = attackFlightProgress();
            float travel = progress * progress * (3f - 2f * progress);
            float x = originX + (endX - originX) * travel;
            float y = originY + (endY - originY) * travel +
                    (float) Math.sin(travel * Math.PI) * 24f;
            // A indicacao antecipada deixa clara a direcao do disparo.
            shapes.setColor(HIT.r, HIT.g, HIT.b, 0.16f * (1f - progress * 0.5f));
            shapes.rectLine(originX, originY, endX, endY, 2f);
            for (int i = 3; i >= 1; i--) {
                float old = Math.max(0f, travel - i * 0.045f);
                float trailX = originX + (endX - originX) * old;
                float trailY = originY + (endY - originY) * old +
                        (float) Math.sin(old * Math.PI) * 24f;
                shapes.setColor(HIT.r, HIT.g, HIT.b, (4 - i) * 0.13f);
                shapes.rectLine(trailX, trailY, x, y, 2f + (4 - i) * 2f);
            }
            float radius = 6f + 11f * travel;
            shapes.setColor(HIT.r, HIT.g, HIT.b, 0.27f);
            shapes.circle(x, y, radius * 1.8f);
            shapes.setColor(1f, 0.73f, 0.95f, 0.95f);
            shapes.circle(x, y, radius);
            shapes.setColor(1f, 1f, 1f, 0.90f);
            shapes.circle(x, y, radius * 0.36f);
        } else {
            float strength = impactBeamStrength();
            float radius = 18f + (1f - strength) * 45f;
            shapes.setColor(attackBlocked ? GUARD.r : HIT.r,
                    attackBlocked ? GUARD.g : HIT.g,
                    attackBlocked ? GUARD.b : HIT.b, strength * 0.42f);
            for (int i = 0; i < 10; i++) {
                double angle = i * Math.PI / 5d + time * 0.8f;
                float ringX = endX + (float) Math.cos(angle) * radius;
                float ringY = endY + (float) Math.sin(angle) * radius;
                float nextX = endX + (float) Math.cos(angle + 0.22f) * radius;
                float nextY = endY + (float) Math.sin(angle + 0.22f) * radius;
                shapes.rectLine(ringX, ringY, nextX, nextY, 2f + strength * 4f);
            }
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI / 3d + time * 0.8f;
                float shardX = endX + (float) Math.cos(angle) * radius;
                float shardY = endY + (float) Math.sin(angle) * radius;
                shapes.rectLine(endX + (shardX - endX) * 0.68f,
                        endY + (shardY - endY) * 0.68f,
                        shardX, shardY, 2f + strength * 3f);
            }
        }
    }

    void draw(ShapeRenderer shapes, float x, float y, float time, MascotShield shield) {
        time = VisualComfort.ambientTime(time);
        if (shield.active()) {
            float breathe = (float) Math.sin(time * 5f) * 3f;
            float radius = 57f + breathe;
            shapes.setColor(GUARD.r, GUARD.g, GUARD.b, 0.045f);
            shapes.circle(x, y, radius);
            shapes.setColor(GUARD.r, GUARD.g, GUARD.b, 0.58f);
            for (int i = 0; i < 24; i++) {
                double angle = i * Math.PI / 12d;
                shapes.rectLine(x + (float) Math.cos(angle) * radius,
                        y + (float) Math.sin(angle) * radius,
                        x + (float) Math.cos(angle + 0.19f) * radius,
                        y + (float) Math.sin(angle + 0.19f) * radius, 2.5f);
            }
            for (int i = 0; i < 6; i++) {
                double angle = time * 1.6 + i * Math.PI / 3.0;
                shapes.setColor(GUARD.r, GUARD.g, GUARD.b, 0.48f);
                shapes.circle(x + (float) Math.cos(angle) * radius,
                        y + (float) Math.sin(angle) * radius, 3.5f);
            }
        }
        if (hitActive()) {
            float pulse = hitStrength();
            float radius = 32f + (1f - pulse) * 48f;
            for (int i = 0; i < 10; i++) {
                double angle = i * Math.PI / 5.0 + time * 0.45f;
                float x1 = x + (float) Math.cos(angle) * radius;
                float y1 = y + (float) Math.sin(angle) * radius;
                float x2 = x + (float) Math.cos(angle + 0.18f) * radius;
                float y2 = y + (float) Math.sin(angle + 0.18f) * radius;
                shapes.setColor(HIT.r, HIT.g, HIT.b, pulse * 0.75f);
                shapes.rectLine(x1, y1, x2, y2, 2f + pulse * 3f);
                shapes.setColor(1f, 0.82f, 0.94f, pulse * 0.7f);
                shapes.circle(x1, y1, 2f + pulse * 3f);
            }
        }
        if (blockedSeconds > 0f || shieldStartSeconds > 0f) {
            float pulse = Math.max(blockedSeconds / 0.75f, shieldStartSeconds / 0.8f);
            float radius = 62f + (1f - pulse) * 48f;
            shapes.setColor(GUARD.r, GUARD.g, GUARD.b, pulse * 0.62f);
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6d;
                shapes.rectLine(x + (float) Math.cos(angle) * radius,
                        y + (float) Math.sin(angle) * radius,
                        x + (float) Math.cos(angle + 0.19f) * radius,
                        y + (float) Math.sin(angle + 0.19f) * radius, 3f);
            }
        }
    }

    /** Defesa discreta da fase 3: facetas vazadas deixam ELO e o chefe visiveis. */
    void drawCompact(ShapeRenderer shapes, float x, float y, float time,
                     MascotShield shield) {
        time = VisualComfort.ambientTime(time);
        if (shield.active() || blockedSeconds > 0f || shieldStartSeconds > 0f) {
            float feedback = Math.max(blockedSeconds / 0.75f,
                    shieldStartSeconds / 0.8f);
            float radius = 39f + feedback * 5f;
            float opacity = shield.active() ? 0.72f : feedback * 0.72f;
            shapes.setColor(GUARD.r, GUARD.g, GUARD.b, opacity);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3d - Math.PI / 6d;
                double b = (i + 1) * Math.PI / 3d - Math.PI / 6d;
                shapes.rectLine(x + (float) Math.cos(a) * radius,
                        y + (float) Math.sin(a) * radius,
                        x + (float) Math.cos(b) * radius,
                        y + (float) Math.sin(b) * radius, 2.2f);
            }
            if (shield.active()) {
                float side = (float) Math.sin(time * 4f) * 0.16f;
                shapes.setColor(0.85f, 1f, 0.95f, 0.55f + side);
                shapes.triangle(x, y + radius + 5f, x + 5f, y + radius,
                        x, y + radius - 5f);
            }
        }
        if (hitActive()) {
            float pulse = hitStrength();
            shapes.setColor(HIT.r, HIT.g, HIT.b, pulse * 0.85f);
            for (int side = -1; side <= 1; side += 2) {
                shapes.rectLine(x + side * 27f, y - 25f,
                        x + side * (30f + (1f - pulse) * 17f), y - 8f, 3f);
                shapes.rectLine(x + side * 27f, y + 25f,
                        x + side * (30f + (1f - pulse) * 17f), y + 8f, 3f);
            }
        }
    }
}

