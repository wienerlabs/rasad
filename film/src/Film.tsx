import React from "react";
import { AbsoluteFill, Html5Audio, Sequence, staticFile } from "remotion";
import { Grain, Vignette } from "./components/ui";
import { AppShowcase } from "./scenes/AppShowcase";
import { Astrolabe } from "./scenes/Astrolabe";
import { Ignite } from "./scenes/Ignite";
import { Moon } from "./scenes/Moon";
import { Names } from "./scenes/Names";
import { Outro } from "./scenes/Outro";
import { Proof } from "./scenes/Proof";
import { TimeMachine } from "./scenes/TimeMachine";
import { WorldMap } from "./scenes/WorldMap";
import { color, scene } from "./theme";

const SCENES: { name: Parameters<typeof scene>[0]; Component: React.FC }[] = [
  { name: "ignite", Component: Ignite },
  { name: "names", Component: Names },
  { name: "astrolabe", Component: Astrolabe },
  { name: "app", Component: AppShowcase },
  { name: "moon", Component: Moon },
  { name: "map", Component: WorldMap },
  { name: "proof", Component: Proof },
  { name: "time", Component: TimeMachine },
  { name: "outro", Component: Outro },
];

export const Film: React.FC<{ withScore?: boolean }> = ({ withScore = true }) => (
  <AbsoluteFill style={{ background: color.ink }}>
    {SCENES.map(({ name, Component }) => {
      const { from, duration } = scene(name);
      return (
        <Sequence key={name} name={name} from={from} durationInFrames={duration}>
          <Component />
        </Sequence>
      );
    })}
    <Vignette />
    <Grain />
    {withScore ? <Html5Audio src={staticFile("audio/score.wav")} /> : null}
  </AbsoluteFill>
);
