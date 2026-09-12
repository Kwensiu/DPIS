package com.dpis.module.home.presentation

import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.compose.SecondaryPageContentTokens
import com.dpis.module.ui.compose.SecondaryPageScaffold
import com.dpis.module.ui.compose.SheetVisualChrome
import com.dpis.module.ui.compose.dpisClickable
import com.dpis.module.ui.compose.edgeToEdgeContentBottomPadding
import com.dpis.module.ui.compose.setFeatureContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateSupportPage(onBack: () -> Unit) {
    var supportersVisible by remember { mutableStateOf(false) }
    SecondaryPageScaffold(titleRes = R.string.donate_title, onBack = onBack) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                SupportersSheet()
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
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
            )
            Text(
                stringResource(titleRes),
                Modifier.padding(12.dp)
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
private fun SupportersSheet() {
    val supporters = listOf(
        R.string.donate_supporter_nickyoung_name to R.string.donate_supporter_nickyoung_amount,
        R.string.donate_supporter_tadow_name to R.string.donate_supporter_tadow_amount,
        R.string.donate_supporter_han_name to R.string.donate_supporter_han_amount,
        R.string.donate_supporter_spine_name to R.string.donate_supporter_spine_amount,
        R.string.donate_supporter_anonymous_name to R.string.donate_supporter_anonymous_amount,
    )
    Column(
        Modifier.fillMaxWidth().heightIn(min = 512.dp).padding(horizontal = 24.dp).padding(bottom = 24.dp),
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
        supporters.forEachIndexed { index, (nameRes, amountRes) ->
            Card(
                Modifier.fillMaxWidth().padding(top = if (index == 0) 12.dp else 8.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(nameRes),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(amountRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            stringResource(R.string.donate_supporters_sheet_note),
            Modifier.fillMaxWidth().padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
