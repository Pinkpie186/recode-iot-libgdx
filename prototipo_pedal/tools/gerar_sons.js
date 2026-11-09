// Gera trilhas e efeitos originais do ReCode, sem arquivos de terceiros.
const fs = require('fs');
const path = require('path');
const RATE = 22050;
const output = path.join(__dirname, '..', 'desktop', 'src', 'main', 'resources', 'audio');
fs.mkdirSync(output, {recursive: true});
let seed = 5206;
const random = () => ((seed = (1664525 * seed + 1013904223) >>> 0) / 4294967296) * 2 - 1;
const midi = note => 440 * 2 ** ((note - 69) / 12);
const buffer = seconds => new Float32Array(Math.ceil(seconds * RATE));

function tone(out, start, length, note, volume, voice = 'soft', attack = 0.02) {
  const first = Math.floor(start * RATE);
  const count = Math.min(Math.floor(length * RATE), out.length - first);
  if (first < 0 || count <= 0) return;
  for (let j = 0; j < count; j++) {
    const t = j / RATE;
    let envelope = Math.min(1, t / attack) *
      Math.max(0, Math.min(1, (length - t) / Math.min(0.24, length * 0.45)));
    const phase = 2 * Math.PI * midi(note) * t;
    let signal;
    if (voice === 'bell') {
      signal = Math.sin(phase) + 0.27 * Math.sin(2.01 * phase) + 0.12 * Math.sin(3.97 * phase);
      envelope *= Math.exp(-2 * t / Math.max(0.1, length));
    } else if (voice === 'bass') {
      signal = 0.8 * Math.sin(phase) + 0.22 * Math.sin(2 * phase);
      envelope *= Math.exp(-1.8 * t / Math.max(0.1, length));
    } else if (voice === 'edge') {
      signal = 0.62 * Math.sin(phase) + 0.25 * Math.sin(3 * phase) + 0.1 * Math.sin(5 * phase);
    } else {
      signal = Math.sin(phase) + 0.18 * Math.sin(2 * phase);
    }
    out[first + j] += signal * envelope * volume;
  }
}

function drum(out, start, strength, kind) {
  const first = Math.floor(start * RATE);
  const length = kind === 'kick' ? 0.21 : 0.11;
  for (let j = 0; j < Math.min(Math.floor(length * RATE), out.length - first); j++) {
    const t = j / RATE;
    const signal = kind === 'kick'
      ? Math.sin(2 * Math.PI * (78 - 42 * t / length) * t) * Math.exp(-19 * t)
      : random() * Math.exp(-36 * t);
    out[first + j] += signal * strength;
  }
}

function save(name, samples, peak = 0.82) {
  let maximum = 0;
  for (const value of samples) maximum = Math.max(maximum, Math.abs(value));
  const scale = Math.min(1, peak / (maximum || 1));
  const wav = Buffer.alloc(44 + samples.length * 2);
  wav.write('RIFF', 0);
  wav.writeUInt32LE(wav.length - 8, 4);
  wav.write('WAVEfmt ', 8);
  wav.writeUInt32LE(16, 16);
  wav.writeUInt16LE(1, 20);
  wav.writeUInt16LE(1, 22);
  wav.writeUInt32LE(RATE, 24);
  wav.writeUInt32LE(RATE * 2, 28);
  wav.writeUInt16LE(2, 32);
  wav.writeUInt16LE(16, 34);
  wav.write('data', 36);
  wav.writeUInt32LE(samples.length * 2, 40);
  for (let i = 0; i < samples.length; i++)
    wav.writeInt16LE(Math.round(Math.tanh(samples[i] * scale) * 32767), 44 + i * 2);
  fs.writeFileSync(path.join(output, name), wav);
}

function music(name, bpm, chords, melody, intensity) {
  const beat = 60 / bpm;
  const out = buffer(16 * beat);
  chords.forEach((chord, bar) => {
    const start = bar * 4 * beat;
    chord.forEach(note => tone(out, start, 3.95 * beat, note, 0.055, 'soft', 0.20));
    if (intensity === 0) {
      tone(out, start, 2.8 * beat, chord[0] - 12, 0.085, 'bass', 0.08);
    } else {
      for (let step = 0; step < 4; step++) {
        const at = start + step * beat;
        tone(out, at, beat * 0.87, chord[0] - 12, 0.12 + intensity * 0.025, 'bass');
        drum(out, at, 0.095, 'kick');
        if (step % 2) drum(out, at, 0.07, 'hat');
      }
    }
    for (let step = 0; step < 4; step++) {
      const note = melody[bar * 4 + step];
      if (note != null) tone(out, start + step * beat, 0.72 * beat, note,
        intensity === 0 ? 0.08 : 0.115, intensity === 0 ? 'bell' : 'edge');
    }
  });
  for (let j = 0; j < Math.min(Math.floor(0.14 * RATE), out.length); j++)
    out[out.length - j - 1] *= j / (0.14 * RATE);
  save(name, out, 0.72);
}

music('menu.wav', 88,
  [[48, 55, 60], [46, 53, 58], [44, 51, 56], [43, 50, 55]],
  [72, null, 79, null, 70, null, 77, null,
    68, null, 75, null, 67, null, 74, null], 0);
music('fase_1.wav', 80,
  [[48, 55, 60], [53, 60, 65], [50, 57, 62], [55, 62, 67]],
  [72, null, 76, 79, 77, null, 72, 69, 74, null, 77, 81, 79, 76, 74, null], 0);
music('fase_2.wav', 120,
  [[45, 52, 57], [48, 55, 60], [43, 50, 55], [45, 52, 57]],
  [69, 72, 76, 72, 67, 72, 75, 72, 67, 70, 74, 70, 69, 72, 76, 72], 2);
music('fase_3.wav', 132,
  [[41, 48, 53], [44, 51, 56], [46, 53, 58], [40, 47, 52]],
  [65, 68, 72, 68, 63, 68, 72, 75, 70, 74, 77, 74, 64, 67, 71, 76], 3);

const repair = buffer(0.55);
[[0, 72], [0.11, 76], [0.22, 79], [0.33, 84]]
  .forEach(([at, note]) => tone(repair, at, 0.21, note, 0.25, 'bell'));
save('reparo.wav', repair);

const shield = buffer(0.72);
[[0, 67], [0.09, 74], [0.18, 79], [0.27, 86], [0.37, 91]]
  .forEach(([at, note]) => tone(shield, at, 0.31, note, 0.21, 'bell'));
save('escudo.wav', shield);

const hit = buffer(0.45);
for (let j = 0; j < hit.length; j++) {
  const t = j / RATE;
  hit[j] = (0.35 * Math.sin(2 * Math.PI * (190 - 230 * t) * t) +
    0.19 * random()) * Math.exp(-9 * t);
}
save('impacto.wav', hit);

const defeat = buffer(1.7);
[[0, 64], [0.28, 60], [0.56, 56], [0.84, 52], [1.12, 40]]
  .forEach(([at, note]) => tone(defeat, at, 0.48, note, 0.23, 'edge', 0.03));
save('fim_de_jogo.wav', defeat);

const navigate = buffer(0.09);
tone(navigate, 0, 0.085, 79, 0.20, 'bell', 0.004);
save('navegar.wav', navigate);

const confirm = buffer(0.22);
tone(confirm, 0, 0.14, 72, 0.21, 'bell', 0.005);
tone(confirm, 0.07, 0.14, 79, 0.20, 'bell', 0.005);
save('confirmar.wav', confirm);
