package com.darmix.darmixnight.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DnGlyph(val pathData: String) {
    ACCESSIBILITY("M12 2.75 A1.75 1.75 0 1 1 12 6.25 A1.75 1.75 0 1 1 12 2.75 Z M4.75 8.25 L12 9.75 L19.25 8.25 M12 9.75 V14 M12 14 L8.75 20.75 M12 14 L15.25 20.75"),
    OVERLAY("M8.5 3.75 H18.25 A2 2 0 0 1 20.25 5.75 V15.5 M5.75 7.75 H14.25 A2 2 0 0 1 16.25 9.75 V18.25 A2 2 0 0 1 14.25 20.25 H5.75 A2 2 0 0 1 3.75 18.25 V9.75 A2 2 0 0 1 5.75 7.75 Z"),
    BATTERY("M5 7 H16.5 A2 2 0 0 1 18.5 9 V15 A2 2 0 0 1 16.5 17 H5 A2 2 0 0 1 3 15 V9 A2 2 0 0 1 5 7 Z M21 10.5 V13.5 M11.75 9.25 L9 12.25 H12.5 L9.75 14.75"),
    BELL("M6.25 16.25 V11 A5.75 5.75 0 0 1 17.75 11 V16.25 L19.25 18 H4.75 Z M10 20.5 A2.1 2.1 0 0 0 14 20.5"),
    ALARM("M12 6.5 A6.75 6.75 0 1 1 12 20 A6.75 6.75 0 1 1 12 6.5 Z M12 10 V13.25 L14.25 14.75 M4 6.25 L6.75 3.75 M20 6.25 L17.25 3.75"),
    SHIELD("M12 3 L19 5.75 V11.25 C19 15.75 16 19.25 12 20.75 C8 19.25 5 15.75 5 11.25 V5.75 Z M12 9.25 A1.6 1.6 0 1 1 12 12.45 A1.6 1.6 0 1 1 12 9.25 Z M12 12.45 V15"),
    POWER("M12 3.5 V11.5 M7.25 6.75 A7 7 0 1 0 16.75 6.75"),
    CHECK("M5.25 12.5 L9.75 17 L18.75 7.5"),
    OPEN("M7.5 16.5 L16.5 7.5 M9.5 7.5 H16.5 V14.5"),
    CHEVRON("M8.5 10 L12 13.5 L15.5 10"),
    LOCK("M8 10.75 V8 A4 4 0 0 1 16 8 V10.75 M6.5 10.75 H17.5 A1.75 1.75 0 0 1 19.25 12.5 V18.75 A1.75 1.75 0 0 1 17.5 20.5 H6.5 A1.75 1.75 0 0 1 4.75 18.75 V12.5 A1.75 1.75 0 0 1 6.5 10.75 Z M12 14.25 V17"),
    SUN("M12 8 A4 4 0 1 1 12 16 A4 4 0 1 1 12 8 Z M12 2.75 V4.75 M12 19.25 V21.25 M2.75 12 H4.75 M19.25 12 H21.25 M5.45 5.45 L6.85 6.85 M17.15 17.15 L18.55 18.55 M5.45 18.55 L6.85 17.15 M17.15 6.85 L18.55 5.45"),
    MOON("M19.5 14.75 A8 8 0 1 1 9.25 4.5 A6.25 6.25 0 0 0 19.5 14.75 Z"),
    PLUS("M12 6 V18 M6 12 H18"),
    MINUS("M6 12 H18"),
    PHONE("M8.5 3.5 H15.5 A1.75 1.75 0 0 1 17.25 5.25 V18.75 A1.75 1.75 0 0 1 15.5 20.5 H8.5 A1.75 1.75 0 0 1 6.75 18.75 V5.25 A1.75 1.75 0 0 1 8.5 3.5 Z M10.75 17.5 H13.25"),
    HOURGLASS("M7 3.75 H17 M7 20.25 H17 M8 3.75 C8 8.5 12 9.5 12 12 C12 14.5 8 15.5 8 20.25 M16 3.75 C16 8.5 12 9.5 12 12 C12 14.5 16 15.5 16 20.25"),
    HOLD("M12 9 A3 3 0 1 1 12 15 A3 3 0 1 1 12 9 Z M4.5 12 A7.5 7.5 0 1 1 19.5 12 A7.5 7.5 0 1 1 4.5 12 Z"),
    KEY("M8 8.5 A3.5 3.5 0 1 1 8 15.5 A3.5 3.5 0 1 1 8 8.5 Z M11.5 12 H20.25 M17.25 12 V15 M20.25 12 V14.25"),
    INFO("M12 3.75 A8.25 8.25 0 1 1 12 20.25 A8.25 8.25 0 1 1 12 3.75 Z M12 11 V16 M12 8 V8.1");

    internal val path: Path by lazy { PathParser().parsePathString(pathData).toPath() }
}

private const val GLYPH_VIEWPORT = 24f

@Composable
fun Glyph(
    glyph: DnGlyph,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    strokeWidth: Float = 1.7f
) {
    Spacer(
        modifier
            .size(size)
            .drawWithCache {
                val factor = this.size.minDimension / GLYPH_VIEWPORT
                val stroke = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                onDrawBehind {
                    scale(factor, factor, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                        drawPath(glyph.path, tint, style = stroke)
                    }
                }
            }
    )
}
