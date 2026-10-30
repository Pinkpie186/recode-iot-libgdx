package br.edu.recode;

/** Gesto exclusivo da chave de rota: sair do neutro e sustentar a inclinacao. */
final class RouteHoldGesture {
    static final long HOLD_MILLIS = 650L;
    static final float ENTER_DEGREES = 25f;
    static final float NEUTRAL_DEGREES = 10f;

    private boolean armed;
    private long startedMillis = -1L;
    private long lastMillis = -1L;

    boolean accept(long deviceMillis, float angleDeg, int requiredDirection) {
        if (!Float.isFinite(angleDeg) || requiredDirection == 0 ||
                lastMillis >= 0 && deviceMillis <= lastMillis) {
            reset();
            return false;
        }
        lastMillis = deviceMillis;
        if (Math.abs(angleDeg) <= NEUTRAL_DEGREES) {
            armed = true;
            startedMillis = -1L;
            return false;
        }
        if (!armed || angleDeg * requiredDirection < ENTER_DEGREES) {
            startedMillis = -1L;
            return false;
        }
        if (startedMillis < 0L) startedMillis = deviceMillis;
        if (deviceMillis - startedMillis < HOLD_MILLIS) return false;
        reset();
        return true;
    }

    float fraction() {
        return startedMillis < 0L ? 0f :
                Math.min(1f, (lastMillis - startedMillis) / (float) HOLD_MILLIS);
    }

    void reset() {
        armed = false;
        startedMillis = lastMillis = -1L;
    }
}
