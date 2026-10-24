package com.darmix.darmixnight.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.R
import com.darmix.darmixnight.ui.theme.DnTheme

@Composable
fun MoonMark(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val colors = DnTheme.colors
    Image(
        painter = painterResource(R.drawable.dn_panda_avatar),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, (if (colors.isDark) colors.glow else colors.accent).copy(alpha = 0.45f), CircleShape)
    )
}
