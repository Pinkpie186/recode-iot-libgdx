package br.edu.recode;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Animacoes compartilhadas do impacto de No Zero e da defesa do mascote. */
final class CombatVisuals {
    private static final Color HIT = new Color(1f, 0.21f, 0.47f, 1f);
    private static final Color GUARD = new Color(0.34f, 0.96f, 1f, 1f);
    private static final float ATTACK_FLIGHT_SECONDS = 0.55f;
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

    void draw(ShapeRenderer shapes, float x, float y, float time, MascotShield shield) {
        time = VisualComfort.ambientTime(time);
        if (shield.active()) {
            float breathe = (float) Math.sin(time * 5f) * 3f;
            shapes.setColor(GUARD.r, GUARD.g, GUARD.b, 0.12f);
            shapes.circle(x, y, 57f + breathe);
            for (int i = 0; i < 6; i++) {
                double angle = time * 1.6 + i * Math.PI / 3.0;
                shapes.setColor(GUARD.r, GUARD.g, GUARD.b, 0.48f);
                shapes.circle(x + (float) Math.cos(angle) * 58f,
                        y + (float) Math.sin(angle) * 58f, 3.5f);
            }
        }
        if (hitActive()) {
            float pulse = hitStrength();
            shapes.setColor(HIT.r, HIT.g, HIT.b, pulse * 0.38f);
            shapes.circle(x, y, 38f + (1f - pulse) * 55f);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0 + time * 3f;
                shapes.rect(x + (float) Math.cos(angle) * (45f + (1f - pulse) * 35f),
                        y + (float) Math.sin(angle) * (45f + (1f - pulse) * 35f), 6f, 6f);
            }
        }
        if (blockedSeconds > 0f || shieldStartSeconds > 0f) {
            float pulse = Math.max(blockedSeconds / 0.75f, shieldStartSeconds / 0.8f);
            shapes.setColor(GUARD.r, GUARD.g, GUARD.b, pulse * 0.37f);
            shapes.circle(x, y, 62f + (1f - pulse) * 48f);
        }
    }
}

