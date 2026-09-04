package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Layout contract for the per-app editor.
 *
 * The values preserve the established sheet's hierarchy while normalising controls to Material's
 * 48dp minimum interactive height. Template tokens deliberately do not own this surface.
 */
internal object AppConfigSheetUiTokens {
    // The app header begins directly after the chrome. Horizontal and bottom padding remain
    // content-owned, while the chrome alone owns the vertical separation above it.
    // The editor keeps a single bottom reserve for the save action and the system gesture handle
    // on both the app sheet and the template sheet.
    val ContentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 20.dp)
    val HeaderToFirstInputGap = 12.dp
    val AppIconSize = 56.dp
    val AppIconGap = 16.dp
    val AppIconShape = RoundedCornerShape(12.dp)
    val FeedbackActionSize = 32.dp
    // Both affordances use the chrome's real center line. Increasing the container moves them
    // together without introducing child offsets or asymmetric internal layout bounds.
    // The shared chrome reserves one compact 56dp slot for the centered indicator, optional
    // unsaved badge, and app-only trailing action without inflating the editor's visible height.
    val TopChromeHeight = 56.dp
    // Expanded sheets draw under the system chrome. Keep the visual handle clear of a cutout
    // without translating the whole chrome by the full status-bar inset.
    val ChromeSafeOffset = 20.dp
    // Expanded editor sheets stop below the system chrome instead of becoming a full-screen page.
    val ExpandedTopClearance = 16.dp
    val TopChromeIndicatorWidth = 52.dp
    val TopChromeIndicatorHeight = 5.dp
    val WizardHintTopOffset = 40.dp
    val FeedbackActionShape = CircleShape
    val WizardHintShape = RoundedCornerShape(18.dp)
    val WizardHintCloseSize = 28.dp

    // Child pages share the standard 48dp title row below the sheet chrome, keeping tabs aligned
    // without inflating the editor's already prominent top region.
    val ChildPageHeaderHeight = 48.dp
    val ChildPageHorizontalPadding = 20.dp

    // 18dp is the sheet's dominant control radius. The mode thumb's legacy XML shape was 16dp,
    // but Compose clips it inside the 18dp track; the shared control intentionally uses 18dp so
    // its 1dp outline cannot be cropped at the outer corners.
    val FieldAndActionShape = RoundedCornerShape(18.dp)
    val LegacyModeThumbShape = RoundedCornerShape(16.dp)
    val ModeThumbBorderWidth = 1.dp
    val UnsavedBadgeShape = RoundedCornerShape(14.dp)
    val TopChromeIndicatorShape = RoundedCornerShape(3.dp)

    // A field row owns the space reserved for the floating label. Its input outline and mode
    // selector share one bottom edge instead of relying on independent child offsets.
    val FieldRowHeight = 56.dp
    val FieldTopInset = 6.dp
    // The visible input outline starts 8dp into each 56dp row. With the next row's matching
    // inset, a 4dp layout gap produces the same 12dp visible gap as the other control groups.
    val InputRowLayoutGap = 4.dp
    val ControlGroupGap = 12.dp
    // The legacy sheet keeps 28dp between the save outline and the advanced divider.
    val SaveToAdvancedDividerGap = 28.dp
    val CollapsedBottomClearance = 20.dp
    val ProcessActionGap = 6.dp
    val TypefaceHookGap = 10.dp
    val SaveRowBottomGap = 16.dp
    val AdvancedDividerTopGap = 12.dp
    val AdvancedTitleTopGap = 8.dp
    val AdvancedRowTopGap = 8.dp
    val DisableActionTopGap = 8.dp

    val ActionHeight = 48.dp
    val ActionShape = FieldAndActionShape
    // Secondary editor controls track the available row width until this cap. This keeps mode,
    // typeface, and hook affordances balanced on tablets without taking disproportionate space
    // away from the value that users are actively editing.
    val SecondaryControlMaxWidth = 180.dp
    const val PortraitSecondaryControlWidthFraction = 0.4f
    // Landscape editors have enough horizontal room to make values readable, so reserve a
    // smaller proportional share for their secondary control before applying the same cap.
    const val LandscapeSecondaryControlWidthFraction = 0.32f
    const val EditorRowWidthAnimationDurationMillis = 180
}
