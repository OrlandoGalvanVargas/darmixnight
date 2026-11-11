package com.darmix.darmixnight.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.EmberButton
import com.darmix.darmixnight.ui.components.QuietLink
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.InstrumentSerif
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ItemHeight = 46.dp
private const val VISIBLE_ITEMS = 5

private val WheelStyle = TextStyle(
    fontFamily = InstrumentSerif,
    fontSize = 30.sp,
    lineHeight = 34.sp
)

private val Hours = (1..12).map { it.toString() }
private val Minutes = (0..59).map { "%02d".format(it) }
private val Periods = listOf("am", "pm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeWheelSheet(
    handle: ScheduleEdge,
    initialMinutes: Int,
    otherMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val colors = DnTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val h24 = (initialMinutes / 60) % 24
    var hourIndex by remember { mutableIntStateOf((if (h24 % 12 == 0) 12 else h24 % 12) - 1) }
    var minuteIndex by remember { mutableIntStateOf(initialMinutes % 60) }
    var periodIndex by remember { mutableIntStateOf(if (h24 < 12) 0 else 1) }

    val picked = ((hourIndex + 1) % 12 + periodIndex * 12) * 60 + minuteIndex
    val isStart = handle == ScheduleEdge.START
    val duration = if (isStart) TimeText.between(picked, otherMinutes) else TimeText.between(otherMinutes, picked)
    val invalid = picked == otherMinutes

    fun close(after: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { after() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        contentColor = colors.text,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(colors.hairline, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
        ) {
            val tone = if (isStart) colors.ember else colors.amber
            Row(verticalAlignment = Alignment.CenterVertically) {
                Glyph(
                    if (isStart) DnGlyph.MOON else DnGlyph.SUN,
                    tone,
                    size = 18.dp,
                    strokeWidth = 2f
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (isStart) "Me duermo a las" else "Despierto a las",
                    style = DnType.lead.copy(fontSize = 22.sp),
                    color = tone
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                buildAnnotatedString {
                    append(TimeText.hour(picked))
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontSize = 22.sp, color = tone)) {
                        append("\u2009" + TimeText.period(picked).lowercase())
                    }
                },
                style = DnType.timeLarge.copy(fontSize = 56.sp, lineHeight = 56.sp),
                color = colors.text
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ItemHeight * VISIBLE_ITEMS),
                contentAlignment = Alignment.Center
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.hairline)
                    )
                    Spacer(Modifier.height(ItemHeight - 2.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.hairline)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Wheel(Hours, hourIndex, width = 76.dp) { hourIndex = it }
                    Text(":", style = WheelStyle.copy(fontSize = 30.sp), color = colors.textDim)
                    Wheel(Minutes, minuteIndex, width = 76.dp) { minuteIndex = it }
                    Spacer(Modifier.width(12.dp))
                    Wheel(Periods, periodIndex, width = 64.dp) { periodIndex = it }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(ItemHeight * VISIBLE_ITEMS)
                        .background(
                            Brush.verticalGradient(
                                0f to colors.card,
                                0.3f to colors.card.copy(alpha = 0f),
                                0.7f to colors.card.copy(alpha = 0f),
                                1f to colors.card
                            )
                        )
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = if (invalid) "El inicio y el fin no pueden ser la misma hora."
                else "Descanso de ${TimeText.span(duration)}",
                style = DnType.note.copy(fontSize = 19.sp),
                color = if (invalid) colors.danger else colors.textDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))

            EmberButton(
                label = "Guardar",
                enabled = !invalid,
                onClick = { close { onConfirm(picked) } }
            )
            Spacer(Modifier.height(4.dp))
            QuietLink(
                label = "Cancelar",
                onClick = { close(onDismiss) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun Wheel(
    items: List<String>,
    initialIndex: Int,
    width: Dp,
    onSelected: (Int) -> Unit
) {
    val colors = DnTheme.colors
    val haptics = LocalHapticFeedback.current
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val fling = rememberSnapFlingBehavior(state)

    val selected by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: initialIndex
        }
    }

    var reported by remember { mutableIntStateOf(initialIndex) }
    LaunchedEffect(selected) {
        if (selected != reported) {
            reported = selected
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelected(selected)
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = ItemHeight * (VISIBLE_ITEMS / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width)
            .height(ItemHeight * VISIBLE_ITEMS)
    ) {
        itemsIndexed(items) { index, label ->
            val distance = abs(index - selected)
            Box(
                modifier = Modifier
                    .size(width, ItemHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = WheelStyle.copy(
                        fontSize = if (distance == 0) 30.sp else 22.sp,
                        fontStyle = if (items === Periods) FontStyle.Italic else FontStyle.Normal
                    ),
                    color = when (distance) {
                        0 -> colors.text
                        1 -> colors.clayDim
                        else -> colors.textMute.copy(alpha = 0.6f)
                    }
                )
            }
        }
    }
}
