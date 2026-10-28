package com.dpis.module.onboarding.presentation

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import android.graphics.Canvas as AndroidCanvas

private data class BrandParticle(
    val position: Offset,
    val phase: Float,
    val textParticle: Boolean,
    val normalizedX: Float,
    val normalizedY: Float,
    val radialFieldA: Float,
    val radialFieldB: Float,
)

private data class GridCell(
    val row: Int,
    val column: Int,
    val position: Offset,
    val textParticle: Boolean,
)

private val TypographyDustPoints = listOf(
    -0.05f to 0.12f,
    0.08f to -0.05f,
    0.32f to -0.07f,
    0.68f to -0.05f,
    1.05f to 0.12f,
    -0.08f to 0.38f,
    0.06f to 0.34f,
    0.94f to 0.36f,
    1.08f to 0.48f,
    -0.06f to 0.64f,
    0.12f to 0.72f,
    0.86f to 0.68f,
    1.05f to 0.62f,
    -0.04f to 0.88f,
    0.18f to 1.06f,
    0.42f to 0.96f,
    0.72f to 1.05f,
    1.04f to 0.90f,
)

@Composable
internal fun BrandMark(
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var touchTarget by remember { mutableStateOf<Offset?>(null) }
    var touchRenderPoint by remember { mutableStateOf(Offset.Zero) }
    var touchStrength by remember { mutableFloatStateOf(0f) }
    var frameNanos by remember { mutableLongStateOf(0L) }
    val densityScale = LocalDensity.current.density
    val particles = remember(canvasSize, densityScale) {
        if (canvasSize == IntSize.Zero) {
            emptyList()
        } else {
            createBrandParticles(
                width = canvasSize.width,
                height = canvasSize.height,
                spacing = 2.8f * densityScale,
            )
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos {
                frameNanos = it
                touchTarget?.let { target ->
                    touchRenderPoint = Offset(
                        x = touchRenderPoint.x + (target.x - touchRenderPoint.x) * 0.22f,
                        y = touchRenderPoint.y + (target.y - touchRenderPoint.y) * 0.22f,
                    )
                }
                val targetStrength = if (touchTarget == null) 0f else 1f
                touchStrength += (targetStrength - touchStrength) * 0.18f
            }
        }
    }

    val particleColor = MaterialTheme.colorScheme.onSurface
    val glowColor = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .fillMaxWidth()
            .height(132.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchTarget = down.position
                    do {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull()?.let { change ->
                            touchTarget = change.position
                        }
                    } while (event.changes.any { it.pressed })
                    touchTarget = null
                }
            }
            .testTag("onboarding-brand-mark"),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it },
        ) {
            val time = frameNanos / 1_000_000_000f
            particles.forEach { particle ->
                val touchDx = touchRenderPoint.x - particle.position.x
                val touchDy = touchRenderPoint.y - particle.position.y
                val touchDistance = sqrt(touchDx * touchDx + touchDy * touchDy)
                val touchInfluence = (
                        1f - touchDistance / 68.dp.toPx()
                        ).coerceIn(0f, 1f) * touchStrength
                val pulse = irregularPulse(
                    time = time,
                    phase = particle.phase,
                    x = particle.normalizedX,
                    y = particle.normalizedY,
                    radialFieldA = particle.radialFieldA,
                    radialFieldB = particle.radialFieldB,
                )
                val baseRadius = if (particle.textParticle) 0.9.dp.toPx() else 0.34.dp.toPx()
                val radius = baseRadius * (0.62f + pulse * 0.58f) +
                        touchInfluence * if (particle.textParticle) 0.55.dp.toPx() else 0.28.dp.toPx()
                val alpha = if (particle.textParticle) {
                    0.76f + pulse * 0.16f + touchInfluence * 0.05f
                } else {
                    0.1f + pulse * 0.16f + touchInfluence * 0.08f
                }

                drawCircle(
                    color = glowColor.copy(alpha = alpha * 0.12f),
                    radius = radius * 1.9f,
                    center = particle.position,
                )
                drawCircle(
                    color = particleColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                    radius = radius,
                    center = particle.position,
                )
            }
        }
    }
}

private fun irregularPulse(
    time: Float,
    phase: Float,
    x: Float,
    y: Float,
    radialFieldA: Float,
    radialFieldB: Float,
): Float {
    val value =
        sin(time * 1.21f - radialFieldA + phase) * 0.34f +
                sin(time * 1.83f - radialFieldB + phase * 1.71f) * 0.27f +
                sin(time * 2.67f + x * 17f + y * 11f + phase * 2.43f) * 0.23f +
                sin(time * 0.79f + x * 7f - y * 13f + phase * 3.17f) * 0.16f
    return ((value + 1f) / 2f).coerceIn(0f, 1f)
}

private fun createBrandParticles(
    width: Int,
    height: Int,
    spacing: Float,
): List<BrandParticle> {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textSize = min(height * 0.9f, width * 0.27f)
    }
    val text = "DPIS"
    val textWidth = paint.measureText(text)
    val metrics = paint.fontMetrics
    val baseline = (height - metrics.top - metrics.bottom) / 2f
    canvas.drawText(text, (width - textWidth) / 2f, baseline, paint)

    val gridCells = ArrayList<GridCell>()
    var row = 0
    var y = spacing / 2f
    while (y < height) {
        var column = 0
        var x = spacing / 2f
        while (x < width) {
            val pixel = bitmap.getPixel(
                x.toInt().coerceIn(0, width - 1),
                y.toInt().coerceIn(0, height - 1),
            )
            val textParticle = android.graphics.Color.alpha(pixel) > 64
            gridCells += GridCell(
                row = row,
                column = column,
                position = Offset(x, y),
                textParticle = textParticle,
            )
            column++
            x += spacing
        }
        row++
        y += spacing
    }
    bitmap.recycle()

    val textCells = gridCells.filter { it.textParticle }
    if (textCells.isEmpty()) {
        return emptyList()
    }
    val minColumn = textCells.minOf { it.column }
    val maxColumn = textCells.maxOf { it.column }
    val minRow = textCells.minOf { it.row }
    val maxRow = textCells.maxOf { it.row }
    val dustCells = TypographyDustPoints.map { (x, y) ->
        (minRow + ((maxRow - minRow) * y).roundToInt()) to
                (minColumn + ((maxColumn - minColumn) * x).roundToInt())
    }.toSet()

    return gridCells.mapNotNull { cell ->
        val dustParticle = !cell.textParticle && (cell.row to cell.column) in dustCells
        if (!cell.textParticle && !dustParticle) {
            return@mapNotNull null
        }
        BrandParticle(
            position = cell.position,
            phase = (
                    cell.row * 1.37f +
                            cell.column * 2.19f +
                            ((cell.row * 17 + cell.column * 31) % 19) * 0.47f
                    ),
            textParticle = cell.textParticle,
            normalizedX = cell.position.x / width,
            normalizedY = cell.position.y / height,
            radialFieldA = radialField(
                cell.position.x / width,
                cell.position.y / height,
                0.28f,
                0.42f,
                9f
            ),
            radialFieldB = radialField(
                cell.position.x / width,
                cell.position.y / height,
                0.76f,
                0.64f,
                11f
            ),
        )
    }
}

private fun radialField(
    x: Float,
    y: Float,
    centerX: Float,
    centerY: Float,
    multiplier: Float,
): Float = sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)) * multiplier
