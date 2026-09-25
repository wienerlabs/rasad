import React from "react";
import { Composition } from "remotion";
import { Film } from "./Film";
import timeline from "./timeline.json";

export const Root: React.FC = () => (
  <>
    <Composition id="Rasad" component={Film} durationInFrames={timeline.duration * timeline.fps} fps={timeline.fps} width={1920} height={1080} defaultProps={{ withScore: true }} />
    <Composition id="RasadSilent" component={Film} durationInFrames={timeline.duration * timeline.fps} fps={timeline.fps} width={1920} height={1080} defaultProps={{ withScore: false }} />
  </>
);
