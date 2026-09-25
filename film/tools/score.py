import json
import sys
import wave
from pathlib import Path

import numpy as np
from scipy import ndimage, signal

FILM = Path(__file__).resolve().parents[1]
OUT = FILM / "public" / "audio" / "score.wav"
TIMELINE = json.loads((FILM / "src" / "timeline.json").read_text())
CUE = TIMELINE["cues"]
SR = 48000
DURATION = float(TIMELINE["duration"])
LENGTH = int(round(DURATION * SR))
BEAT = 60.0 / TIMELINE["bpm"]
RNG = np.random.default_rng(1453)

CHORD = {
    "D": [50, 57, 62, 66, 69],
    "Eb": [51, 58, 63, 67, 70],
    "Cm": [48, 55, 60, 63, 67],
    "Gm": [43, 50, 55, 58, 62],
}
BASS_RIFF = {
    "D": [38, 38, 50, 38, 39, 38, 42, 39],
    "Eb": [39, 39, 51, 39, 38, 39, 43, 42],
    "Cm": [36, 36, 48, 36, 38, 36, 39, 38],
}
ROOT = {"D": 38, "Eb": 39, "Cm": 36, "Gm": 31}
OSTINATO = {
    "D": [69, 74, 75, 78, 75, 74, 69, 74],
    "Eb": [70, 75, 74, 75, 79, 78, 75, 74],
    "Cm": [67, 72, 74, 75, 74, 72, 70, 69],
}


def samples(seconds: float) -> int:
    return max(1, int(round(seconds * SR)))


def clock(n: int) -> np.ndarray:
    return np.arange(n) / SR


def midi(note: float) -> float:
    return 440.0 * 2.0 ** ((note - 69.0) / 12.0)


def noise(n: int) -> np.ndarray:
    return RNG.standard_normal(n)


def lowpass(x: np.ndarray, cutoff: float, order: int = 2) -> np.ndarray:
    sos = signal.butter(order, min(cutoff, SR * 0.45), btype="lowpass", fs=SR, output="sos")
    return signal.sosfilt(sos, x, axis=-1)


def highpass(x: np.ndarray, cutoff: float, order: int = 2) -> np.ndarray:
    sos = signal.butter(order, cutoff, btype="highpass", fs=SR, output="sos")
    return signal.sosfilt(sos, x, axis=-1)


def bandpass(x: np.ndarray, low: float, high: float, order: int = 2) -> np.ndarray:
    sos = signal.butter(order, [low, min(high, SR * 0.45)], btype="bandpass", fs=SR, output="sos")
    return signal.sosfilt(sos, x, axis=-1)


def sweep(x: np.ndarray, cutoffs: np.ndarray, kind: str = "lowpass", width: float = 1.4, block: int = 256) -> np.ndarray:
    y = np.empty_like(x)
    state = None
    for start in range(0, len(x), block):
        end = min(len(x), start + block)
        c = float(np.clip(cutoffs[start], 30.0, SR * 0.42 / width))
        if kind == "lowpass":
            sos = signal.butter(2, c, btype="lowpass", fs=SR, output="sos")
        else:
            sos = signal.butter(2, [c / width, c * width], btype="bandpass", fs=SR, output="sos")
        if state is None or state.shape[0] != sos.shape[0]:
            state = np.zeros((sos.shape[0], 2))
        y[start:end], state = signal.sosfilt(sos, x[start:end], zi=state)
    return y


def edges(x: np.ndarray, attack: float = 0.002, release: float = 0.01) -> np.ndarray:
    n = x.shape[-1]
    env = np.ones(n)
    a = min(n, samples(attack))
    r = min(n, samples(release))
    env[:a] = np.sin(np.linspace(0, np.pi / 2, a)) ** 2
    env[n - r:] *= np.cos(np.linspace(0, np.pi / 2, r)) ** 2
    return x * env


def stereo(sound: np.ndarray, pan: float = 0.0) -> np.ndarray:
    if sound.ndim == 2:
        return np.vstack([sound[0] * min(1.0, 1.0 - pan), sound[1] * min(1.0, 1.0 + pan)])
    angle = (pan + 1.0) * np.pi / 4.0
    return np.vstack([sound * np.cos(angle), sound * np.sin(angle)]) * np.sqrt(2.0)


class Bus:
    def __init__(self) -> None:
        self.data = np.zeros((2, LENGTH))

    def add(self, sound: np.ndarray, at: float, gain: float = 1.0, pan: float = 0.0) -> None:
        wide = stereo(sound, pan)
        start = int(round(at * SR))
        if start >= LENGTH:
            return
        if start < 0:
            wide = wide[:, -start:]
            start = 0
        end = min(LENGTH, start + wide.shape[1])
        self.data[:, start:end] += wide[:, : end - start] * gain


TABLES: dict[int, np.ndarray] = {}


def saw_table(frequency: float, size: int = 4096) -> np.ndarray:
    harmonics = max(1, int(SR * 0.45 / frequency))
    key = min(harmonics, 1200)
    if key not in TABLES:
        k = np.arange(1, key + 1)
        phase = np.arange(size) / size
        table = (np.sin(2 * np.pi * np.outer(k, phase)) / k[:, None]).sum(axis=0)
        TABLES[key] = table / np.max(np.abs(table))
    return TABLES[key]


def oscillate(table: np.ndarray, frequency: np.ndarray | float, n: int, phase: float = 0.0) -> np.ndarray:
    increments = np.broadcast_to(np.asarray(frequency, dtype=np.float64) / SR, (n,))
    position = (phase + np.cumsum(increments)) % 1.0
    index = position * len(table)
    lower = index.astype(np.int64) % len(table)
    frac = index - np.floor(index)
    return table[lower] * (1.0 - frac) + table[(lower + 1) % len(table)] * frac


def kick(punch: float = 1.0, muffled: bool = False) -> np.ndarray:
    n = samples(0.6)
    t = clock(n)
    frequency = 52.0 + 120.0 * np.exp(-t / 0.034)
    body = np.sin(2 * np.pi * np.cumsum(frequency) / SR) * np.exp(-t / (0.19 * punch))
    click = highpass(noise(n), 2500.0) * np.exp(-t / 0.0035) * 0.3
    out = np.tanh(1.8 * (body + click)) / np.tanh(1.8)
    if muffled:
        out = lowpass(out, 240.0, order=4) * 1.4
    return edges(out, 0.0005, 0.02)


def snare() -> np.ndarray:
    n = samples(0.4)
    t = clock(n)
    tone = np.sin(2 * np.pi * 188.0 * t) * np.exp(-t / 0.055) * 0.55 + np.sin(2 * np.pi * 330.0 * t) * np.exp(-t / 0.035) * 0.22
    rattle = bandpass(noise(n), 1400.0, 9000.0) * np.exp(-t / 0.11)
    return edges(np.tanh(1.3 * (tone + rattle * 0.8)), 0.0005, 0.02)


def clap() -> np.ndarray:
    n = samples(0.45)
    t = clock(n)
    env = np.zeros(n)
    for offset in (0.0, 0.011, 0.023):
        shifted = np.clip(t - offset, 0.0, None)
        env += np.where(t >= offset, np.exp(-shifted / 0.0045), 0.0)
    env += np.exp(-t / 0.09) * 0.55
    return edges(bandpass(noise(n), 900.0, 5200.0) * env * 0.8, 0.0005, 0.02)


def hat(open_hat: bool = False) -> np.ndarray:
    n = samples(0.32 if open_hat else 0.09)
    t = clock(n)
    metal = highpass(noise(n), 7200.0, order=4)
    return edges(metal * np.exp(-t / (0.14 if open_hat else 0.024)), 0.0003, 0.01)


def tom(note: float) -> np.ndarray:
    n = samples(0.5)
    t = clock(n)
    frequency = midi(note) * (1.0 + 0.6 * np.exp(-t / 0.03))
    body = np.sin(2 * np.pi * np.cumsum(frequency) / SR) * np.exp(-t / 0.2)
    return edges(np.tanh(1.5 * body), 0.0005, 0.02)


def impact(size: float = 1.0) -> np.ndarray:
    n = samples(2.6 * size + 0.6)
    t = clock(n)
    frequency = 40.0 + 70.0 * np.exp(-t / 0.09)
    boom = np.tanh(2.0 * np.sin(2 * np.pi * np.cumsum(frequency) / SR) * np.exp(-t / (0.5 * size))) * 0.62
    sides = []
    for _ in range(2):
        rumble = bandpass(noise(n), 45.0, 220.0) * np.exp(-t / (0.4 * size)) * 0.9
        body = bandpass(noise(n), 220.0, 1400.0) * np.exp(-t / (0.16 * size)) * 0.35
        crack = bandpass(noise(n), 650.0, 7000.0) * np.exp(-t / 0.05) * 0.6
        air = highpass(noise(n), 5000.0) * np.exp(-t / (0.35 * size)) * 0.1
        sides.append(boom + rumble + body + crack + air)
    ring = sum(np.sin(2 * np.pi * f * t) * a for f, a in ((146.8, 0.05), (220.0, 0.03), (587.3 * 1.37, 0.012)))
    out = np.vstack(sides) + ring * np.exp(-t / (1.1 * size))
    return edges(out, 0.0005, 0.05)


def whoosh(length: float = 0.6, peak: float = 0.75, low: float = 260.0, high: float = 5200.0, pan_from: float = -0.7, pan_to: float = 0.7) -> np.ndarray:
    n = samples(length)
    t = clock(n)
    u = t / length
    shape = np.where(u < peak, (u / peak) ** 2.2, np.exp(-(u - peak) / max(1e-3, 1.0 - peak) * 4.0))
    centers = low * (high / low) ** np.clip(shape, 0.0, 1.0)
    body = sweep(noise(n), centers, kind="bandpass", width=1.5) * shape
    body /= np.max(np.abs(body)) + 1e-9
    pan = pan_from + (pan_to - pan_from) * u
    angle = (pan + 1.0) * np.pi / 4.0
    return edges(np.vstack([body * np.cos(angle), body * np.sin(angle)]) * np.sqrt(2.0), 0.005, 0.02)


def riser(length: float = 1.0, low: float = 300.0, high: float = 9000.0, pitch_from: float = 50.0, pitch_to: float = 74.0) -> np.ndarray:
    n = samples(length)
    t = clock(n)
    u = t / length
    envelope = u ** 2.3
    sides = []
    for _ in range(2):
        air = sweep(noise(n), low * (high / low) ** u, kind="lowpass")
        sides.append(air / (np.max(np.abs(air)) + 1e-9))
    frequency = np.array([midi(pitch_from + (pitch_to - pitch_from) * v ** 1.4) for v in u[:: 64]])
    frequency = np.interp(np.arange(n), np.arange(0, n, 64)[: len(frequency)], frequency)
    phase = 2 * np.pi * np.cumsum(frequency) / SR
    tone = (np.sin(phase) + 0.45 * np.sin(2 * phase) + 0.25 * np.sin(3 * phase)) * 0.28
    out = (np.vstack(sides) * 0.75 + tone) * envelope
    return edges(out, 0.01, 0.008)


def reverse_swell(length: float = 0.8) -> np.ndarray:
    n = samples(length)
    t = clock(n)
    sides = []
    for _ in range(2):
        crash = highpass(noise(n), 2800.0) * np.exp(-t / (length * 0.55)) + bandpass(noise(n), 400.0, 3000.0) * np.exp(-t / (length * 0.3)) * 0.5
        sides.append(crash[::-1])
    out = np.vstack(sides)
    return edges(out / (np.max(np.abs(out)) + 1e-9), 0.02, 0.006)


def bell(note: float, length: float = 3.5, ratio: float = 3.5, index: float = 2.2, decay: float = 1.3) -> np.ndarray:
    n = samples(length)
    t = clock(n)
    f = midi(note)
    modulation = index * np.exp(-t / 0.45)
    tone = np.sin(2 * np.pi * f * t + modulation * np.sin(2 * np.pi * ratio * f * t))
    tone += 0.22 * np.sin(2 * np.pi * 2.756 * f * t) * np.exp(-t / 0.3)
    env = (1.0 - np.exp(-t / 0.0015)) * np.exp(-t / decay)
    return edges(tone * env * 0.8, 0.001, 0.05)


def pluck(note: float, length: float = 2.4, damping: float = 0.9965, brightness: float = 0.55, detune: float = 0.0) -> np.ndarray:
    n = samples(length)
    period = SR / (midi(note) * 2.0 ** (detune / 1200.0))
    delay = int(np.floor(period - 0.6))
    fraction = period - 0.5 - delay
    c = (1.0 - fraction) / (1.0 + fraction)
    excitation = lowpass(noise(delay), 1200.0 + 8000.0 * brightness)
    excitation -= np.mean(excitation)
    x = np.zeros(n)
    x[:delay] = excitation / (np.max(np.abs(excitation)) + 1e-9)
    a = np.zeros(delay + 3)
    a[0] = 1.0
    a[1] = c
    a[delay] += -0.5 * damping * c
    a[delay + 1] += -0.5 * damping * (1.0 + c)
    a[delay + 2] += -0.5 * damping
    y = signal.lfilter([1.0, c], a, x)
    return edges(y / (np.max(np.abs(y)) + 1e-9), 0.0005, 0.08)


def kanun(note: float, length: float = 2.4, brightness: float = 0.55) -> np.ndarray:
    left = pluck(note, length, brightness=brightness, detune=-3.0)
    right = pluck(note, length, brightness=brightness, detune=3.0)
    center = pluck(note, length, brightness=brightness)
    return np.vstack([center * 0.6 + left * 0.4, center * 0.6 + right * 0.4])


def pad(notes: list[float], length: float, attack: float = 0.8, release: float = 1.2, cutoff: float = 1600.0, voices: int = 5, detune: float = 11.0, open_to: float | None = None) -> np.ndarray:
    n = samples(length + release)
    t = clock(n)
    out = np.zeros((2, n))
    for note in notes:
        f = midi(note)
        table = saw_table(f)
        for v in range(voices):
            spread = (v - (voices - 1) / 2.0) / max(1.0, (voices - 1) / 2.0)
            drift = 1.0 + 0.0015 * np.sin(2 * np.pi * (0.13 + 0.07 * v) * t + v)
            wave_ = oscillate(table, f * 2.0 ** (spread * detune / 1200.0) * drift, n, RNG.random())
            angle = (spread * 0.8 + 1.0) * np.pi / 4.0
            out[0] += wave_ * np.cos(angle)
            out[1] += wave_ * np.sin(angle)
    out /= np.sqrt(len(notes) * voices) * 2.2
    dark = lowpass(out, cutoff, order=2)
    if open_to is not None:
        bright = lowpass(out, open_to, order=2)
        mix = np.clip(t / max(length, 1e-3), 0.0, 1.0) ** 1.5
        dark = dark * (1.0 - mix) + bright * mix
    env = np.ones(n)
    a = samples(attack)
    env[:a] = np.sin(np.linspace(0.0, np.pi / 2, a)) ** 2
    hold = samples(length)
    env[hold:] = np.cos(np.linspace(0.0, np.pi / 2, n - hold)) ** 2
    return dark * env


def bass_note(note: float, length: float, cutoff: float = 900.0) -> np.ndarray:
    n = samples(length + 0.03)
    t = clock(n)
    f = midi(note)
    body = oscillate(saw_table(f), f, n) * 0.55 + np.sin(2 * np.pi * f * t) * 0.75
    env = np.minimum(1.0, t / 0.003) * (0.7 + 0.3 * np.exp(-t / 0.09))
    closed = lowpass(body, cutoff, order=2)
    opened = lowpass(body, cutoff * 3.5, order=2)
    punch = np.exp(-t / 0.05)
    out = np.tanh(1.6 * (closed * (1.0 - punch) + opened * punch) * env)
    return edges(out, 0.001, 0.03)


def sub(note: float, length: float) -> np.ndarray:
    n = samples(length)
    t = clock(n)
    return edges(np.sin(2 * np.pi * midi(note) * t) * np.exp(-t / (length * 0.45)), 0.01, 0.2)


def drone(length: float, notes: tuple[float, ...] = (38.0, 45.0, 50.0), cutoff: float = 650.0) -> np.ndarray:
    n = samples(length)
    t = clock(n)
    out = np.zeros((2, n))
    for i, note in enumerate(notes):
        f = midi(note)
        for side in range(2):
            phase = 2 * np.pi * f * (1.0 + (side - 0.5) * 0.002) * t + i
            out[side] += (np.sin(phase) + 0.18 * np.sin(3 * phase)) * (0.8 + 0.2 * np.sin(2 * np.pi * (0.11 + 0.05 * i) * t))
    out = lowpass(out / len(notes), cutoff)
    return edges(out, length * 0.35, length * 0.25)


def tick(frequency: float = 2400.0) -> np.ndarray:
    n = samples(0.035)
    t = clock(n)
    tone = np.sin(2 * np.pi * frequency * t) * np.exp(-t / 0.0045)
    click = highpass(noise(n), 4000.0) * np.exp(-t / 0.0012) * 0.35
    return edges(tone + click, 0.0002, 0.005)


def blip(note: float) -> np.ndarray:
    n = samples(0.16)
    t = clock(n)
    frequency = midi(note) * (1.0 + 0.5 * np.exp(-t / 0.012))
    return edges(np.sin(2 * np.pi * np.cumsum(frequency) / SR) * np.exp(-t / 0.05), 0.0005, 0.01)


def wind(length: float, low: float, high: float) -> np.ndarray:
    n = samples(length)
    t = clock(n)
    u = t / length
    sides = [sweep(noise(n), low * (high / low) ** u, kind="bandpass", width=1.8) for _ in range(2)]
    out = np.vstack(sides)
    out /= np.max(np.abs(out)) + 1e-9
    return edges(out * (0.25 + 0.75 * u ** 1.6), 0.15, 0.05)


def glide(length: float, note_from: float, note_to: float) -> np.ndarray:
    n = samples(length)
    t = clock(n)
    u = t / length
    frequency = midi(note_from) * (midi(note_to) / midi(note_from)) ** (u ** 1.3)
    phase = 2 * np.pi * np.cumsum(frequency) / SR
    return edges((np.sin(phase) + 0.3 * np.sin(2 * phase)) * u ** 1.2, 0.2, 0.01)


def reverb_ir(seconds: float, predelay: float, brightness: float, seed: int) -> np.ndarray:
    generator = np.random.default_rng(seed)
    n = samples(seconds)
    t = clock(n)
    decay = np.exp(-6.91 * t / (seconds * 0.85))
    ir = generator.standard_normal((2, n)) * decay
    low_part = lowpass(ir, brightness, order=2)
    high_part = (ir - low_part) * np.exp(-t / (seconds * 0.12))
    ir = low_part + high_part
    for delay, gain in ((0.011, 0.5), (0.019, 0.35), (0.027, 0.28), (0.041, 0.2)):
        index = samples(delay)
        ir[0, index] += gain
        ir[1, samples(delay * 1.13)] += gain
    ir = np.hstack([np.zeros((2, samples(predelay))), ir])
    return ir / np.sqrt(np.sum(ir ** 2) / 2.0)


def convolve(dry: np.ndarray, ir: np.ndarray) -> np.ndarray:
    wet = np.vstack([signal.fftconvolve(dry[0], ir[0])[:LENGTH], signal.fftconvolve(dry[1], ir[1])[:LENGTH]])
    return wet


def ping_pong(dry: np.ndarray, delay: float, feedback: float, taps: int = 6) -> np.ndarray:
    mono = dry.mean(axis=0)
    out = np.zeros_like(dry)
    shift = samples(delay)
    for k in range(1, taps + 1):
        offset = shift * k
        if offset >= LENGTH:
            break
        out[k % 2, offset:] += mono[: LENGTH - offset] * feedback ** k
    return lowpass(out, 5200.0)


def limiter(x: np.ndarray, ceiling: float = 0.89, window: float = 0.03) -> np.ndarray:
    peak = np.max(np.abs(x), axis=0)
    gain = np.minimum(1.0, ceiling / np.maximum(peak, 1e-9))
    width = samples(window)
    floor = ndimage.minimum_filter1d(gain, size=2 * width + 1)
    smooth = ndimage.uniform_filter1d(ndimage.uniform_filter1d(floor, size=width + 1), size=width + 1)
    return x * np.minimum(gain, smooth)


def shelf(x: np.ndarray, frequency: float, gain_db: float, kind: str) -> np.ndarray:
    amplitude = 10 ** (gain_db / 40.0)
    w0 = 2 * np.pi * frequency / SR
    alpha = np.sin(w0) / 2 * np.sqrt(2.0)
    cos_w0 = np.cos(w0)
    root = 2 * np.sqrt(amplitude) * alpha
    if kind == "low":
        b = [amplitude * ((amplitude + 1) - (amplitude - 1) * cos_w0 + root), 2 * amplitude * ((amplitude - 1) - (amplitude + 1) * cos_w0), amplitude * ((amplitude + 1) - (amplitude - 1) * cos_w0 - root)]
        a = [(amplitude + 1) + (amplitude - 1) * cos_w0 + root, -2 * ((amplitude - 1) + (amplitude + 1) * cos_w0), (amplitude + 1) + (amplitude - 1) * cos_w0 - root]
    else:
        b = [amplitude * ((amplitude + 1) + (amplitude - 1) * cos_w0 + root), -2 * amplitude * ((amplitude - 1) + (amplitude + 1) * cos_w0), amplitude * ((amplitude + 1) + (amplitude - 1) * cos_w0 - root)]
        a = [(amplitude + 1) - (amplitude - 1) * cos_w0 + root, 2 * ((amplitude - 1) - (amplitude + 1) * cos_w0), (amplitude + 1) - (amplitude - 1) * cos_w0 - root]
    return signal.lfilter(np.array(b) / a[0], np.array(a) / a[0], x, axis=-1)


def main() -> int:
    drums, hits, music, bells, fx, low = Bus(), Bus(), Bus(), Bus(), Bus(), Bus()
    kicks: list[tuple[float, float]] = []

    def add_kick(at: float, gain: float = 1.0, punch: float = 1.0, muffled: bool = False) -> None:
        drums.add(kick(punch, muffled), at, gain)
        if not muffled:
            kicks.append((at, gain))

    def roll(start: float, end: float, gain: float = 0.35) -> None:
        at = start
        step = BEAT / 4
        while at < end - 1e-6:
            u = (at - start) / (end - start)
            drums.add(snare(), at, gain * (0.35 + 0.65 * u), pan=0.1)
            at += step if u < 0.5 else step / 2

    music.add(drone(4.8), 0.0, 0.5)
    bells.add(bell(86, 4.0, ratio=2.0, index=1.4, decay=1.6), CUE["spark"], 0.22, pan=-0.05)
    bells.add(bell(93, 3.0, ratio=2.0, index=0.9, decay=1.0), CUE["spark"] + 0.02, 0.08, pan=0.2)
    fx.add(glide(CUE["shock"] - CUE["spark"], 86, 98), CUE["spark"], 0.035)
    fx.add(reverse_swell(0.85), CUE["shock"] - 0.85, 0.5)
    fx.add(riser(0.85, 400.0, 7000.0, 62.0, 74.0), CUE["shock"] - 0.85, 0.22)
    hits.add(impact(1.25), CUE["shock"], 1.0)
    music.add(pad(CHORD["D"], 3.0, attack=0.5, release=1.2, cutoff=900.0, open_to=2600.0), CUE["shock"], 0.62)
    fx.add(reverse_swell(0.9), CUE["vega"] - 0.9, 0.42)

    hits.add(impact(0.85), CUE["vega"], 0.75)
    bells.add(bell(81, 4.5, decay=1.8), CUE["vega"], 0.34, pan=0.15)
    bells.add(bell(74, 4.5, decay=1.8), CUE["vega"] + 0.01, 0.24, pan=-0.15)
    music.add(pad(CHORD["Eb"], 3.0, attack=0.9, release=0.6, cutoff=1100.0), CUE["vega"], 0.5)
    low.add(drone(3.6, (39.0, 46.0, 51.0), 600.0), CUE["vega"], 0.4)
    music.add(pad(CHORD["D"], 4.1, attack=0.4, release=0.5, cutoff=1300.0, open_to=2400.0), CUE["montage"][0] - 0.2, 0.46)
    low.add(drone(4.6, (38.0, 45.0, 50.0), 600.0), CUE["montage"][0] - 0.2, 0.42)
    montage_notes = [69, 70, 69, 67, 66, 63, 62]
    for i, (at, note) in enumerate(zip(CUE["montage"], montage_notes)):
        pan = -0.55 if i % 2 == 0 else 0.55
        fx.add(whoosh(0.5, peak=0.82, pan_from=-pan, pan_to=pan), at - 0.41, 0.32)
        music.add(kanun(note, 2.6), at, 0.5, pan=pan * 0.3)
        bells.add(bell(note + 12, 1.6, ratio=2.0, index=1.0, decay=0.7), at + 0.004, 0.09, pan=pan * 0.5)
        add_kick(at, 0.32, muffled=True)
    fx.add(riser(0.8, 300.0, 9000.0, 50.0, 74.0), CUE["astrolabeHit"] - 0.8, 0.28)
    fx.add(reverse_swell(0.6), CUE["astrolabeHit"] - 0.6, 0.35)

    hits.add(impact(1.0), CUE["astrolabeHit"], 0.9)
    music.add(pad(CHORD["D"], 3.0, attack=0.2, release=0.8, cutoff=1400.0), CUE["astrolabeHit"], 0.42)
    low.add(drone(4.5, (38.0, 45.0, 50.0), 600.0), CUE["astrolabeHit"], 0.4)
    at = CUE["astrolabeHit"] + BEAT / 2
    k = 0
    while at < CUE["logo"] - 0.1:
        near_lock = abs(at - CUE["qiblaLock"]) < 0.05
        if not near_lock:
            fx.add(tick(2600.0 if k % 2 == 0 else 1900.0), at, 0.11 if at < CUE["qiblaLock"] else 0.07, pan=-0.3 if k % 2 == 0 else 0.3)
        at += BEAT / 2
        k += 1
    for beat in np.arange(CUE["astrolabeHit"] + 2 * BEAT, CUE["logo"], 2 * BEAT):
        add_kick(float(beat), 0.34, muffled=True)
    fx.add(tick(1500.0), CUE["qiblaLock"], 0.45, pan=-0.1)
    fx.add(tick(1050.0), CUE["qiblaLock"] + 0.045, 0.4, pan=0.1)
    hits.add(impact(0.5), CUE["qiblaLock"], 0.5)
    bells.add(bell(86, 3.0), CUE["qiblaLock"], 0.22)
    bells.add(bell(90, 3.0), CUE["qiblaLock"] + 0.03, 0.14, pan=0.2)
    music.add(pad(CHORD["Cm"], 1.5, attack=0.3, release=0.3, cutoff=1300.0), CUE["qiblaLock"], 0.34)
    fx.add(reverse_swell(0.7), CUE["logo"] - 0.7, 0.5)
    for i, note in enumerate([74, 78, 81, 86, 90]):
        bells.add(bell(note, 3.0, decay=1.4), CUE["logo"] + i * 0.055, 0.3 - i * 0.03, pan=-0.4 + i * 0.2)
    hits.add(impact(0.7), CUE["logo"], 0.6)
    music.add(pad([62, 66, 69, 74, 78], 1.2, attack=0.04, release=0.5, cutoff=2600.0), CUE["logo"], 0.4)
    fx.add(riser(1.0, 300.0, 11000.0, 50.0, 74.0), CUE["appHit"] - 1.0, 0.34)
    roll(CUE["appHit"] - 1.0, CUE["appHit"], 0.3)

    hits.add(impact(1.1), CUE["appHit"], 0.9)
    bars = [(CUE["appHit"] + i * 4 * BEAT, name) for i, name in enumerate(["D", "Eb", "D", "Cm", "D"])]
    groove_end = CUE["moonHit"] - BEAT
    for bar_index, (bar_start, name) in enumerate(bars):
        music.add(pad(CHORD[name], 4 * BEAT - 0.05, attack=0.02, release=0.35, cutoff=1700.0), bar_start, 0.36)
        for step, note in enumerate(BASS_RIFF[name]):
            at = bar_start + step * BEAT / 2
            if at < groove_end:
                low.add(bass_note(note, BEAT / 2 * 0.8), at, 0.34)
        if bar_index == 2:
            continue
        for step, note in enumerate(OSTINATO[name]):
            at = bar_start + step * BEAT / 2
            if at < groove_end:
                music.add(kanun(note, 1.2, brightness=0.7), at, 0.3 if step % 2 == 0 else 0.22, pan=-0.35 if step % 2 == 0 else 0.35)
    beat = CUE["appHit"]
    index = 0
    while beat < groove_end - 1e-6:
        add_kick(beat, 0.92)
        if index % 2 == 1:
            drums.add(clap(), beat, 0.5)
            drums.add(snare(), beat, 0.26)
        drums.add(hat(open_hat=index % 4 == 3), beat + BEAT / 2, 0.2, pan=0.25)
        drums.add(hat(), beat + BEAT / 4, 0.08, pan=-0.3)
        drums.add(hat(), beat + 3 * BEAT / 4, 0.08, pan=-0.3)
        beat += BEAT
        index += 1
    stat_notes = [86, 90, 93, 98]
    for at, note in zip(CUE["stats"], stat_notes):
        count = 13
        for k in range(count):
            fraction = k / count
            fx.add(tick(3400.0 + 900.0 * fraction), at + 0.9 * (1.0 - (1.0 - fraction) ** (1.0 / 3.0)), 0.07, pan=0.35)
        bells.add(blip(note), at + 0.9, 0.22, pan=0.2)
    fx.add(whoosh(0.6, peak=0.75), CUE["cutVega"] - 0.45, 0.4)
    hits.add(impact(0.6), CUE["cutVega"], 0.55)
    bells.add(bell(81, 3.5, decay=1.6), CUE["cutVega"] + 0.45, 0.28, pan=0.2)
    bells.add(bell(69, 3.5, decay=1.6), CUE["cutVega"] + 0.47, 0.16, pan=-0.2)
    fx.add(whoosh(0.6, peak=0.75, pan_from=0.7, pan_to=-0.7), CUE["cutTriptych"] - 0.45, 0.4)
    for i, note in enumerate([50, 54, 57]):
        drums.add(tom(note), CUE["cutTriptych"] + i * 0.12, 0.45, pan=-0.4 + i * 0.4)
    fx.add(riser(1.0, 300.0, 11000.0, 50.0, 74.0), CUE["moonHit"] - 1.0, 0.32)
    fx.add(reverse_swell(0.5), CUE["moonHit"] - 0.5, 0.45)

    hits.add(impact(1.35), CUE["moonHit"], 1.0)
    music.add(pad(CHORD["Gm"], 2.9, attack=0.35, release=0.9, cutoff=1000.0), CUE["moonHit"], 0.5)
    low.add(sub(43, 3.0), CUE["moonHit"], 0.42)
    arpeggio = [67, 70, 74, 79, 74, 70]
    for k, at in enumerate(np.arange(CUE["moonHit"] + BEAT, CUE["crescent"] - 0.2, BEAT)):
        music.add(kanun(arpeggio[k % len(arpeggio)], 2.0, brightness=0.45), float(at), 0.26, pan=-0.3 + 0.12 * (k % 6))
    fx.add(reverse_swell(0.8), CUE["crescent"] - 0.8, 0.42)
    hits.add(impact(0.6), CUE["crescent"], 0.5)
    music.add(pad(CHORD["D"], 2.8, attack=0.25, release=0.8, cutoff=1500.0), CUE["crescent"], 0.5)
    low.add(sub(38, 3.0), CUE["crescent"], 0.38)
    bells.add(bell(78, 3.0), CUE["crescent"], 0.2, pan=0.3)
    arpeggio = [62, 66, 69, 74, 69, 66]
    for k, at in enumerate(np.arange(CUE["crescent"] + BEAT, CUE["mapHit"] - 0.6, BEAT)):
        music.add(kanun(arpeggio[k % len(arpeggio)], 2.0, brightness=0.5), float(at), 0.24, pan=0.3 - 0.12 * (k % 6))
    bells.add(bell(93, 3.0, ratio=2.0, index=1.2, decay=1.6), CUE["category"], 0.3, pan=0.35)
    bells.add(bell(86, 3.0, ratio=2.0, index=1.2, decay=1.6), CUE["category"] + 0.02, 0.22, pan=0.2)
    add_kick(CUE["category"], 0.35, muffled=True)
    fx.add(riser(1.0, 300.0, 10000.0, 50.0, 74.0), CUE["mapHit"] - 1.0, 0.3)
    roll(CUE["mapHit"] - 0.5, CUE["mapHit"], 0.26)

    hits.add(impact(1.0), CUE["mapHit"], 0.9)
    map_bars = [(CUE["mapHit"], "D"), (CUE["mapHit"] + 4 * BEAT, "Eb"), (CUE["mapHit"] + 8 * BEAT, "Cm"), (CUE["mapHit"] + 12 * BEAT, "D")]
    for bar_start, name in map_bars:
        music.add(pad(CHORD[name], 4 * BEAT - 0.05, attack=0.08, release=0.5, cutoff=1400.0), bar_start, 0.36)
        low.add(bass_note(ROOT[name], 4 * BEAT * 0.45), bar_start, 0.36)
        low.add(bass_note(ROOT[name], 4 * BEAT * 0.2), bar_start + 1.5 * BEAT, 0.26)
        add_kick(bar_start, 0.9)
        add_kick(bar_start + 1.5 * BEAT, 0.55)
        drums.add(snare(), bar_start + 2 * BEAT, 0.38)
        drums.add(clap(), bar_start + 2 * BEAT, 0.28)
        for eighth in range(8):
            drums.add(hat(open_hat=eighth == 7), bar_start + eighth * BEAT / 2, 0.1 if eighth % 2 else 0.05, pan=0.25)
    for at, note in zip(CUE["evenings"], [74, 75, 72]):
        fx.add(whoosh(1.7, peak=0.45, low=500.0, high=3200.0, pan_from=-0.8, pan_to=0.8), at, 0.22)
        bells.add(bell(note, 3.0, decay=1.5), at, 0.2, pan=-0.2)
        music.add(kanun(note - 12, 2.2), at, 0.22, pan=-0.2)
    hits.add(impact(0.9), CUE["ramadan"], 0.8)
    for i, note in enumerate([74, 75, 78, 79, 81]):
        bells.add(bell(note, 2.4, decay=1.1), CUE["ramadan"] + i * 0.25, 0.22, pan=-0.4 + i * 0.2)
        music.add(kanun(note - 12, 1.8), CUE["ramadan"] + i * 0.25, 0.26, pan=-0.4 + i * 0.2)
    fx.add(riser(1.0, 300.0, 12000.0, 50.0, 86.0), CUE["slams"][0] - 1.0, 0.36)
    roll(CUE["slams"][0] - 1.0, CUE["slams"][0], 0.32)

    stabs = [38, 38, 39, 38, 42, 43, 45, 50]
    for i, at in enumerate(CUE["slams"]):
        add_kick(at, 1.0, punch=1.2)
        drums.add(snare(), at, 0.42)
        drums.add(clap(), at, 0.3)
        hits.add(impact(0.42), at, 0.5, pan=-0.2 if i % 2 else 0.2)
        low.add(bass_note(stabs[i], 0.4, cutoff=1400.0), at, 0.4)
        music.add(pad([stabs[i] + 12, stabs[i] + 19, stabs[i] + 24, stabs[i] + 31], 0.38, attack=0.003, release=0.12, cutoff=3600.0), at, 0.44)
        music.add(kanun(stabs[i] + 36, 0.6, brightness=0.85), at, 0.2, pan=0.3 if i % 2 else -0.3)
        for sixteenth in range(1, 4):
            drums.add(hat(), at + sixteenth * BEAT / 4, 0.08, pan=0.3)
    fx.add(wind(4.0, 700.0, 6500.0), CUE["slams"][0], 0.2)

    hits.add(impact(0.95), CUE["timeHit"], 0.85)
    music.add(pad([50, 57, 62, 69, 74], 3.3, attack=0.2, release=0.3, cutoff=1200.0, open_to=3400.0), CUE["timeHit"], 0.32)
    low.add(drone(3.6, (38.0, 45.0, 50.0), 600.0), CUE["timeHit"], 0.4)
    lapse_start = CUE["timeHit"] + 0.2
    lapse_end = CUE["outroHit"] - 0.6
    rate_from, rate_to = 2.0, 34.0
    phase = 0.0
    step = 1.0 / 2000.0
    at = lapse_start
    k = 0
    while at < lapse_end:
        u = (at - lapse_start) / (lapse_end - lapse_start)
        phase += (rate_from * (rate_to / rate_from) ** u) * step
        if phase >= 1.0:
            phase -= 1.0
            fx.add(tick(2000.0 + 1800.0 * u if k % 2 == 0 else 1500.0 + 1400.0 * u), at, 0.13 + 0.1 * u, pan=-0.35 if k % 2 == 0 else 0.35)
            k += 1
        at += step
    fx.add(wind(lapse_end - lapse_start, 300.0, 8000.0), lapse_start, 0.3)
    fx.add(glide(lapse_end - lapse_start, 50.0, 74.0), lapse_start, 0.07)
    for pulse in np.arange(CUE["timeHit"] + BEAT, lapse_end, BEAT):
        add_kick(float(pulse), 0.42 + 0.25 * (pulse - CUE["timeHit"]) / (lapse_end - CUE["timeHit"]), muffled=True)
    fx.add(reverse_swell(0.7), CUE["outroHit"] - 0.7, 0.62)

    hits.add(impact(1.7), CUE["outroHit"], 1.1)
    low.add(sub(38, 4.0), CUE["outroHit"], 0.45)
    music.add(pad([38, 45, 50, 57, 62, 66, 69, 74], 4.2, attack=0.02, release=1.8, cutoff=1100.0, open_to=2600.0), CUE["outroHit"], 0.5)
    for i, note in enumerate([74, 81, 86, 90, 93]):
        bells.add(bell(note, 4.0, decay=1.8), CUE["wordmark"] + i * 0.09, 0.28 - i * 0.025, pan=-0.4 + i * 0.2)
    bells.add(bell(86, 3.5, ratio=2.0, index=1.0, decay=1.6), CUE["wordmark"] + 1.3, 0.16, pan=0.1)
    fx.add(tick(3000.0), CUE["wordmark"] + 1.8, 0.05)

    duck = np.zeros(LENGTH)
    for at, gain in kicks:
        start = int(round(at * SR))
        n = min(LENGTH - start, samples(0.35))
        if n > 0:
            duck[start:start + n] = np.maximum(duck[start:start + n], gain * np.exp(-clock(n) / 0.11))
    duck = np.clip(duck, 0.0, 1.0)

    hall = reverb_ir(3.2, 0.025, 3800.0, 7)
    room = reverb_ir(0.9, 0.008, 6000.0, 11)
    pumped_music = music.data * (1.0 - 0.45 * duck)
    pumped_low = low.data * (1.0 - 0.55 * duck)
    hall_send = pumped_music * 0.3 + bells.data * 0.5 + hits.data * 0.28 + fx.data * 0.3 + drums.data * 0.05
    room_send = drums.data * 0.14 + hits.data * 0.1
    echoes = ping_pong(bells.data * 0.6 + music.data * 0.12, BEAT * 0.75, 0.38)
    wet = convolve(hall_send, hall) * 0.42 + convolve(room_send, room) * 0.3
    master = drums.data * 0.95 + hits.data * 0.9 + pumped_music + pumped_low + bells.data + fx.data + wet + echoes * 0.35
    master = highpass(master, 32.0, order=2)
    master = shelf(master, 70.0, -4.5, "low")
    master = shelf(master, 7000.0, 2.5, "high")
    master /= np.max(np.abs(master)) + 1e-9
    master = limiter(master * 1.6, 0.9)
    master = np.tanh(master * 1.15) / np.tanh(1.15)
    fade_start = int(round((CUE["end"] - 0.4) * SR))
    fade = np.ones(LENGTH)
    fade[fade_start:] = np.cos(np.linspace(0.0, np.pi / 2, LENGTH - fade_start)) ** 2
    master *= fade
    master /= np.max(np.abs(master)) + 1e-9
    master *= 10 ** (-1.0 / 20.0)
    dither = (RNG.random((2, LENGTH)) - RNG.random((2, LENGTH))) / 32768.0
    pcm = np.clip(np.round((master + dither) * 32767.0), -32768, 32767).astype("<i2")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT), "wb") as handle:
        handle.setnchannels(2)
        handle.setsampwidth(2)
        handle.setframerate(SR)
        handle.writeframes(pcm.T.tobytes())
    rms = 20 * np.log10(np.sqrt(np.mean(master ** 2)) + 1e-12)
    print(f"wrote {OUT} {DURATION:.1f}s rms {rms:.1f} dBFS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
