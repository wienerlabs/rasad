# Rasad

A pocket observatory for Android. Point the phone at the sky and every star, planet and constellation appears where it really is, together with the Arabic story behind its name, the qibla on the horizon and a crescent (hilal) visibility engine for the start of every Hijri month.

Everything is computed on the device. No account, no server, no network needed for the sky.

<p>
  <img src="docs/screenshots/night.png" width="24%" />
  <img src="docs/screenshots/vega.png" width="24%" />
  <img src="docs/screenshots/dusk.png" width="24%" />
  <img src="docs/screenshots/qibla.png" width="24%" />
</p>
<p>
  <img src="docs/screenshots/hilal.png" width="24%" />
  <img src="docs/screenshots/evenings.png" width="24%" />
  <img src="docs/screenshots/map.png" width="24%" />
</p>

## Film

<a href="https://github.com/wienerlabs/rasad/releases/download/film-v1/rasad-film.mp4"><img src="docs/film/preview.gif" width="100%" alt="Rasad film preview" /></a>

A 56 second, 1080p60 film rendered entirely from code. It uses the app's own star catalog, astrolabe math, Moon lighting and hilal data, with a procedural soundtrack in D Hicaz. [Watch the full film (MP4, 59 MB)](https://github.com/wienerlabs/rasad/releases/download/film-v1/rasad-film.mp4) or read [how it is made](film/README.md).

## What it does

**Live sky**
- Sensor-fused AR view (rotation vector with accelerometer plus magnetometer fallback), magnetic declination corrected with the World Magnetic Model.
- 8,920 stars down to magnitude 6.5, colored by B-V index, sized by magnitude, dimmed by atmospheric extinction and by the actual sky brightness (Sun altitude, moonlight).
- A single AGSL shader renders the atmosphere (day, civil and nautical twilight, night), the Milky Way from a rasterized equirectangular map, the ground, and a photoreal Moon lit by the real Sun direction with the correct phase and bright limb.
- Sun, Moon and all planets from Astronomy Engine; 89 constellation figures with Turkish names; an astrolabe style horizon ring with degree ticks and cardinal points.
- Qibla marker on the horizon with a haptic tick when the phone points at it.
- Time machine: jog dial with hour haptics, day and hour jumps, and a time-lapse mode.
- Night vision mode (full red color matrix) to keep dark adaptation.
- In compass mode the reticle names the constellation you are pointing at.

**Finding things**

<img src="docs/screenshots/guidance.jpg" width="100%" alt="Guided finding: path to an off-screen target, a below-horizon target with a time jump, the framed result, search and the constellation card" />

- Search stars, planets and all 88 constellations by Turkish, Latin, classical Arabic or folk name: `simak` finds both Arcturus and Spica, `kepce` finds the Big Dipper, `Ülker` finds Taurus. Every result shows where it is right now, and an empty search opens with what is above the horizon at this moment.
- Guided finding: a great circle path of flowing chevrons runs from the centre of the view to the target, an edge arrow tells how many degrees are left, and a guidance card spells out the turn (`Sola 42° · 18° yukarı`). The phone ticks when the target comes into view and confirms when it is centred.
- A found constellation draws itself in along its lines, gets a frame, its Latin and classical Arabic names and its brightest stars labelled, while the rest of the sky steps back. Flights to a constellation zoom out until the whole figure fits.
- Knows when you cannot see it: a target below the horizon shows its rise time, one that never rises at your latitude says so, and one tap jumps the clock to the best dark moment in the next 24 hours.
- Constellation cards: classical Arabic name with transliteration and meaning, rise, transit and set of the figure, best season, size, brightest star, and tappable member stars.

**Star lore**
- 126 star names with the original Arabic (or Persian and Latin) form set in Amiri, the Ottoman Turkish transliteration, the meaning and a short story: the Sirius verse in Surat an-Najm, the Alcor eyesight test, the daughters of Na'sh in the Big Dipper, Demirkazik for Polaris.

**Hilal**
- Lunation: phase, illumination, age, next quarters, Umm al-Qura Hijri date.
- Next conjunction and the Hijri month it opens.
- Three evenings after conjunction evaluated with the Yallop (1997) q test at the best time Tb = Ts + 4/9 Lag, with a western horizon diagram at Tb.
- A world visibility map (A to F zones) for any evening, from a fast grid solver that samples the geocentric Sun and Moon every 10 minutes once and then evaluates 11,041 locations (2 degree grid) in about 0.1 s on a laptop JVM. Unit tests check the fast solver against the detailed per location model.
- Twelve month outlook: for every upcoming month, the first evening the crescent can be seen from your location.

## Architecture

```
app/src/main/java/xyz/wienerlabs/rasad
  astro/     ephemeris wrapper, rise and set, qibla, Yallop, world grid solver, Hijri calendar
  sky/       catalog loader, camera and stereographic projection, sensor tracker,
             AGSL sky shader, canvas renderer, controller (frame loop, gestures, flights)
  ui/        Compose screens: sky, info sheet, search, time dial, hilal, map, diagram
  location/  on-device location and reverse geocoding
tools/build_data.py   downloads and compiles the catalogs into compact binary assets
```

The frame loop runs in `withFrameNanos`; the canvas reads a frame counter only in the draw phase, so the sky redraws at display rate without recomposing the UI. Stars are bucketed by magnitude and color and drawn with a handful of `drawPoints` calls.

## Build

Requirements: JDK 17, Android SDK 36.

```bash
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

The release APK is signed with the debug key so it can be sideloaded directly. To rebuild the data assets:

```bash
python3 tools/build_data.py
```

## Deep links

Useful for demos and for sharing a view:

```
rasad://sky?az=225&alt=38&fov=100&time=2026-10-12T17:45:00Z&lat=41.0082&lon=28.9784&place=Istanbul
rasad://sky?select=Vega
rasad://sky?target=Venus&play=1
rasad://hilal?time=2026-09-25T18:00:00Z
```

## Accuracy notes

- Positions come from Astronomy Engine (VSOP87 and a lunar theory accurate to about an arcminute).
- Crescent predictions follow Yallop's method. They are astronomical estimates; in Turkey the official month start is announced by the Presidency of Religious Affairs (Diyanet).
- Hijri dates use ICU's Umm al-Qura calendar.

## Data and licenses

See [NOTICE.md](NOTICE.md).
