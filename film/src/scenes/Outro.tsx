import React from "react";
import { AbsoluteFill, Img, staticFile, useVideoConfig } from "remotion";
import { FadeText, Flash, mixNumber, range, SplitText, useSeconds } from "../components/ui";
import { StarField } from "../gl/layers";
import { lookBasis } from "../lib/sky";
import { STAR } from "../lib/stars";
import { color, cue, ease, font, scene } from "../theme";
import { RasadMark } from "./Astrolabe";

const START = scene("outro").start;

export const Outro: React.FC = () => {
  const t = useSeconds();
  const { width, height } = useVideoConfig();
  const wordmarkAt = cue.wordmark - START;
  const endAt = cue.end - START;
  const reveal = range(t, 0, 1.5, ease.out);
  const diagonal = Math.hypot(width, height) / 2;
  const revealRadius = 20 + reveal * (diagonal + 280);
  const ring = 1 - reveal;
  const view = lookBasis(STAR.Polaris, [1, 0, 0], -t * 7);
  const fov = mixNumber(104, 82, range(t, 0, 6, ease.out));
  const mark = range(t, wordmarkAt, wordmarkAt + 1.2, ease.out);
  const settle = range(t, wordmarkAt, wordmarkAt + 2.4, ease.out);
  const fadeOut = range(t, endAt, 6.0, ease.inOut);
  const breathe = 0.85 + 0.15 * Math.sin(t * 2.2);

  return (
    <AbsoluteFill style={{ background: color.ink }}>
      <AbsoluteFill style={{ opacity: 1 - fadeOut }}>
        <StarField view={view} fov={fov} time={t + START} milkyGain={1.05} starGain={1.0} limit={6.4} revealRadius={revealRadius} />
        <AbsoluteFill style={{ background: `radial-gradient(circle at 50% 46%, rgba(5,7,11,${0.72 * mark}) 0%, rgba(5,7,11,${0.4 * mark}) 30%, rgba(5,7,11,0) 62%)` }} />
        <svg width={width} height={height} style={{ position: "absolute", left: 0, top: 0 }}>
          {ring > 0 ? (
            <>
              <circle cx={width / 2} cy={height / 2} r={revealRadius} fill="none" stroke="rgba(230,238,255,0.9)" strokeWidth={1 + ring * 5} opacity={ring * 0.85} />
              <circle cx={width / 2} cy={height / 2} r={revealRadius * 0.84} fill="none" stroke="rgba(230,238,255,0.6)" strokeWidth={1} opacity={ring * 0.4} />
            </>
          ) : null}
        </svg>

        <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", transform: `translateY(${-40 + (1 - settle) * 20}px)` }}>
          <div style={{ transform: `scale(${1.25 - 0.25 * settle})` }}>
            <RasadMark size={176} progress={mark} glow={breathe * mark} />
          </div>
          <div style={{ fontFamily: font.display, fontSize: 152, letterSpacing: "-0.045em", color: color.text, lineHeight: 1, marginTop: 30 }}>
            <SplitText text="Rasad" start={wordmarkAt + 0.2} stagger={0.06} duration={0.8} />
          </div>
          <div style={{ fontFamily: font.sans, fontSize: 38, color: color.muted, marginTop: 18 }}>
            <SplitText text="Cebindeki rasathane" start={wordmarkAt + 0.6} stagger={0.022} duration={0.55} rise={0.3} blur={6} />
          </div>
          <FadeText start={wordmarkAt + 1.3} style={{ fontFamily: font.sans, fontSize: 26, color: color.faint, marginTop: 30, letterSpacing: "0.01em" }}>
            Gökyüzünü yeniden oku.
          </FadeText>
        </AbsoluteFill>

        <AbsoluteFill style={{ justifyContent: "flex-end", alignItems: "center", paddingBottom: 70 }}>
          <FadeText start={wordmarkAt + 1.8} rise={10}>
            <div style={{ display: "flex", alignItems: "center", gap: 18 }}>
              <Img src={staticFile("data/wiener-logo-white.png")} style={{ height: 34, opacity: 0.92 }} />
              <div style={{ fontFamily: font.sans, fontSize: 24, color: color.text, letterSpacing: "0.01em" }}>Wiener Labs</div>
              <div style={{ width: 1, height: 22, background: color.hairline }} />
              <div style={{ fontFamily: font.sans, fontSize: 24, color: color.muted }}>Android için</div>
            </div>
          </FadeText>
        </AbsoluteFill>
      </AbsoluteFill>

      <Flash at={0} duration={0.55} intensity={0.6} />
      <Flash at={wordmarkAt} duration={0.4} intensity={0.16} />
    </AbsoluteFill>
  );
};
