import React from "react";
import { AbsoluteFill, useVideoConfig } from "remotion";
import { FadeText, Flash, mixNumber, range, SplitText, useSeconds } from "../components/ui";
import { QMapLayer } from "../gl/layers";
import { loadLand, loadQManifest, useAsset } from "../lib/assets";
import { color, cue, ease, font, scene } from "../theme";

const START = scene("map").start;
const RECT: [number, number, number, number] = [120, 250, 1680, 560];
const OCTOBER_DECLINATION = (-7.2 * Math.PI) / 180;
const FEBRUARY_DECLINATION = (-15.5 * Math.PI) / 180;

const toMap = (latitude: number, longitude: number): [number, number] => [
  RECT[0] + ((longitude + 180) / 360) * RECT[2],
  RECT[1] + ((60 - latitude) / 120) * RECT[3],
];

type Evening = { at: number; grid: string; label: string; detail: string; share: number };

export const WorldMap: React.FC = () => {
  const t = useSeconds();
  const { width, height } = useVideoConfig();
  const land = useAsset("land", loadLand);
  const manifest = useAsset("qmanifest", loadQManifest);
  const evenings: Evening[] = [
    { at: cue.evenings[0] - START, grid: "q-2026-10-10.png", label: "10 Ekim akşamı", detail: "Ay, Güneş'ten önce batıyor", share: 0 },
    { at: cue.evenings[1] - START, grid: "q-2026-10-11.png", label: "11 Ekim akşamı", detail: "Önce güney yarımküreden görülüyor", share: 48 },
    { at: cue.evenings[2] - START, grid: "q-2026-10-12.png", label: "12 Ekim akşamı", detail: "İstanbul dahil, dünyanın büyük kısmı", share: 88 },
    { at: cue.ramadan - START, grid: "q-2027-02-07.png", label: "Ramazan 1448 · 7 Şubat 2027", detail: "Bu kez önce kuzey yarımküre, İstanbul dahil", share: 44 },
  ];
  let index = 0;
  for (let i = 0; i < evenings.length; i++) if (t >= evenings[i].at) index = i;
  const current = evenings[index];
  const previous = evenings[Math.max(0, index - 1)];
  const mix = index === 0 ? 1 : range(t, current.at, current.at + 0.8, ease.inOut);
  const sweep = range(t, current.at, current.at + 1.7, ease.inOut);
  const firstReveal = range(t, evenings[0].at, evenings[0].at + 1.7, ease.inOut);
  const utc = mixNumber(3, 32, firstReveal);
  const glowUtc = mixNumber(3, 32, sweep);
  const coast = range(t, 0.1, 1.5, ease.inOut);
  const mapIn = range(t, 0, 0.6);
  const outro = range(t, 7.65, 8.0, ease.in);
  const declination = index === 3 ? mixNumber(OCTOBER_DECLINATION, FEBRUARY_DECLINATION, mix) : OCTOBER_DECLINATION;
  const istanbul = toMap(41.0, 29.0);
  const mecca = toMap(21.42, 39.83);
  const pulse = (t * 1.6) % 1;

  return (
    <AbsoluteFill style={{ background: color.ink, opacity: 1 - outro }}>
      <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 55%, rgba(40,52,80,0.35) 0%, rgba(5,7,11,0) 60%)" }} />
      {land ? (
        <svg width={RECT[2]} height={RECT[3]} viewBox="0 300 3600 1200" preserveAspectRatio="none" style={{ position: "absolute", left: RECT[0], top: RECT[1], opacity: mapIn }}>
          <g fill="#12151c">{land.paths.map((d, i) => <path key={i} d={d} />)}</g>
        </svg>
      ) : null}
      {manifest ? (
        <QMapLayer
          gridA={index === 0 ? current.grid : previous.grid}
          gridB={current.grid}
          mix={mix}
          rect={RECT}
          utc={utc}
          glowUtc={glowUtc}
          declination={declination}
          reveal={mapIn}
          low={manifest.low}
          high={manifest.high}
        />
      ) : null}
      {land ? (
        <svg width={RECT[2]} height={RECT[3]} viewBox="0 300 3600 1200" preserveAspectRatio="none" style={{ position: "absolute", left: RECT[0], top: RECT[1], mixBlendMode: "difference" }}>
          <g fill="none" stroke="rgba(244,242,236,0.7)" strokeWidth={2.2} vectorEffect="non-scaling-stroke">
            {land.paths.map((d, i) => (
              <path key={i} d={d} pathLength={1} strokeDasharray="1 1" strokeDashoffset={1 - coast} vectorEffect="non-scaling-stroke" style={{ strokeWidth: 1 }} />
            ))}
          </g>
        </svg>
      ) : null}
      <svg width={width} height={height} style={{ position: "absolute", left: 0, top: 0 }}>
        <rect x={RECT[0]} y={RECT[1]} width={RECT[2]} height={RECT[3]} fill="none" stroke={color.hairline} strokeWidth={1} opacity={mapIn} />
        {[-30, 0, 30].map((lat) => {
          const [, y] = toMap(lat, 0);
          return <line key={lat} x1={RECT[0]} x2={RECT[0] + RECT[2]} y1={y} y2={y} stroke="rgba(244,242,236,0.08)" strokeDasharray={lat === 0 ? undefined : "3 6"} />;
        })}
        {[istanbul, mecca].map(([x, y], i) => (
          <g key={i} opacity={range(t, 0.8 + i * 0.2, 1.2 + i * 0.2)}>
            <circle cx={x} cy={y} r={5} fill="#fff" />
            <circle cx={x} cy={y} r={6 + pulse * 22} fill="none" stroke="#fff" strokeWidth={1.2} opacity={1 - pulse} />
          </g>
        ))}
        <text x={istanbul[0] + 14} y={istanbul[1] - 10} fill={color.text} fontFamily="Funnel Sans" fontSize={22} opacity={range(t, 0.9, 1.3)}>İstanbul</text>
        <text x={mecca[0] + 14} y={mecca[1] + 26} fill={color.muted} fontFamily="Funnel Sans" fontSize={20} opacity={range(t, 1.1, 1.5)}>Mekke</text>
      </svg>

      <AbsoluteFill style={{ padding: "70px 0 0 120px" }}>
        <div style={{ fontFamily: font.display, fontSize: 76, letterSpacing: "-0.03em", color: color.text }}>
          <SplitText text="Hilal nereden görülür?" start={0.25} stagger={0.025} />
        </div>
      </AbsoluteFill>

      <AbsoluteFill style={{ justifyContent: "flex-end", padding: "0 120px 70px 120px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end" }}>
          <div key={current.label} style={{ opacity: range(t, current.at, current.at + 0.4), transform: `translateY(${(1 - range(t, current.at, current.at + 0.4)) * 14}px)` }}>
            <div style={{ fontFamily: font.display, fontSize: 48, letterSpacing: "-0.02em", color: color.text }}>{current.label}</div>
            <div style={{ fontFamily: font.sans, fontSize: 26, color: color.muted }}>{current.detail}</div>
          </div>
          <div style={{ textAlign: "right" }}>
            <div style={{ fontFamily: font.display, fontSize: 64, color: color.text, fontVariantNumeric: "tabular-nums", letterSpacing: "-0.02em" }}>
              %{Math.round(mixNumber(previous.share, current.share, mix))}
            </div>
            <div style={{ fontFamily: font.sans, fontSize: 22, color: color.muted }}>çıplak gözle görülen alan</div>
          </div>
        </div>
        <FadeText start={1.8} style={{ fontFamily: font.sans, fontSize: 20, color: color.faint, marginTop: 18 }}>
          43.681 noktanın her biri için Yallop q değeri, gün batımıyla birlikte açılıyor
        </FadeText>
      </AbsoluteFill>

      <Flash at={0} duration={0.4} intensity={0.3} />
      {evenings.slice(1).map((evening) => (
        <Flash key={evening.at} at={evening.at} duration={0.25} intensity={0.12} />
      ))}
    </AbsoluteFill>
  );
};
