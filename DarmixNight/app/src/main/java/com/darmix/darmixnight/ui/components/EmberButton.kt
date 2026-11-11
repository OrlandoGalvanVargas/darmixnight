package com.darmix.darmixnight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.InstrumentSerif

private val EmberLight = Color(0xFFFF9A5C)
private val EmberDeep = Color(0xFFE5582C)

@Composable
internal fun EmberButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = DnTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(EmberLight, EmberDeep)))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = InstrumentSerif, fontSize = 22.sp, lineHeight = 26.sp),
            color = colors.ink
        )
    }
}

@Composable
internal fun QuietLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DnTheme.colors
    Text(
        label,
        style = DnType.note.copy(fontSize = 18.sp, lineHeight = 22.sp),
        color = colors.clay,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .drawBehind {
                val y = size.height - 7.dp.toPx()
                drawLine(
                    colors.clay.copy(alpha = 0.45f),
                    Offset(10.dp.toPx(), y),
                    Offset(size.width - 10.dp.toPx(), y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 10.dp, vertical = 10.dp)
    )
}
