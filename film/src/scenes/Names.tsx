import React from "react";
import { AbsoluteFill, useVideoConfig } from "remotion";
import { ConstellationLines } from "../components/Constellations";
import { ArabicReveal, FadeText, Flash, HBlurFilter, mixNumber, range, SplitText, useSeconds } from "../components/ui";
import { StarField } from "../gl/layers";
import { add, lookBasis, normalize, project, scale, slerpBasis, stereographicScale, type Mat3 } from "../lib/sky";
import { MONTAGE, NCP, STAR } from "../lib/stars";
import { color, cue, ease, font, scene } from "../theme";

const START = scene("names").start;

const vegaRight = (): Mat3 => {
  const base = lookBasis(STAR.Vega, NCP, 4);
  const forward = normalize(add(STAR.Vega, scale(base[0], -0.42)));
  return lookBasis(forward, NCP, 4);
};

export const Names: React.FC = () => {
  const t = useSeconds();
  const { width, height } = useVideoConfig();
  const montageStart = MONTAGE.map((_, i) => cue.montage[i] - START);
  const firstMontage = montageStart[0];

  let view: Mat3;
  let fov: number;
  let whip = 0;
  const intro = lookBasis(STAR.Vega, NCP, 4);
  if (t < firstMontage) {
    const p = range(t, 0, 2.4, ease.inOut);
    view = slerpBasis(intro, vegaRight(), p);
    fov = mixNumber(86, 60, p);
  } else {
    let index = 0;
    for (let i = 0; i < montageStart.length; i++) if (t >= montageStart[i]) index = i;
    const previous = index === 0 ? vegaRight() : lookBasis(STAR[MONTAGE[index - 1].star], NCP, index % 2 === 0 ? 5 : -5);
    const target = lookBasis(STAR[MONTAGE[index].star], NCP, index % 2 === 0 ? -5 : 5);
    const p = range(t, montageStart[index], montageStart[index] + 0.17, ease.snap);
    view = slerpBasis(previous, target, p);
    whip = Math.sin(p * Math.PI);
    fov = mixNumber(index === 0 ? 60 : 50, 50, p);
  }
  const outro = range(t, 7.72, 8.0, ease.in);
  fov = fov + outro * 90;

  const vegaScreen = project(view, STAR.Vega, width, height, stereographicScale(width, fov));
  const vegaCard = range(t, 0.35, 0.9) * (1 - range(t, firstMontage - 0.35, firstMontage - 0.05, ease.in));
  const lines = range(t, 0.2, 2.2, ease.inOut) * (1 - range(t, firstMontage - 0.3, firstMontage, ease.in));

  return (
    <AbsoluteFill style={{ background: color.ink, opacity: 1 - outro }}>
      <HBlurFilter id="whip" x={whip * 26} y={whip * 2} />
      <AbsoluteFill style={{ filter: whip > 0.02 ? "url(#whip)" : undefined }}>
        <StarField view={view} fov={fov} time={t + START} milkyGain={1.15} limit={6.4} size={1.05} />
        <ConstellationLines view={view} fov={fov} codes={["Lyr", "Cyg", "Aql", "Her", "Dra", "Sge", "Vul"]} progress={lines} opacity={0.38 * (lines > 0 ? 1 : 0)} />
      </AbsoluteFill>

      {vegaCard > 0 && vegaScreen[2] > 0 ? (
        <svg width={width} height={height} style={{ position: "absolute", left: 0, top: 0, opacity: vegaCard }}>
          <circle cx={vegaScreen[0]} cy={vegaScreen[1]} r={30 + (1 - vegaCard) * 30} fill="none" stroke={color.text} strokeWidth={1.3} />
          {[45, 135, 225, 315].map((a) => {
            const r = (a * Math.PI) / 180;
            return <line key={a} x1={vegaScreen[0] + Math.cos(r) * 36} y1={vegaScreen[1] + Math.sin(r) * 36} x2={vegaScreen[0] + Math.cos(r) * 46} y2={vegaScreen[1] + Math.sin(r) * 46} stroke={color.text} strokeWidth={1.3} />;
          })}
          <line x1={vegaScreen[0] - 46} y1={vegaScreen[1]} x2={880} y2={vegaScreen[1]} stroke={color.faint} strokeWidth={1} strokeDasharray="4 6" />
        </svg>
      ) : null}

      {t < firstMontage ? (
        <AbsoluteFill style={{ padding: "0 0 0 150px", justifyContent: "center", opacity: 1 - range(t, firstMontage - 0.35, firstMontage - 0.05, ease.in) }}>
          <div style={{ fontFamily: font.sans, fontSize: 24, color: color.faint, letterSpacing: "0.02em", marginBottom: 6 }}>
            <SplitText text="α Lyr · Çalgı takımyıldızı · 25 ışık yılı" start={1.7} stagger={0.012} duration={0.5} rise={0.3} blur={6} />
          </div>
          <div style={{ fontFamily: font.display, fontSize: 176, fontWeight: 400, letterSpacing: "-0.04em", color: color.text, lineHeight: 1 }}>
            <SplitText text="Vega" start={cue.vega - START} stagger={0.06} duration={0.8} />
          </div>
          <ArabicReveal text="النسر الواقع" start={0.95} duration={1.1} size={124} style={{ textAlign: "left", width: 620, marginTop: 10 }} />
          <FadeText start={1.55} style={{ fontFamily: font.sans, fontSize: 40, color: color.text, marginTop: 6 }}>en-Nesr el-Vâki'</FadeText>
          <FadeText start={1.8} style={{ fontFamily: font.sans, fontSize: 32, color: color.muted, fontStyle: "italic" }}>“konan kartal”</FadeText>
        </AbsoluteFill>
      ) : null}

      {MONTAGE.map((card, i) => {
        const start = montageStart[i];
        const end = i + 1 < montageStart.length ? montageStart[i + 1] : 7.75;
        if (t < start || t > end) return null;
        const inP = range(t, start + 0.1, start + 0.32);
        const outP = range(t, end - 0.12, end, ease.in);
        const layout = i % 3;
        const align = layout === 0 ? "center" : layout === 1 ? "flex-start" : "flex-end";
        return (
          <AbsoluteFill key={card.star} style={{ justifyContent: "center", alignItems: align, padding: "0 170px", opacity: inP * (1 - outP) }}>
            <div style={{ textAlign: layout === 0 ? "center" : layout === 1 ? "left" : "right", transform: `translateY(${(1 - inP) * 30}px) scale(${1 + outP * 0.06})`, filter: `blur(${(1 - inP) * 10 + outP * 8}px)` }}>
              <div style={{ fontFamily: font.display, fontSize: 64, fontWeight: 500, letterSpacing: "-0.02em", color: color.text }}>{card.star}</div>
              <div style={{ fontFamily: font.arabic, fontSize: 200, lineHeight: 1.25, color: color.text, direction: "rtl", textShadow: "0 0 60px rgba(255,236,200,0.35)" }}>{card.arabic}</div>
              <div style={{ fontFamily: font.sans, fontSize: 34, color: color.text }}>
                {card.transliteration}
                <span style={{ color: color.muted }}> · {card.meaning}</span>
              </div>
            </div>
          </AbsoluteFill>
        );
      })}

      {t >= firstMontage ? (
        <AbsoluteFill style={{ justifyContent: "flex-end", padding: "0 0 80px 90px" }}>
          <FadeText start={firstMontage + 0.3} end={7.6} style={{ fontFamily: font.sans, fontSize: 28, color: color.muted, letterSpacing: "0.01em" }}>
            126 yıldızın adı, kendi dilinde
            <div style={{ height: 2, width: 420 * range(t, firstMontage, 7.6, ease.inOut), background: color.text, marginTop: 14, opacity: 0.7 }} />
          </FadeText>
        </AbsoluteFill>
      ) : null}

      {montageStart.map((s) => (
        <Flash key={s} at={s + 0.1} duration={0.18} intensity={0.12} />
      ))}
    </AbsoluteFill>
  );
};
