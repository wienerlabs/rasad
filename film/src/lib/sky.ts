export type Vec3 = [number, number, number];
export type Mat3 = [Vec3, Vec3, Vec3];

export const DEG = Math.PI / 180;

export const ISTANBUL = { lat: 41.0082, lon: 28.9784 };

export const dot = (a: Vec3, b: Vec3) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
export const cross = (a: Vec3, b: Vec3): Vec3 => [
  a[1] * b[2] - a[2] * b[1],
  a[2] * b[0] - a[0] * b[2],
  a[0] * b[1] - a[1] * b[0],
];
export const scale = (a: Vec3, s: number): Vec3 => [a[0] * s, a[1] * s, a[2] * s];
export const add = (a: Vec3, b: Vec3): Vec3 => [a[0] + b[0], a[1] + b[1], a[2] + b[2]];
export const sub = (a: Vec3, b: Vec3): Vec3 => [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
export const length = (a: Vec3) => Math.sqrt(dot(a, a));
export const normalize = (a: Vec3): Vec3 => {
  const l = length(a) || 1;
  return [a[0] / l, a[1] / l, a[2] / l];
};

export const fromRaDec = (raHours: number, decDegrees: number): Vec3 => {
  const ra = raHours * 15 * DEG;
  const dec = decDegrees * DEG;
  return [Math.cos(dec) * Math.cos(ra), Math.cos(dec) * Math.sin(ra), Math.sin(dec)];
};

export const julianDay = (millis: number) => millis / 86400000 + 2440587.5;

export const localSiderealDegrees = (millis: number, longitude: number) => {
  const d = julianDay(millis) - 2451545.0;
  const t = d / 36525;
  const gmst = 280.46061837 + 360.98564736629 * d + 0.000387933 * t * t;
  return (((gmst + longitude) % 360) + 360) % 360;
};

export const equatorialToHorizon = (millis: number, latitude: number, longitude: number): Mat3 => {
  const lst = localSiderealDegrees(millis, longitude) * DEG;
  const phi = latitude * DEG;
  const c = Math.cos(lst);
  const s = Math.sin(lst);
  return [
    [-s, c, 0],
    [-Math.sin(phi) * c, -Math.sin(phi) * s, Math.cos(phi)],
    [Math.cos(phi) * c, Math.cos(phi) * s, Math.sin(phi)],
  ];
};

export const multiply = (m: Mat3, v: Vec3): Vec3 => [dot(m[0], v), dot(m[1], v), dot(m[2], v)];

export const transpose = (m: Mat3): Mat3 => [
  [m[0][0], m[1][0], m[2][0]],
  [m[0][1], m[1][1], m[2][1]],
  [m[0][2], m[1][2], m[2][2]],
];

export const compose = (a: Mat3, b: Mat3): Mat3 => {
  const bt = transpose(b);
  return [
    [dot(a[0], bt[0]), dot(a[0], bt[1]), dot(a[0], bt[2])],
    [dot(a[1], bt[0]), dot(a[1], bt[1]), dot(a[1], bt[2])],
    [dot(a[2], bt[0]), dot(a[2], bt[1]), dot(a[2], bt[2])],
  ];
};

export const lookBasis = (forward: Vec3, upHint: Vec3, rollDegrees = 0): Mat3 => {
  const f = normalize(forward);
  let r = cross(f, upHint);
  if (length(r) < 1e-6) r = cross(f, [1, 0, 0]);
  r = normalize(r);
  let u = normalize(cross(r, f));
  if (rollDegrees !== 0) {
    const a = rollDegrees * DEG;
    const r2 = add(scale(r, Math.cos(a)), scale(u, Math.sin(a)));
    const u2 = add(scale(u, Math.cos(a)), scale(r, -Math.sin(a)));
    r = r2;
    u = u2;
  }
  return [r, u, f];
};

export const horizonDirection = (azimuthDegrees: number, altitudeDegrees: number): Vec3 => {
  const az = azimuthDegrees * DEG;
  const alt = altitudeDegrees * DEG;
  return [Math.cos(alt) * Math.sin(az), Math.cos(alt) * Math.cos(az), Math.sin(alt)];
};

export const horizonCamera = (azimuthDegrees: number, altitudeDegrees: number, rollDegrees = 0): Mat3 =>
  lookBasis(horizonDirection(azimuthDegrees, altitudeDegrees), [0, 0, 1], rollDegrees);

export const skyCamera = (millis: number, azimuthDegrees: number, altitudeDegrees: number, rollDegrees = 0): Mat3 =>
  compose(horizonCamera(azimuthDegrees, altitudeDegrees, rollDegrees), equatorialToHorizon(millis, ISTANBUL.lat, ISTANBUL.lon));

export const stereographicScale = (width: number, fovDegrees: number) => width / 2 / (2 * Math.tan((fovDegrees * DEG) / 4));

export const project = (view: Mat3, v: Vec3, width: number, height: number, pxScale: number): [number, number, number] => {
  const c = multiply(view, v);
  const k = (2 / (1 + c[2])) * pxScale;
  return [width / 2 + c[0] * k, height / 2 - c[1] * k, c[2]];
};

export const slerpBasis = (a: Mat3, b: Mat3, t: number): Mat3 => {
  const f = normalize(add(scale(a[2], 1 - t), scale(b[2], t)));
  const u = normalize(add(scale(a[1], 1 - t), scale(b[1], t)));
  return lookBasis(f, u);
};

export const matToGl = (m: Mat3) => new Float32Array([m[0][0], m[1][0], m[2][0], m[0][1], m[1][1], m[2][1], m[0][2], m[1][2], m[2][2]]);

export const bvToRgb = (bv: number): Vec3 => {
  const t = 4600 * (1 / (0.92 * bv + 1.7) + 1 / (0.92 * bv + 0.62));
  const k = t / 100;
  let r: number;
  let g: number;
  let b: number;
  if (k <= 66) {
    r = 255;
    g = 99.4708025861 * Math.log(k) - 161.1195681661;
    b = k <= 19 ? 0 : 138.5177312231 * Math.log(k - 10) - 305.0447927307;
  } else {
    r = 329.698727446 * Math.pow(k - 60, -0.1332047592);
    g = 288.1221695283 * Math.pow(k - 60, -0.0755148492);
    b = 255;
  }
  const clamp = (x: number) => Math.min(255, Math.max(0, x)) / 255;
  const white: Vec3 = [1, 1, 1];
  const rgb: Vec3 = [clamp(r), clamp(g), clamp(b)];
  return add(scale(rgb, 0.72), scale(white, 0.28));
};

export const sunEquatorial = (millis: number): Vec3 => {
  const n = julianDay(millis) - 2451545.0;
  const meanLongitude = (280.46 + 0.9856474 * n) * DEG;
  const meanAnomaly = (357.528 + 0.9856003 * n) * DEG;
  const eclipticLongitude = meanLongitude + (1.915 * Math.sin(meanAnomaly) + 0.02 * Math.sin(2 * meanAnomaly)) * DEG;
  const obliquity = (23.439 - 0.0000004 * n) * DEG;
  return [Math.cos(eclipticLongitude), Math.cos(obliquity) * Math.sin(eclipticLongitude), Math.sin(obliquity) * Math.sin(eclipticLongitude)];
};

export const sunHorizon = (millis: number): Vec3 => multiply(equatorialToHorizon(millis, ISTANBUL.lat, ISTANBUL.lon), sunEquatorial(millis));
