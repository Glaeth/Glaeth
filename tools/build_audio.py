#!/usr/bin/env python3
"""Build game music, effects, and per-size dog barks.

Barks are sliced from the public-domain recording
Wikimedia Commons File:Dog.ogg (PDsounds.org, artist soerena)
and pitch-shifted so each size sounds like a different dog.
Music and effects are original synthesis.
"""

import math
import struct
import wave
from pathlib import Path

SR = 22050
OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "raw"
SOURCE = Path("/tmp/barks/dog.wav")


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


def load_source():
    with wave.open(str(SOURCE)) as handle:
        if handle.getframerate() != SR or handle.getnchannels() != 1:
            raise SystemExit("expected 22050 Hz mono source wav")
        count = handle.getnframes()
        raw = handle.readframes(count)
    return struct.unpack("<" + "h" * count, raw)


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


def main():
    if not SOURCE.exists():
        raise SystemExit(f"missing {SOURCE}; convert File:Dog.ogg to 22050 Hz mono wav first")
    OUT.mkdir(parents=True, exist_ok=True)
    source = load_source()
    write_wav(
        "bark_tiny.wav",
        bark_from(
            source,
            [(1.245, 1.40, 0.07), (0.895, 1.03, 0.0)],
            ratio=2.55,
            hp=280,
        ),
    )
    write_wav(
        "bark_small.wav",
        bark_from(
            source,
            [(46.05, 46.24, 0.085), (46.90, 47.05, 0.0)],
            ratio=2.05,
            hp=180,
        ),
    )
    write_wav(
        "bark_medium.wav",
        bark_from(
            source,
            [(0.575, 0.78, 0.11), (46.42, 46.66, 0.0)],
            ratio=1.35,
            hp=90,
            lp=4200,
        ),
    )
    write_wav(
        "bark_big.wav",
        bark_from(
            source,
            [(72.56, 73.12, 0.0)],
            ratio=0.82,
            hp=60,
            lp=2400,
        ),
    )
    write_wav("sfx_jump.wav", build_jump())
    write_wav("sfx_point.wav", build_point())
    write_wav("sfx_crash.wav", build_crash())
    write_wav("sfx_catch.wav", build_catch())
    write_wav("game_music.wav", build_music())


if __name__ == "__main__":
    main()
