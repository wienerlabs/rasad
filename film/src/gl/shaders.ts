const NOISE = `
float hash31(vec3 p) {
  p = fract(p * 0.3183099 + vec3(0.71, 0.113, 0.419));
  p *= 17.0;
  return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}
float noise3(vec3 x) {
  vec3 i = floor(x);
  vec3 f = fract(x);
  f = f * f * (3.0 - 2.0 * f);
  return mix(
    mix(mix(hash31(i + vec3(0, 0, 0)), hash31(i + vec3(1, 0, 0)), f.x), mix(hash31(i + vec3(0, 1, 0)), hash31(i + vec3(1, 1, 0)), f.x), f.y),
    mix(mix(hash31(i + vec3(0, 0, 1)), hash31(i + vec3(1, 0, 1)), f.x), mix(hash31(i + vec3(0, 1, 1)), hash31(i + vec3(1, 1, 1)), f.x), f.y),
    f.z);
}
float fbm3(vec3 p) {
  float value = 0.0;
  float amplitude = 0.5;
  for (int i = 0; i < 5; i++) {
    value += amplitude * noise3(p);
    p = p * 2.03 + vec3(1.7, 9.2, 3.1);
    amplitude *= 0.5;
  }
  return value;
}
`;

const VIEW = `
uniform vec2 uResolution;
uniform float uScale;
uniform mat3 uView;
vec3 viewDirection(vec2 frag) {
  vec2 p = (frag - 0.5 * uResolution) / uScale;
  float rho2 = dot(p, p);
  float inv = 1.0 / (4.0 + rho2);
  vec3 c = vec3(4.0 * p.x * inv, 4.0 * p.y * inv, (4.0 - rho2) * inv);
  return normalize(transpose(uView) * c);
}
`;

export const STAR_VERTEX = `#version 300 es
in vec3 aPos;
in float aMag;
in vec3 aColor;
uniform mat3 uView;
uniform vec2 uResolution;
uniform float uScale;
uniform float uLimit;
uniform float uSize;
uniform float uTime;
uniform float uRevealRadius;
uniform float uGain;
out vec3 vColor;
out float vIntensity;
void main() {
  vec3 c = uView * aPos;
  if (c.z < -0.8 || aMag > uLimit) {
    gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
    gl_PointSize = 0.0;
    vIntensity = 0.0;
    vColor = vec3(0.0);
    return;
  }
  float k = 2.0 / (1.0 + c.z) * uScale;
  vec2 screen = c.xy * k;
  gl_Position = vec4(screen / (uResolution * 0.5), 0.0, 1.0);
  float rel = max(uLimit - aMag, 0.0);
  float size = (1.2 + 0.85 * pow(rel, 1.1)) * uSize;
  float twinkle = 1.0 + 0.10 * sin(uTime * (2.5 + fract(aMag * 13.7) * 6.0) + aMag * 41.0);
  float reveal = 1.0 - smoothstep(uRevealRadius - 60.0, uRevealRadius + 20.0, length(screen));
  vIntensity = clamp(0.18 + rel / 3.2, 0.0, 1.8) * twinkle * reveal * uGain;
  gl_PointSize = min(size * 3.4, 96.0);
  vColor = aColor;
}`;

export const STAR_FRAGMENT = `#version 300 es
precision highp float;
in vec3 vColor;
in float vIntensity;
out vec4 outColor;
void main() {
  vec2 p = gl_PointCoord * 2.0 - 1.0;
  float r2 = dot(p, p);
  if (r2 > 1.0) discard;
  float core = exp(-r2 * 30.0);
  float halo = exp(-r2 * 6.0) * 0.32;
  float spike = exp(-abs(p.x) * 40.0) * exp(-p.y * p.y * 3.0) + exp(-abs(p.y) * 40.0) * exp(-p.x * p.x * 3.0);
  float a = (core + halo + spike * 0.10 * smoothstep(0.9, 1.6, vIntensity)) * vIntensity;
  outColor = vec4(vColor * a, a);
}`;

export const MILKY_FRAGMENT = `#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uMilky;
uniform float uMilkyGain;
uniform float uFade;
uniform float uTime;
${VIEW}
${NOISE}
const float PI = 3.14159265;
void main() {
  vec3 d = viewDirection(gl_FragCoord.xy);
  float ra = atan(d.y, d.x);
  if (ra < 0.0) ra += 2.0 * PI;
  float dec = asin(clamp(d.z, -1.0, 1.0));
  vec2 uv = vec2(ra / (2.0 * PI), 0.5 - dec / PI);
  float mw = texture(uMilky, uv).r;
  float detail = fbm3(d * 7.0);
  float dust = fbm3(d * 19.0 + vec3(3.1, 1.7, 8.2));
  float density = mw * (0.45 + 1.05 * detail);
  density *= 1.0 - 0.55 * smoothstep(0.52, 0.82, dust) * smoothstep(0.25, 0.8, mw);
  float warmth = smoothstep(0.3, 0.85, fbm3(d * 2.1 + vec3(7.0, 2.0, 5.0)));
  vec3 cool = vec3(0.52, 0.60, 0.82);
  vec3 warm = vec3(0.95, 0.78, 0.58);
  vec3 color = mix(cool, warm, warmth * smoothstep(0.2, 0.9, mw)) * density * density * 0.62 * uMilkyGain;
  color += vec3(0.30, 0.34, 0.46) * density * 0.06 * uMilkyGain;
  float sparkle = pow(hash31(floor(d * 1100.0)), 70.0) * smoothstep(0.1, 0.7, mw) * uMilkyGain;
  color += vec3(sparkle * 0.9);
  float nebula = fbm3(d * 3.0 + vec3(uTime * 0.01));
  color += vec3(0.06, 0.035, 0.08) * smoothstep(0.55, 0.85, nebula) * 0.5 * uMilkyGain;
  color += vec3(0.003, 0.005, 0.011);
  outColor = vec4(color * uFade, 1.0);
}`;

export const MOON_FRAGMENT = `#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform vec2 uResolution;
uniform vec2 uCenter;
uniform float uRadius;
uniform vec3 uSun;
uniform float uNorth;
uniform float uSpin;
uniform float uRelief;
uniform float uEarthshine;
uniform float uGain;
uniform float uHalo;
uniform sampler2D uMoonTex;
uniform vec2 uTexSize;
const float PI = 3.14159265;
float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }
void main() {
  vec2 p = (gl_FragCoord.xy - uCenter) / uRadius;
  float r = length(p);
  float lit = clamp(uSun.z * 0.5 + 0.5, 0.0, 1.0);
  float halo = exp(-(max(r, 1.0) - 1.0) * 6.0) * 0.16 * (0.15 + 0.85 * lit) * uHalo;
  halo *= 1.0 - smoothstep(1.05, 1.6, r);
  if (r > 1.0 + 1.0 / uRadius) {
    outColor = vec4(vec3(halo) * vec3(0.8, 0.85, 1.0), halo);
    return;
  }
  float rc = min(r, 1.0);
  float zs = sqrt(max(1.0 - rc * rc, 0.0));
  vec3 n = vec3(p, zs);
  float c = cos(uNorth);
  float s = sin(uNorth);
  float xn = p.x * c - p.y * s;
  float yn = p.x * s + p.y * c;
  float lat = asin(clamp(yn, -1.0, 1.0));
  float lon = atan(xn, zs) + uSpin;
  vec2 uv = vec2(lon / (2.0 * PI) + 0.5, 0.5 - lat / PI);
  vec3 albedo = texture(uMoonTex, uv).rgb;
  float h = luma(albedo);
  float hx = luma(texture(uMoonTex, uv + vec2(1.5 / uTexSize.x, 0.0)).rgb);
  float hy = luma(texture(uMoonTex, uv + vec2(0.0, 1.5 / uTexSize.y)).rgb);
  vec3 bumped = normalize(n + vec3(-(hx - h), (hy - h), 0.0) * uRelief);
  float terminator = smoothstep(-0.03, 0.10, dot(n, uSun));
  float diffuse = max(dot(bumped, uSun), 0.0);
  float limb = 0.80 + 0.20 * zs;
  vec3 color = albedo * (mix(terminator, diffuse * 1.25, 0.65) * uGain * limb) + albedo * uEarthshine * (1.0 - terminator);
  float coverage = clamp((1.0 - r) * uRadius + 0.5, 0.0, 1.0);
  vec3 premul = color * coverage + vec3(halo) * (1.0 - coverage);
  outColor = vec4(premul, max(max(premul.r, premul.g), premul.b));
}`;

export const TRAILS_FRAGMENT = `#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uPrefix;
uniform vec2 uPrefixSize;
uniform float uMaxPolar;
uniform float uSpan;
uniform float uGain;
${VIEW}
const float PI = 3.14159265;
vec3 prefixAt(float row, float column) {
  float W = uPrefixSize.x;
  float c = mod(column, W);
  float c0 = floor(c);
  float f = c - c0;
  float c1 = mod(c0 + 1.0, W);
  vec3 a = texelFetch(uPrefix, ivec2(int(c0), int(row)), 0).rgb;
  vec3 b = texelFetch(uPrefix, ivec2(int(c1), int(row)), 0).rgb;
  if (c1 < c0) b += texelFetch(uPrefix, ivec2(int(W) - 1, int(row)), 0).rgb;
  return mix(a, b, f);
}
vec3 cumulative(float row, float column) {
  float W = uPrefixSize.x;
  float laps = floor(column / W);
  vec3 total = texelFetch(uPrefix, ivec2(int(W) - 1, int(row)), 0).rgb;
  return prefixAt(row, column - laps * W) + total * laps;
}
vec3 arc(float row, float column, float spanColumns) {
  return max(cumulative(row, column) - cumulative(row, column - spanColumns), vec3(0.0));
}
void main() {
  vec3 d = viewDirection(gl_FragCoord.xy);
  float polar = acos(clamp(d.z, -1.0, 1.0));
  if (polar > uMaxPolar) { outColor = vec4(0.0); return; }
  float rowF = polar / uMaxPolar * (uPrefixSize.y - 1.0);
  float ra = atan(d.y, d.x);
  if (ra < 0.0) ra += 2.0 * PI;
  float column = ra / (2.0 * PI) * uPrefixSize.x;
  float spanColumns = max(uSpan / (2.0 * PI) * uPrefixSize.x, 0.5);
  float r0 = floor(rowF);
  float fr = rowF - r0;
  float r1 = min(r0 + 1.0, uPrefixSize.y - 1.0);
  vec3 trail = mix(arc(r0, column, spanColumns), arc(r1, column, spanColumns), fr);
  vec3 head = mix(arc(r0, column + 0.6, 1.2), arc(r1, column + 0.6, 1.2), fr);
  vec3 color = (trail + head * 1.8) * uGain;
  color = 1.0 - exp(-color * 1.6);
  outColor = vec4(color, max(color.r, max(color.g, color.b)));
}`;

export const QMAP_FRAGMENT = `#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform sampler2D uQa;
uniform sampler2D uQb;
uniform float uMix;
uniform vec4 uRect;
uniform float uLow;
uniform float uHigh;
uniform float uLatTop;
uniform float uLatBottom;
uniform float uUtc;
uniform float uGlowUtc;
uniform float uDecl;
uniform float uReveal;
float band(float q, float t, float w) { return smoothstep(t - w, t + w, q); }
float line(float q, float t, float w) { return 1.0 - smoothstep(0.0, w, abs(q - t)); }
void main() {
  vec2 m = (gl_FragCoord.xy - uRect.xy) / uRect.zw;
  if (m.x < 0.0 || m.x > 1.0 || m.y < 0.0 || m.y > 1.0) { outColor = vec4(0.0); return; }
  vec2 size = vec2(textureSize(uQa, 0));
  vec2 tuv = (vec2(m.x, 1.0 - m.y) * (size - 1.0) + 0.5) / size;
  vec4 s = mix(texture(uQa, tuv), texture(uQb, tuv), uMix);
  float q = uLow + (s.r * 65280.0 + s.b * 255.0) / 65535.0 * (uHigh - uLow);
  float w = max(fwidth(q) * 1.1, 0.004);
  float alpha = 0.05 * band(q, -0.293, w) + 0.06 * band(q, -0.232, w) + 0.10 * band(q, -0.160, w) + 0.13 * band(q, -0.014, w) + 0.20 * band(q, 0.216, w);
  float edges = line(q, -0.160, w * 1.6) * 0.35 + line(q, -0.014, w * 1.6) * 0.55 + line(q, 0.216, w * 1.6) * 0.85;
  alpha *= 1.0 - s.g;
  edges *= 1.0 - s.g;
  float lon = -180.0 + m.x * 360.0;
  float lat = radians(uLatBottom + m.y * (uLatTop - uLatBottom));
  float cosH = clamp(-tan(lat) * tan(uDecl), -1.0, 1.0);
  float hourAngle = degrees(acos(cosH));
  float sunset = 12.0 + (hourAngle - lon) / 15.0;
  float revealed = smoothstep(sunset - 0.25, sunset + 0.25, uUtc);
  float terminator = exp(-pow((uGlowUtc - sunset) * 3.2, 2.0));
  float glow = alpha * revealed + edges * revealed * 0.9;
  vec3 color = vec3(1.0) * alpha * revealed + vec3(1.0, 0.96, 0.88) * edges * revealed * 0.9;
  color += vec3(1.0, 0.62, 0.30) * terminator * 0.55 * uReveal;
  float a = glow + terminator * 0.55 * uReveal + s.g * 0.35 * revealed;
  outColor = vec4(color * uReveal, a * uReveal);
}`;

export const TUNNEL_FRAGMENT = `#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform vec2 uResolution;
uniform float uTime;
uniform float uSpeed;
uniform float uFade;
float hash11(float p) { p = fract(p * 0.1031); p *= p + 33.33; p *= p + p; return fract(p); }
void main() {
  vec2 p = (gl_FragCoord.xy - 0.5 * uResolution) / uResolution.y;
  float radius = length(p);
  float angle = atan(p.y, p.x);
  vec3 color = vec3(0.0);
  for (int layer = 0; layer < 4; layer++) {
    float count = 90.0 + float(layer) * 70.0;
    float a = (angle / 6.2831853 + 0.5) * count;
    float cell = floor(a);
    float within = fract(a) - 0.5;
    float seed = hash11(cell * 7.13 + float(layer) * 131.7);
    if (seed < 0.35) continue;
    float z = fract(seed * 5.31 + uTime * uSpeed * (0.35 + seed * 0.9));
    float head = z * z * 1.4;
    float len = 0.02 + 0.45 * z * uSpeed;
    float tail = smoothstep(head - len, head, radius) * (1.0 - smoothstep(head, head + 0.004, radius));
    float width = 1.0 - smoothstep(0.0, 0.5 * (0.2 + z), abs(within));
    vec3 tint = mix(vec3(0.62, 0.72, 1.0), vec3(1.0, 0.86, 0.70), hash11(seed * 91.0));
    color += tint * tail * width * (0.4 + z * 1.4);
  }
  color += vec3(0.02, 0.025, 0.05) * (1.0 - radius);
  outColor = vec4(color * uFade, 1.0);
}`;

export const SKY_FRAGMENT = `#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform vec3 uSun;
uniform float uGround;
${VIEW}
vec3 atmosphere(vec3 dir) {
  float sunHeight = uSun.z;
  float upness = clamp(dir.z, 0.0, 1.0);
  float lower = 1.0 - upness;
  float horizonWeight = lower * lower * lower;
  float day = smoothstep(-0.10, 0.14, sunHeight);
  float twilight = smoothstep(-0.32, -0.03, sunHeight) * (1.0 - smoothstep(0.03, 0.32, sunHeight));
  vec3 zenith = mix(mix(vec3(0.004, 0.007, 0.016), vec3(0.035, 0.065, 0.170), twilight), vec3(0.120, 0.320, 0.700), day);
  vec3 horizon = mix(mix(vec3(0.016, 0.020, 0.036), vec3(0.150, 0.130, 0.220), twilight), vec3(0.560, 0.700, 0.880), day);
  vec2 flatDir = normalize(dir.xy + vec2(0.00001, 0.0));
  vec2 flatSun = normalize(uSun.xy + vec2(0.00001, 0.0));
  float towardSun = 0.5 + 0.5 * dot(flatDir, flatSun);
  horizon += vec3(1.0, 0.50, 0.20) * twilight * pow(towardSun, 5.0) * 0.9;
  horizon += vec3(0.32, 0.20, 0.30) * twilight * pow(1.0 - towardSun, 3.0) * 0.35 * (1.0 - day);
  vec3 color = mix(zenith, horizon, horizonWeight);
  float sunDot = max(dot(dir, uSun), 0.0);
  color += vec3(1.0, 0.86, 0.64) * pow(sunDot, 60.0) * 0.4 * (day + twilight);
  color += vec3(1.0, 0.60, 0.32) * pow(sunDot, 7.0) * 0.2 * twilight;
  return color;
}
void main() {
  vec3 dir = viewDirection(gl_FragCoord.xy);
  vec3 color = atmosphere(dir);
  vec3 horizonTint = atmosphere(normalize(vec3(dir.x + 0.00001, dir.y, 0.0)));
  if (dir.z < 0.0) {
    float depth = clamp(-dir.z * 5.0, 0.0, 1.0);
    vec3 ground = mix(horizonTint * 0.22 + vec3(0.010, 0.010, 0.012), vec3(0.008, 0.008, 0.010), depth);
    color = mix(color, ground, uGround * smoothstep(0.0, 0.004, -dir.z));
  }
  color += horizonTint * exp(-abs(dir.z) * 55.0) * 0.10;
  outColor = vec4(color, 1.0);
}`;

export const FOG_FRAGMENT = `#version 300 es
precision highp float;
in vec2 vUv;
out vec4 outColor;
uniform vec2 uResolution;
uniform float uTime;
uniform vec3 uTintA;
uniform vec3 uTintB;
uniform float uFade;
${NOISE}
void main() {
  vec2 p = (gl_FragCoord.xy - 0.5 * uResolution) / uResolution.y;
  vec3 q = vec3(p * 2.2, uTime * 0.08);
  float n = fbm3(q + fbm3(q * 1.7 + vec3(0.0, 0.0, uTime * 0.05)) * 1.4);
  float vignette = 1.0 - smoothstep(0.35, 1.1, length(p));
  vec3 color = mix(uTintA, uTintB, smoothstep(0.35, 0.8, n)) * n * n * vignette;
  outColor = vec4(color * uFade, 1.0);
}`;
