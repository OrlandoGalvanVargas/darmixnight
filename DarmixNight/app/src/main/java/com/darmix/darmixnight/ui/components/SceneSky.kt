package com.darmix.darmixnight.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

internal val SceneStarLight = Color(0xFFFFEEDD)

internal class SceneSway(val area: Rect, val amplitude: Float, val speed: Float = 1f)

internal class ShootingSky(
    val paths: Array<Pair<Offset, Offset>>,
    val period: Float,
    val start: Float,
    val length: Float,
    val width: Float
)

private const val SWAY_SHADER = """
uniform shader content;
uniform float time;
uniform float unit;
uniform float strength;
uniform float amplitude;
uniform float speed;
uniform float4 area;

half4 main(float2 p) {
    if (strength <= 0.0 || p.x < area.x || p.x > area.z || p.y < area.y || p.y > area.w) {
        return content.eval(p);
    }
    float rise = clamp((area.w - p.y) / (area.w - area.y), 0.0, 1.0);
    float hw = (area.z - area.x) * 0.5;
    float taper = smoothstep(0.0, 1.0, clamp(1.0 - abs(p.x - area.x - hw) / hw, 0.0, 1.0));
    float y = p.y / unit;
    float x = p.x / unit;
    float s = time * speed;
    float wave = sin(s * 1.9 + y * 0.016) * 0.7 + sin(s * 3.1 + 1.3 + y * 0.03) * 0.3;
    float flutter = sin(s * 4.2 + y * 0.07 + x * 0.04) * 0.12 * rise;
    float dx = amplitude * unit * strength * pow(rise, 1.5) * taper * (wave + flutter);
    float dy = amplitude * 0.12 * unit * strength * rise * taper * sin(s * 2.4 + x * 0.04);
    return content.eval(float2(p.x - dx, p.y - dy));
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createSwayShader(): Any = RuntimeShader(SWAY_SHADER)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun swayEffect(shader: Any, sway: SceneSway, time: Float, unit: Float, strength: Float): RenderEffect {
    val runtime = shader as RuntimeShader
    runtime.setFloatUniform("time", time)
    runtime.setFloatUniform("unit", unit)
    runtime.setFloatUniform("strength", strength)
    runtime.setFloatUniform("amplitude", sway.amplitude)
    runtime.setFloatUniform("speed", sway.speed)
    runtime.setFloatUniform(
        "area",
        sway.area.left * unit,
        sway.area.top * unit,
        sway.area.right * unit,
        sway.area.bottom * unit
    )
    return android.graphics.RenderEffect.createRuntimeShaderEffect(runtime, "content").asComposeRenderEffect()
}

@Composable
internal fun rememberSwayShader(): Any? =
    remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) createSwayShader() else null }

internal fun GraphicsLayerScope.applySway(shader: Any?, sway: SceneSway, time: () -> Float, roomWidth: Float, strength: Float) {
    renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null && strength > 0f) {
        swayEffect(shader, sway, time(), size.width / roomWidth, strength)
    } else {
        null
    }
}

internal fun DrawScope.drawTwinkles(glow: Brush, stars: FloatArray, t: Float, unit: Float, alive: Float, size: Float) {
    val count = stars.size / 2
    for (i in 0 until count) {
        val speed = 0.9f + (i % 3) * 0.8f
        val phase = i * 2.399f
        val pulse = max(0f, sin(t * speed + phase))
        val level = pulse * pulse * pulse * 0.75f * alive
        if (level < 0.02f) continue
        val center = Offset(stars[i * 2], stars[i * 2 + 1]) * unit
        val radius = (size + (i % 2) * size * 0.3f) * unit
        drawGlow(glow, center, radius, radius, level)
    }
}

internal fun DrawScope.drawShootingStar(sky: ShootingSky, t: Float, unit: Float, alive: Float) {
    val cycle = (t / sky.period).toInt()
    val local = (t % sky.period) - sky.start
    if (local < 0f || local > sky.length) return
    val (from, to) = sky.paths[(cycle * 3) % sky.paths.size]
    val u = local / sky.length
    val eased = 1f - (1f - u) * (1f - u)
    val head = lerpOffset(from, to, eased) * unit
    val tailReach = 0.34f * (u * 3f).coerceAtMost(1f)
    val tail = lerpOffset(from, to, (eased - tailReach).coerceAtLeast(0f)) * unit
    drawLine(
        Brush.linearGradient(
            0f to SceneStarLight.copy(alpha = 0f),
            1f to SceneStarLight.copy(alpha = 0.85f),
            start = tail,
            end = head
        ),
        start = tail,
        end = head,
        strokeWidth = sky.width * unit,
        cap = StrokeCap.Round,
        alpha = sin(PI.toFloat() * u) * alive,
        blendMode = BlendMode.Screen
    )
}

private fun lerpOffset(a: Offset, b: Offset, f: Float): Offset = Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
