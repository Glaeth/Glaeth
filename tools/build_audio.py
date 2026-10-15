#!/usr/bin/env python3
"""Build game music, effects, and one real bark per dog.

Each bark is a single voiced burst cut from a short Wikimedia Commons
recording (CC BY-SA; see settings_bark_credit). Pitch is left alone.
Music and effects are original synthesis.
"""

import math
import struct
import wave
from pathlib import Path

SR = 22050
OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "raw"
BARK_DIR = Path(__file__).resolve().parent / "audio"


def write_wav(name, samples):
    peak = max(1e-6, max(abs(s) for s in samples))
    data = bytearray()
    for s in samples:
        v = max(-1.0, min(1.0, s / peak * 0.9))
        data += struct.pack("<h", int(v * 32767))
    path = OUT / name
    with wave.open(str(path), "w") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(SR)
        handle.writeframes(data)
    print(f"{name:16} {len(samples) / SR:5.2f}s peak-in {peak:.3f}")


def clip(samples, start, end):
    a = int(start * SR)
    b = int(end * SR)
    return [s / 32768.0 for s in samples[a:b]]


def resample(samples, ratio):
    if not samples:
        return []
    length = max(1, int(len(samples) / ratio))
    out = []
    last = len(samples) - 1
    for index in range(length):
        pos = index * ratio
        left = int(pos)
        right = min(left + 1, last)
        mix = pos - left
        out.append(samples[left] * (1 - mix) + samples[right] * mix)
    return out


def highpass(samples, cutoff):
    coeff = math.exp(-2 * math.pi * cutoff / SR)
    previous = 0.0
    output = 0.0
    result = []
    for sample in samples:
        output = coeff * (output + sample - previous)
        previous = sample
        result.append(output)
    return result


def lowpass(samples, cutoff):
    coeff = math.exp(-2 * math.pi * cutoff / SR)
    state = 0.0
    result = []
    for sample in samples:
        state = (1 - coeff) * sample + coeff * state
        result.append(state)
    return result


def fade(samples, fade_in=0.004, fade_out=0.045):
    count = len(samples)
    enter = max(1, int(fade_in * SR))
    leave = max(1, int(fade_out * SR))
    result = []
    for index, sample in enumerate(samples):
        gain = 1.0
        if index < enter:
            gain *= index / enter
        if index > count - leave:
            gain *= max(0.0, (count - index) / leave)
        result.append(sample * gain)
    return result


def silence(seconds):
    return [0.0] * int(seconds * SR)


def bark_from(source, slices, ratio, hp, lp=None):
    parts = []
    for index, (start, end, gap) in enumerate(slices):
        piece = resample(clip(source, start, end), ratio)
        piece = highpass(piece, hp)
        if lp:
            piece = lowpass(piece, lp)
        parts.extend(fade(piece, 0.003, 0.03))
        if index < len(slices) - 1:
            parts.extend(silence(gap))
    return fade(parts, 0.002, 0.04)


def tone(freq, dur, vol, kind="sine"):
    count = int(dur * SR)
    out = []
    for index in range(count):
        t = index / SR
        phase = (freq * t) % 1.0
        if kind == "tri":
            sample = 4 * abs(phase - 0.5) - 1
        else:
            sample = math.sin(2 * math.pi * phase)
        attack = min(1.0, index / (0.01 * SR))
        release = min(1.0, (count - index) / (0.035 * SR))
        out.append(sample * vol * attack * release)
    return out


def place(buffer, start, samples):
    at = int(start * SR)
    for index, sample in enumerate(samples):
        pos = at + index
        if pos < len(buffer):
            buffer[pos] += sample


def build_music():
    beats = 16
    beat = 0.5
    length = int(beats * beat * SR)
    buffer = [0.0] * length
    melody = [
        (0, 329.63, 0.45, 0.16),
        (1, 392.00, 0.45, 0.15),
        (2, 440.00, 0.48, 0.16),
        (3, 392.00, 0.22, 0.13),
        (3.5, 329.63, 0.22, 0.13),
        (4, 293.66, 0.45, 0.15),
        (5, 329.63, 0.45, 0.15),
        (6, 392.00, 0.9, 0.16),
        (8, 523.25, 0.45, 0.15),
        (9, 440.00, 0.45, 0.14),
        (10, 392.00, 0.45, 0.14),
        (11, 329.63, 0.45, 0.14),
        (12, 293.66, 0.45, 0.14),
        (13, 329.63, 0.45, 0.15),
        (14, 261.63, 0.9, 0.16),
    ]
    for step, freq, dur, vol in melody:
        place(buffer, step * beat, tone(freq, dur, vol, "sine"))
    bass = [(0, 130.81), (4, 110.00), (8, 98.00), (12, 130.81)]
    for step, freq in bass:
        place(buffer, step * beat, tone(freq, 1.7, 0.1, "tri"))
    rng_seed = 17
    for step in range(beats * 2):
        start = int(step * beat * 0.5 * SR)
        burst = int(0.03 * SR)
        state = rng_seed + step * 17
        for index in range(burst):
            state = (1103515245 * state + 12345) & 0x7FFFFFFF
            noise = ((state / 0x7FFFFFFF) * 2 - 1) * 0.035
            env = 1 - index / burst
            pos = start + index
            if pos < length:
                buffer[pos] += noise * env
    # Soft edges so the loop does not click.
    edge = int(0.02 * SR)
    for index in range(edge):
        gain = index / edge
        buffer[index] *= gain
        buffer[-1 - index] *= gain
    return buffer


def build_jump():
    count = int(0.11 * SR)
    out = []
    for index in range(count):
        t = index / count
        freq = 320 + 520 * t
        sample = math.sin(2 * math.pi * freq * (index / SR))
        out.append(sample * (1 - t) ** 1.4 * 0.8)
    return out


def build_point():
    first = tone(880, 0.08, 0.55)
    second = tone(1318.5, 0.11, 0.5)
    return first + silence(0.02) + second


def build_crash():
    count = int(0.28 * SR)
    state = 99
    noise = []
    for _ in range(count):
        state = (1103515245 * state + 12345) & 0x7FFFFFFF
        noise.append((state / 0x7FFFFFFF) * 2 - 1)
    noise = lowpass(noise, 700)
    out = []
    for index, sample in enumerate(noise):
        t = index / count
        thud = math.sin(2 * math.pi * (140 - 80 * t) * (index / SR))
        out.append((sample * 0.7 + thud * 0.5) * (1 - t) ** 1.6)
    return out


def build_catch():
    pop = tone(520, 0.06, 0.4)
    spark = tone(1046, 0.09, 0.35)
    return pop + spark


# One isolated bark each. (file, start seconds, end seconds). No pitch shift.
CLIPS = {
    "bark_pug": ("sound.wav", 0.03, 0.30),
    "bark_pamuk": ("sound.wav", 0.70, 1.00),
    "bark_boncuk": ("amada.wav", 0.16, 0.34),
    "bark_tombul": ("amada.wav", 1.14, 1.34),
    "bark_bebek": ("amada.wav", 1.84, 2.06),
    "bark_kalp": ("amada2.wav", 0.02, 0.34),
    "bark_somun": ("amada2.wav", 0.62, 0.90),
    "bark_kahkaha": ("amada2.wav", 1.46, 1.80),
    "bark_goz": ("amada2.wav", 2.40, 2.74),
    "bark_ceket": ("amada2.wav", 3.32, 3.68),
    "bark_kangal": ("ladrando.wav", 3.88, 4.14),
}


def load_clip(filename):
    path = BARK_DIR / filename
    with wave.open(str(path)) as handle:
        if handle.getframerate() != SR or handle.getnchannels() != 1:
            raise SystemExit(f"{path} must be 22050 Hz mono")
        count = handle.getnframes()
        raw = handle.readframes(count)
    return list(struct.unpack("<" + "h" * count, raw))


def isolated_bark(filename, start, end):
    samples = load_clip(filename)
    begin = int(start * SR)
    stop = min(len(samples), int(end * SR))
    piece = [s / 32768.0 for s in samples[begin:stop]]
    # Drop the room tone that made earlier cuts sound like something being dragged.
    peak = max(1e-6, max(abs(s) for s in piece))
    gate = peak * 0.045
    gated = []
    for sample in piece:
        if abs(sample) >= gate:
            gated.append(sample)
        else:
            gated.append(sample * (abs(sample) / gate) ** 2)
    return fade(highpass(gated, 90), 0.006, 0.03)


def melody_loop(seconds, notes, bass, hat_gap, hat_vol):
    length = int(seconds * SR)
    buffer = [0.0] * length
    for start, freq, dur, vol in notes:
        place(buffer, start, tone(freq, dur, vol, "sine"))
    for start, freq, dur, vol in bass:
        place(buffer, start, tone(freq, dur, vol, "tri"))
    state = 41
    step = 0.0
    while step < seconds:
        at = int(step * SR)
        burst = int(0.025 * SR)
        for index in range(burst):
            state = (1103515245 * state + 12345) & 0x7FFFFFFF
            noise = ((state / 0x7FFFFFFF) * 2 - 1) * hat_vol
            pos = at + index
            if pos < length:
                buffer[pos] += noise * (1 - index / burst)
        step += hat_gap
    edge = int(0.02 * SR)
    for index in range(edge):
        gain = index / edge
        buffer[index] *= gain
        buffer[-1 - index] *= gain
    return buffer


def music_drop():
    # Bright, bouncy, food-catching.
    beat = 0.5
    notes = []
    tune = [523.25, 659.25, 783.99, 659.25, 587.33, 523.25, 440.0, 523.25]
    for index, freq in enumerate(tune * 2):
        notes.append((index * beat, freq, 0.22, 0.16))
    bass = [(i * 2 * beat, 130.81 if i % 2 == 0 else 98.0, 0.7, 0.1) for i in range(8)]
    return melody_loop(8.0, notes, bass, 0.25, 0.03)


def music_flap():
    # Slow and airy.
    notes = []
    chord = [659.25, 783.99, 987.77, 1318.5]
    for bar in range(4):
        for index, freq in enumerate(chord):
            notes.append((bar * 2.0 + index * 0.45, freq, 0.7, 0.11))
    bass = [(i * 2.0, 164.81 if i % 2 == 0 else 196.0, 1.6, 0.07) for i in range(4)]
    return melody_loop(8.0, notes, bass, 1.0, 0.012)


def music_run():
    # Fast and low, for the park run.
    beat = 0.36
    tune = [392.0, 440.0, 349.23, 392.0, 329.63, 349.23, 293.66, 329.63]
    notes = [(index * beat, freq, 0.16, 0.15) for index, freq in enumerate(tune * 2)]
    bass = [(index * beat, 98.0 if index % 4 == 0 else 82.41, 0.16, 0.14) for index in range(16)]
    return melody_loop(len(tune) * 2 * beat, notes, bass, beat / 2, 0.045)


def noise_burst(seconds, cutoff, seed):
    count = int(seconds * SR)
    state = seed
    raw = []
    for _ in range(count):
        state = (1103515245 * state + 12345) & 0x7FFFFFFF
        raw.append((state / 0x7FFFFFFF) * 2 - 1)
    shaped = lowpass(raw, cutoff)
    return [sample * (1 - index / count) ** 1.3 for index, sample in enumerate(shaped)]


def sfx_drop():
    catch = tone(640, 0.05, 0.5) + tone(980, 0.08, 0.45)
    miss = [math.sin(2 * math.pi * 110 * (i / SR)) * (1 - i / int(0.12 * SR)) for i in range(int(0.12 * SR))]
    over = noise_burst(0.22, 500, 7)
    return catch, miss, over


def sfx_flap():
    whoosh = highpass(noise_burst(0.12, 2400, 3), 600)
    point = tone(1568, 0.09, 0.45) + silence(0.02) + tone(2093, 0.12, 0.4)
    crash = noise_burst(0.2, 900, 11)
    return whoosh, point, crash


def sfx_run():
    count = int(0.16 * SR)
    boing = []
    for index in range(count):
        t = index / count
        freq = 220 + 380 * (1 - t) + 40 * math.sin(t * 18)
        boing.append(math.sin(2 * math.pi * freq * (index / SR)) * (1 - t))
    point = tone(880, 0.05, 0.4, "tri") + tone(1320, 0.07, 0.35, "tri")
    crash = noise_burst(0.26, 420, 19)
    return boing, point, crash


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, (filename, start, end) in CLIPS.items():
        write_wav(name + ".wav", isolated_bark(filename, start, end))
    catch, miss, over = sfx_drop()
    write_wav("sfx_drop_catch.wav", catch)
    write_wav("sfx_drop_miss.wav", miss)
    write_wav("sfx_drop_over.wav", over)
    whoosh, point, crash = sfx_flap()
    write_wav("sfx_flap_jump.wav", whoosh)
    write_wav("sfx_flap_point.wav", point)
    write_wav("sfx_flap_crash.wav", crash)
    boing, point, crash = sfx_run()
    write_wav("sfx_run_jump.wav", boing)
    write_wav("sfx_run_point.wav", point)
    write_wav("sfx_run_crash.wav", crash)
    write_wav("music_drop.wav", music_drop())
    write_wav("music_flap.wav", music_flap())
    write_wav("music_run.wav", music_run())


if __name__ == "__main__":
    main()
