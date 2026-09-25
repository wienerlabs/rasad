import React, { useMemo } from "react";
import { AbsoluteFill, useVideoConfig } from "remotion";
import { FadeText, Flash, mixNumber, range, SplitText, useSeconds } from "../components/ui";
import { StarField, TrailsLayer } from "../gl/layers";
import { DEG, horizonCamera, horizonDirection, project, skyCamera, stereographicScale, sunHorizon, type Vec3 } from "../lib/sky";
import { STAR } from "../lib/stars";
import { color, ease, font, scene } from "../theme";

const START = scene("time").start;
const NIGHT_START = Date.parse("2026-10-12T17:00:00Z");
const NIGHT_HOURS = 10;
const SIDEREAL_DEGREES_PER_HOUR = 15.041;
const CAMERA_AZIMUTH = 0;
const CAMERA_ALTITUDE = 28;
const FOV = 112;
const ISTANBUL_OFFSET_HOURS = 3;
const SILHOUETTE = "#020306";
const WINDOW = "#ffcf8a";
const NORTH_CELESTIAL_POLE: Vec3 = [0, 0, 1];

const seeded = (seed: number) => {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let x = state;
    x = Math.imul(x ^ (x >>> 15), x | 1);
    x ^= x + Math.imul(x ^ (x >>> 7), x | 61);
    return ((x ^ (x >>> 14)) >>> 0) / 4294967296;
  };
};

type House = { x: number; width: number; height: number; pitched: boolean; windows: { dx: number; dy: number; off: number }[] };

const buildHouses = (width: number): House[] => {
  const random = seeded(1453);
  const houses: House[] = [];
  let x = -30;
  while (x < width + 30) {
    const w = 26 + random() * 52;
    const tall = random() < 0.18;
    const h = tall ? 46 + random() * 34 : 12 + random() * 30;
    const windows: House["windows"] = [];
    const count = Math.floor(random() * (tall ? 6 : 3));
    for (let i = 0; i < count; i++) windows.push({ dx: 5 + random() * (w - 12), dy: 6 + random() * (h - 12), off: 0.08 + random() * 1.4 });
    houses.push({ x, width: w, height: h, pitched: !tall && random() < 0.45, windows });
    x += w * (0.72 + random() * 0.3);
  }
  return houses;
};

const Minaret: React.FC<{ x: number; base: number; height: number; width?: number }> = ({ x, base, height, width = 9 }) => {
  const top = base - height;
  return (
    <g>
      <rect x={x - width / 2} y={top} width={width} height={height + 200} />
      <rect x={x - width * 0.85} y={base - height * 0.7} width={width * 1.7} height={4} />
      <rect x={x - width * 0.85} y={base - height * 0.86} width={width * 1.7} height={4} />
      <polygon points={`${x - width / 2 - 1},${top} ${x + width / 2 + 1},${top} ${x},${top - width * 4.4}`} />
      <rect x={x - 0.6} y={top - width * 4.4 - 9} width={1.2} height={9} />
    </g>
  );
};

const crescent = (cx: number, cy: number) =>
  `M${cx - 4.4},${cy - 2.375} A5,5 0 1 0 ${cx + 4.4},${cy - 2.375} A4.4,4.4 0 0 1 ${cx - 4.4},${cy - 2.375} Z`;

const Dome: React.FC<{ x: number; base: number; radius: number; drum?: number; finial?: boolean }> = ({ x, base, radius, drum = 0, finial = false }) => {
  const spring = base - drum;
  return (
    <g>
      {drum > 0 ? <rect x={x - radius * 0.94} y={spring} width={radius * 1.88} height={drum + 200} /> : null}
      <path d={`M${x - radius},${spring} A${radius},${radius} 0 0 1 ${x + radius},${spring} Z`} />
      {finial ? (
        <>
          <rect x={x - 0.8} y={spring - radius - 16} width={1.6} height={17} />
          <path d={crescent(x, spring - radius - 20)} />
        </>
      ) : null}
    </g>
  );
};

const pad = (n: number) => String(n).padStart(2, "0");

export const TimeMachine: React.FC = () => {
  const t = useSeconds();
  const { width, height } = useVideoConfig();
  const pxScale = stereographicScale(width, FOV);
  const lapse = range(t, 0.2, 3.5, ease.inOut);
  const hours = lapse * NIGHT_HOURS;
  const millis = NIGHT_START + hours * 3600000;
  const view = skyCamera(millis, CAMERA_AZIMUTH, CAMERA_ALTITUDE);
  const span = hours * SIDEREAL_DEGREES_PER_HOUR * DEG;
  const pole = project(view, NORTH_CELESTIAL_POLE, width, height, pxScale);
  const polaris = project(view, STAR.Polaris, width, height, pxScale);

  const ground = useMemo(() => {
    const camera = horizonCamera(CAMERA_AZIMUTH, CAMERA_ALTITUDE);
    const points: [number, number][] = [];
    for (let az = -84; az <= 84; az += 2) {
      const [x, y] = project(camera, horizonDirection(az, 0), width, height, pxScale);
      points.push([x, y]);
    }
    return points;
  }, [width, height, pxScale]);

  const horizonAt = (x: number) => {
    for (let i = 1; i < ground.length; i++) {
      const [x0, y0] = ground[i - 1];
      const [x1, y1] = ground[i];
      if (x >= x0 && x <= x1) return y0 + ((y1 - y0) * (x - x0)) / (x1 - x0);
    }
    return x < ground[0][0] ? ground[0][1] : ground[ground.length - 1][1];
  };

  const houses = useMemo(() => buildHouses(width), [width]);

  const sun = sunHorizon(millis);
  const sunAltitude = Math.asin(sun[2]) / DEG;
  const sunScreenX = width / 2 + Math.sin(Math.atan2(sun[0], sun[1])) * width * 0.62;
  const twilight = Math.max(0, Math.min(1, (sunAltitude + 19) / 9));
  const cityGlow = mixNumber(0.26, 0.15, lapse);

  const local = new Date(millis + ISTANBUL_OFFSET_HOURS * 3600000);
  const clock = `${pad(local.getUTCHours())}:${pad(local.getUTCMinutes())}`;
  const day = `${local.getUTCDate()} Ekim`;

  const exit = range(t, 3.45, 4.0, ease.in);
  const zoom = 1 + exit * exit * 2.4;
  const fadeIn = range(t, 0, 0.35);

  const mosqueX = width * 0.37;
  const mosqueBase = horizonAt(mosqueX) + 4;
  const smallMosqueX = width * 0.565;
  const smallMosqueBase = horizonAt(smallMosqueX) + 4;
  const galataX = width * 0.7;
  const galataBase = horizonAt(galataX) + 4;
  const towerX = width * 0.86;
  const towerBase = horizonAt(towerX) + 4;
  const bridgeLeft = width * 0.04;
  const bridgeRight = width * 0.22;
  const bridgeBase = horizonAt((bridgeLeft + bridgeRight) / 2) - 6;
  const bridgeTowerHeight = 92;
  const deckY = bridgeBase - 26;
  const cableSag = 58;
  const beacon = 0.55 + 0.45 * Math.max(0, Math.sin(t * 9.0));

  const scrubLeft = width / 2 - 330;
  const scrubWidth = 660;
  const scrubY = height - 36;

  return (
    <AbsoluteFill style={{ background: color.ink, opacity: fadeIn * (1 - exit), filter: exit > 0 ? `blur(${exit * 7}px)` : undefined }}>
      <AbsoluteFill style={{ transform: `scale(${zoom})`, transformOrigin: `${pole[0]}px ${pole[1]}px` }}>
        <StarField view={view} fov={FOV} time={t + START} milkyGain={mixNumber(0.6, 0.22, lapse)} starGain={mixNumber(1.05, 0.6, lapse)} limit={6.2} />
        <TrailsLayer view={view} fov={FOV} span={span} gain={1.05} />
        <AbsoluteFill style={{ background: `radial-gradient(ellipse 70% 42% at 50% 100%, rgba(255,146,70,${cityGlow}) 0%, rgba(255,120,60,${cityGlow * 0.35}) 45%, rgba(0,0,0,0) 100%)` }} />
        <AbsoluteFill
          style={{
            background: `radial-gradient(ellipse 55% 50% at ${sunScreenX}px 100%, rgba(120,150,210,${0.5 * twilight}) 0%, rgba(90,110,170,${0.22 * twilight}) 45%, rgba(0,0,0,0) 100%)`,
            mixBlendMode: "screen",
          }}
        />
        <AbsoluteFill style={{ background: "linear-gradient(180deg, rgba(5,7,11,0.62) 0%, rgba(5,7,11,0) 26%)" }} />

        <svg width={width} height={height} style={{ position: "absolute", left: 0, top: 0 }}>
          <g fill={SILHOUETTE}>
            <polygon points={`${ground.map(([x, y]) => `${x},${y + 3}`).join(" ")} ${width + 400},${height + 10} ${-400},${height + 10}`} />
            {houses.map((house, i) => {
              const base = horizonAt(house.x + house.width / 2) + 5;
              const top = base - house.height;
              return (
                <g key={i}>
                  <rect x={house.x} y={top} width={house.width} height={house.height + 240} />
                  {house.pitched ? <polygon points={`${house.x - 2},${top} ${house.x + house.width + 2},${top} ${house.x + house.width / 2},${top - house.height * 0.42}`} /> : null}
                </g>
              );
            })}
          </g>
          <g fill={WINDOW}>
            {houses.flatMap((house, i) => {
              const base = horizonAt(house.x + house.width / 2) + 5;
              return house.windows
                .filter((w) => lapse < w.off)
                .map((w, j) => <rect key={`${i}-${j}`} x={house.x + w.dx} y={base - house.height + w.dy} width={3} height={4} opacity={0.75} />);
            })}
          </g>
          <g fill={SILHOUETTE}>
            <rect x={mosqueX - 170} y={mosqueBase - 44} width={340} height={260} />
            <Dome x={mosqueX - 128} base={mosqueBase - 44} radius={17} />
            <Dome x={mosqueX + 128} base={mosqueBase - 44} radius={17} />
            <Dome x={mosqueX - 74} base={mosqueBase - 44} radius={40} drum={6} />
            <Dome x={mosqueX + 74} base={mosqueBase - 44} radius={40} drum={6} />
            <Dome x={mosqueX} base={mosqueBase - 44} radius={64} drum={16} finial />
            <Minaret x={mosqueX - 196} base={mosqueBase} height={190} />
            <Minaret x={mosqueX - 158} base={mosqueBase} height={158} />
            <Minaret x={mosqueX + 158} base={mosqueBase} height={158} />
            <Minaret x={mosqueX + 196} base={mosqueBase} height={190} />

            <rect x={smallMosqueX - 58} y={smallMosqueBase - 30} width={116} height={240} />
            <Dome x={smallMosqueX} base={smallMosqueBase - 30} radius={34} drum={8} finial />
            <Dome x={smallMosqueX - 44} base={smallMosqueBase - 30} radius={13} />
            <Dome x={smallMosqueX + 44} base={smallMosqueBase - 30} radius={13} />
            <Minaret x={smallMosqueX - 74} base={smallMosqueBase} height={128} width={8} />
            <Minaret x={smallMosqueX + 74} base={smallMosqueBase} height={128} width={8} />

            <rect x={galataX - 24} y={galataBase - 118} width={48} height={330} />
            <rect x={galataX - 30} y={galataBase - 124} width={60} height={9} />
            <polygon points={`${galataX - 27},${galataBase - 123} ${galataX + 27},${galataBase - 123} ${galataX},${galataBase - 182}`} />
            <rect x={galataX - 0.8} y={galataBase - 196} width={1.6} height={15} />

            <polygon points={`${towerX - 9},${towerBase + 40} ${towerX + 9},${towerBase + 40} ${towerX + 4},${towerBase - 250} ${towerX - 4},${towerBase - 250}`} />
            <rect x={towerX - 17} y={towerBase - 206} width={34} height={30} rx={6} />
            <rect x={towerX - 13} y={towerBase - 232} width={26} height={14} rx={4} />
            <rect x={towerX - 1.3} y={towerBase - 318} width={2.6} height={70} />

            <rect x={bridgeLeft - 40} y={deckY} width={bridgeRight - bridgeLeft + 80} height={5} />
            {[bridgeLeft + 26, bridgeRight - 26].map((x) => (
              <g key={x}>
                <rect x={x - 5} y={bridgeBase - bridgeTowerHeight} width={4} height={bridgeTowerHeight + 60} />
                <rect x={x + 1} y={bridgeBase - bridgeTowerHeight} width={4} height={bridgeTowerHeight + 60} />
                <rect x={x - 5} y={bridgeBase - bridgeTowerHeight + 8} width={10} height={3} />
              </g>
            ))}
          </g>
          <g fill="none" stroke={SILHOUETTE} strokeWidth={2.2}>
            <path d={`M${bridgeLeft - 40},${deckY - 2} Q${bridgeLeft - 8},${bridgeBase - bridgeTowerHeight + 20} ${bridgeLeft + 26},${bridgeBase - bridgeTowerHeight}`} />
            <path d={`M${bridgeLeft + 26},${bridgeBase - bridgeTowerHeight} Q${(bridgeLeft + bridgeRight) / 2},${bridgeBase - bridgeTowerHeight + cableSag * 2} ${bridgeRight - 26},${bridgeBase - bridgeTowerHeight}`} />
            <path d={`M${bridgeRight - 26},${bridgeBase - bridgeTowerHeight} Q${bridgeRight + 8},${bridgeBase - bridgeTowerHeight + 20} ${bridgeRight + 40},${deckY - 2}`} />
          </g>
          <g stroke={SILHOUETTE} strokeWidth={1}>
            {Array.from({ length: 15 }).map((_, i) => {
              const u = (i + 1) / 16;
              const x = bridgeLeft + 26 + u * (bridgeRight - bridgeLeft - 52);
              const cableY = bridgeBase - bridgeTowerHeight + cableSag * 2 * 2 * u * (1 - u);
              return <line key={i} x1={x} x2={x} y1={cableY} y2={deckY} />;
            })}
          </g>
          <g fill={WINDOW}>
            {Array.from({ length: 13 }).map((_, i) => {
              const x = bridgeLeft - 30 + (i / 12) * (bridgeRight - bridgeLeft + 60);
              return <circle key={i} cx={x} cy={deckY + 1} r={1.6} opacity={0.85} />;
            })}
          </g>
          <circle cx={towerX} cy={towerBase - 320} r={2.6} fill="#ff4a3a" opacity={beacon} />
          <circle cx={towerX} cy={towerBase - 320} r={9} fill="#ff4a3a" opacity={beacon * 0.18} />

          <g opacity={range(t, 0.55, 1.0)}>
            <circle cx={polaris[0]} cy={polaris[1]} r={15} fill="none" stroke={color.text} strokeWidth={1.2} opacity={0.8} />
            <line x1={polaris[0] + 15} y1={polaris[1]} x2={polaris[0] + 58} y2={polaris[1]} stroke={color.text} strokeWidth={1} opacity={0.5} />
          </g>

          <g opacity={range(t, 0.3, 0.8)}>
            <line x1={scrubLeft} x2={scrubLeft + scrubWidth} y1={scrubY} y2={scrubY} stroke={color.hairline} strokeWidth={2} />
            <line x1={scrubLeft} x2={scrubLeft + scrubWidth * lapse} y1={scrubY} y2={scrubY} stroke={color.text} strokeWidth={2} />
            {Array.from({ length: NIGHT_HOURS + 1 }).map((_, i) => {
              const x = scrubLeft + (i / NIGHT_HOURS) * scrubWidth;
              const labelled = i % 2 === 0;
              return (
                <g key={i}>
                  <line x1={x} x2={x} y1={scrubY - (labelled ? 9 : 5)} y2={scrubY} stroke={color.text} strokeWidth={1} opacity={0.55} />
                  {labelled ? (
                    <text x={x} y={scrubY - 16} fill={color.muted} fontFamily="Funnel Sans" fontSize={17} textAnchor="middle">
                      {pad((20 + i) % 24)}
                    </text>
                  ) : null}
                </g>
              );
            })}
            <circle cx={scrubLeft + scrubWidth * lapse} cy={scrubY} r={7} fill={color.text} />
            <circle cx={scrubLeft + scrubWidth * lapse} cy={scrubY} r={14} fill="none" stroke={color.text} strokeWidth={1} opacity={0.35} />
          </g>
        </svg>

        <div style={{ position: "absolute", left: polaris[0] + 66, top: polaris[1] - 22, opacity: range(t, 0.6, 1.05), whiteSpace: "nowrap", textShadow: "0 0 18px rgba(0,0,0,0.8)" }}>
          <div style={{ fontFamily: font.sans, fontSize: 24, color: color.text }}>Kutup Yıldızı</div>
          <div style={{ fontFamily: font.sans, fontSize: 19, color: color.muted, display: "flex", alignItems: "baseline", gap: 10 }}>
            <span>Demirkazık</span>
            <span style={{ fontFamily: font.arabic, fontSize: 24 }}>الجدي</span>
          </div>
        </div>
      </AbsoluteFill>

      <AbsoluteFill style={{ padding: "64px 120px", flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", opacity: 1 - exit }}>
        <div>
          <div style={{ fontFamily: font.display, fontSize: 76, letterSpacing: "-0.03em", color: color.text, lineHeight: 1 }}>
            <SplitText text="Zaman makinesi" start={0.1} stagger={0.03} />
          </div>
          <FadeText start={0.45} style={{ fontFamily: font.sans, fontSize: 28, color: color.muted, marginTop: 14 }}>
            10 saatlik gece, 4 saniyede
          </FadeText>
        </div>
        <FadeText start={0.25} style={{ textAlign: "right" }}>
          <div style={{ fontFamily: font.display, fontSize: 118, letterSpacing: "-0.03em", color: color.text, lineHeight: 0.95, fontVariantNumeric: "tabular-nums" }}>{clock}</div>
          <div style={{ fontFamily: font.sans, fontSize: 26, color: color.muted, marginTop: 10 }}>{day} · İstanbul</div>
        </FadeText>
      </AbsoluteFill>

      <Flash at={0} duration={0.35} intensity={0.3} />
    </AbsoluteFill>
  );
};
