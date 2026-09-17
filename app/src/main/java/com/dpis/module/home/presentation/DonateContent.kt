package com.dpis.module.home.presentation

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.home.DonationRecord
import com.dpis.module.home.DonationSupportWall
import com.dpis.module.home.DonationWallEntry
import com.dpis.module.ui.presentation.design.dpisClickable
import com.dpis.module.ui.presentation.design.setFeatureContent
import com.dpis.module.ui.presentation.editor.FeedbackIconButton
import com.dpis.module.ui.presentation.editor.SheetDestinationAnimatedContent
import com.dpis.module.ui.presentation.editor.SheetVisualChrome
import com.dpis.module.ui.presentation.workspace.SecondaryPageScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateSupportPage(onBack: () -> Unit) {
    var supportersVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val wall = remember(context) { DonationCatalogLoader.loadWall(context) }
    SecondaryPageScaffold(titleRes = R.string.donate_title, onBack = onBack) {
            item {
                SupportCard {
                    Text(stringResource(R.string.donate_message), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.donate_trust_note),
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SupportCard(
                    modifier = Modifier.dpisClickable(onClick = { supportersVisible = true }),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.donate_supporters_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.donate_supporters_summary),
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                DonationQrCard(
                    R.drawable.donate_wechat,
                    R.string.donate_wechat_title,
                    R.string.donate_wechat_qr_description,
                )
            }
            item {
                DonationQrCard(
                    R.drawable.donate_alipay,
                    R.string.donate_alipay_title,
                    R.string.donate_alipay_qr_description,
                )
            }
    }
    if (supportersVisible) {
        ModalBottomSheet(
            onDismissRequest = { supportersVisible = false },
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.fillMaxWidth()) {
                SheetVisualChrome()
                SupportersSheet(wall)
            }
        }
    }
}

fun ComponentActivity.installDonate() {
    setFeatureContent {
        DonateSupportPage(onBack = ::finish)
    }
}

@Composable
private fun SupportCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun DonationQrCard(
    @DrawableRes imageRes: Int,
    @StringRes titleRes: Int,
    @StringRes descriptionRes: Int,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Box {
            Image(
                painterResource(imageRes),
                stringResource(descriptionRes),
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
            )
            Text(
                stringResource(titleRes),
                Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SupportersSheet(wall: DonationSupportWall) {
    var selected by remember { mutableStateOf<DonationWallEntry?>(null) }
    BackHandler(enabled = selected != null) {
        selected = null
    }
    SheetDestinationAnimatedContent(
        targetState = selected,
        animateSize = true,
        clipContentToAnimatedBounds = false,
        contentKey = { entry ->
            when {
                entry == null -> WALL_CONTENT_KEY
                entry.anonymous -> DonationSupportWall.ANONYMOUS_ID
                else -> "named:${entry.id}"
            }
        },
        towardChild = { it != null },
        label = "donate-supporters-destination",
    ) { entry ->
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 512.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            if (entry == null) {
                SupportersWallContent(
                    wall = wall,
                    onOpen = { selected = it },
                )
            } else {
                SupporterDetailsContent(
                    entry = entry,
                    onBack = { selected = null },
                )
            }
        }
    }
}

@Composable
private fun SupportersWallContent(
    wall: DonationSupportWall,
    onOpen: (DonationWallEntry) -> Unit,
) {
    Text(
        stringResource(R.string.donate_supporters_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(
        stringResource(R.string.donate_supporters_summary),
        Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    wall.namedSupporters.forEachIndexed { index, entry ->
        SupporterWallRow(
            title = entry.displayName ?: entry.id,
            amount = entry.formattedTotal(),
            first = index == 0,
            onClick = { onOpen(entry) },
        )
    }
    wall.anonymous?.let { entry ->
        SupporterWallRow(
            title = stringResource(R.string.donate_supporters_anonymous),
            amount = entry.formattedTotal(),
            first = wall.namedSupporters.isEmpty(),
            onClick = { onOpen(entry) },
        )
    }
    Text(
        stringResource(R.string.donate_supporters_sheet_note),
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SupporterDetailsContent(
    entry: DonationWallEntry,
    onBack: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeedbackIconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_24),
                contentDescription = stringResource(R.string.system_settings_back),
            )
        }
        Text(
            if (entry.anonymous) {
                stringResource(R.string.donate_supporters_anonymous)
            } else {
                entry.displayName ?: entry.id
            },
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
    entry.donations.forEachIndexed { index, record ->
        DonationReceiptCard(
            record = record,
            first = index == 0,
        )
    }
}

@Composable
private fun SupporterWallRow(
    title: String,
    amount: String,
    first: Boolean,
    onClick: () -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(top = if (first) 12.dp else 8.dp)
            .dpisClickable(onClick = onClick),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                amount,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right_24),
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DonationReceiptCard(
    record: DonationRecord,
    first: Boolean,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(top = if (first) 12.dp else 8.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(
                record.formattedAmount(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            record.date?.let { date ->
                ReceiptMeta(date)
            }
            record.platform?.let { platform ->
                ReceiptMeta(platformLabel(platform))
            }
            record.orderRef?.let { orderRef ->
                ReceiptMeta(orderRef)
            }
        }
    }
}

@Composable
private fun ReceiptMeta(text: String) {
    Text(
        text,
        Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun platformLabel(platform: String): String {
    return when (platform) {
        "wechat" -> stringResource(R.string.donate_wechat_title)
        "alipay" -> stringResource(R.string.donate_alipay_title)
        else -> platform
    }
}

private const val WALL_CONTENT_KEY = "wall"
