import { useEffect, useRef, useState } from "react";
import { cancelRender, continueRender, delayRender, staticFile } from "remotion";
import { bvToRgb, fromRaDec, type Vec3 } from "./sky";

const cache = new Map<string, unknown>();
const pending = new Map<string, Promise<unknown>>();

const load = <T,>(key: string, loader: () => Promise<T>): Promise<T> => {
  if (cache.has(key)) return Promise.resolve(cache.get(key) as T);
  const existing = pending.get(key);
  if (existing) return existing as Promise<T>;
  const promise = loader().then((value) => {
    cache.set(key, value);
    pending.delete(key);
    return value;
  });
  pending.set(key, promise);
  return promise;
};

export const useAsset = <T,>(key: string, loader: () => Promise<T>): T | null => {
  const [, setVersion] = useState(0);
  const handles = useRef(new Map<string, number>());
  const latestLoader = useRef(loader);
  latestLoader.current = loader;
  if (!cache.has(key) && !handles.current.has(key)) {
    handles.current.set(key, delayRender(`asset ${key}`, { timeoutInMilliseconds: 120000 }));
  }
  useEffect(() => {
    if (cache.has(key)) return;
    let active = true;
    load(key, latestLoader.current)
      .then(() => {
        if (active) setVersion((version) => version + 1);
      })
      .catch((error) => cancelRender(error));
    return () => {
      active = false;
    };
  }, [key]);
  const value = cache.has(key) ? (cache.get(key) as T) : null;
  useEffect(() => {
    if (value === null) return;
    const handle = handles.current.get(key);
    if (handle === undefined) return;
    handles.current.delete(key);
    continueRender(handle);
  }, [key, value]);
  useEffect(() => {
    const pendingHandles = handles.current;
    return () => {
      pendingHandles.forEach((handle) => continueRender(handle));
      pendingHandles.clear();
    };
  }, []);
  return value;
};

export type StarCatalog = {
  count: number;
  positions: Float32Array;
  magnitudes: Float32Array;
  colors: Float32Array;
};

export const loadStars = async (): Promise<StarCatalog> => {
  const buffer = await fetch(staticFile("data/stars.bin")).then((r) => r.arrayBuffer());
  const view = new DataView(buffer);
  const count = view.getInt32(0, true);
  const positions = new Float32Array(count * 3);
  const magnitudes = new Float32Array(count);
  const colors = new Float32Array(count * 3);
  for (let i = 0; i < count; i++) {
    const base = 4 + i * 24;
    positions[i * 3] = view.getFloat32(base, true);
    positions[i * 3 + 1] = view.getFloat32(base + 4, true);
    positions[i * 3 + 2] = view.getFloat32(base + 8, true);
    magnitudes[i] = view.getFloat32(base + 12, true);
    const rgb = bvToRgb(view.getFloat32(base + 16, true));
    colors[i * 3] = rgb[0];
    colors[i * 3 + 1] = rgb[1];
    colors[i * 3 + 2] = rgb[2];
  }
  return { count, positions, magnitudes, colors };
};

export type Figure = { code: string; label: Vec3; segments: Float32Array };

export const loadFigures = async (): Promise<Figure[]> => {
  const buffer = await fetch(staticFile("data/constellations.bin")).then((r) => r.arrayBuffer());
  const view = new DataView(buffer);
  const count = view.getInt32(0, true);
  let offset = 4;
  const figures: Figure[] = [];
  for (let i = 0; i < count; i++) {
    const code = String.fromCharCode(view.getUint8(offset), view.getUint8(offset + 1), view.getUint8(offset + 2), view.getUint8(offset + 3)).trim();
    offset += 4;
    const ra = view.getFloat32(offset, true);
    const dec = view.getFloat32(offset + 4, true);
    const segmentCount = view.getInt32(offset + 12, true);
    offset += 16;
    const segments = new Float32Array(segmentCount * 6);
    for (let s = 0; s < segmentCount; s++) {
      const a = fromRaDec(view.getFloat32(offset, true) / 15, view.getFloat32(offset + 4, true));
      const b = fromRaDec(view.getFloat32(offset + 8, true) / 15, view.getFloat32(offset + 12, true));
      segments.set([...a, ...b], s * 6);
      offset += 16;
    }
    figures.push({ code, label: fromRaDec(ra / 15, dec), segments });
  }
  return figures;
};

export type NamedStar = { v: Vec3; mag: number; bv: number };

export const loadNamed = async (): Promise<Record<string, NamedStar>> =>
  fetch(staticFile("data/named.json")).then((r) => r.json());

export type Land = { width: number; height: number; paths: string[] };

export const loadLand = async (): Promise<Land> => fetch(staticFile("data/land.json")).then((r) => r.json());

export type QGridManifest = {
  low: number;
  high: number;
  latTop: number;
  latBottom: number;
  grids: { date: string; conjunction: string; file: string; nakedEyeShare: number }[];
};

export const loadQManifest = async (): Promise<QGridManifest> => fetch(staticFile("data/qgrids.json")).then((r) => r.json());

export const loadImage = (path: string): Promise<HTMLImageElement> =>
  new Promise((resolve, reject) => {
    const image = new Image();
    image.crossOrigin = "anonymous";
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error(`image ${path}`));
    image.src = staticFile(path);
  });

export const loadImages = (paths: string[]) => Promise.all(paths.map(loadImage));
