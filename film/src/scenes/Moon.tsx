import React from "react";
import { AbsoluteFill, useVideoConfig } from "remotion";
import { FadeText, Flash, mixNumber, range, SplitText, useSeconds } from "../components/ui";
import { MoonLayer, TwilightSky } from "../gl/layers";
import { horizonCamera, horizonDirection, project, stereographicScale, type Vec3 } from "../lib/sky";
import { color, cue, ease, font, scene } from "../theme";

const START = scene("moon").start;
const SUN_AZ = 263;
const SUN_ALT = -2.7;
const MOON_AZ = 249.2;
const MOON_ALT = 2.2;

const sunFromPhase = (phaseDegrees: number, screenAngle: number): Vec3 => {
  const phase = (phaseDegrees * Math.PI) / 180;
  return [Math.sin(phase) * Math.cos(screenAngle), Math.sin(phase) * Math.sin(screenAngle), Math.cos(phase)];
};

export const Moon: React.FC = () => {
  const t = useSeconds();
  const { width, height } = useVideoConfig();
  const crescentAt = cue.crescent - START;
  const categoryAt = cue.category - START;
  const toHorizon = range(t, crescentAt - 0.4, crescentAt + 0.8, ease.inOut);

  const sweep = range(t, 0, 2.1, ease.inOut);
  const back = range(t, 2.1, crescentAt + 0.4, ease.inOut);
  const phase = mixNumber(mixNumber(158, 18, sweep), 156, back);

  const view = horizonCamera(256, 6);
  const fov = 58;
  const pxScale = stereographicScale(width, fov);
  const moonScreen = project(view, horizonDirection(MOON_AZ, MOON_ALT), width, height, pxScale);
  const sunScreen = project(view, horizonDirection(SUN_AZ, SUN_ALT), width, height, pxScale);
  const towardSun = Math.atan2(-(sunScreen[1] - moonScreen[1]), sunScreen[0] - moonScreen[0]);

  const bigCenter: [number, number] = [width * 0.62, height / 2];
  const center: [number, number] = [mixNumber(bigCenter[0], moonScreen[0], toHorizon), mixNumber(bigCenter[1], moonScreen[1], toHorizon)];
  const radius = mixNumber(430 + t * 10, 46, toHorizon);
  const screenAngle = mixNumber(0, towardSun, toHorizon);

  return (
    <AbsoluteFill style={{ background: color.ink }}>
      <AbsoluteFill style={{ opacity: toHorizon }}>
        <TwilightSky view={view} fov={fov} sun={horizonDirection(SUN_AZ, SUN_ALT)} />
        <AbsoluteFill style={{ background: "linear-gradient(180deg, rgba(8,10,26,0.66) 0%, rgba(8,10,26,0.3) 42%, rgba(8,10,26,0) 72%)" }} />
      </AbsoluteFill>
      <MoonLayer center={center} radius={radius} sun={sunFromPhase(phase, screenAngle)} spin={0.4 + t * 0.05} relief={mixNumber(2.6, 1.2, toHorizon)} gain={mixNumber(1.35, 3.2, toHorizon)} earthshine={0.05} halo={1 - toHorizon * 0.6} />

      <AbsoluteFill style={{ padding: "0 0 0 130px", justifyContent: "center", opacity: 1 - range(t, crescentAt - 0.5, crescentAt, ease.in) }}>
        <div style={{ fontFamily: font.display, fontSize: 98, letterSpacing: "-0.035em", color: color.text, lineHeight: 1.02, width: 600 }}>
          <SplitText text="Her ay, bir hilal." start={0.45} stagger={0.03} />
        </div>
        <FadeText start={1.1} style={{ fontFamily: font.sans, fontSize: 28, color: color.muted, marginTop: 20 }}>
          Kavuşum · 10 Ekim 2026 · 18:50
        </FadeText>
        <FadeText start={1.4} style={{ fontFamily: font.sans, fontSize: 24, color: color.faint, marginTop: 8, width: 520, lineHeight: 1.45 }}>
          Ay'ın evresi, gerçek Güneş yönüne göre NASA'nın LRO dokusu üzerinde aydınlanıyor.
        </FadeText>
      </AbsoluteFill>

      {toHorizon > 0.6 ? (
        <AbsoluteFill style={{ opacity: range(t, crescentAt + 0.4, crescentAt + 0.9) }}>
          <svg width={width} height={height} style={{ position: "absolute" }}>
            <circle cx={moonScreen[0]} cy={moonScreen[1]} r={76} fill="none" stroke={color.text} strokeWidth={1.2} opacity={0.7} />
            <circle cx={sunScreen[0]} cy={sunScreen[1]} r={20} fill="none" stroke={color.warm} strokeWidth={1.2} strokeDasharray="4 5" opacity={0.8} />
            <line x1={moonScreen[0]} y1={moonScreen[1]} x2={sunScreen[0]} y2={sunScreen[1]} stroke={color.text} strokeDasharray="3 7" opacity={0.35} />
          </svg>
          <div style={{ position: "absolute", left: moonScreen[0] + 92, top: moonScreen[1] - 26, fontFamily: font.sans, fontSize: 26, color: color.text, textShadow: "0 1px 14px rgba(40,16,0,0.55)" }}>Ay 2,2°</div>
          <div style={{ position: "absolute", left: sunScreen[0] + 34, top: sunScreen[1] - 4, fontFamily: font.sans, fontSize: 26, color: color.warm, textShadow: "0 1px 14px rgba(40,16,0,0.55)" }}>Güneş −2,7°</div>
          <div style={{ position: "absolute", left: 130, top: 120, fontFamily: font.sans, fontSize: 26, color: color.muted }}>12 Ekim akşamı · İstanbul · batı ufku</div>
          <div style={{ position: "absolute", left: 130, top: 160, fontFamily: font.display, fontSize: 64, color: color.text, letterSpacing: "-0.02em" }}>En iyi gözlem 18:41</div>
        </AbsoluteFill>
      ) : null}

      {t >= categoryAt ? (
        <AbsoluteFill style={{ justifyContent: "flex-end", alignItems: "flex-end", padding: "0 130px 130px 0" }}>
          <FadeText start={categoryAt} style={{ textAlign: "right", textShadow: "0 2px 24px rgba(20,8,0,0.45)" }}>
            <div style={{ fontFamily: font.sans, fontSize: 26, color: color.muted }}>Yallop q değeri</div>
            <div style={{ fontFamily: font.display, fontSize: 132, letterSpacing: "-0.04em", color: color.text, lineHeight: 1, fontVariantNumeric: "tabular-nums" }}>+0,157</div>
            <div style={{ display: "inline-flex", alignItems: "center", gap: 14, marginTop: 14, padding: "10px 20px", borderRadius: 999, background: "rgba(244,242,236,0.92)", color: color.ink, fontFamily: font.sans, fontSize: 26, fontWeight: 500 }}>
              <span style={{ fontFamily: font.display, fontWeight: 600 }}>B</span> İdeal şartlarda çıplak gözle görülür
            </div>
          </FadeText>
        </AbsoluteFill>
      ) : null}

      <Flash at={0} duration={0.45} intensity={0.35} />
      <Flash at={categoryAt} duration={0.3} intensity={0.2} />
    </AbsoluteFill>
  );
};
