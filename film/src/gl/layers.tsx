import React, { useCallback } from "react";
import { useVideoConfig } from "remotion";
import { loadImage, loadImages, loadStars, useAsset, type StarCatalog } from "../lib/assets";
import { matToGl, stereographicScale, type Mat3, type Vec3 } from "../lib/sky";
import { Gl, GlCanvas, QUAD_VERTEX } from "./GlCanvas";
import {
  FOG_FRAGMENT,
  MILKY_FRAGMENT,
  MOON_FRAGMENT,
  QMAP_FRAGMENT,
  SKY_FRAGMENT,
  STAR_FRAGMENT,
  STAR_VERTEX,
  TRAILS_FRAGMENT,
  TUNNEL_FRAGMENT,
} from "./shaders";

const useStars = () => useAsset("stars", loadStars);
const useMilky = () => useAsset("milky", () => loadImage("data/milkyway.png"));
const useMoonTexture = () => useAsset("moon", () => loadImage("data/moon.jpg"));

export type SkyView = {
  view: Mat3;
  fov: number;
  time: number;
  milkyGain?: number;
  starGain?: number;
  limit?: number;
  size?: number;
  revealRadius?: number;
  fade?: number;
};

const drawStars = (g: Gl, stars: StarCatalog, width: number, height: number, sky: SkyView, pxScale: number) => {
  const gl = g.gl;
  const program = g.program("stars", STAR_VERTEX, STAR_FRAGMENT);
  g.use(program, {
    uView: matToGl(sky.view),
    uResolution: [width, height],
    uScale: pxScale,
    uLimit: sky.limit ?? 6.5,
    uSize: sky.size ?? 1,
    uTime: sky.time,
    uRevealRadius: sky.revealRadius ?? 1e6,
    uGain: (sky.starGain ?? 1) * (sky.fade ?? 1),
  });
  g.attribute(program, "aPos", g.buffer("starPos", stars.positions), 3);
  g.attribute(program, "aMag", g.buffer("starMag", stars.magnitudes), 1);
  g.attribute(program, "aColor", g.buffer("starColor", stars.colors), 3);
  g.blend("additive");
  gl.drawArrays(gl.POINTS, 0, stars.count);
};

export const StarField: React.FC<SkyView & { style?: React.CSSProperties }> = ({ style, ...sky }) => {
  const { width, height } = useVideoConfig();
  const stars = useStars();
  const milky = useMilky();
  const draw = useCallback(
    (g: Gl) => {
      g.clear(0, 0, 0, 1);
      if (!stars || !milky) return;
      const pxScale = stereographicScale(width, sky.fov);
      const program = g.program("milky", QUAD_VERTEX, MILKY_FRAGMENT);
      g.blend("none");
      g.use(
        program,
        {
          uResolution: [width, height],
          uScale: pxScale,
          uView: matToGl(sky.view),
          uMilkyGain: sky.milkyGain ?? 1,
          uFade: sky.fade ?? 1,
          uTime: sky.time,
        },
        { uMilky: g.texture("milky", milky, { repeatX: true, mipmap: true }) },
      );
      g.drawQuad(program);
      drawStars(g, stars, width, height, sky, pxScale);
    },
    [stars, milky, width, height, sky],
  );
  return <GlCanvas width={width} height={height} draw={draw} style={style} />;
};

export type MoonParams = {
  center: [number, number];
  radius: number;
  sun: Vec3;
  north?: number;
  spin?: number;
  relief?: number;
  earthshine?: number;
  gain?: number;
  halo?: number;
};

export const MoonLayer: React.FC<MoonParams & { style?: React.CSSProperties }> = ({ style, ...moon }) => {
  const { width, height } = useVideoConfig();
  const texture = useMoonTexture();
  const draw = useCallback(
    (g: Gl) => {
      g.clear(0, 0, 0, 0);
      if (!texture) return;
      const program = g.program("moon", QUAD_VERTEX, MOON_FRAGMENT);
      g.blend("none");
      g.use(
        program,
        {
          uResolution: [width, height],
          uCenter: [moon.center[0], height - moon.center[1]],
          uRadius: moon.radius,
          uSun: moon.sun,
          uNorth: moon.north ?? 0,
          uSpin: moon.spin ?? 0,
          uRelief: moon.relief ?? 2.2,
          uEarthshine: moon.earthshine ?? 0.04,
          uGain: moon.gain ?? 1.3,
          uHalo: moon.halo ?? 1,
          uTexSize: [texture.width, texture.height],
        },
        { uMoonTex: g.texture("moonTex", texture, { repeatX: true, mipmap: true }) },
      );
      g.drawQuad(program);
    },
    [texture, width, height, moon],
  );
  return <GlCanvas width={width} height={height} draw={draw} style={style} />;
};

export const TwilightSky: React.FC<{ view: Mat3; fov: number; sun: Vec3; ground?: number; style?: React.CSSProperties }> = ({ view, fov, sun, ground = 0.94, style }) => {
  const { width, height } = useVideoConfig();
  const draw = useCallback(
    (g: Gl) => {
      g.clear(0, 0, 0, 1);
      const program = g.program("sky", QUAD_VERTEX, SKY_FRAGMENT);
      g.blend("none");
      g.use(program, { uResolution: [width, height], uScale: stereographicScale(width, fov), uView: matToGl(view), uSun: sun, uGround: ground });
      g.drawQuad(program);
    },
    [width, height, view, fov, sun, ground],
  );
  return <GlCanvas width={width} height={height} draw={draw} style={style} />;
};

const TRAIL_COLUMNS = 2048;
const TRAIL_ROWS = 440;
const TRAIL_MAX_POLAR = (110 * Math.PI) / 180;

const buildPrefix = (stars: StarCatalog): Float32Array => {
  const data = new Float32Array(TRAIL_COLUMNS * TRAIL_ROWS * 4);
  for (let i = 0; i < stars.count; i++) {
    const magnitude = stars.magnitudes[i];
    if (magnitude > 5.4) break;
    const z = stars.positions[i * 3 + 2];
    const polar = Math.acos(Math.max(-1, Math.min(1, z)));
    if (polar > TRAIL_MAX_POLAR) continue;
    const ra = Math.atan2(stars.positions[i * 3 + 1], stars.positions[i * 3]);
    const column = Math.floor((((ra / (2 * Math.PI)) % 1) + 1) % 1 * TRAIL_COLUMNS) % TRAIL_COLUMNS;
    const row = (polar / TRAIL_MAX_POLAR) * (TRAIL_ROWS - 1);
    const weight = Math.pow(10, -0.3 * (magnitude - 1.2)) * 0.8 + 0.02;
    const sigma = 0.5 + Math.max(0, 2.0 - magnitude) * 0.12;
    for (let k = Math.floor(row - 3 * sigma); k <= Math.ceil(row + 3 * sigma); k++) {
      if (k < 0 || k >= TRAIL_ROWS) continue;
      const g = Math.exp(-((k - row) ** 2) / (2 * sigma * sigma)) * weight;
      const offset = (k * TRAIL_COLUMNS + column) * 4;
      data[offset] += stars.colors[i * 3] * g;
      data[offset + 1] += stars.colors[i * 3 + 1] * g;
      data[offset + 2] += stars.colors[i * 3 + 2] * g;
    }
  }
  for (let k = 0; k < TRAIL_ROWS; k++) {
    let r = 0;
    let gSum = 0;
    let b = 0;
    for (let c = 0; c < TRAIL_COLUMNS; c++) {
      const offset = (k * TRAIL_COLUMNS + c) * 4;
      r += data[offset];
      gSum += data[offset + 1];
      b += data[offset + 2];
      data[offset] = r;
      data[offset + 1] = gSum;
      data[offset + 2] = b;
      data[offset + 3] = 1;
    }
  }
  return data;
};

export const TrailsLayer: React.FC<{ view: Mat3; fov: number; span: number; gain?: number; style?: React.CSSProperties }> = ({ view, fov, span, gain = 1, style }) => {
  const { width, height } = useVideoConfig();
  const stars = useStars();
  const draw = useCallback(
    (g: Gl) => {
      g.clear(0, 0, 0, 0);
      if (!stars) return;
      const program = g.program("trails", QUAD_VERTEX, TRAILS_FRAGMENT);
      g.blend("none");
      g.use(
        program,
        {
          uResolution: [width, height],
          uScale: stereographicScale(width, fov),
          uView: matToGl(view),
          uPrefixSize: [TRAIL_COLUMNS, TRAIL_ROWS],
          uMaxPolar: TRAIL_MAX_POLAR,
          uSpan: span,
          uGain: gain,
        },
        { uPrefix: g.floatTexture("prefix", TRAIL_COLUMNS, TRAIL_ROWS, () => buildPrefix(stars)) },
      );
      g.drawQuad(program);
    },
    [stars, width, height, view, fov, span, gain],
  );
  return <GlCanvas width={width} height={height} draw={draw} style={style} />;
};

export type QMapParams = {
  gridA: string;
  gridB: string;
  mix: number;
  rect: [number, number, number, number];
  utc: number;
  glowUtc: number;
  declination: number;
  reveal: number;
  low: number;
  high: number;
};

export const QMapLayer: React.FC<QMapParams & { style?: React.CSSProperties }> = ({ style, ...map }) => {
  const { width, height } = useVideoConfig();
  const images = useAsset(`q:${map.gridA}|${map.gridB}`, () => loadImages([`data/${map.gridA}`, `data/${map.gridB}`]));
  const draw = useCallback(
    (g: Gl) => {
      g.clear(0, 0, 0, 0);
      if (!images) return;
      const program = g.program("qmap", QUAD_VERTEX, QMAP_FRAGMENT);
      g.blend("none");
      const [x, y, w, h] = map.rect;
      g.use(
        program,
        {
          uRect: [x, height - y - h, w, h],
          uMix: map.mix,
          uLow: map.low,
          uHigh: map.high,
          uLatTop: 60,
          uLatBottom: -60,
          uUtc: map.utc,
          uGlowUtc: map.glowUtc,
          uDecl: map.declination,
          uReveal: map.reveal,
        },
        { uQa: g.texture(map.gridA, images[0]), uQb: g.texture(map.gridB, images[1]) },
      );
      g.drawQuad(program);
    },
    [images, height, map],
  );
  return <GlCanvas width={width} height={height} draw={draw} style={style} />;
};

export const TunnelLayer: React.FC<{ time: number; speed: number; fade?: number; style?: React.CSSProperties }> = ({ time, speed, fade = 1, style }) => {
  const { width, height } = useVideoConfig();
  const draw = useCallback(
    (g: Gl) => {
      g.clear(0, 0, 0, 1);
      const program = g.program("tunnel", QUAD_VERTEX, TUNNEL_FRAGMENT);
      g.blend("none");
      g.use(program, { uResolution: [width, height], uTime: time, uSpeed: speed, uFade: fade });
      g.drawQuad(program);
    },
    [width, height, time, speed, fade],
  );
  return <GlCanvas width={width} height={height} draw={draw} style={style} />;
};

export const FogLayer: React.FC<{ time: number; tintA: Vec3; tintB: Vec3; fade?: number; style?: React.CSSProperties }> = ({ time, tintA, tintB, fade = 1, style }) => {
  const { width, height } = useVideoConfig();
  const draw = useCallback(
    (g: Gl) => {
      g.clear(0, 0, 0, 1);
      const program = g.program("fog", QUAD_VERTEX, FOG_FRAGMENT);
      g.blend("none");
      g.use(program, { uResolution: [width, height], uTime: time, uTintA: tintA, uTintB: tintB, uFade: fade });
      g.drawQuad(program);
    },
    [width, height, time, tintA, tintB, fade],
  );
  return <GlCanvas width={width} height={height} draw={draw} style={style} />;
};
