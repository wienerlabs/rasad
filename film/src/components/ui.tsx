import React from "react";
import { AbsoluteFill, interpolate, OffthreadVideo, staticFile, useCurrentFrame, useVideoConfig, type EasingFunction } from "remotion";
import { color, ease, font } from "../theme";

export const useSeconds = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return frame / fps;
};

export const range = (t: number, start: number, end: number, easing: EasingFunction = ease.out) =>
  interpolate(t, [start, end], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing });

export const mixNumber = (a: number, b: number, p: number) => a + (b - a) * p;

type SplitTextProps = {
  text: string;
  start: number;
  stagger?: number;
  duration?: number;
  exit?: number;
  exitDuration?: number;
  style?: React.CSSProperties;
  rise?: number;
  blur?: number;
};

export const SplitText: React.FC<SplitTextProps> = ({ text, start, stagger = 0.035, duration = 0.7, exit, exitDuration = 0.35, style, rise = 0.55, blur = 14 }) => {
  const t = useSeconds();
  const words = text.split(" ");
  let index = 0;
  const exitProgress = exit === undefined ? 0 : range(t, exit, exit + exitDuration, ease.in);
  return (
    <span style={{ display: "inline-block", whiteSpace: "pre-wrap", ...style }}>
      {words.map((word, w) => (
        <span key={w} style={{ display: "inline-block", whiteSpace: "nowrap" }}>
          {Array.from(word).map((letter) => {
            const i = index++;
            const p = range(t, start + i * stagger, start + i * stagger + duration);
            const y = (1 - p) * rise + exitProgress * -0.35;
            return (
              <span
                key={i}
                style={{
                  display: "inline-block",
                  transform: `translateY(${y}em)`,
                  opacity: p * (1 - exitProgress),
                  filter: `blur(${(1 - p) * blur + exitProgress * 10}px)`,
                }}
              >
                {letter}
              </span>
            );
          })}
          {w < words.length - 1 ? <span style={{ display: "inline-block", width: "0.26em" }} /> : null}
        </span>
      ))}
    </span>
  );
};

export const FadeText: React.FC<{ start: number; end?: number; children: React.ReactNode; style?: React.CSSProperties; rise?: number }> = ({ start, end, children, style, rise = 18 }) => {
  const t = useSeconds();
  const p = range(t, start, start + 0.6);
  const out = end === undefined ? 0 : range(t, end, end + 0.4, ease.in);
  return <div style={{ opacity: p * (1 - out), transform: `translateY(${(1 - p) * rise - out * 10}px)`, filter: `blur(${(1 - p) * 8}px)`, ...style }}>{children}</div>;
};

export const ArabicReveal: React.FC<{ text: string; start: number; duration?: number; size: number; style?: React.CSSProperties; glow?: number }> = ({ text, start, duration = 0.9, size, style, glow = 0.45 }) => {
  const t = useSeconds();
  const p = range(t, start, start + duration, ease.inOut);
  const edge = 18;
  const hidden = (1 - p) * (100 + edge);
  return (
    <div
      style={{
        fontFamily: font.arabic,
        fontSize: size,
        lineHeight: 1.35,
        direction: "rtl",
        color: color.text,
        whiteSpace: "nowrap",
        WebkitMaskImage: `linear-gradient(to left, transparent ${hidden - edge}%, black ${hidden}%)`,
        maskImage: `linear-gradient(to left, transparent ${hidden - edge}%, black ${hidden}%)`,
        textShadow: `0 0 ${size * 0.35}px rgba(255,236,200,${glow * p})`,
        ...style,
      }}
    >
      {text}
    </div>
  );
};

export const Counter: React.FC<{ to: number; start: number; duration?: number; decimals?: number; style?: React.CSSProperties; suffix?: string }> = ({ to, start, duration = 1.1, decimals = 0, style, suffix = "" }) => {
  const t = useSeconds();
  const p = range(t, start, start + duration, ease.out);
  const value = to * p;
  const formatted = value.toLocaleString("tr-TR", { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
  return <span style={{ fontVariantNumeric: "tabular-nums", ...style }}>{formatted}{suffix}</span>;
};

export const Slam: React.FC<{ start: number; end: number; top: string; bottom?: string; align?: "left" | "center" | "right"; size?: number }> = ({ start, end, top, bottom, align = "center", size = 210 }) => {
  const t = useSeconds();
  if (t < start - 0.02 || t > end + 0.02) return null;
  const inP = range(t, start, start + 0.12, ease.out);
  const outP = range(t, end - 0.09, end, ease.in);
  const scale = 1.45 - 0.45 * inP;
  const slices = 7;
  const justify = align === "left" ? "flex-start" : align === "right" ? "flex-end" : "center";
  return (
    <AbsoluteFill style={{ justifyContent: "center", alignItems: justify, padding: "0 150px", flexDirection: "column" }}>
      <div style={{ position: "relative", transform: `scale(${scale})`, filter: `blur(${(1 - inP) * 22}px)`, opacity: inP, textAlign: align }}>
        {Array.from({ length: slices }).map((_, i) => {
          const offset = outP * (i % 2 === 0 ? 1 : -1) * (80 + i * 30);
          const sliceTop = (i / slices) * 100;
          const sliceBottom = 100 - ((i + 1) / slices) * 100;
          return (
            <div
              key={i}
              style={{
                position: i === 0 ? "relative" : "absolute",
                left: 0,
                top: 0,
                clipPath: `inset(${sliceTop}% 0 ${sliceBottom}% 0)`,
                transform: `translateX(${offset}px)`,
                opacity: 1 - outP * 0.6,
              }}
            >
              <div style={{ fontFamily: font.display, fontWeight: 500, fontSize: size, lineHeight: 0.95, letterSpacing: "-0.035em", color: color.text, fontVariantNumeric: "tabular-nums", whiteSpace: "nowrap" }}>{top}</div>
              {bottom ? <div style={{ fontFamily: font.sans, fontSize: size * 0.2, color: color.muted, marginTop: size * 0.08, letterSpacing: "0.01em" }}>{bottom}</div> : null}
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

export const Grain: React.FC<{ opacity?: number }> = ({ opacity = 0.075 }) => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ pointerEvents: "none", mixBlendMode: "overlay", opacity }}>
      <svg width="100%" height="100%">
        <filter id="grain">
          <feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" seed={frame % 97} stitchTiles="stitch" />
          <feColorMatrix type="saturate" values="0" />
        </filter>
        <rect width="100%" height="100%" filter="url(#grain)" />
      </svg>
    </AbsoluteFill>
  );
};

export const Vignette: React.FC<{ strength?: number }> = ({ strength = 0.62 }) => (
  <AbsoluteFill style={{ pointerEvents: "none", background: `radial-gradient(ellipse at center, rgba(0,0,0,0) 46%, rgba(0,0,0,${strength}) 100%)` }} />
);

export const Flash: React.FC<{ at: number; duration?: number; intensity?: number; tint?: string }> = ({ at, duration = 0.35, intensity = 0.85, tint = "255,250,240" }) => {
  const t = useSeconds();
  if (t < at || t > at + duration) return null;
  const p = 1 - range(t, at, at + duration, ease.out);
  const core = p * intensity;
  return (
    <AbsoluteFill
      style={{
        background: `radial-gradient(ellipse 75% 85% at 50% 50%, rgba(${tint},${Math.min(1, core * 1.35)}) 0%, rgba(${tint},${core * 0.55}) 45%, rgba(${tint},${core * 0.18}) 100%)`,
        mixBlendMode: "screen",
        pointerEvents: "none",
      }}
    />
  );
};

export const HBlurFilter: React.FC<{ id: string; x: number; y?: number }> = ({ id, x, y = 0 }) => (
  <svg width="0" height="0" style={{ position: "absolute" }}>
    <filter id={id} x="-10%" y="-10%" width="120%" height="120%">
      <feGaussianBlur stdDeviation={`${x.toFixed(2)} ${y.toFixed(2)}`} />
    </filter>
  </svg>
);

type PhoneProps = {
  clip: string;
  clipStart?: number;
  height: number;
  x: number;
  y: number;
  rotateY?: number;
  rotateX?: number;
  rotateZ?: number;
  scale?: number;
  opacity?: number;
  playbackRate?: number;
};

export const Phone: React.FC<PhoneProps> = ({ clip, clipStart = 0, height, x, y, rotateY = 0, rotateX = 0, rotateZ = 0, scale = 1, opacity = 1, playbackRate = 1 }) => {
  const { fps } = useVideoConfig();
  const width = height * (1080 / 2400);
  const bezel = height * 0.018;
  const radius = height * 0.058;
  const glare = interpolate(rotateY, [-40, 40], [-30, 130]);
  return (
    <div
      style={{
        position: "absolute",
        left: x - width / 2 - bezel,
        top: y - height / 2 - bezel,
        width: width + bezel * 2,
        height: height + bezel * 2,
        transform: `perspective(2200px) rotateY(${rotateY}deg) rotateX(${rotateX}deg) rotateZ(${rotateZ}deg) scale(${scale})`,
        transformStyle: "preserve-3d",
        opacity,
      }}
    >
      <div
        style={{
          position: "absolute",
          inset: 0,
          borderRadius: radius + bezel,
          background: "linear-gradient(145deg, #3a3d44 0%, #121418 38%, #25282e 100%)",
          boxShadow: `0 ${height * 0.05}px ${height * 0.12}px rgba(0,0,0,0.65), inset 0 0 0 1.5px rgba(255,255,255,0.18)`,
        }}
      />
      <div style={{ position: "absolute", left: bezel, top: bezel, width, height, borderRadius: radius, overflow: "hidden", background: "#000" }}>
        <OffthreadVideo
          src={staticFile(`clips/${clip}`)}
          trimBefore={Math.round(clipStart * fps)}
          playbackRate={playbackRate}
          muted
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
        <div style={{ position: "absolute", inset: 0, background: `linear-gradient(115deg, rgba(255,255,255,0) ${glare - 20}%, rgba(255,255,255,0.10) ${glare}%, rgba(255,255,255,0) ${glare + 18}%)` }} />
      </div>
      <div style={{ position: "absolute", left: "50%", top: bezel + height * 0.018, width: height * 0.022, height: height * 0.022, marginLeft: -height * 0.011, borderRadius: "50%", background: "#050505", boxShadow: "inset 0 0 0 1px rgba(255,255,255,0.08)" }} />
    </div>
  );
};
