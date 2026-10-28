package com.dpis.module.onboarding.presentation

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpis.module.R
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import android.graphics.Canvas as AndroidCanvas

@Composable
internal fun WelcomeFeatureArtwork(
    index: Int,
    modifier: Modifier,
    routingProgress: State<Float>,
    scaleProgress: State<Float>,
    orbitActive: Boolean,
) {
    when (index) {
        0 -> WelcomeRuntimeOrbitArtwork(modifier, orbitActive)
        1 -> WelcomeInterfaceSkeletonArtwork(modifier, scaleProgress)
        else -> WelcomeFontRoutingArtwork(modifier, routingProgress)
    }
}

@Composable
private fun WelcomeFontRoutingArtwork(
    modifier: Modifier,
    progress: State<Float>,
) {
    val textColor = MaterialTheme.colorScheme.onTertiaryContainer
    val context = LocalContext.current
    val density = LocalDensity.current
    val asciiArt = remember(context) {
        context.resources.openRawResource(R.raw.font_routing_ascii).bufferedReader().use {
            it.readText().trim('\n')
        }
    }
    val asciiParticles = remember(asciiArt, textColor, density) {
        createAsciiParticles(asciiArt, textColor.toArgb(), density)
    }
    Canvas(modifier) {
        val currentProgress = progress.value
        val baselineY = size.height * 0.92f
        val lineColor = textColor
        val top = size.height * 0.02f
        val rulerStrokeWidth = 2.dp.toPx()
        val rulerBottom = baselineY - rulerStrokeWidth / 2f
        val spacing = (rulerBottom - top) / 5f
        val targetPositions = listOf(0f, 0.12f, 0.25f, 0.45f, 0.7f, 1f)
        val tickWidth = size.width * 0.06f
        val aPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 92.sp.toPx()
        }
        val aWidth = aPaint.measureText("A")
        val aBounds = android.graphics.Rect()
        aPaint.getTextBounds("A", 0, 1, aBounds)
        val particleWidth = 48.dp.toPx()
        val aGap = 8.dp.toPx()
        val rulerGap = 28.dp.toPx()
        val groupWidth = aWidth + aGap + particleWidth + rulerGap + tickWidth
        val groupStart = (size.width - groupWidth) / 2f
        val aX = groupStart
        val particleX = aX + aWidth + aGap
        val spineX = particleX + particleWidth + rulerGap
        val aScaleY = 1.8f
        val aBaseline = baselineY - aBounds.bottom * aScaleY - 0.5.dp.toPx()

        drawLine(
            lineColor,
            Offset(spineX, top),
            Offset(spineX, rulerBottom),
            rulerStrokeWidth,
            StrokeCap.Round
        )
        targetPositions.forEachIndexed { index, targetFraction ->
            val startY = top + spacing * index
            val targetY = top + (rulerBottom - top) * targetFraction
            val y = startY + (targetY - startY) * currentProgress
            drawLine(
                lineColor,
                Offset(spineX, y),
                Offset(spineX + tickWidth, y),
                rulerStrokeWidth,
                StrokeCap.Round,
            )
        }
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(aX, aBaseline)
            canvas.scale(1f, aScaleY)
            canvas.nativeCanvas.drawText("A", 0f, 0f, aPaint)
            canvas.restore()
        }

        val particleRadius = 0.4.dp.toPx()
        val particleBottom = (asciiParticles.maxOfOrNull { it.y } ?: 0f) + particleRadius
        val particleOriginY = baselineY - particleBottom
        asciiParticles.forEach { particle ->
            drawCircle(
                color = textColor.copy(alpha = 0.9f),
                radius = particleRadius,
                center = Offset(particleX + particle.x, particleOriginY + particle.y),
            )
        }
    }
}

private fun createAsciiParticles(
    asciiArt: String,
    color: Int,
    density: androidx.compose.ui.unit.Density,
): List<Offset> {
    val width = with(density) { 48.dp.roundToPx() }
    val height = with(density) { 64.dp.roundToPx() }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val cellWidth = width / 29f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textSize = with(density) { 2.sp.toPx() }
        textAlign = Paint.Align.LEFT
        textScaleX = cellWidth / measureText(" ")
    }
    val firstBaseline = -paint.fontMetrics.ascent
    val lineHeight = with(density) { 3.sp.toPx() }
    asciiArt.split('\n').forEachIndexed { row, rawLine ->
        val line = rawLine.padEnd(35, ' ').substring(6, 35)
        line.forEachIndexed { column, character ->
            if (character != ' ') {
                canvas.drawText(
                    character.toString(),
                    column * cellWidth,
                    firstBaseline + row * lineHeight,
                    paint
                )
            }
        }
    }
    val sampleSpacing = with(density) { 1.dp.toPx() }
    return buildList {
        var y = sampleSpacing / 2f
        while (y < height) {
            var x = sampleSpacing / 2f
            while (x < width) {
                val pixel = bitmap.getPixel(
                    x.roundToInt().coerceIn(0, width - 1),
                    y.roundToInt().coerceIn(0, height - 1),
                )
                if (android.graphics.Color.alpha(pixel) > 40) add(Offset(x, y))
                x += sampleSpacing
            }
            y += sampleSpacing
        }
    }
}

@Composable
private fun WelcomeInterfaceSkeletonArtwork(
    modifier: Modifier,
    progress: State<Float>,
) {
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer
    val outlineColor = textColor.copy(alpha = 0.68f)
    val mutedColor = textColor.copy(alpha = 0.24f)
    val frameFillColor = MaterialTheme.colorScheme.secondaryContainer
    Canvas(modifier) {
        val currentProgress = progress.value
        val radius = 8.dp.toPx()
        val stroke = Stroke(width = 1.dp.toPx())
        val contentScale = 1f - 0.28f * currentProgress

        fun drawSkeletonFrame(
            centerX: Float,
            bottomY: Float,
            frameSize: Size,
            rowCount: Int,
            opaque: Boolean = false,
        ) {
            val frameTopLeft = Offset(centerX - frameSize.width / 2f, bottomY - frameSize.height)
            if (opaque) {
                drawRoundRect(
                    frameFillColor,
                    frameTopLeft,
                    frameSize,
                    CornerRadius(radius),
                )
            }
            drawRoundRect(
                outlineColor,
                frameTopLeft,
                frameSize,
                CornerRadius(radius),
                style = stroke,
            )
            val inset = 8.dp.toPx()
            val contentLeft = frameTopLeft.x + inset
            val contentTop = frameTopLeft.y + inset
            val contentWidth = frameSize.width - inset * 2f
            val contentHeight = frameSize.height - inset * 2f
            clipRect(
                contentLeft,
                contentTop,
                contentLeft + contentWidth,
                contentTop + contentHeight
            ) {
                translate(contentLeft, contentTop) {
                    scale(contentScale, contentScale, Offset.Zero) {
                        val logicalWidth = contentWidth / contentScale
                        val logicalHeight = contentHeight / contentScale
                        val avatarRadius = minOf(10.dp.toPx(), logicalWidth * 0.16f)
                        drawCircle(
                            mutedColor,
                            avatarRadius,
                            Offset(avatarRadius, avatarRadius),
                        )
                        drawRoundRect(
                            mutedColor,
                            Offset(avatarRadius * 2f + 6.dp.toPx(), 0f),
                            Size(logicalWidth * 0.52f, 20.dp.toPx()),
                            CornerRadius(10.dp.toPx()),
                        )
                        val firstRowY = 30.dp.toPx()
                        val lastRowY = (logicalHeight - 5.dp.toPx()).coerceAtLeast(firstRowY)
                        val rowStep = if (rowCount > 1) {
                            (lastRowY - firstRowY) / (rowCount - 1)
                        } else {
                            0f
                        }
                        repeat(rowCount) { row ->
                            val y = firstRowY + rowStep * row
                            val widthFraction = if (row % 2 == 0) 0.88f else 0.62f
                            drawRoundRect(
                                mutedColor,
                                Offset(0f, y),
                                Size(logicalWidth * widthFraction, 5.dp.toPx()),
                                CornerRadius(2.5.dp.toPx()),
                            )
                        }
                    }
                }
            }
        }

        val leftFrameSize = Size(size.width * 0.4f, size.height * 0.78f)
        val leftFrameTopLeft = Offset(
            size.width * 0.34f - leftFrameSize.width / 2f,
            size.height * 0.92f - leftFrameSize.height,
        )
        val rightFrameSize = Size(size.width * 0.48f, size.height * 0.42f)
        val rightFrameTopLeft = Offset(
            size.width * 0.64f - rightFrameSize.width / 2f,
            size.height * 0.92f - rightFrameSize.height,
        )
        drawSkeletonFrame(
            size.width * 0.34f,
            size.height * 0.92f,
            leftFrameSize,
            8 + (currentProgress * 4f).roundToInt(),
        )
        drawSkeletonFrame(
            size.width * 0.64f,
            size.height * 0.92f,
            rightFrameSize,
            3 + (currentProgress * 2f).roundToInt(),
            opaque = true,
        )
        drawIntoCanvas { canvas ->
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor.copy(alpha = 0.56f).toArgb()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 10.sp.toPx()
            }
            val scalePercent = "${(100 + currentProgress * 50f).roundToInt()}%"
            val densityValue = "${(398 + currentProgress * 199f).roundToInt()}dp"
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.translate(
                leftFrameTopLeft.x + leftFrameSize.width + 4.dp.toPx(),
                leftFrameTopLeft.y + 4.dp.toPx(),
            )
            canvas.nativeCanvas.rotate(90f)
            canvas.nativeCanvas.drawText(scalePercent, 0f, 0f, valuePaint)
            canvas.nativeCanvas.restore()
            val densityWidth = valuePaint.measureText(densityValue)
            canvas.nativeCanvas.drawText(
                densityValue,
                rightFrameTopLeft.x + rightFrameSize.width - densityWidth - 4.dp.toPx(),
                rightFrameTopLeft.y - 4.dp.toPx(),
                valuePaint,
            )
        }

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeRuntimeOrbitArtwork(
    modifier: Modifier,
    active: Boolean,
) {
    val rotation = remember { Animatable(0f) }
    val orbitProgress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            launch {
                orbitProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(520, easing = FastOutSlowInEasing),
                )
            }
            var lastFrameNanos = withFrameNanos { it }
            val animationStartNanos = lastFrameNanos
            while (true) {
                val frameNanos = withFrameNanos { it }
                val elapsedMillis = (frameNanos - animationStartNanos) / 1_000_000f
                val deltaMillis = (frameNanos - lastFrameNanos) / 1_000_000f
                val degreesPerMillis = if (elapsedMillis < 520f) {
                    360f / 520f
                } else {
                    360f / 7200f
                }
                rotation.snapTo(rotation.value + deltaMillis * degreesPerMillis)
                lastFrameNanos = frameNanos
            }
        } else {
            orbitProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(420, easing = FastOutSlowInEasing),
            )
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val geistMono = FontFamily(Font(R.font.geist_mono_102, FontWeight.Normal))
    val orbitColors = listOf(
        primary.copy(alpha = 0.78f),
        primary.copy(alpha = 0.48f),
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
    )
    val orbitRadii = listOf(68.dp, 96.dp, 118.dp)
    val orbitShapes = listOf(
        MaterialShapes.Cookie12Sided.toShape(),
        MaterialShapes.Circle.toShape(),
        MaterialShapes.Flower.toShape(),
    )
    val orbitDirections = listOf(1f, -0.72f, 0.48f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val centerRadius = 38.dp.toPx()
            orbitRadii.forEachIndexed { index, radiusDp ->
                val targetRadius = radiusDp.toPx()
                val radius = centerRadius + (targetRadius - centerRadius) * orbitProgress.value
                val orbitAlpha = orbitProgress.value
                drawCircle(
                    orbitColors[index].copy(alpha = orbitColors[index].alpha * orbitAlpha),
                    radius,
                    center,
                    style = Stroke(1.dp.toPx()),
                )
                val angle = Math.toRadians(
                    (-90f + rotation.value * orbitDirections[index]).toDouble(),
                ).toFloat()
                val node = Offset(
                    center.x + cos(angle) * radius,
                    center.y + sin(angle) * radius,
                )
                val nodeSize = if (index == 0) 18.dp.toPx() else 14.dp.toPx()
                val shapeColor = orbitColors[index].copy(
                    alpha = orbitColors[index].alpha * orbitProgress.value,
                )
                translate(node.x - nodeSize / 2f, node.y - nodeSize / 2f) {
                    val outline = orbitShapes[index].createOutline(
                        Size(nodeSize, nodeSize),
                        layoutDirection,
                        this,
                    )
                    when (outline) {
                        is Outline.Generic -> drawPath(outline.path, shapeColor)
                        is Outline.Rounded -> drawRoundRect(
                            color = shapeColor,
                            topLeft = Offset(outline.roundRect.left, outline.roundRect.top),
                            size = Size(outline.roundRect.width, outline.roundRect.height),
                            cornerRadius = CornerRadius(
                                outline.roundRect.topLeftCornerRadius.x,
                                outline.roundRect.topLeftCornerRadius.y,
                            ),
                        )

                        is Outline.Rectangle -> drawRect(
                            color = shapeColor,
                            topLeft = Offset(outline.rect.left, outline.rect.top),
                            size = Size(outline.rect.width, outline.rect.height),
                        )
                    }
                }
            }
            drawCircle(primary, centerRadius, center)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "102",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = geistMono),
                fontWeight = FontWeight.Normal,
                color = onPrimary,
            )
            Text(
                "API",
                style = MaterialTheme.typography.labelMedium,
                color = onPrimary.copy(alpha = 0.82f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeFeatureArtworkPreview() {
    ComposeDesignSystem(darkTheme = false) {
        val zeroProgress = remember { mutableFloatStateOf(0f) }
        val fullProgress = remember { mutableFloatStateOf(1f) }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WelcomeFeatureArtwork(
                0, Modifier
                    .fillMaxWidth()
                    .height(160.dp), zeroProgress, zeroProgress, false
            )
            WelcomeFeatureArtwork(
                1, Modifier
                    .fillMaxWidth()
                    .height(160.dp), zeroProgress, fullProgress, false
            )
            WelcomeFeatureArtwork(
                2, Modifier
                    .fillMaxWidth()
                    .height(160.dp), fullProgress, zeroProgress, false
            )
        }
    }
}
