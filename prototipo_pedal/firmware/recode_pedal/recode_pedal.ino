// ReCode: giros do antebraco com MPU6050 no ESP32.
// Fixe o eixo X do modulo ao longo do antebraco e use somente 3,3 V.
// O nome da pasta foi mantido para preservar os caminhos existentes do projeto.
#include <Wire.h>
#include <math.h>

constexpr uint8_t SDA_PIN = 21;
constexpr uint8_t SCL_PIN = 22;
constexpr uint8_t REG_WHO_AM_I = 0x75;
constexpr uint8_t REG_POWER = 0x6B;
constexpr uint8_t REG_ACCEL_CONFIG = 0x1C;
constexpr uint8_t REG_CONFIG = 0x1A;
constexpr uint8_t REG_ACCEL_XOUT_H = 0x3B;
constexpr uint32_t SAMPLE_MS = 20;
constexpr uint32_t SEND_MS = 100;
constexpr uint32_t MIN_GESTURE_MS = 250;
constexpr int CALIBRATION_SAMPLES = 100;
constexpr float ENTER_DEGREES = 25.0f;
constexpr float RELEASE_DEGREES = 10.0f;
// Troque para -1 se a montagem inverter cima/baixo.
constexpr int DIRECTION_SIGN = 1;

uint8_t sensorAddress = 0;
bool sensorReady = false;
bool calibrated = false;
uint32_t lastProbeAt = 0;
uint32_t lastSampleAt = 0;
uint32_t lastSentAt = 0;
uint32_t lastGestureAt = 0;
uint32_t sequenceNumber = 0;
uint32_t movementCount = 0;
int8_t zone = 0;
int8_t lastDirection = 0;
int calibrationCount = 0;
float referenceY = 0.0f;
float referenceZ = 0.0f;
float sumY = 0.0f;
float sumZ = 0.0f;
float filteredY = 0.0f;
float filteredZ = 0.0f;

bool readRegister(uint8_t address, uint8_t reg, uint8_t &value) {
  Wire.beginTransmission(address);
  Wire.write(reg);
  if (Wire.endTransmission(false) != 0 || Wire.requestFrom(address, (uint8_t)1) != 1)
    return false;
  value = Wire.read();
  return true;
}

bool writeRegister(uint8_t address, uint8_t reg, uint8_t value) {
  Wire.beginTransmission(address);
  Wire.write(reg);
  Wire.write(value);
  return Wire.endTransmission() == 0;
}

bool findSensor() {
  const uint8_t addresses[2] = { 0x68, 0x69 };
  for (uint8_t i = 0; i < 2; ++i) {
    const uint8_t address = addresses[i];
    uint8_t identity = 0;
    if (readRegister(address, REG_WHO_AM_I, identity) && identity == 0x68 &&
        writeRegister(address, REG_POWER, 0x01) &&
        writeRegister(address, REG_ACCEL_CONFIG, 0x00) &&
        writeRegister(address, REG_CONFIG, 0x03)) {
      sensorAddress = address;
      calibrated = false;
      calibrationCount = 0;
      sumY = sumZ = 0.0f;
      zone = lastDirection = 0;
      movementCount = lastGestureAt = 0;
      return true;
    }
  }
  return false;
}

bool readAcceleration(int16_t &ax, int16_t &ay, int16_t &az) {
  Wire.beginTransmission(sensorAddress);
  Wire.write(REG_ACCEL_XOUT_H);
  if (Wire.endTransmission(false) != 0 ||
      Wire.requestFrom(sensorAddress, (uint8_t)6) != 6) return false;
  ax = (int16_t)((Wire.read() << 8) | Wire.read());
  ay = (int16_t)((Wire.read() << 8) | Wire.read());
  az = (int16_t)((Wire.read() << 8) | Wire.read());
  return true;
}

void sampleMotion(uint32_t now) {
  int16_t ax, ay, az;
  if (!readAcceleration(ax, ay, az)) {
    sensorReady = calibrated = false;
    return;
  }
  const float g = sqrtf((float)ax * ax + (float)ay * ay + (float)az * az) / 16384.0f;
  // A inclinacao pelo acelerometro so e usada com gravidade proxima de 1 g.
  if (g < 0.70f || g > 1.30f) return;

  if (!calibrated) {
    sumY += ay;
    sumZ += az;
    if (++calibrationCount >= CALIBRATION_SAMPLES) {
      referenceY = sumY / calibrationCount;
      referenceZ = sumZ / calibrationCount;
      if (sqrtf(referenceY * referenceY + referenceZ * referenceZ) < 6000.0f) {
        calibrationCount = 0;
        sumY = sumZ = 0.0f;
        return;
      }
      filteredY = referenceY;
      filteredZ = referenceZ;
      calibrated = true;
    }
    return;
  }

  filteredY = filteredY * 0.82f + ay * 0.18f;
  filteredZ = filteredZ * 0.82f + az * 0.18f;
  const float dot = referenceY * filteredY + referenceZ * filteredZ;
  const float cross = referenceY * filteredZ - referenceZ * filteredY;
  const float angle = DIRECTION_SIGN * atan2f(cross, dot) * 180.0f / PI;
  if (zone != 0) {
    if (fabsf(angle) <= RELEASE_DEGREES) zone = 0;
    return;
  }
  const int8_t next = angle >= ENTER_DEGREES ? 1 : angle <= -ENTER_DEGREES ? -1 : 0;
  if (next == 0 || (movementCount > 0 && now - lastGestureAt < MIN_GESTURE_MS)) return;
  zone = next;
  lastDirection = next;
  movementCount++;
  lastGestureAt = now;
}

void sendFrame(uint32_t now) {
  Serial.print("{\"v\":2,\"seq\":");
  Serial.print(sequenceNumber++);
  Serial.print(",\"ms\":");
  Serial.print(now);
  Serial.print(",\"mode\":\"forearm\",\"movement\":");
  Serial.print(movementCount);
  Serial.print(",\"lastMovementMs\":");
  Serial.print(lastGestureAt);
  Serial.print(",\"direction\":");
  Serial.print(lastDirection);
  Serial.print(",\"state\":\"");
  Serial.print(!sensorReady ? "sensor_error" : !calibrated ? "calibrating" : "ok");
  Serial.println("\"}");
}

void setup() {
  Serial.begin(115200);
  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(400000);
  sensorReady = findSensor();
  lastProbeAt = lastSampleAt = lastSentAt = millis();
}

void loop() {
  const uint32_t now = millis();
  if (!sensorReady && now - lastProbeAt >= 1000) {
    lastProbeAt = now;
    sensorReady = findSensor();
  }
  if (sensorReady && now - lastSampleAt >= SAMPLE_MS) {
    lastSampleAt = now;
    sampleMotion(now);
  }
  if (now - lastSentAt >= SEND_MS) {
    lastSentAt = now;
    sendFrame(now);
  }
}
