package com.dpis.module.onboarding.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.dpis.module.R
import com.dpis.module.settings.AppLocaleManager
import com.dpis.module.settings.presentation.SettingsChoiceMenu
import com.dpis.module.settings.presentation.SettingsChoiceOption
import com.dpis.module.ui.presentation.design.LocalSpacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WelcomeStep() {
    val spacing = LocalSpacing.current
    val carouselState = rememberCarouselState { WELCOME_FEATURE_COUNT }
    val routingProgress = animateFloatAsState(
        targetValue = if (carouselState.currentItem == 2) 1f else 0f,
        animationSpec = tween(durationMillis = 520),
        label = "font-routing-morph",
    )
    val scaleProgress = animateFloatAsState(
        targetValue = if (carouselState.currentItem == 1) 1f else 0f,
        animationSpec = tween(durationMillis = 520),
        label = "interface-scale-morph",
    )
    Column(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            BrandMark(modifier = Modifier.widthIn(max = 320.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_message),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        HorizontalCenteredHeroCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            maxItemWidth = 300.dp,
            itemSpacing = spacing.sm,
            contentPadding = PaddingValues(horizontal = spacing.lg),
        ) { index ->
            val drawInfo = carouselItemDrawInfo
            val rawCompression = (
                    (drawInfo.maxSize - drawInfo.size) /
                            (drawInfo.maxSize - drawInfo.minSize).coerceAtLeast(1f)
                    ).coerceIn(0f, 1f)
            val compression = if (rawCompression < 0.1f) {
                0f
            } else {
                ((rawCompression - 0.1f) / 0.9f).coerceIn(0f, 1f)
            }
            WelcomeFeatureCard(
                index = index,
                routingProgress = routingProgress,
                scaleProgress = scaleProgress,
                compressedFraction = compression,
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.medium),
            )
        }
        WelcomeCommunityLinks()
    }
}

@Composable
private fun WelcomeFeatureCard(
    index: Int,
    routingProgress: State<Float>,
    scaleProgress: State<Float>,
    compressedFraction: Float,
    modifier: Modifier,
) {
    val spacing = LocalSpacing.current
    val title = when (index) {
        0 -> R.string.onboarding_feature_dpi_title
        1 -> R.string.onboarding_feature_scale_title
        else -> R.string.onboarding_feature_font_title
    }
    val description = when (index) {
        0 -> R.string.onboarding_feature_dpi_description
        1 -> R.string.onboarding_feature_scale_description
        else -> R.string.onboarding_feature_font_description
    }
    val cardColor = when (index) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val onCardColor = when (index) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val orbitActive = compressedFraction < 0.18f && index == 0
    Surface(
        modifier = modifier,
        color = cardColor,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (index == 0) {
                WelcomeFeatureArtwork(
                    index = index,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur((compressedFraction * 10f).coerceAtMost(10f).dp),
                    routingProgress = routingProgress,
                    scaleProgress = scaleProgress,
                    orbitActive = orbitActive,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.lg),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "0${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = onCardColor.copy(alpha = 0.72f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (index == 1) "Viewport" else "Fonts",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardColor.copy(alpha = 0.72f),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        WelcomeFeatureArtwork(
                            index = index,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur((compressedFraction * 10f).coerceAtMost(10f).dp),
                            routingProgress = routingProgress,
                            scaleProgress = scaleProgress,
                            orbitActive = orbitActive,
                        )
                    }
                    Column(
                        modifier = Modifier.graphicsLayer {
                            alpha = (1f - compressedFraction / 0.72f).coerceIn(0f, 1f)
                        },
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        Text(
                            text = stringResource(title),
                            style = MaterialTheme.typography.titleSmall,
                            color = onCardColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(description),
                            style = MaterialTheme.typography.labelMedium,
                            color = onCardColor.copy(alpha = 0.78f),
                        )
                    }
                }
            }
            if (index == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.lg),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "01",
                            style = MaterialTheme.typography.labelMedium,
                            color = onCardColor.copy(alpha = 0.72f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "LSPosed",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardColor.copy(alpha = 0.72f),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        modifier = Modifier.graphicsLayer {
                            alpha = (1f - compressedFraction / 0.72f).coerceIn(0f, 1f)
                        },
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        Text(
                            text = stringResource(title),
                            style = MaterialTheme.typography.titleSmall,
                            color = onCardColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(description),
                            style = MaterialTheme.typography.labelMedium,
                            color = onCardColor.copy(alpha = 0.78f),
                        )
                    }
                }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = ((compressedFraction - 0.42f) / 0.34f).coerceIn(0f, 1f)
                    },
            ) {
                drawIntoCanvas { canvas ->
                    val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = onCardColor.toArgb()
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 18.sp.toPx()
                        textAlign = Paint.Align.CENTER
                    }
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = onCardColor.copy(alpha = 0.9f).toArgb()
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 12.sp.toPx()
                        textAlign = Paint.Align.CENTER
                    }

                    fun drawRotatedText(text: String, paint: Paint, centerY: Float) {
                        val centerX = size.width / 2f
                        val baseline = centerY - (paint.ascent() + paint.descent()) / 2f
                        canvas.save()
                        canvas.nativeCanvas.rotate(90f, centerX, centerY)
                        canvas.nativeCanvas.drawText(text, centerX, baseline, paint)
                        canvas.restore()
                    }
                    drawRotatedText("0${index + 1}", numberPaint, 34.dp.toPx())
                    drawRotatedText(
                        if (index == 0) "LSPosed" else if (index == 1) "Viewport" else "Fonts",
                        labelPaint,
                        size.height - 44.dp.toPx(),
                    )
                }
            }
        }
    }
}

private const val WELCOME_FEATURE_COUNT = 3

@Composable
internal fun WelcomeLanguageOverlay(
    onLanguageSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val options = AppLocaleManager.supportedLanguages().map { option ->
        val label = if (option.tag == AppLocaleManager.TAG_FOLLOW_SYSTEM) {
            context.applicationContext.getString(option.labelResId)
        } else {
            stringResource(option.labelResId)
        }
        SettingsChoiceOption(option.tag, label)
    }
    SettingsChoiceMenu(
        expanded = expanded,
        options = options,
        selected = AppLocaleManager.getLanguageTag(context),
        onDismiss = { expanded = false },
        onSelected = { tag ->
            expanded = false
            onLanguageSelected(tag)
        },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .offset(y = spacing.sm)
                    .testTag("onboarding-language-button"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_language_24),
                    contentDescription = stringResource(R.string.settings_language_label),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeCommunityLinks() {
    val context = LocalContext.current
    val links = listOf(
        Triple(R.drawable.ic_qq_24, R.string.home_feedback_qq, R.string.home_feedback_qq_url),
        Triple(R.drawable.ic_github_24, R.string.home_feedback_github, R.string.about_source_url),
        Triple(
            R.drawable.ic_telegram_24,
            R.string.home_feedback_telegram,
            R.string.home_feedback_telegram_url,
        ),
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp),
    ) {
        if (maxWidth < 340.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                links.forEach { (iconRes, labelRes, urlRes) ->
                    FilledIconButton(
                        onClick = {
                            openExternalLink(context, context.getString(urlRes))
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = context.getString(labelRes),
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            ) {
                links.forEach { (iconRes, labelRes, urlRes) ->
                    FilledTonalButton(
                        onClick = {
                            openExternalLink(context, context.getString(urlRes))
                        },
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                        )
                        Text(context.getString(labelRes))
                    }
                }
            }
        }
    }
}

private fun openExternalLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // A missing browser or community app is an unavailable optional action, not a crash.
    }
}
