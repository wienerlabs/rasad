import React from "react";
import { useVideoConfig } from "remotion";
import { loadFigures, useAsset } from "../lib/assets";
import { project, stereographicScale, type Mat3 } from "../lib/sky";

type Props = {
  view: Mat3;
  fov: number;
  codes?: string[];
  progress: number;
  opacity?: number;
  width?: number;
};

export const ConstellationLines: React.FC<Props> = ({ view, fov, codes, progress, opacity = 0.42, width: strokeWidth = 1.3 }) => {
  const { width, height } = useVideoConfig();
  const figures = useAsset("figures", loadFigures);
  if (!figures) return null;
  const pxScale = stereographicScale(width, fov);
  const lines: React.ReactNode[] = [];
  const selected = codes ? figures.filter((f) => codes.includes(f.code)) : figures;
  let index = 0;
  const total = selected.reduce((n, f) => n + f.segments.length / 6, 0);
  for (const figure of selected) {
    const s = figure.segments;
    for (let i = 0; i < s.length; i += 6) {
      const order = index++ / Math.max(total, 1);
      const local = Math.min(1, Math.max(0, (progress - order * 0.6) / 0.4));
      if (local <= 0) continue;
      const a = project(view, [s[i], s[i + 1], s[i + 2]], width, height, pxScale);
      const b = project(view, [s[i + 3], s[i + 4], s[i + 5]], width, height, pxScale);
      if (a[2] < 0.05 || b[2] < 0.05) continue;
      const x2 = a[0] + (b[0] - a[0]) * local;
      const y2 = a[1] + (b[1] - a[1]) * local;
      lines.push(<line key={`${figure.code}-${i}`} x1={a[0]} y1={a[1]} x2={x2} y2={y2} />);
    }
  }
  return (
    <svg width={width} height={height} style={{ position: "absolute", left: 0, top: 0, mixBlendMode: "screen" }}>
      <g stroke="rgba(236,242,255,1)" strokeWidth={strokeWidth} strokeLinecap="round" opacity={opacity} style={{ filter: "drop-shadow(0 0 3px rgba(170,200,255,0.5))" }}>
        {lines}
      </g>
    </svg>
  );
};
