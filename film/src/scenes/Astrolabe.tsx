import React from "react";
import { AbsoluteFill, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { FadeText, Flash, range, mixNumber, SplitText, useSeconds } from "../components/ui";
import { StarField } from "../gl/layers";
import { lookBasis } from "../lib/sky";
import { RETE_STARS, STAR } from "../lib/stars";
import { color, cue, ease, font, scene } from "../theme";

const START = scene("astrolabe").start;
const LATITUDE = 41.0082;
const EQUATOR_RADIUS = 245;
const OBLIQUITY = 23.44;
const QIBLA = 151.6;

const radiusFor = (declination: number) => EQUATOR_RADIUS * Math.tan(((90 - declination) * Math.PI) / 360);

const almucantar = (altitude: number) => {
  const north = altitude <= LATITUDE ? { dec: 90 - LATITUDE + altitude, upper: false } : { dec: 90 - altitude + LATITUDE, upper: true };
  const southDec = altitude + LATITUDE - 90;
  const ya = north.upper ? -radiusFor(north.dec) : radiusFor(north.dec);
  const yb = -radiusFor(southDec);
  return { cy: (ya + yb) / 2, r: Math.abs(ya - yb) / 2 };
};

const Circle: React.FC<{ cx: number; cy: number; r: number; progress: number; width?: number; opacity?: number; dash?: string }> = ({ cx, cy, r, progress, width = 1.2, opacity = 0.7, dash }) => {
  if (progress <= 0) return null;
  const circumference = 2 * Math.PI * r;
  return (
    <circle
      cx={cx}
      cy={cy}
      r={r}
      fill="none"
      stroke={color.text}
      strokeWidth={width}
      opacity={opacity}
      strokeDasharray={dash ?? `${circumference} ${circumference}`}
      strokeDashoffset={dash ? 0 : circumference * (1 - progress)}
      transform={`rotate(-90 ${cx} ${cy})`}
    />
  );
};

export const Astrolabe: React.FC = () => {
  const t = useSeconds();
  const frame = useCurrentFrame();
  const { width, height, fps } = useVideoConfig();
  const cx = width / 2 + 170;
  const cy = height / 2;
  const collapse = range(t, 4.25, 5.1, ease.inOut);
  const logo = range(t, cue.logo - START, cue.logo - START + 0.9, ease.out);
  const limb = range(t, 0.0, 0.9, ease.inOut);
  const ticks = range(t, 0.15, 1.3, ease.inOut);
  const plate = range(t, 0.35, 1.9, ease.inOut);
  const rete = range(t, 1.0, 1.9, ease.out);
  const lst = mixNumber(-40, 290, range(t, 1.0, 6.0, ease.inOut));
  const qiblaStart = 1.55;
  const needleSpring = spring({ frame: frame - Math.round(qiblaStart * fps), fps, config: { damping: 11, mass: 0.9, stiffness: 70 } });
  const needleAzimuth = QIBLA * needleSpring;
  const needleAngle = needleAzimuth - 180;
  const locked = t >= cue.qiblaLock - START;
  const lockGlow = range(t, cue.qiblaLock - START, cue.qiblaLock - START + 0.25) * (1 - range(t, cue.qiblaLock - START + 0.25, cue.qiblaLock - START + 1.2));
  const capricorn = radiusFor(-OBLIQUITY);
  const cancer = radiusFor(OBLIQUITY);
  const eclipticCenter = (capricorn - cancer) / 2;
  const eclipticRadius = (capricorn + cancer) / 2;
  const limbInner = capricorn + 18;
  const limbOuter = capricorn + 52;
  const compass = limbOuter + 34;
  const view = lookBasis(STAR.Polaris, [1, 0, 0], lst * 0.2);
  const groupScale = 1 - collapse * 0.55;
  const groupOpacity = 1 - collapse;

  const almucantars = [0, 10, 20, 30, 40, 50, 60, 70, 80].map((h) => ({ h, ...almucantar(h) }));

  return (
    <AbsoluteFill style={{ background: color.ink }}>
      <AbsoluteFill style={{ opacity: 0.55 * (1 - collapse * 0.7) }}>
        <StarField view={view} fov={120} time={t + START} milkyGain={0.7} starGain={0.75} limit={5.8} />
      </AbsoluteFill>
      <AbsoluteFill style={{ background: "radial-gradient(circle at 59% 50%, rgba(5,7,11,0) 0%, rgba(5,7,11,0.55) 55%, rgba(5,7,11,0.85) 100%)" }} />

      <svg width={width} height={height} style={{ position: "absolute", left: 0, top: 0, opacity: groupOpacity }}>
        <g transform={`translate(${cx} ${cy}) scale(${groupScale}) translate(${-cx} ${-cy})`}>
          <Circle cx={cx} cy={cy} r={limbOuter} progress={limb} width={1.6} opacity={0.85} />
          <Circle cx={cx} cy={cy} r={limbInner} progress={limb} width={1.1} opacity={0.6} />
          <Circle cx={cx} cy={cy} r={compass} progress={limb} width={0.9} opacity={0.35} />
          {Array.from({ length: 360 }).map((_, d) => {
            if (d / 360 > ticks) return null;
            const long = d % 15 === 0 ? 20 : d % 5 === 0 ? 12 : 6;
            const a = (d * Math.PI) / 180;
            return (
              <line
                key={d}
                x1={cx + Math.sin(a) * limbInner}
                y1={cy - Math.cos(a) * limbInner}
                x2={cx + Math.sin(a) * (limbInner + long)}
                y2={cy - Math.cos(a) * (limbInner + long)}
                stroke={color.text}
                strokeWidth={d % 15 === 0 ? 1.4 : 0.9}
                opacity={d % 5 === 0 ? 0.8 : 0.45}
              />
            );
          })}
          {Array.from({ length: 12 }).map((_, i) => {
            const d = i * 30;
            const a = (d * Math.PI) / 180;
            const r = limbOuter - 12;
            return (
              <text key={d} x={cx + Math.sin(a) * r} y={cy - Math.cos(a) * r + 6} fill={color.text} opacity={0.7 * ticks} fontFamily="Funnel Sans" fontSize={15} textAnchor="middle">
                {d}
              </text>
            );
          })}
          {[
            { label: "G", az: 180 },
            { label: "B", az: 270 },
            { label: "K", az: 0 },
            { label: "D", az: 90 },
          ].map(({ label, az }) => {
            const a = ((az - 180) * Math.PI) / 180;
            const r = compass + 22;
            return (
              <text key={label} x={cx + Math.sin(a) * r} y={cy - Math.cos(a) * r + 9} fill={color.text} opacity={0.9 * limb} fontFamily="Funnel Display" fontSize={26} fontWeight={500} textAnchor="middle">
                {label}
              </text>
            );
          })}

          <Circle cx={cx} cy={cy} r={capricorn} progress={plate} width={1.2} opacity={0.75} />
          <Circle cx={cx} cy={cy} r={EQUATOR_RADIUS} progress={plate} width={1} opacity={0.55} />
          <Circle cx={cx} cy={cy} r={cancer} progress={plate} width={1} opacity={0.55} />
          <clipPath id="astrolabe-plate">
            <circle cx={cx} cy={cy} r={capricorn} />
          </clipPath>
          <g clipPath="url(#astrolabe-plate)">
            {almucantars.map(({ h, cy: offset, r }, i) => (
              <Circle key={h} cx={cx} cy={cy + offset} r={r} progress={range(t, 0.45 + i * 0.1, 1.25 + i * 0.1, ease.inOut)} width={h === 0 ? 1.8 : 0.9} opacity={h === 0 ? 0.95 : 0.42} />
            ))}
          </g>
          <line x1={cx} y1={cy - capricorn} x2={cx} y2={cy + capricorn} stroke={color.text} strokeWidth={0.9} opacity={0.5 * plate} />
          <line x1={cx - capricorn} y1={cy} x2={cx + capricorn} y2={cy} stroke={color.text} strokeWidth={0.9} opacity={0.35 * plate} />

          <g transform={`rotate(${lst} ${cx} ${cy})`} opacity={rete}>
            <circle cx={cx + eclipticCenter} cy={cy} r={eclipticRadius} fill="none" stroke={color.text} strokeWidth={2.2} opacity={0.9} />
            <circle cx={cx + eclipticCenter} cy={cy} r={eclipticRadius - 12} fill="none" stroke={color.text} strokeWidth={0.8} opacity={0.5} />
            <circle cx={cx} cy={cy} r={capricorn - 6} fill="none" stroke={color.text} strokeWidth={2} opacity={0.75} />
            {RETE_STARS.map((star) => {
              const alpha = (star.ra * 15 * Math.PI) / 180;
              const r = radiusFor(star.dec);
              const x = cx - Math.sin(alpha) * r;
              const y = cy - Math.cos(alpha) * r;
              const baseR = Math.min(capricorn - 6, r + 26);
              const bx = cx - Math.sin(alpha) * baseR;
              const by = cy - Math.cos(alpha) * baseR;
              const px = -Math.cos(alpha) * 7;
              const py = Math.sin(alpha) * 7;
              return (
                <g key={star.name}>
                  <path d={`M${bx + px} ${by + py} L${x} ${y} L${bx - px} ${by - py} Z`} fill={color.text} opacity={0.85} />
                  <circle cx={x} cy={y} r={3.2} fill="#fff" />
                  <text x={x} y={y - 10} fill={color.text} opacity={0.7} fontFamily="Funnel Sans" fontSize={13} textAnchor="middle" transform={`rotate(${-lst} ${x} ${y})`}>
                    {star.name}
                  </text>
                </g>
              );
            })}
          </g>

          <g opacity={range(t, qiblaStart - 0.2, qiblaStart + 0.3)}>
            <line
              x1={cx}
              y1={cy}
              x2={cx + Math.sin((needleAngle * Math.PI) / 180) * (compass + 6)}
              y2={cy - Math.cos((needleAngle * Math.PI) / 180) * (compass + 6)}
              stroke={locked ? "#ffffff" : color.text}
              strokeWidth={locked ? 3 : 2}
              style={{ filter: `drop-shadow(0 0 ${8 + lockGlow * 22}px rgba(255,236,200,${0.5 + lockGlow * 0.5}))` }}
            />
            <circle cx={cx} cy={cy} r={7} fill={color.text} />
            <rect
              x={cx + Math.sin((needleAngle * Math.PI) / 180) * (compass + 30) - 13}
              y={cy - Math.cos((needleAngle * Math.PI) / 180) * (compass + 30) - 13}
              width={26}
              height={26}
              fill={locked ? "#fff" : "none"}
              stroke="#fff"
              strokeWidth={1.6}
            />
            {locked ? (
              <circle
                cx={cx + Math.sin((needleAngle * Math.PI) / 180) * (compass + 30)}
                cy={cy - Math.cos((needleAngle * Math.PI) / 180) * (compass + 30)}
                r={20 + lockGlow * 40}
                fill="none"
                stroke="#fff"
                strokeWidth={1.5}
                opacity={lockGlow}
              />
            ) : null}
          </g>
        </g>
      </svg>

      <AbsoluteFill style={{ padding: "0 0 0 130px", justifyContent: "center", opacity: 1 - collapse }}>
        <div style={{ fontFamily: font.sans, fontSize: 24, color: color.faint, marginBottom: 14 }}>
          <SplitText text="Stereografik izdüşüm · İstanbul, 41° K" start={0.5} stagger={0.012} duration={0.5} rise={0.3} blur={6} />
        </div>
        <div style={{ fontFamily: font.display, fontSize: 84, lineHeight: 1.02, letterSpacing: "-0.03em", color: color.text, width: 560 }}>
          <SplitText text="Gökyüzünün hesap makinesi." start={0.7} stagger={0.028} />
        </div>
        <FadeText start={cue.qiblaLock - START} style={{ marginTop: 44 }}>
          <div style={{ fontFamily: font.sans, fontSize: 24, color: color.muted }}>Kıble</div>
          <div style={{ fontFamily: font.display, fontSize: 72, color: color.text, fontVariantNumeric: "tabular-nums", letterSpacing: "-0.02em" }}>151,6°</div>
          <div style={{ fontFamily: font.sans, fontSize: 24, color: color.muted }}>İstanbul'dan Kâbe'ye 2.405 km</div>
        </FadeText>
      </AbsoluteFill>

      {logo > 0 ? (
        <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", opacity: logo }}>
          <RasadMark size={250 + (1 - logo) * 120} progress={logo} />
          <div style={{ fontFamily: font.display, fontSize: 128, letterSpacing: "-0.04em", color: color.text, marginTop: 28 }}>
            <SplitText text="Rasad" start={cue.logo - START + 0.15} stagger={0.05} />
          </div>
          <div style={{ fontFamily: font.sans, fontSize: 32, color: color.muted, marginTop: 6 }}>
            <SplitText text="Cebindeki rasathane" start={cue.logo - START + 0.5} stagger={0.02} duration={0.5} rise={0.3} blur={6} />
          </div>
        </AbsoluteFill>
      ) : null}
      <Flash at={0} duration={0.4} intensity={0.35} />
      <Flash at={cue.qiblaLock - START} duration={0.45} intensity={0.3} />
    </AbsoluteFill>
  );
};

export const RasadMark: React.FC<{ size: number; progress: number; glow?: number }> = ({ size, progress, glow = 1 }) => {
  const circumference = 2 * Math.PI * 24;
  return (
    <svg width={size} height={size} viewBox="30 30 48 48" style={{ overflow: "visible", filter: `drop-shadow(0 0 ${14 * glow}px rgba(255,236,200,0.45))` }}>
      <circle cx={54} cy={54} r={24} fill="none" stroke="#f4f2ec" strokeWidth={1.4} strokeDasharray={`${circumference} ${circumference}`} strokeDashoffset={circumference * (1 - Math.min(1, progress * 1.3))} transform="rotate(-90 54 54)" />
      <g stroke="#f4f2ec" strokeWidth={1.4} strokeLinecap="round" opacity={Math.min(1, progress * 2)}>
        <path d="M54,25.5L54,30M78.5,54L83,54M54,78L54,82.5M25,54L29.5,54" />
      </g>
      <path d="M50.02,41A13,13 0,1 0,60.87 61.14A11.5,11.5 0,0 1,50.02 41Z" fill="#f4f2ec" opacity={Math.min(1, Math.max(0, progress * 1.6 - 0.4))} />
    </svg>
  );
};
