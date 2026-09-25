import React from "react";
import { AbsoluteFill, Sequence, useVideoConfig } from "remotion";
import { ArabicReveal, Counter, FadeText, Flash, mixNumber, Phone, range, useSeconds } from "../components/ui";
import { StarField } from "../gl/layers";
import { lookBasis } from "../lib/sky";
import { NCP, STAR } from "../lib/stars";
import { color, cue, ease, font, scene } from "../theme";

const START = scene("app").start;

const STATS = [
  { value: 8920, label: "yıldız, gerçek konumunda" },
  { value: 89, label: "takımyıldız, Türkçe adıyla" },
  { value: 126, label: "yıldızın Arapça ad kökeni" },
  { value: 60, label: "karede bir, tek shader'da gökyüzü", suffix: " fps" },
];

export const AppShowcase: React.FC = () => {
  const t = useSeconds();
  const { height, fps } = useVideoConfig();
  const vegaCut = cue.cutVega - START;
  const triptych = cue.cutTriptych - START;
  const zoom = range(t, 9.35, 10.0, ease.in);

  const enter = range(t, 0, 0.9, ease.out);
  const toVega = range(t, vegaCut - 0.35, vegaCut + 0.35, ease.inOut);
  const toTriptych = range(t, triptych - 0.3, triptych + 0.2, ease.inOut);

  const phoneX = mixNumber(mixNumber(1560, 1330, enter), 640, toVega);
  const phoneRotate = mixNumber(mixNumber(-34, -14, enter), 12, toVega);
  const drift = Math.sin(t * 0.9) * 2;
  const view = lookBasis(STAR.Deneb, NCP, t * 3);

  return (
    <AbsoluteFill style={{ background: color.ink }}>
      <AbsoluteFill style={{ filter: "blur(5px) brightness(0.55)", transform: `scale(${1.08 + t * 0.01})` }}>
        <StarField view={view} fov={95} time={t + START} milkyGain={1.1} limit={6} />
      </AbsoluteFill>
      <AbsoluteFill style={{ background: "linear-gradient(90deg, rgba(5,7,11,0.85) 0%, rgba(5,7,11,0.25) 55%, rgba(5,7,11,0.6) 100%)" }} />

      {t < triptych + 0.2 ? (
        <AbsoluteFill style={{ opacity: 1 - toTriptych }}>
          <Sequence durationInFrames={Math.round(vegaCut * fps)} layout="none">
            <Phone clip="dusk.mp4" clipStart={0.4} playbackRate={2} height={900} x={phoneX} y={height / 2 + 10} rotateY={phoneRotate + drift} rotateX={4} opacity={enter} />
          </Sequence>
          <Sequence from={Math.round(vegaCut * fps)} layout="none">
            <Phone clip="vega.mp4" clipStart={0.2} height={900} x={phoneX} y={height / 2 + 10} rotateY={phoneRotate + drift} rotateX={4} opacity={enter} />
          </Sequence>
        </AbsoluteFill>
      ) : null}

      {t < vegaCut + 0.2 ? (
        <AbsoluteFill style={{ padding: "0 0 0 140px", justifyContent: "center", opacity: 1 - range(t, vegaCut - 0.3, vegaCut, ease.in) }}>
          {STATS.map((stat, i) => {
            const at = cue.stats[i] - START;
            return (
              <FadeText key={stat.label} start={at} style={{ marginBottom: 26 }}>
                <div style={{ display: "flex", alignItems: "baseline", gap: 22 }}>
                  <div style={{ fontFamily: font.display, fontSize: 104, letterSpacing: "-0.035em", color: color.text, lineHeight: 1, width: 380 }}>
                    <Counter to={stat.value} start={at} duration={0.9} suffix={stat.suffix} />
                  </div>
                  <div style={{ fontFamily: font.sans, fontSize: 30, color: color.muted, width: 420 }}>{stat.label}</div>
                </div>
                <div style={{ height: 1, width: 800 * range(t, at + 0.2, at + 0.9, ease.inOut), background: color.hairline, marginTop: 18 }} />
              </FadeText>
            );
          })}
        </AbsoluteFill>
      ) : null}

      {t >= vegaCut - 0.1 && t < triptych + 0.2 ? (
        <AbsoluteFill style={{ padding: "0 150px 0 1060px", justifyContent: "center", opacity: 1 - toTriptych }}>
          <FadeText start={vegaCut + 0.1} style={{ fontFamily: font.sans, fontSize: 26, color: color.faint }}>Adın hikâyesi</FadeText>
          <FadeText start={vegaCut + 0.2} style={{ fontFamily: font.display, fontSize: 120, letterSpacing: "-0.035em", color: color.text, lineHeight: 1.05 }}>Vega</FadeText>
          <ArabicReveal text="النسر الواقع" start={vegaCut + 0.45} duration={0.9} size={108} style={{ textAlign: "left", width: 640 }} />
          <FadeText start={vegaCut + 0.8} style={{ fontFamily: font.sans, fontSize: 34, color: color.text }}>
            en-Nesr el-Vâki' <span style={{ color: color.muted, fontStyle: "italic" }}>· konan kartal</span>
          </FadeText>
          <FadeText start={vegaCut + 1.1} style={{ fontFamily: font.sans, fontSize: 24, color: color.muted, marginTop: 22, width: 620, lineHeight: 1.5 }}>
            Kanatlarını kapatıp yere inen bir kartal. Vega adı, Latince çevirilerde “vâki'” kelimesinin bozulmasından doğdu.
          </FadeText>
        </AbsoluteFill>
      ) : null}

      {t >= triptych - 0.1 ? (
        <AbsoluteFill style={{ transform: `scale(${1 + zoom * 2.4})`, transformOrigin: "1400px 520px", opacity: 1 - zoom * 0.9 }}>
          {[
            { clip: "qibla.mp4", start: 0.6, x: 560, label: "Kıble yönü, 151,6°" },
            { clip: "search.mp4", start: 0.9, x: 980, label: "Arapça adla arama" },
            { clip: "hilal.mp4", start: 0.3, x: 1400, label: "Hilal hesabı" },
          ].map((item, i) => {
            const p = range(t, triptych + i * 0.12, triptych + 0.7 + i * 0.12, ease.out);
            return (
              <React.Fragment key={item.clip}>
                <Sequence from={Math.round((triptych - 0.1) * fps)} layout="none">
                  <Phone clip={item.clip} clipStart={item.start} height={760} x={item.x} y={height / 2 - 20 + (1 - p) * 700} rotateY={(i - 1) * -9} rotateX={3} opacity={p} />
                </Sequence>
                <div
                  style={{
                    position: "absolute",
                    left: item.x - 200,
                    width: 400,
                    top: height / 2 + 400,
                    textAlign: "center",
                    fontFamily: font.sans,
                    fontSize: 28,
                    color: color.text,
                    opacity: range(t, triptych + 0.5 + i * 0.12, triptych + 0.9 + i * 0.12),
                  }}
                >
                  {item.label}
                </div>
              </React.Fragment>
            );
          })}
        </AbsoluteFill>
      ) : null}

      <Flash at={0} duration={0.4} intensity={0.4} />
      <Flash at={vegaCut} duration={0.25} intensity={0.18} />
      <Flash at={triptych} duration={0.25} intensity={0.18} />
      <AbsoluteFill style={{ background: "#000", opacity: range(t, 9.7, 10, ease.in) * 0.4 }} />
    </AbsoluteFill>
  );
};
