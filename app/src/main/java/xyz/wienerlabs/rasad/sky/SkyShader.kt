package xyz.wienerlabs.rasad.sky

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import xyz.wienerlabs.rasad.astro.SkySnapshot
import kotlin.math.cos

class SkyShader(milkyWay: Bitmap, moonTexture: Bitmap) {
    val shader = RuntimeShader(SOURCE)

    init {
        shader.setInputShader("milkyWay", texture(milkyWay))
        shader.setInputShader("moonTexture", texture(moonTexture))
        shader.setFloatUniform("uMilkySize", milkyWay.width.toFloat(), milkyWay.height.toFloat())
        shader.setFloatUniform("uMoonSize", moonTexture.width.toFloat(), moonTexture.height.toFloat())
    }

    private fun texture(bitmap: Bitmap) = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP).apply {
        filterMode = BitmapShader.FILTER_MODE_LINEAR
    }

    fun update(
        camera: SkyCamera,
        snapshot: SkySnapshot,
        milkyWayStrength: Float,
        moonRadiusRadians: Double,
        sunRadiusRadians: Double,
        groundOpacity: Float,
    ) {
        shader.setFloatUniform("uCenter", camera.centerX, camera.centerY)
        shader.setFloatUniform("uScale", camera.scale.toFloat())
        shader.setFloatUniform("uRight", camera.right[0].toFloat(), camera.right[1].toFloat(), camera.right[2].toFloat())
        shader.setFloatUniform("uUp", camera.up[0].toFloat(), camera.up[1].toFloat(), camera.up[2].toFloat())
        shader.setFloatUniform("uForward", camera.forward[0].toFloat(), camera.forward[1].toFloat(), camera.forward[2].toFloat())
        val sun = snapshot.sun.direction
        val moon = snapshot.moon.direction
        shader.setFloatUniform("uSun", sun.x.toFloat(), sun.y.toFloat(), sun.z.toFloat())
        shader.setFloatUniform("uMoon", moon.x.toFloat(), moon.y.toFloat(), moon.z.toFloat())
        val m = snapshot.eqjToEnu
        shader.setFloatUniform("uPole", m[2].toFloat(), m[5].toFloat(), m[8].toFloat())
        shader.setFloatUniform("uEqjX", m[0].toFloat(), m[3].toFloat(), m[6].toFloat())
        shader.setFloatUniform("uEqjY", m[1].toFloat(), m[4].toFloat(), m[7].toFloat())
        shader.setFloatUniform("uEqjZ", m[2].toFloat(), m[5].toFloat(), m[8].toFloat())
        shader.setFloatUniform("uMoonRadius", moonRadiusRadians.toFloat())
        shader.setFloatUniform("uMoonCos", cos(moonRadiusRadians * 1.1).toFloat())
        shader.setFloatUniform("uSunRadius", sunRadiusRadians.toFloat())
        shader.setFloatUniform("uMoonLight", snapshot.moon.phaseFraction.toFloat())
        shader.setFloatUniform("uGround", groundOpacity)
        shader.setFloatUniform("uPixelAngle", (1.0 / camera.scale).toFloat())
        val sunHeight = sun.z
        val day = smoothstep(-0.10, 0.14, sunHeight)
        val twilight = smoothstep(-0.32, -0.03, sunHeight) * (1.0 - smoothstep(0.03, 0.32, sunHeight))
        val darkness = 1.0 - smoothstep(-0.30, -0.06, sunHeight)
        shader.setFloatUniform("uDay", day.toFloat())
        shader.setFloatUniform("uTwilight", twilight.toFloat())
        shader.setFloatUniform("uMilkyWay", (milkyWayStrength * darkness).toFloat())
        setColor("uZenith", mixColor(mixColor(NIGHT_ZENITH, DUSK_ZENITH, twilight), DAY_ZENITH, day))
        setColor("uHorizon", mixColor(mixColor(NIGHT_HORIZON, DUSK_HORIZON, twilight), DAY_HORIZON, day))
    }

    private fun setColor(name: String, color: DoubleArray) {
        shader.setFloatUniform(name, color[0].toFloat(), color[1].toFloat(), color[2].toFloat())
    }

    private fun mixColor(a: DoubleArray, b: DoubleArray, t: Double) = DoubleArray(3) { a[it] + (b[it] - a[it]) * t }

    private fun smoothstep(edge0: Double, edge1: Double, x: Double): Double {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
        return t * t * (3 - 2 * t)
    }

    companion object {
        private val NIGHT_ZENITH = doubleArrayOf(0.004, 0.007, 0.016)
        private val NIGHT_HORIZON = doubleArrayOf(0.016, 0.020, 0.036)
        private val DUSK_ZENITH = doubleArrayOf(0.035, 0.065, 0.170)
        private val DUSK_HORIZON = doubleArrayOf(0.150, 0.130, 0.220)
        private val DAY_ZENITH = doubleArrayOf(0.120, 0.320, 0.700)
        private val DAY_HORIZON = doubleArrayOf(0.560, 0.700, 0.880)

        private val SOURCE = """
            uniform float2 uCenter;
            uniform float uScale;
            uniform float3 uRight;
            uniform float3 uUp;
            uniform float3 uForward;
            uniform float3 uSun;
            uniform float3 uMoon;
            uniform float3 uPole;
            uniform float3 uEqjX;
            uniform float3 uEqjY;
            uniform float3 uEqjZ;
            uniform float uMoonRadius;
            uniform float uMoonCos;
            uniform float uSunRadius;
            uniform float uMoonLight;
            uniform float uMilkyWay;
            uniform float uGround;
            uniform float uPixelAngle;
            uniform float uDay;
            uniform float uTwilight;
            uniform float3 uZenith;
            uniform float3 uHorizon;
            uniform float2 uMilkySize;
            uniform float2 uMoonSize;
            uniform shader milkyWay;
            uniform shader moonTexture;

            const float PI = 3.14159265;

            float3 viewDirection(float2 fragCoord) {
                float2 p = (fragCoord - uCenter) / uScale;
                p.y = -p.y;
                float rho2 = dot(p, p);
                float inv = 1.0 / (4.0 + rho2);
                float3 local = float3(4.0 * p.x * inv, 4.0 * p.y * inv, (4.0 - rho2) * inv);
                return normalize(local.x * uRight + local.y * uUp + local.z * uForward);
            }

            float3 horizonColor(float3 dir) {
                float3 color = uHorizon;
                if (uTwilight > 0.001) {
                    float2 flatDir = normalize(dir.xy + float2(0.00001, 0.0));
                    float2 flatSun = normalize(uSun.xy + float2(0.00001, 0.0));
                    float towardSun = 0.5 + 0.5 * dot(flatDir, flatSun);
                    float toward2 = towardSun * towardSun;
                    float away = 1.0 - towardSun;
                    color += float3(1.0, 0.50, 0.20) * uTwilight * (toward2 * toward2 * towardSun) * 0.9;
                    color += float3(0.32, 0.20, 0.30) * uTwilight * (away * away * away) * 0.35 * (1.0 - uDay);
                }
                return color;
            }

            float3 milkyWayColor(float3 dir) {
                float3 eqj = float3(dot(uEqjX, dir), dot(uEqjY, dir), dot(uEqjZ, dir));
                float ra = atan(eqj.y, eqj.x);
                if (ra < 0.0) { ra += 2.0 * PI; }
                float dec = asin(clamp(eqj.z, -1.0, 1.0));
                float2 uv = float2(ra / (2.0 * PI) * uMilkySize.x, (0.5 - dec / PI) * uMilkySize.y);
                float value = milkyWay.eval(uv).r;
                return float3(0.66, 0.70, 0.80) * value * value * 0.16 + float3(0.30, 0.32, 0.38) * value * 0.05;
            }

            float3 moonSurface(float3 dir, float3 background) {
                float cosMoon = dot(dir, uMoon);
                if (cosMoon < uMoonCos) { return background; }
                float3 north = normalize(uPole - uMoon * dot(uPole, uMoon));
                float3 east = cross(north, uMoon);
                float3 offset = dir - uMoon * cosMoon;
                float s = sin(uMoonRadius);
                float x = dot(offset, east) / s;
                float y = dot(offset, north) / s;
                float r = sqrt(x * x + y * y);
                if (r >= 1.0 + 0.5 * uPixelAngle / uMoonRadius) { return background; }
                float zs = sqrt(max(1.0 - min(r, 1.0) * min(r, 1.0), 0.0));
                float3 normal = x * east + y * north - zs * uMoon;
                float light = smoothstep(-0.03, 0.09, dot(normal, uSun));
                float lat = asin(clamp(y, -1.0, 1.0));
                float lon = atan(-x, zs);
                float2 uv = float2((lon / (2.0 * PI) + 0.5) * uMoonSize.x, (0.5 - lat / PI) * uMoonSize.y);
                float3 albedo = moonTexture.eval(uv).rgb;
                float radiusPixels = uMoonRadius / max(uPixelAngle, 0.000001);
                float gain = mix(1.9, 1.30, clamp((radiusPixels - 20.0) / 80.0, 0.0, 1.0));
                float3 surface = albedo * (light * gain + 0.03 * (1.0 - light));
                float coverage = clamp((1.0 - r) * radiusPixels + 0.5, 0.0, 1.0);
                return background + surface * coverage;
            }

            half4 main(float2 fragCoord) {
                float3 dir = viewDirection(fragCoord);
                float3 horizon = horizonColor(dir);
                float upness = clamp(dir.z, 0.0, 1.0);
                float lower = 1.0 - upness;
                float horizonWeight = lower * lower * lower;
                float3 color = mix(uZenith, horizon, horizonWeight);

                float sunDot = dot(dir, uSun);
                if (sunDot > 0.0) {
                    float sun2 = sunDot * sunDot;
                    float sun4 = sun2 * sun2;
                    float sun8 = sun4 * sun4;
                    float sun64 = sun8 * sun8;
                    sun64 = sun64 * sun64;
                    sun64 = sun64 * sun64;
                    color += float3(1.0, 0.86, 0.64) * sun64 * 0.40 * (uDay + uTwilight);
                    color += float3(1.0, 0.60, 0.32) * sun8 * 0.20 * uTwilight;
                    if (sunDot > 0.985) {
                        float sunAngle = acos(clamp(sunDot, -1.0, 1.0));
                        float sunCore = 1.0 - smoothstep(uSunRadius * 0.92, uSunRadius * 1.08, sunAngle);
                        color += float3(1.0, 0.94, 0.82) * exp(-sunAngle / max(uSunRadius, 0.001) * 0.9) * 0.25;
                        color = mix(color, float3(1.0, 0.97, 0.90), sunCore);
                    }
                }

                if (uDay < 0.999) {
                    float moonDot = max(dot(dir, uMoon), 0.0);
                    float m2 = moonDot * moonDot;
                    float m4 = m2 * m2;
                    float m8 = m4 * m4;
                    float m64 = m8 * m8;
                    m64 = m64 * m64;
                    m64 = m64 * m64;
                    color += float3(0.55, 0.60, 0.72) * m64 * 0.20 * uMoonLight * (1.0 - uDay);
                }

                if (uMilkyWay > 0.001 && dir.z > -0.01) {
                    float horizonFade = smoothstep(-0.01, 0.22, dir.z);
                    color += milkyWayColor(dir) * uMilkyWay * horizonFade;
                }

                color = moonSurface(dir, color);

                if (dir.z < 0.0) {
                    float depth = clamp(-dir.z * 5.0, 0.0, 1.0);
                    float3 ground = mix(horizon * 0.22 + float3(0.010, 0.010, 0.012), float3(0.008, 0.008, 0.010), depth);
                    float edge = smoothstep(0.0, 0.004, -dir.z);
                    color = mix(color, ground, uGround * edge);
                }
                color += horizon * exp(-abs(dir.z) * 55.0) * 0.10;

                float noise = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);
                color += (noise - 0.5) / 255.0;
                return half4(half3(clamp(color, 0.0, 1.0)), 1.0);
            }
        """.trimIndent()
    }
}
