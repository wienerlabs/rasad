import React from "react";
import { AbsoluteFill, useVideoConfig } from "remotion";
import { Flash, range, mixNumber, SplitText, useSeconds } from "../components/ui";
import { StarField } from "../gl/layers";
import { lookBasis } from "../lib/sky";
import { NCP, STAR } from "../lib/stars";
import { color, cue, ease, font } from "../theme";

export const Ignite: React.FC = () => {
  const t = useSeconds();
  const { width, height } = useVideoConfig();
  const spark = range(t, cue.spark, cue.spark + 0.55, ease.out);
  const sparkFade = 1 - range(t, cue.shock + 0.1, cue.shock + 1.4, ease.inOut);
  const reveal = range(t, cue.shock, cue.shock + 1.9, ease.out);
  const diagonal = Math.hypot(width, height) / 2;
  const revealRadius = 10 + reveal * (diagonal + 260);
  const fov = mixNumber(58, 86, range(t, 0, 4, ease.inOut));
  const roll = mixNumber(-7, 4, range(t, 0, 4, ease.inOut));
  const view = lookBasis(STAR.Vega, NCP, roll);
  const flicker = 0.82 + 0.18 * Math.sin(t * 41) * Math.sin(t * 17);
  const sparkSize = 18 + spark * 70 * flicker;
  const ring = 1 - reveal;

  return (
    <AbsoluteFill style={{ background: color.ink }}>
      <StarField view={view} fov={fov} time={t} milkyGain={1.15 * reveal} starGain={1.05} revealRadius={revealRadius} limit={6.4} size={1.05} />
      <AbsoluteFill style={{ opacity: spark * sparkFade, mixBlendMode: "screen" }}>
        <div
          style={{
            position: "absolute",
            left: width / 2 - sparkSize * 3,
            top: height / 2 - sparkSize * 3,
            width: sparkSize * 6,
            height: sparkSize * 6,
            borderRadius: "50%",
            background: "radial-gradient(circle, rgba(255,255,255,1) 0%, rgba(210,228,255,0.55) 12%, rgba(160,190,255,0.12) 38%, rgba(0,0,0,0) 70%)",
          }}
        />
        <div style={{ position: "absolute", left: 0, top: height / 2 - 1.5, width, height: 3, background: `linear-gradient(90deg, rgba(0,0,0,0) 20%, rgba(200,220,255,${0.55 * spark}) 50%, rgba(0,0,0,0) 80%)` }} />
        <div style={{ position: "absolute", top: 0, left: width / 2 - 1, height, width: 2, background: `linear-gradient(180deg, rgba(0,0,0,0) 30%, rgba(200,220,255,${0.35 * spark}) 50%, rgba(0,0,0,0) 70%)` }} />
      </AbsoluteFill>
      <svg width={width} height={height} style={{ position: "absolute", left: 0, top: 0 }}>
        {t > cue.shock ? (
          <>
            <circle cx={width / 2} cy={height / 2} r={revealRadius} fill="none" stroke="rgba(230,238,255,0.9)" strokeWidth={1 + ring * 5} opacity={ring * 0.85} />
            <circle cx={width / 2} cy={height / 2} r={revealRadius * 0.82} fill="none" stroke="rgba(230,238,255,0.6)" strokeWidth={1} opacity={ring * 0.4} />
          </>
        ) : null}
      </svg>
      <Flash at={cue.shock} duration={0.5} intensity={0.55} />
      <AbsoluteFill style={{ justifyContent: "flex-end", alignItems: "center", paddingBottom: 170 }}>
        <div style={{ fontFamily: font.display, fontWeight: 400, fontSize: 112, letterSpacing: "-0.035em", color: color.text, textShadow: "0 0 40px rgba(0,0,0,0.6)" }}>
          <SplitText text="Gökyüzü bir kitaptı." start={1.9} stagger={0.03} exit={3.5} />
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
