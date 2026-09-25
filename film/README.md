# Rasad film

A 56 second, 1920x1080, 60 fps motion film for Rasad, written as code with Remotion, WebGL2 shaders and a procedural score.

Nothing in the film is stock footage or a mockup. The stars come from the same HYG catalog the app ships, the astrolabe is a real stereographic projection for Istanbul, the Moon phase is lit from the true Sun direction on NASA LRO imagery, every hilal map is the app's own Yallop q field (43,681 grid points per evening), the star trails rotate at the sidereal rate over a real night, and the phone clips are screen recordings of the app on an emulator.

## Scenes

| Time | Scene | What it shows |
| --- | --- | --- |
| 0 to 4 s | Ignite | Vega ignites, a shockwave reveals the Milky Way |
| 4 to 12 s | Names | Arabic star names, one card per cut |
| 12 to 18 s | Astrolabe | Istanbul plate, rotating rete, qibla needle locks at 151.6 degrees |
| 18 to 28 s | App | Real app footage with live counters |
| 28 to 34 s | Moon | Phase sweep, then the 12 October crescent at q +0.157 |
| 34 to 42 s | Map | Worldwide hilal visibility, revealed with the sunset terminator |
| 42 to 46 s | Proof | Verified numbers on a hyperspace tunnel |
| 46 to 50 s | Time machine | Ten hours of star trails over Istanbul |
| 50 to 56 s | Outro | Wordmark centred on the celestial pole |

Scene boundaries and every sync point live in `src/timeline.json`. The score reads the same file, so moving a cue moves both picture and sound.

## Build

```bash
npm install
npm run assets
npm run score
npm run render
```

`npm run render` renders the picture without sound, then muxes `public/audio/score.wav` with ffmpeg so the AAC encoder delay is written into the MP4 edit list. Every hit lands on its frame; muxing through the renderer instead left the sound 43 ms late.

`tools/prepare.py` copies catalog assets and fonts from the app, builds the land outlines and the star name table, and encodes the raw q grids in `tools/qgrids` as 16 bit textures upsampled 4x with cubic splines. `tools/score.py` synthesises the soundtrack at 120 BPM in D Hicaz with numpy and scipy and writes `public/audio/score.wav`. The render lands in `out/rasad-film.mp4`.

`node tools/stills.mjs 300 1968 2850` renders single frames into `out/stills` without audio, which is the fastest way to check a change.

## Requirements

Node 20, ffmpeg, Python 3 with numpy, scipy and Pillow, and a machine with a GPU. Chrome renders the shaders through ANGLE.
