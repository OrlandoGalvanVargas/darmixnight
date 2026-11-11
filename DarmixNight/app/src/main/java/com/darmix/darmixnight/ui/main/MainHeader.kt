package com.darmix.darmixnight.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType

@Composable
internal fun MainHeader(modifier: Modifier = Modifier) {
    val colors = DnTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = "DarmixNight"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.height(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Darmix",
                style = DnType.brand,
                color = colors.text,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                "Night",
                style = DnType.brandAccent,
                color = colors.ember,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 1.dp)
            )
        }
        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(
                Modifier
                    .width(64.dp)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(colors.ember.copy(alpha = 0f), colors.ember.copy(alpha = 0.5f))))
            )
            Glyph(DnGlyph.MOON, colors.ember, size = 16.dp, strokeWidth = 1.8f)
            Spacer(
                Modifier
                    .width(64.dp)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(colors.ember.copy(alpha = 0.5f), colors.ember.copy(alpha = 0f))))
            )
        }
    }
}
