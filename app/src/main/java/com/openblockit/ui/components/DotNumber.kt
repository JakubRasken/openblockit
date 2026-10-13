package com.openblockit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.openblockit.ui.theme.LocalPal

private val GLYPHS: Map<Char, List<String>> = mapOf(
    '0' to listOf("0110", "1001", "1001", "1001", "1001", "0110"),
    '1' to listOf("0000", "0010", "0110", "1010", "0010", "0010"),
    '2' to listOf("0000", "1110", "0001", "0110", "1000", "1111"),
    '3' to listOf("0000", "1110", "0001", "0111", "0001", "1110"),
    '4' to listOf("0000", "1001", "1001", "0111", "0001", "0001"),
    '5' to listOf("0000", "1111", "1000", "1110", "0001", "1110"),
    '6' to listOf("0000", "0110", "1000", "1110", "1001", "0110"),
    '7' to listOf("0000", "1110", "0001", "0001", "0001", "0001"),
    '8' to listOf("0000", "0110", "1001", "0110", "1001", "0110"),
    '9' to listOf("0000", "1110", "1001", "1111", "0001", "1110"),
    ':' to listOf("0", "0", "1", "0", "1", "0"),
    '.' to listOf("0", "0", "0", "0", "0", "1"),
    '-' to listOf("000", "000", "000", "111", "000", "000"),
    ' ' to listOf("00", "00", "00", "00", "00", "00"),
    'A' to listOf("0000", "0110", "1001", "1111", "1001", "1001"),
    'B' to listOf("0000", "1110", "1001", "1110", "1001", "1110"),
    'C' to listOf("0000", "0111", "1000", "1000", "1000", "0111"),
    'D' to listOf("0000", "1110", "1001", "1001", "1001", "1110"),
    'E' to listOf("0000", "0111", "1000", "1110", "1000", "0111"),
    'F' to listOf("0000", "1111", "1000", "1110", "1000", "1000"),
    'G' to listOf("0000", "0111", "1000", "1011", "1001", "0111"),
    'H' to listOf("0000", "1001", "1001", "1111", "1001", "1001"),
    'I' to listOf("000", "111", "010", "010", "010", "111"),
    'J' to listOf("0000", "0011", "0001", "0001", "1001", "0110"),
    'K' to listOf("0000", "1001", "1010", "1100", "1010", "1001"),
    'L' to listOf("0000", "1000", "1000", "1000", "1000", "0111"),
    'M' to listOf("00000", "01010", "10101", "10101", "10101", "10101"),
    'N' to listOf("00000", "10001", "11001", "10101", "10011", "10001"),
    'O' to listOf("0000", "0110", "1001", "1001", "1001", "0110"),
    'P' to listOf("0000", "1110", "1001", "1110", "1000", "1000"),
    'Q' to listOf("0000", "0110", "1001", "1001", "1010", "0101"),
    'R' to listOf("0000", "1110", "1001", "1110", "1010", "1001"),
    'S' to listOf("0000", "0111", "1000", "0110", "0001", "1110"),
    'T' to listOf("000", "111", "010", "010", "010", "010"),
    'U' to listOf("0000", "1001", "1001", "1001", "1001", "0110"),
    'V' to listOf("000", "101", "101", "101", "101", "010"),
    'W' to listOf("00000", "10001", "10101", "10101", "10101", "01010"),
    'X' to listOf("0000", "1001", "1001", "0110", "1001", "1001"),
    'Y' to listOf("0000", "1001", "1001", "0111", "0001", "0110"),
    'Z' to listOf("0000", "1111", "0001", "0110", "1000", "1111"),
)

@Composable
fun DotText(
    text: String,
    tileSize: Dp,
    gap: Dp,
    color: Color? = null,
    modifier: Modifier = Modifier,
    letterGap: Dp = gap * 2,
) {
    val lit = color ?: LocalPal.current.timerTile
    Row(modifier = modifier) {
        text.uppercase().forEachIndexed { index, ch ->
            val glyph = GLYPHS[ch] ?: GLYPHS[' ']!!
            TileGlyph(glyph, tileSize, gap, lit)
            if (index != text.lastIndex) Spacer(Modifier.width(letterGap))
        }
    }
}

@Composable
fun DotNumber(
    value: String,
    tileSize: Dp,
    gap: Dp,
    color: Color? = null,
    dimColor: Color? = null,
    modifier: Modifier = Modifier,
    letterGap: Dp = gap * 2,
) {
    val lit = color ?: LocalPal.current.timerTile
    val dim = dimColor ?: lit.copy(alpha = 0.7f)
    Row(modifier = modifier) {
        value.uppercase().forEachIndexed { index, ch ->
            val glyph = GLYPHS[ch] ?: GLYPHS[' ']!!
            val chColor = if (ch == ':' || ch == '.') dim else lit
            TileGlyph(glyph, tileSize, gap, chColor)
            if (index != value.lastIndex) Spacer(Modifier.width(letterGap))
        }
    }
}

@Composable
private fun TileGlyph(glyph: List<String>, tileSize: Dp, gap: Dp, color: Color) {
    val columns = glyph.maxOf { it.length }
    val rows = glyph.size
    Box(
        modifier = Modifier
            .width(tileSize * columns + gap * (columns - 1))
            .height(tileSize * rows + gap * (rows - 1)),
    ) {
        Canvas(modifier = Modifier.width(tileSize * columns + gap * (columns - 1)).height(tileSize * rows + gap * (rows - 1))) {
            val ts = tileSize.toPx()
            val g = gap.toPx()
            glyph.forEachIndexed { r, row ->
                row.forEachIndexed { c, bit ->
                    if (bit == '1') {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(c * (ts + g), r * (ts + g)),
                            size = Size(ts, ts),
                            cornerRadius = CornerRadius(ts * 0.12f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SquareDot(size: Dp, color: Color? = null) {
    val c = color ?: LocalPal.current.timerTile
    Canvas(modifier = Modifier.width(size).height(size)) {
        drawRoundRect(color = c, cornerRadius = CornerRadius(size.toPx() * 0.15f))
    }
}
