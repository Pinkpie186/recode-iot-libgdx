package br.edu.recode;

final class PedalCounter {
    private long lastSequence = -1;
    private long lastDeviceMillis = -1;
    private long lastRotations = -1;
    private long lastCycleMillis = -1;
    private long lastReceivedNanos = -1;
    private long lastRotationNanos = -1;
    private double rpm = 0;

    int accept(PedalFrame frame, long receivedNanos) {
        if (lastSequence < 0 || lastReceivedNanos < 0 ||
                receivedNanos - lastReceivedNanos > 2_000_000_000L) {
            baseline(frame, receivedNanos);
            return 0;
        }

        // O ESP32 pode reiniciar quando a porta serial e aberta.
        if (frame.deviceMillis < lastDeviceMillis && frame.rotations <= lastRotations) {
            baseline(frame, receivedNanos);
            return 0;
        }
        if (frame.sequence <= lastSequence || frame.deviceMillis <= lastDeviceMillis ||
                frame.rotations < lastRotations) return 0;

        long delta = frame.rotations - lastRotations;
        long elapsedMs = frame.deviceMillis - lastDeviceMillis;
        long previousCycleMillis = lastCycleMillis;
        long pulseElapsedMs = frame.lastCycleMillis - lastCycleMillis;
        lastSequence = frame.sequence;
        lastDeviceMillis = frame.deviceMillis;
        lastRotations = frame.rotations;
        lastCycleMillis = frame.lastCycleMillis;
        lastReceivedNanos = receivedNanos;

        // Limite de plausibilidade coerente com o filtro de 180 ms do firmware.
        long maximum = Math.max(1, elapsedMs / 180 + 1);
        if (elapsedMs > 2_000 || delta > maximum || delta > Integer.MAX_VALUE) {
            rpm = 0;
            return 0;
        }
        if (delta > 0) {
            rpm = previousCycleMillis > 0 && pulseElapsedMs > 0
                    ? 60_000.0 * delta / pulseElapsedMs : 0;
            lastRotationNanos = receivedNanos;
        }
        return (int) delta;
    }

    double rpm(long nowNanos) {
        if (lastRotationNanos < 0 || nowNanos - lastRotationNanos > 2_000_000_000L) return 0;
        return rpm;
    }

    private void baseline(PedalFrame frame, long receivedNanos) {
        lastSequence = frame.sequence;
        lastDeviceMillis = frame.deviceMillis;
        lastRotations = frame.rotations;
        lastCycleMillis = frame.lastCycleMillis;
        lastReceivedNanos = receivedNanos;
        lastRotationNanos = -1;
        rpm = 0;
    }
}
