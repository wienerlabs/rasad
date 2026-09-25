import React from "react";
import { AbsoluteFill } from "remotion";
import { Flash, mixNumber, range, Slam, useSeconds } from "../components/ui";
import { TunnelLayer } from "../gl/layers";
import { color, cue, ease, scene } from "../theme";

const START = scene("proof").start;

const LINES: { top: string; bottom: string; align: "left" | "center" | "right"; size?: number }[] = [
  { top: "23 test", bottom: "hepsi yeşil", align: "center" },
  { top: "JPL Horizons", bottom: "ile bağımsız doğrulandı", align: "left", size: 170 },
  { top: "q −0,844", bottom: "Horizons ile birebir aynı", align: "right" },
  { top: "3,2 MB", bottom: "bütün uygulama", align: "center" },
  { top: "770 ms", bottom: "soğuk açılış", align: "left" },
  { top: "İnternetsiz", bottom: "bütün gökyüzü cihazda hesaplanıyor", align: "right", size: 170 },
  { top: "8.920", bottom: "yıldız, gerçek katalogdan", align: "center" },
  { top: "1 shader", bottom: "atmosfer, Samanyolu ve Ay", align: "center" },
];

export const Proof: React.FC = () => {
  const t = useSeconds();
  const speed = mixNumber(0.5, 1.8, range(t, 0, 4, ease.in));
  const fadeIn = range(t, 0, 0.3);
  const slams = cue.slams.map((s) => s - START);
  return (
    <AbsoluteFill style={{ background: color.ink }}>
      <TunnelLayer time={t + START} speed={speed} fade={0.9 * fadeIn} />
      <AbsoluteFill style={{ background: "radial-gradient(circle at center, rgba(5,7,11,0.55) 0%, rgba(5,7,11,0.15) 45%, rgba(5,7,11,0.7) 100%)" }} />
      {LINES.map((line, i) => (
        <Slam key={line.top} start={slams[i]} end={i + 1 < slams.length ? slams[i + 1] - 0.02 : 4.0} top={line.top} bottom={line.bottom} align={line.align} size={line.size} />
      ))}
      {slams.map((s) => (
        <Flash key={s} at={s} duration={0.16} intensity={0.22} />
      ))}
    </AbsoluteFill>
  );
};
