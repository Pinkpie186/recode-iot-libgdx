// ReCode: uma passagem do ima = uma volta da manivela do mini pedal.
// Chave reed normalmente aberta entre GPIO 27 e GND. Nunca ligar 5 V ao GPIO.

constexpr uint8_t SENSOR_PIN = 27;
constexpr uint32_t DEBOUNCE_MS = 20;
constexpr uint32_t MIN_ROTATION_MS = 180;
constexpr uint32_t SEND_EVERY_MS = 100;

bool lastRawClosed = false;
bool stableClosed = false;
uint32_t rawChangedAt = 0;
uint32_t lastRotationAt = 0;
uint32_t rotations = 0;
uint32_t sequenceNumber = 0;
uint32_t lastSentAt = 0;

void setup() {
  pinMode(SENSOR_PIN, INPUT_PULLUP);
  Serial.begin(115200);
  lastRawClosed = digitalRead(SENSOR_PIN) == LOW;
  stableClosed = lastRawClosed;
  rawChangedAt = millis();
  lastSentAt = millis();
}

void loop() {
  const uint32_t now = millis();
  const bool rawClosed = digitalRead(SENSOR_PIN) == LOW;

  if (rawClosed != lastRawClosed) {
    lastRawClosed = rawClosed;
    rawChangedAt = now;
  }

  if (rawClosed != stableClosed && now - rawChangedAt >= DEBOUNCE_MS) {
    stableClosed = rawClosed;
    if (stableClosed &&
        (rotations == 0 || now - lastRotationAt >= MIN_ROTATION_MS)) {
      ++rotations;
      lastRotationAt = now;
    }
  }

  if (now - lastSentAt >= SEND_EVERY_MS) {
    lastSentAt = now;
    Serial.print("{\"v\":1,\"seq\":");
    Serial.print(sequenceNumber++);
    Serial.print(",\"ms\":");
    Serial.print(now);
    Serial.print(",\"mode\":\"pedal\",\"cycle\":");
    Serial.print(rotations);
    Serial.print(",\"lastCycleMs\":");
    Serial.print(lastRotationAt);
    Serial.println(",\"state\":\"ok\"}");
  }
}
