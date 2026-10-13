package com.openblockit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openblockit.core.Haptic
import com.openblockit.ui.theme.LocalPal
import kotlin.math.roundToInt

data class Corners(
    val topStart: Dp = 0.dp,
    val topEnd: Dp = 0.dp,
    val bottomStart: Dp = 0.dp,
    val bottomEnd: Dp = 0.dp,
) {
    fun shape() = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
}

@Composable
fun SlideToStart(
    text: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    knobIcon: Boolean = true,
) {
    val context = LocalContext.current
    val pal = LocalPal.current
    val density = LocalDensity.current
    var offset by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(0f) }
    val knobSize = 60.dp
    val knobPx = with(density) { knobSize.toPx() }
    val maxOffset = (width - knobPx - 12f).coerceAtLeast(1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(pal.barSurface)
            .pointerInput(enabled, width) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offset >= maxOffset - 10f) {
                            Haptic.tick(context, 30)
                            offset = 0f
                            onComplete()
                        } else {
                            offset = 0f
                        }
                    },
                    onHorizontalDrag = { change, drag ->
                        change.consume()
                        offset = (offset + drag).coerceIn(0f, maxOffset)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 80.dp),
            textAlign = TextAlign.Center,
            color = if (enabled) pal.onSurface else pal.tileMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .offset(x = with(density) { offset.toDp() }),
        ) {
            Box(
                modifier = Modifier
                    .size(knobSize)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (enabled) pal.primary else pal.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (knobIcon) IconPlay(color = pal.onPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun RailWheel(
    value: Int,
    minValue: Int,
    maxValue: Int,
    step: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pal = LocalPal.current
    val density = LocalDensity.current
    val lineSpacing = 14.dp
    val lineSpacingPx = with(density) { lineSpacing.toPx() }
    var accumulated by remember { mutableFloatStateOf(0f) }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.outline.copy(alpha = 0.6f)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(value) {
                    detectVerticalDragGestures(
                        onDragStart = { accumulated = 0f },
                        onVerticalDrag = { change, drag ->
                            change.consume()
                            accumulated += -drag
                            while (accumulated >= lineSpacingPx) {
                                accumulated -= lineSpacingPx
                                val next = (value + step).coerceAtMost(maxValue)
                                if (next != value) {
                                    onChange(next)
                                    Haptic.tick(context, 6)
                                }
                            }
                            while (accumulated <= -lineSpacingPx) {
                                accumulated += lineSpacingPx
                                val next = (value - step).coerceAtLeast(minValue)
                                if (next != value) {
                                    onChange(next)
                                    Haptic.tick(context, 6)
                                }
                            }
                        },
                    )
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sp = lineSpacing.toPx()
                val scroll = ((value - minValue) / step.toFloat()) * sp
                val scrollOffset = scroll.mod(sp)
                val lineW = size.width * 0.62f
                var y = size.height / 2f - scrollOffset
                while (y > -sp) {
                    if (y in 4f..(size.height - 4f)) {
                        drawLine(
                            color = pal.onSurface.copy(alpha = 0.4f),
                            start = Offset((size.width - lineW) / 2f, y),
                            end = Offset((size.width + lineW) / 2f, y),
                            strokeWidth = 1.5f,
                        )
                    }
                    y -= sp
                }
                drawRoundRect(
                    color = pal.primaryContainer,
                    topLeft = Offset(size.width * 0.22f, size.height / 2f - 3.dp.toPx()),
                    size = Size(size.width * 0.56f, 6.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
    }
}

@Composable
fun RailArrows(
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.outline.copy(alpha = 0.6f)),
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { onUp() },
                contentAlignment = Alignment.Center,
            ) {
                IconChevron(up = true, color = pal.onSurfaceVariant, modifier = Modifier.size(26.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(pal.outline.copy(alpha = 0.5f)))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { onDown() },
                contentAlignment = Alignment.Center,
            ) {
                IconChevron(up = false, color = pal.onSurfaceVariant, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
fun RulerBar(
    fraction: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.outline.copy(alpha = 0.6f)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, drag ->
                        change.consume()
                        val delta = drag / size.width
                        onChange((fraction + delta).coerceIn(0f, 1f))
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val spacing = size.width / 28f
                val h = size.height
                var x = spacing / 2f
                while (x < size.width) {
                    drawLine(
                        color = pal.onSurface.copy(alpha = 0.35f),
                        start = Offset(x, h * 0.22f),
                        end = Offset(x, h * 0.78f),
                        strokeWidth = 1.5f,
                    )
                    x += spacing
                }
                val mx = (size.width * fraction).coerceIn(4f, size.width - 4f)
                drawLine(
                    color = pal.primaryContainer,
                    start = Offset(mx, h * 0.12f),
                    end = Offset(mx, h * 0.88f),
                    strokeWidth = 3.dp.toPx(),
                )
            }
        }
    }
}

@Composable
fun DashedRuler(
    marker: Float,
    modifier: Modifier = Modifier,
    segments: Int = 32,
    color: Color? = null,
    markerColor: Color? = null,
) {
    val pal = LocalPal.current
    val base = color ?: pal.onSurface.copy(alpha = 0.55f)
    val hot = markerColor ?: pal.primaryContainer
    Canvas(modifier = modifier) {
        val gap = 3.dp.toPx()
        val segW = (size.width - gap * (segments - 1)) / segments
        val h = size.height
        val markerIndex = (marker * (segments - 1)).roundToInt().coerceIn(0, segments - 1)
        for (i in 0 until segments) {
            val x = i * (segW + gap)
            drawRoundRect(
                color = if (i == markerIndex) hot else base,
                topLeft = Offset(x, (h - 5.dp.toPx()) / 2f),
                size = Size(segW, 5.dp.toPx()),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
            )
        }
    }
}

@Composable
fun DialGraph(
    value: Float,
    maxValue: Float,
    modifier: Modifier = Modifier,
    ticks: Int = 60,
    accent: Color? = null,
) {
    val pal = LocalPal.current
    val hot = accent ?: pal.primaryContainer
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) * 0.44f
        val ringColor = pal.onSurface.copy(alpha = 0.35f)
        for (i in 0 until ticks) {
            val angle = (i.toFloat() / ticks) * (2 * Math.PI) - Math.PI / 2
            val long = i % 5 == 0
            val inner = radius - if (long) radius * 0.18f else radius * 0.10f
            val outer = radius
            val sx = center.x + (inner * kotlin.math.cos(angle)).toFloat()
            val sy = center.y + (inner * kotlin.math.sin(angle)).toFloat()
            val ex = center.x + (outer * kotlin.math.cos(angle)).toFloat()
            val ey = center.y + (outer * kotlin.math.sin(angle)).toFloat()
            drawLine(
                color = ringColor,
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = if (long) 2.5f else 1.5f,
            )
        }
        val ratio = if (maxValue > 0f) (value / maxValue).coerceIn(0f, 1f) else 0f
        val angle = ratio * (2 * Math.PI) - Math.PI / 2
        val dotRadius = radius * 0.82f
        val dx = center.x + (dotRadius * kotlin.math.cos(angle)).toFloat()
        val dy = center.y + (dotRadius * kotlin.math.sin(angle)).toFloat()
        drawCircle(color = hot, radius = 9.dp.toPx(), center = Offset(dx, dy))
    }
}

@Composable
fun PresetCard(
    corners: Corners,
    modifier: Modifier = Modifier,
    container: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val pal = LocalPal.current
    Surface(
        color = container ?: pal.surfaceVariant,
        shape = corners.shape(),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun MutedLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text,
        modifier = modifier,
        color = color ?: LocalPal.current.tileMuted,
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
fun ChuteToken(
    size: Dp = 40.dp,
    background: Color? = null,
    iconColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(background ?: pal.onSurface),
        contentAlignment = Alignment.Center,
    ) {
        IconParachute(
            color = iconColor ?: pal.background,
            modifier = Modifier.size(size * 0.6f).then(Modifier),
        )
    }
}

@Composable
fun NavPill(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    Surface(
        color = pal.barSurface,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.height(64.dp),
    ) {
        Row(
            modifier = Modifier.wrapContentWidth().fillMaxHeight().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (i in 0 until 4) {
                val active = i == selected
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) pal.primaryContainer else Color.Transparent)
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    val tint = if (active) pal.onPrimaryContainer else pal.primaryContainer
                    NavIcon(index = i, color = tint, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun NavIcon(index: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        when (index) {
            0 -> {
                drawCircle(color = color, radius = size.minDimension * 0.32f, style = Stroke(stroke))
                drawLine(color, Offset(size.width / 2f, size.height * 0.18f), Offset(size.width / 2f, size.height * 0.34f), stroke)
                drawLine(color, Offset(size.width * 0.34f, size.height * 0.2f), Offset(size.width * 0.66f, size.height * 0.2f), stroke)
                drawLine(color, Offset(size.width / 2f, size.height / 2f), Offset(size.width / 2f, size.height * 0.32f), stroke)
            }
            1 -> {
                val body = size.width * 0.72f
                val left = (size.width - body) / 2f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, size.height * 0.32f),
                    size = Size(body, size.height * 0.5f),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(stroke),
                )
                drawLine(color, Offset(size.width * 0.38f, size.height * 0.32f), Offset(size.width * 0.38f, size.height * 0.2f), stroke)
                drawLine(color, Offset(size.width * 0.62f, size.height * 0.32f), Offset(size.width * 0.62f, size.height * 0.2f), stroke)
                drawLine(color, Offset(size.width * 0.38f, size.height * 0.2f), Offset(size.width * 0.62f, size.height * 0.2f), stroke)
            }
            2 -> {
                val s = size.width * 0.32f
                val gap = size.width * 0.1f
                val start = (size.width - (s * 2 + gap)) / 2f
                drawRoundRect(color, Offset(start, start), Size(s, s), CornerRadius(3.dp.toPx()))
                drawRoundRect(color, Offset(start + s + gap, start), Size(s, s), CornerRadius(3.dp.toPx()))
                drawRoundRect(color, Offset(start, start + s + gap), Size(s, s), CornerRadius(3.dp.toPx()))
                drawRoundRect(color, Offset(start + s + gap, start + s + gap), Size(s, s), CornerRadius(3.dp.toPx()))
            }
            else -> {
                val barW = size.width * 0.16f
                val heights = listOf(0.45f, 0.75f, 0.6f, 0.9f)
                heights.forEachIndexed { i, hf ->
                    val h = size.height * hf
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(size.width * 0.12f + i * (barW + size.width * 0.06f), size.height - h - size.height * 0.1f),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2f),
                    )
                }
            }
        }
    }
}

@Composable
private fun IconChevron(up: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path()
        if (up) {
            path.moveTo(size.width * 0.15f, size.height * 0.62f)
            path.lineTo(size.width * 0.5f, size.height * 0.32f)
            path.lineTo(size.width * 0.85f, size.height * 0.62f)
        } else {
            path.moveTo(size.width * 0.15f, size.height * 0.38f)
            path.lineTo(size.width * 0.5f, size.height * 0.68f)
            path.lineTo(size.width * 0.85f, size.height * 0.38f)
        }
        drawPath(path, color = color, style = Stroke(width = 2.5.dp.toPx()))
    }
}

@Composable
fun IconPlay(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path()
        path.moveTo(size.width * 0.28f, size.height * 0.2f)
        path.lineTo(size.width * 0.8f, size.height * 0.5f)
        path.lineTo(size.width * 0.28f, size.height * 0.8f)
        path.close()
        drawPath(path, color = color)
    }
}

@Composable
fun IconParachute(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dome = Path().apply {
            moveTo(w * 0.06f, h * 0.52f)
            cubicTo(w * 0.06f, h * 0.06f, w * 0.94f, h * 0.06f, w * 0.94f, h * 0.52f)
            close()
        }
        drawPath(dome, color = color)
        drawLine(color, Offset(w * 0.06f, h * 0.52f), Offset(w * 0.5f, h * 0.9f), 1.5.dp.toPx())
        drawLine(color, Offset(w * 0.94f, h * 0.52f), Offset(w * 0.5f, h * 0.9f), 1.5.dp.toPx())
    }
}

@Composable
fun IconLock(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * 0.16f,
                    top = h * 0.42f,
                    right = w * 0.84f,
                    bottom = h * 0.92f,
                    cornerRadius = CornerRadius(w * 0.12f),
                )
            )
        }
        drawPath(body, color = color)
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.3f, h * 0.12f),
            size = Size(w * 0.4f, h * 0.5f),
            style = Stroke(width = w * 0.14f),
        )
    }
}

fun formatWheelValue(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

@Composable
fun StatRow(label: String, value: String, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = pal.tileMuted, fontSize = 13.sp)
        Text(value, color = pal.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TowerGraph(
    streak: Int,
    modifier: Modifier = Modifier,
    maxCells: Int = 42,
) {
    val pal = LocalPal.current
    Canvas(modifier = modifier) {
        val cells = maxCells.coerceAtLeast((streak / 7 + 1) * 7)
        val perRow = 7
        val rows = (cells + perRow - 1) / perRow
        val gap = size.width * 0.014f
        val cellW = (size.width - gap * (perRow - 1)) / perRow
        val cellH = (size.height - gap * (rows - 1)) / rows
        for (i in 0 until cells) {
            val row = rows - 1 - i / perRow
            val col = i % perRow
            val x = col * (cellW + gap)
            val y = row * (cellH + gap)
            drawRoundRect(
                color = when {
                    i < streak -> pal.primaryContainer
                    i == streak -> pal.onSurface
                    else -> pal.surfaceBright.copy(alpha = 0.25f)
                },
                topLeft = Offset(x, y),
                size = Size(cellW, cellH),
                cornerRadius = CornerRadius(cellW * 0.18f),
            )
        }
    }
}

fun wheelClock(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}
