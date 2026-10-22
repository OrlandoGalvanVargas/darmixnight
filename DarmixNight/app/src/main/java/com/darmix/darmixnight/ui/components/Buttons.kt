package com.darmix.darmixnight.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.theme.DnTheme

private val ButtonShape = RoundedCornerShape(18.dp)

@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = 900f),
        label = "pressScale"
    )
    return scale
}

@Composable
fun DnPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: DnGlyph? = null,
    enabled: Boolean = true
) {
    val colors = DnTheme.colors
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        color = if (enabled) colors.accent else colors.textDim.copy(alpha = 0.2f),
        interactionSource = source,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        ButtonContent(label, glyph, if (enabled) colors.onAccent else colors.textDim)
    }
}

@Composable
fun DnSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: DnGlyph? = null,
    tint: Color = DnTheme.colors.accent
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Surface(
        onClick = onClick,
        shape = ButtonShape,
        color = tint.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        interactionSource = source,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        ButtonContent(label, glyph, tint)
    }
}

@Composable
private fun ButtonContent(label: String, glyph: DnGlyph?, tint: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
        if (glyph != null) {
            Spacer(Modifier.width(8.dp))
            Glyph(glyph, tint, size = 18.dp, strokeWidth = 2f)
        }
    }
}
