import { continueRender, delayRender, Easing, staticFile } from "remotion";
import timeline from "./timeline.json";

export const FPS = timeline.fps;
export const seconds = (s: number) => Math.round(s * FPS);
export const scene = (name: keyof typeof timeline.scenes) => {
  const [start, end] = timeline.scenes[name];
  return { from: seconds(start), duration: seconds(end - start), start, end };
};
export const cue = timeline.cues;

export const color = {
  ink: "#05070b",
  text: "#f4f2ec",
  muted: "rgba(244,242,236,0.66)",
  faint: "rgba(244,242,236,0.42)",
  hairline: "rgba(244,242,236,0.16)",
  warm: "#ffc98a",
};

export const font = {
  display: "'Funnel Display', sans-serif",
  sans: "'Funnel Sans', sans-serif",
  arabic: "'Amiri', serif",
};

export const ease = {
  out: Easing.bezier(0.16, 1, 0.3, 1),
  inOut: Easing.bezier(0.65, 0, 0.35, 1),
  in: Easing.bezier(0.7, 0, 0.84, 0),
  snap: Easing.bezier(0.85, 0, 0.15, 1),
};

const fontHandle = delayRender("fonts", { timeoutInMilliseconds: 60000 });
const faces = [
  new FontFace("Funnel Display", `url(${staticFile("fonts/funnel_display.ttf")}) format("truetype")`, { weight: "300 800" }),
  new FontFace("Funnel Sans", `url(${staticFile("fonts/funnel_sans.ttf")}) format("truetype")`, { weight: "300 800" }),
  new FontFace("Amiri", `url(${staticFile("fonts/amiri_regular.ttf")}) format("truetype")`),
];
Promise.all(faces.map((face) => face.load()))
  .then((loaded) => {
    loaded.forEach((face) => document.fonts.add(face));
    continueRender(fontHandle);
  })
  .catch(() => continueRender(fontHandle));
