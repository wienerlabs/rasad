package xyz.wienerlabs.rasad.ui.hilal

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import xyz.wienerlabs.rasad.astro.SkySnapshot
import xyz.wienerlabs.rasad.astro.Vec3
import kotlin.math.atan2
import kotlin.math.min

data class MoonPose(val sunX: Float, val sunY: Float, val sunZ: Float, val northAngle: Float)

object MoonPoses {
    fun fromSnapshot(snapshot: SkySnapshot): MoonPose {
        val moon = snapshot.moon.direction.normalized()
        val sun = snapshot.sun.direction.normalized()
        val m = snapshot.eqjToEnu
        val pole = Vec3(m[2], m[5], m[8])
        val zenithProjected = Vec3.Up - moon * (Vec3.Up dot moon)
        val up = if (zenithProjected.length() > 1e-6) zenithProjected.normalized() else (pole - moon * (pole dot moon)).normalized()
        val right = (moon cross up).normalized()
        val toward = -moon
        val north = (pole - moon * (pole dot moon)).normalized()
        return MoonPose(
            sunX = (sun dot right).toFloat(),
            sunY = (sun dot up).toFloat(),
            sunZ = (sun dot toward).toFloat(),
            northAngle = atan2(north dot right, north dot up).toFloat(),
        )
    }
}

@Composable
fun MoonGlobe(texture: Bitmap, pose: MoonPose, modifier: Modifier = Modifier, halo: Boolean = true, gain: Float = 1.28f, shadowAlpha: Float = 1f) {
    val shader = remember(texture) {
        RuntimeShader(SOURCE).apply {
            setInputShader(
                "moonTexture",
                BitmapShader(texture, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP).apply { filterMode = BitmapShader.FILTER_MODE_LINEAR },
            )
            setFloatUniform("uMoonSize", texture.width.toFloat(), texture.height.toFloat())
        }
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    Canvas(modifier) {
        val radius = min(size.width, size.height) / 2f * if (halo) 0.84f else 0.98f
        shader.setFloatUniform("uCenter", size.width / 2f, size.height / 2f)
        shader.setFloatUniform("uRadius", radius)
        shader.setFloatUniform("uSun", pose.sunX, pose.sunY, pose.sunZ)
        shader.setFloatUniform("uNorthAngle", pose.northAngle)
        shader.setFloatUniform("uHalo", if (halo) 1f else 0f)
        shader.setFloatUniform("uGain", gain)
        shader.setFloatUniform("uShadow", shadowAlpha)
        drawRect(brush)
    }
}

private val SOURCE = """
    uniform float2 uCenter;
    uniform float uRadius;
    uniform float3 uSun;
    uniform float uNorthAngle;
    uniform float uHalo;
    uniform float uGain;
    uniform float uShadow;
    uniform float2 uMoonSize;
    uniform shader moonTexture;

    const float PI = 3.14159265;

    half4 main(float2 fragCoord) {
        float2 p = (fragCoord - uCenter) / uRadius;
        p.y = -p.y;
        float r = length(p);
        float litShare = clamp(uSun.z * 0.5 + 0.5, 0.0, 1.0);
        float glow = exp(-(max(r, 1.0) - 1.0) * 9.0) * 0.12 * (0.2 + 0.8 * litShare) * uHalo;
        glow *= 1.0 - smoothstep(1.02, 1.17, r);
        if (r > 1.0 + 0.5 / uRadius) {
            return half4(half3(glow), half(glow));
        }
        float rc = min(r, 1.0);
        float zs = sqrt(max(1.0 - rc * rc, 0.0));
        float3 normal = float3(p.x, p.y, zs);
        float light = smoothstep(-0.035, 0.10, dot(normal, uSun));
        float c = cos(uNorthAngle);
        float s = sin(uNorthAngle);
        float xn = p.x * c - p.y * s;
        float yn = p.x * s + p.y * c;
        float lat = asin(clamp(yn, -1.0, 1.0));
        float lon = atan(xn, zs);
        float2 uv = float2((lon / (2.0 * PI) + 0.5) * uMoonSize.x, (0.5 - lat / PI) * uMoonSize.y);
        float3 albedo = moonTexture.eval(uv).rgb;
        float limb = 0.82 + 0.18 * zs;
        float3 color = albedo * (light * uGain * limb + 0.05 * (1.0 - light));
        float coverage = clamp((1.0 - r) * uRadius + 0.5, 0.0, 1.0);
        float body = coverage * mix(uShadow, 1.0, light);
        float3 blended = color * body + float3(glow) * (1.0 - body);
        float alpha = body + glow * (1.0 - body);
        return half4(half3(blended), half(alpha));
    }
""".trimIndent()
