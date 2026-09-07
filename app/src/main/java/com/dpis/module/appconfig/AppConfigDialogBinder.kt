package com.dpis.module.appconfig

import android.app.Activity
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.dpis.module.ConfigStoreFactory
import com.dpis.module.DpisApplication
import com.dpis.module.DpisConfigStore
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigDialogInputLogic.parseFontScalePercentOrNull
import com.dpis.module.appconfig.AppConfigDialogInputLogic.parsePositiveIntOrNull
import com.dpis.module.appconfig.AppConfigDialogInputLogic.parseViewportTargetSpecOrNull
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppStatusFormatter
import com.dpis.module.applist.AppStatusFormatter.StatusInput
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.FontLibraryEntry
import com.dpis.module.fonts.FontLibraryStore
import com.dpis.module.fonts.FontTypefaceLoader
import com.dpis.module.fonts.SystemFontEntry
import com.dpis.module.fonts.SystemFontRegistry
import com.dpis.module.quirks.WechatDpiSheetBinder
import com.dpis.module.ui.DialogWindowSizer
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AppConfigDialogBinder @JvmOverloads constructor(
    private val activity: Activity,
    private val host: Host,
    private val showDragHandle: Boolean = true,
) {
    enum class ProcessAction {
        START,
        RESTART,
        STOP
    }

    interface Host {
        fun toggleScope(
            item: AppListItem?,
            currentlyInScope: Boolean,
            onTurnedInScope: Runnable?,
            onTurnedOutScope: Runnable?,
        )

        fun requestScope(
            item: AppListItem?,
            onTurnedInScope: Runnable?,
            onRequestFinished: Runnable?
        ): Boolean

        fun executeProcessAction(item: AppListItem?, action: ProcessAction?)

        fun applyHyperOsNativeProxy(item: AppListItem?, onFinished: Runnable?)

        fun unmountHyperOsNativeProxy(item: AppListItem?, onFinished: Runnable?)

        /** Metadata is deliberately resolved only for an app about to use the proxy.  */
        fun isHyperOsNativeProxyCandidate(item: AppListItem?): Boolean =
            item?.hyperOsNativeProxyCandidate == true

        fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean

        fun showFontHookDomains(
            item: AppListItem?,
            state: AppConfigDialogState?,
            onStateChanged: Runnable?
        )

        fun getFontHookDomainsButtonText(
            item: AppListItem?,
            state: AppConfigDialogState?
        ): String?

        fun openTypefaceLibrary()

        fun startFeedbackDiagnostic(
            item: AppListItem?,
            state: AppConfigDialogState?
        ) {
        }

        fun saveAppConfig(
            dialogView: View?,
            item: AppListItem?,
            dpisEnabled: Boolean,
            viewportInput: TextInputEditText?,
            fontScaleInput: TextInputEditText?,
            viewportMode: String?,
            viewportApplyMode: String?,
            viewportApplyModeResetRequested: Boolean,
            fontMode: String?,
            selectedTypefaceId: String?,
            draftFontHookDomainsRaw: String?,
            fontHookDomainsResetRequested: Boolean,
            viewportScaleInput: String?,
            viewportAbsoluteInput: String?
        ): AppConfigSaveHandler.Result?

        val configStore: DpisConfigStore?

        fun requestAppsLoad()

        fun onRuntimeConfigSaved() {
            requestAppsLoad()
        }

        fun onDraftStateChanged(state: AppConfigDialogState?)

        fun showToast(messageResId: Int)
    }

    fun bind(dialogView: View, item: AppListItem, systemHooksEnabled: Boolean) {
        val views = initDialogViews(dialogView)
        val state = bindDialogInitialState(item, views)
        dialogView.setTag(R.id.dialog_save_button, state)
        dialogView.setTag(R.id.dialog_font_hook_domains_button, views)
        WechatDpiSheetBinder.bind(
            dialogView, item.packageName
        ) { updateSaveButtonState(dialogView, views) }
        updateSaveButtonState(dialogView, views)
        state.captureSavedDraft(views, item.previewFromGlobalPrefill)
        state.bindUnsavedBadge(
            UnsavedBadgeBinder.bind(
                dialogView, { state.hasUnsavedChanges(views) }, showDragHandle
            )
        )
        val style: AppConfigDialogActionStyle = captureDialogActionStyle(views.scopeButton)
        refreshDialogState(views, state, style, systemHooksEnabled, item)
        AppConfigSheetInteractions(this, host)
            .bind(dialogView, item, views, state, style, systemHooksEnabled)
    }

    fun applyRetainedDraft(
        dialogView: View?,
        item: AppListItem?,
        systemHooksEnabled: Boolean,
        selectedTypefaceId: String?,
        draftFontHookDomainsRaw: String?,
        viewportApplyMode: String?,
        fontHookDomainsResetRequested: Boolean,
        viewportApplyModeResetRequested: Boolean
    ) {
        val state: AppConfigDialogState? = stateFor(dialogView)
        val views: AppConfigDialogViews? = viewsFor(dialogView)
        if (state == null || views == null || item == null) {
            return
        }
        state.selectedTypefaceId = normalizeTypefaceId(selectedTypefaceId)
        state.draftFontHookDomainsRaw = draftFontHookDomainsRaw
        state.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode)
        state.fontHookDomainsResetRequested = fontHookDomainsResetRequested
        state.viewportApplyModeResetRequested = viewportApplyModeResetRequested
        bindTypefaceSelector(views.typefaceSelectorButton, state.selectedTypefaceId)
        bindFontHookDomainsButton(
            views.fontHookDomainsButton,
            item,
            state
        )
        updateDialogStatus(
            views.statusView,
            state.scopeSelected,
            state.scopeKnown,
            state.dpisEnabled,
            views.viewportInputView,
            views.viewportModeToggle,
            views.fontInputView,
            views.fontModeToggle,
            state.selectedTypefaceId,
            systemHooksEnabled,
            state.viewportApplyMode
        )
        updateSaveButtonState(dialogView, views)
        state.refreshUnsavedBadge()
    }

    private fun initDialogViews(dialogView: View): AppConfigDialogViews {
        return AppConfigDialogViews(
            requireView(dialogView, R.id.dialog_app_icon),
            requireView(dialogView, R.id.dialog_title),
            requireView(dialogView, R.id.dialog_package),
            requireView(dialogView, R.id.dialog_status),
            requireView(dialogView, R.id.dialog_viewport_input_layout),
            requireView(dialogView, R.id.dialog_viewport_input),
            requireView(dialogView, R.id.dialog_font_scale_input_layout),
            requireView(dialogView, R.id.dialog_font_scale_input),
            ModeToggle(
                requireView(dialogView, R.id.dialog_viewport_mode_toggle_button),
                requireView(dialogView, R.id.dialog_viewport_mode_toggle_thumb),
                requireView(dialogView, R.id.dialog_viewport_mode_system_label),
                requireView(dialogView, R.id.dialog_viewport_mode_compat_label)
            ),
            ModeToggle(
                requireView(dialogView, R.id.dialog_font_mode_toggle_button),
                requireView(dialogView, R.id.dialog_font_mode_toggle_thumb),
                requireView(dialogView, R.id.dialog_font_mode_system_label),
                requireView(dialogView, R.id.dialog_font_mode_compat_label)
            ),
            requireView(dialogView, R.id.dialog_typeface_selector_button),
            requireView(dialogView, R.id.dialog_scope_button),
            requireView(dialogView, R.id.dialog_start_button),
            requireView(dialogView, R.id.dialog_restart_button),
            requireView(dialogView, R.id.dialog_stop_button),
            requireView(dialogView, R.id.dialog_dpis_toggle_button),
            requireView(dialogView, R.id.dialog_font_hook_domains_button),
            requireView(dialogView, R.id.dialog_disable_button),
            requireView(dialogView, R.id.dialog_save_button),
            requireView(dialogView, R.id.dialog_feedback_diagnostic_button)
        )
    }

    private inline fun <reified T : View> requireView(root: View, id: Int): T =
        requireNotNull(root.findViewById<T>(id)) { "Missing required dialog view: $id" }

    private fun bindDialogInitialState(
        item: AppListItem,
        views: AppConfigDialogViews
    ): AppConfigDialogState {
        views.iconView.setImageDrawable(item.icon)
        views.titleView.text = item.label
        views.packageView.text = item.packageName
        val initialViewportInput: String? = formatViewportInput(item.viewportTargetSpec)
        views.viewportInputView.setText(initialViewportInput)
        val initialViewportScaleInput = if (item.viewportScaleMilliPercent != null)
            AppConfigInputValidation.formatScaleMilliPercentInput(item.viewportScaleMilliPercent)
        else
            (if (item.viewportTargetSpec.isRelativeScale) initialViewportInput else "")
        val initialViewportAbsoluteInput =
            if (item.viewportWidthDp != null) item.viewportWidthDp.toString() else
                (if (item.viewportTargetSpec.isAbsoluteDp) initialViewportInput else "")
        views.fontInputView.setText(
            if (item.fontScalePercent != null) item.fontScalePercent.toString() else
                ""
        )
        val initialViewportType = initialViewportTargetType(item)
        bindViewportModeToggle(views.viewportModeToggle, initialViewportType, false)
        bindViewportInputHint(views.viewportInputLayout, initialViewportType)
        bindFontModeToggle(views.fontModeToggle, initialFontMode(item.fontMode), false)
        val selectedTypefaceId = normalizeTypefaceId(item.typefaceId)
        bindTypefaceSelector(views.typefaceSelectorButton, selectedTypefaceId)
        updateSaveButtonState(
            views.viewportInputLayout, views.viewportInputView,
            views.viewportModeToggle,
            views.fontInputLayout, views.fontInputView, views.saveButton
        )
        return AppConfigDialogState(
            item.inScope, item.scopeKnown, item.dpisEnabled,
            item.previewFromGlobalPrefill,
            item.packageName,
            item.effectiveFontHookDomainsRaw(),
            item.viewportMode,
            selectedTypefaceId,
            initialViewportType,
            initialViewportInput,
            initialViewportScaleInput,
            initialViewportAbsoluteInput
        )
    }

    fun refreshDialogState(
        views: AppConfigDialogViews,
        state: AppConfigDialogState,
        style: AppConfigDialogActionStyle,
        systemHooksEnabled: Boolean,
        item: AppListItem
    ) {
        updateDialogStatus(
            views.statusView,
            state.scopeSelected,
            state.scopeKnown,
            state.dpisEnabled,
            views.viewportInputView,
            views.viewportModeToggle,
            views.fontInputView,
            views.fontModeToggle,
            state.selectedTypefaceId,
            systemHooksEnabled,
            state.viewportApplyMode
        )
        bindScopeButton(
            views.scopeButton, state.scopeSelected, state.scopeKnown,
            style.defaultActionBgTint, style.defaultActionStrokeWidth, style.defaultActionTextColor
        )
        bindDpisToggleButton(
            views.dpisToggleButton, state.dpisEnabled,
            style.defaultActionBgTint, style.defaultActionStrokeWidth, style.defaultActionTextColor
        )
        bindFontHookDomainsButton(
            views.fontHookDomainsButton,
            item,
            state
        )
        state.refreshUnsavedBadge()
    }

    fun bindTypefaceSelector(selectorButton: MaterialButton, selectedTypefaceId: String?) {
        configureTypefaceSelectorMarquee(selectorButton)
        selectorButton.text = typefaceSelectorText(selectedTypefaceId)
    }

    /** Shared display contract for View and Compose app editors.  */
    fun typefaceSelectorText(selectedTypefaceId: String?): String {
        return formatTypefaceSelectorText(
            resolveTypefaceDisplayText(
                selectedTypefaceId, listFontLibraryEntries()
            )
        )
    }

    private fun configureTypefaceSelectorMarquee(selectorButton: MaterialButton) {
        selectorButton.isSingleLine = true
        selectorButton.setHorizontallyScrolling(true)
        selectorButton.ellipsize = TextUtils.TruncateAt.MARQUEE
        selectorButton.marqueeRepeatLimit = -1
        selectorButton.isSelected = true
    }

    fun showTypefaceSelector(
        selectorButton: MaterialButton,
        state: AppConfigDialogState,
        onSelectionChanged: Runnable?
    ) {
        val selectedImported =
            !state.selectedTypefaceId.isNullOrBlank() && !SystemFontRegistry.isSystemFontId(
                state.selectedTypefaceId
            )
        val root =
            LayoutInflater.from(activity).inflate(R.layout.dialog_typeface_selection, null, false)
        val tabs = root.findViewById<TabLayout>(R.id.typeface_tabs)
        val listView = root.findViewById<LinearLayout>(R.id.typeface_options_container)
        val importButton = root.findViewById<MaterialButton>(R.id.typeface_import_button)
        val doneButton = root.findViewById<MaterialButton>(R.id.typeface_dialog_done_button)
        tabs.addTab(tabs.newTab().setText(R.string.dialog_typeface_tab_system))
        tabs.addTab(tabs.newTab().setText(R.string.dialog_typeface_tab_imported))
        val dialogHolder = arrayOfNulls<AlertDialog>(1)
        val showSystemFonts = Runnable {
            bindTypefaceOptionRows(
                listView,
                buildSystemTypefaceOptions(
                    SystemFontRegistry.listRecommendedFonts(),
                    state.selectedTypefaceId
                ),
                selectorButton,
                state,
                onSelectionChanged,
                dialogHolder,
                false
            )
        }
        val showImportedFonts = Runnable {
            bindImportedTypefaceCollectionRows(
                listView,
                listFontLibraryEntries(),
                selectorButton,
                state,
                onSelectionChanged
            )
        }
        tabs.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 1) {
                    showImportedFonts.run()
                    return
                }
                showSystemFonts.run()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
        importButton.setOnClickListener { _: View? ->
            if (dialogHolder[0] != null) {
                dialogHolder[0]?.dismiss()
            }
            host.openTypefaceLibrary()
        }
        dialogHolder[0] = MaterialAlertDialogBuilder(activity)
            .setView(root)
            .create()
        doneButton.setOnClickListener { _: View? ->
            if (dialogHolder[0] != null) {
                dialogHolder[0]?.dismiss()
            }
        }
        dialogHolder[0]?.setCanceledOnTouchOutside(true)
        applyTypefaceDialogListHeight(root)
        dialogHolder[0]?.show()
        DialogWindowSizer.applyConfigurationWidth(dialogHolder[0], activity)
        val initialTab = tabs.getTabAt(if (selectedImported) 1 else 0)
        initialTab?.select()
        if (tabs.selectedTabPosition == (if (selectedImported) 1 else 0)) {
            if (selectedImported) {
                showImportedFonts.run()
                return
            }
            showSystemFonts.run()
        }
    }

    private fun resolveTypefaceDisplayText(
        selectedTypefaceId: String?,
        entries: MutableList<FontLibraryEntry>
    ): String? {
        if (selectedTypefaceId.isNullOrBlank()) {
            return activity.getString(R.string.dialog_typeface_default)
        }
        for (entry in SystemFontRegistry.listRecommendedFonts()) {
            if (selectedTypefaceId == entry.id()) {
                return entry.displayName()
            }
        }
        for (entry in entries) {
            if (selectedTypefaceId == entry.id) {
                return entry.displayName
            }
        }
        return formatMissingTypefaceLabel(selectedTypefaceId)
    }

    private fun buildSystemTypefaceOptions(
        entries: MutableList<SystemFontEntry>,
        selectedTypefaceId: String?
    ): MutableList<TypefaceOption> {
        val options: MutableList<TypefaceOption> = ArrayList(entries.size + 2)
        options.add(TypefaceOption(null, activity.getString(R.string.dialog_typeface_default)))
        if (SystemFontRegistry.isSystemFontId(selectedTypefaceId)
            && !containsSystemTypeface(entries, selectedTypefaceId)
        ) {
            options.add(
                TypefaceOption(
                    selectedTypefaceId,
                    formatMissingTypefaceLabel(selectedTypefaceId)
                )
            )
        }
        for (entry in entries) {
            options.add(TypefaceOption(entry.id(), entry.displayName()))
        }
        return options
    }

    private fun buildImportedTypefaceOptions(
        entries: MutableList<FontLibraryEntry>,
        selectedTypefaceId: String?
    ): MutableList<TypefaceOption> {
        val options: MutableList<TypefaceOption> = ArrayList(entries.size + 2)
        options.add(TypefaceOption(null, activity.getString(R.string.dialog_typeface_default)))
        if (!selectedTypefaceId.isNullOrBlank() && !SystemFontRegistry.isSystemFontId(
                selectedTypefaceId
            ) && !containsImportedTypeface(entries, selectedTypefaceId)
        ) {
            options.add(
                TypefaceOption(
                    selectedTypefaceId,
                    formatMissingTypefaceLabel(selectedTypefaceId)
                )
            )
        }
        if (entries.isEmpty()) {
            options.add(
                TypefaceOption(
                    TypefaceOptionModel.DISABLED_ID,
                    activity.getString(R.string.dialog_typeface_imported_empty)
                )
            )
        }
        for (entry in entries) {
            options.add(TypefaceOption(entry.id, resolveFontOptionLabel(entry)))
        }
        return options
    }

    /**
     * A TTC is one imported collection with multiple selectable faces. Keep the first picker
     * focused on collections so users do not need to import the same file repeatedly.
     */
    private fun bindImportedTypefaceCollectionRows(
        listView: LinearLayout,
        entries: MutableList<FontLibraryEntry>,
        selectorButton: MaterialButton,
        state: AppConfigDialogState,
        onSelectionChanged: Runnable?
    ) {
        listView.removeAllViews()
        if (entries.isEmpty()) {
            listView.addView(
                createTypefaceOptionRow(
                    listView,
                    TypefaceOption(
                        TypefaceOptionModel.DISABLED_ID,
                        activity.getString(R.string.dialog_typeface_imported_empty)
                    ),
                    null,
                    false
                ) {}
            )
            return
        }
        val collections: MutableMap<String?, MutableList<FontLibraryEntry>> =
            LinkedHashMap()
        for (entry in entries) {
            collections.computeIfAbsent(entry.collectionId) { ArrayList() }
                .add(entry)
        }
        val fontLibraryStore = createFontLibraryStore()
        for (faces in collections.values) {
            val representative = faces[0]
            val label = if (faces.size == 1)
                resolveFontOptionLabel(representative)
            else
                activity.getString(
                    R.string.dialog_typeface_collection_label,
                    representative.collectionDisplayName, faces.size
                )
            val preview = resolveTypefaceOptionPreview(
                TypefaceOption(representative.id, label), fontLibraryStore
            )
            val selected: Boolean = containsTypefaceId(faces, state.selectedTypefaceId)
            listView.addView(
                createTypefaceOptionRow(
                    listView,
                    TypefaceOption(representative.id, label), preview, selected,
                    Runnable {
                        if (faces.size == 1) {
                            selectImportedTypeface(
                                faces[0], selectorButton, state,
                                onSelectionChanged
                            )
                            bindImportedTypefaceCollectionRows(
                                listView, listFontLibraryEntries(),
                                selectorButton, state, onSelectionChanged
                            )
                            return@Runnable
                        }
                        showTypefaceFaceSelection(
                            faces, selectorButton, state, onSelectionChanged
                        ) {
                            bindImportedTypefaceCollectionRows(
                                listView,
                                listFontLibraryEntries(), selectorButton, state,
                                onSelectionChanged
                            )
                        }
                    })
            )
        }
    }

    private fun showTypefaceFaceSelection(
        faces: MutableList<FontLibraryEntry>,
        selectorButton: MaterialButton,
        state: AppConfigDialogState,
        onSelectionChanged: Runnable?,
        onSelectionApplied: Runnable?
    ) {
        val labels = arrayOfNulls<String>(faces.size)
        var selectedIndex = -1
        for (index in faces.indices) {
            val face = faces[index]
            labels[index] = resolveFontOptionLabel(face)
            if (face.id == state.selectedTypefaceId) {
                selectedIndex = index
            }
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.dialog_typeface_face_select_title)
            .setSingleChoiceItems(
                labels,
                selectedIndex
            ) { dialogInterface: DialogInterface?, which: Int ->
                selectImportedTypeface(
                    faces[which],
                    selectorButton,
                    state,
                    onSelectionChanged
                )
                dialogInterface?.dismiss()
                onSelectionApplied?.run()
            }
            .create()
        dialog.show()
        DialogWindowSizer.applyStandardWidth(dialog, activity)
    }

    private fun selectImportedTypeface(
        entry: FontLibraryEntry,
        selectorButton: MaterialButton,
        state: AppConfigDialogState,
        onSelectionChanged: Runnable?
    ) {
        state.selectedTypefaceId = entry.id
        selectorButton.text = formatTypefaceSelectorText(resolveFontOptionLabel(entry))
        onSelectionChanged?.run()
    }

    private fun applyTypefaceDialogListHeight(root: View?) {
        val scrollView = root?.findViewById<View>(R.id.typeface_scroll) ?: return
        val availableHeight = activity.resources
            .displayMetrics
            .heightPixels
        val reservedHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            260f,
            activity.resources.displayMetrics
        ).roundToInt()
        val configuredHeight = activity.resources.getDimensionPixelSize(
            R.dimen.dialog_typeface_list_height
        )
        val maxListHeight = max(0, (availableHeight * 0.82f).toInt() - reservedHeight)
        val params = scrollView.layoutParams
        params.height = min(configuredHeight, maxListHeight)
        scrollView.layoutParams = params
    }

    private fun bindTypefaceOptionRows(
        listView: LinearLayout,
        options: MutableList<TypefaceOption>,
        selectorButton: MaterialButton,
        state: AppConfigDialogState,
        onSelectionChanged: Runnable?,
        dialogHolder: Array<AlertDialog?>?,
        editableImportedRows: Boolean
    ) {
        listView.removeAllViews()
        val fontLibraryStore = if (editableImportedRows) createFontLibraryStore() else null
        for (option in options) {
            val row = createTypefaceOptionRow(
                listView,
                option,
                resolveTypefaceOptionPreview(option, fontLibraryStore),
                option.matches(state.selectedTypefaceId),
                Runnable {
                    if (option.isDisabled()) {
                        return@Runnable
                    }
                    state.selectedTypefaceId = option.id
                    selectorButton.text = formatTypefaceSelectorText(option.label)
                    onSelectionChanged?.run()
                    bindTypefaceOptionRows(
                        listView,
                        if (editableImportedRows)
                            buildImportedTypefaceOptions(
                                listFontLibraryEntries(),
                                state.selectedTypefaceId
                            )
                        else
                            buildSystemTypefaceOptions(
                                SystemFontRegistry.listRecommendedFonts(), state.selectedTypefaceId
                            ),
                        selectorButton,
                        state,
                        onSelectionChanged,
                        dialogHolder,
                        editableImportedRows
                    )
                })
            listView.addView(
                row, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun createTypefaceOptionRow(
        parent: ViewGroup,
        option: TypefaceOption,
        previewTypeface: Typeface?,
        selected: Boolean,
        onSelect: Runnable
    ): View {
        val row = FrameLayout(activity)
        val rowPaddingVertical = dimenPx(R.dimen.dialog_typeface_option_row_padding_vertical)
        row.setPadding(0, rowPaddingVertical, 0, rowPaddingVertical)
        val optionButton = createTypefaceOptionButton(
            parent, option.label, previewTypeface, selected
        )
        optionButton.isEnabled = !option.isDisabled()
        optionButton.setOnClickListener { _: View? -> onSelect.run() }
        row.addView(
            optionButton, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dimenPx(R.dimen.dialog_typeface_option_min_height),
                Gravity.CENTER_VERTICAL
            )
        )
        return row
    }

    private fun createTypefaceOptionButton(
        parent: ViewGroup,
        text: String?,
        previewTypeface: Typeface?,
        selected: Boolean
    ): MaterialButton {
        val button = MaterialButton(activity)
        button.text = text
        previewTypeface?.let(button::setTypeface)
        button.maxLines = 1
        button.ellipsize = TextUtils.TruncateAt.END
        button.minWidth = 0
        button.minHeight = dimenPx(R.dimen.dialog_typeface_option_min_height)
        button.insetTop = 0
        button.insetBottom = 0
        button.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        button.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        val paddingHorizontal = dimenPx(R.dimen.dialog_typeface_option_padding_horizontal)
        button.setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
        button.cornerRadius = dimenPx(R.dimen.dialog_typeface_option_corner_radius)
        button.strokeWidth = 0
        val backgroundColor = if (selected)
            MaterialColors.getColor(
                parent,
                com.google.android.material.R.attr.colorSecondaryContainer
            )
        else
            MaterialColors.getColor(parent, com.google.android.material.R.attr.colorSurfaceVariant)
        val textColor = MaterialColors.getColor(
            parent,
            if (selected)
                androidx.appcompat.R.attr.colorPrimary
            else
                com.google.android.material.R.attr.colorOnSurface
        )
        button.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        button.setTextColor(textColor)
        return button
    }

    private fun resolveTypefaceOptionPreview(
        option: TypefaceOption?,
        fontLibraryStore: FontLibraryStore?
    ): Typeface? {
        if (option == null || option.id.isNullOrBlank() || option.isDisabled()) {
            return null
        }
        if (SystemFontRegistry.isSystemFontId(option.id)) {
            return SystemFontRegistry.loadTypeface(option.id)
        }
        if (fontLibraryStore == null) {
            return null
        }
        val entry = fontLibraryStore.findById(option.id) ?: return null
        val fontFile = fontLibraryStore.resolveFontFile(option.id) ?: return null
        return FontTypefaceLoader.load(fontFile, entry.ttcIndex)
    }

    private fun formatTypefaceSelectorText(displayText: String?): String {
        return activity.getString(R.string.dialog_typeface_selector_value, displayText)
    }

    private fun formatMissingTypefaceLabel(typefaceId: String?): String {
        val displayId = if (!typefaceId.isNullOrBlank())
            typefaceId
        else
            activity.getString(R.string.dialog_typeface_missing)
        return activity.getString(R.string.dialog_typeface_missing_named, displayId)
    }

    private fun dimenPx(resId: Int): Int {
        return activity.resources.getDimensionPixelSize(resId)
    }

    private fun listFontLibraryEntries(): MutableList<FontLibraryEntry> {
        return createFontLibraryStore().listFonts()
    }

    private fun createFontLibraryStore(): FontLibraryStore {
        return ConfigStoreFactory.createLocalUiFontLibraryStore(
            activity, DpisApplication.xposedService
        )
    }

    private fun updateDialogStatus(
        statusView: MaterialTextView,
        inScope: Boolean,
        scopeKnown: Boolean,
        dpisEnabled: Boolean,
        viewportInputView: TextInputEditText,
        viewportModeToggle: ModeToggle,
        fontInputView: TextInputEditText,
        fontModeToggle: ModeToggle,
        selectedTypefaceId: String?,
        systemHooksEnabled: Boolean,
        currentViewportApplyMode: String?
    ) {
        val viewportTargetSpec: ViewportTargetSpec = parseViewportTargetSpecOrNullSafe(
            viewportInputView, resolveViewportMode(viewportModeToggle)
        )
        val fontScalePercent: Int? = parseFontScalePercentOrNullSafe(fontInputView)
        val fontMode =
            if (fontScalePercent == null) FontApplyMode.OFF else resolveFontMode(fontModeToggle)
        val viewportApplyMode = if (viewportTargetSpec.isEnabled)
            ViewportApplyMode.normalize(currentViewportApplyMode)
        else
            ViewportApplyMode.OFF
        val dialogStatusText = AppStatusFormatter.formatCompact(
            activity.resources,
            StatusInput(
                inScope,
                scopeKnown,
                viewportTargetSpec,
                viewportApplyMode,
                fontScalePercent,
                fontMode,
                selectedTypefaceId,
                dpisEnabled
            )
        )
        val warnViewport = scopeKnown && AppStatusFormatter.shouldWarnViewportEmulation(
            viewportTargetSpec, viewportApplyMode, systemHooksEnabled, dpisEnabled
        )
        val warnFont = scopeKnown && AppStatusFormatter.shouldWarnFontEmulation(
            fontScalePercent, fontMode, systemHooksEnabled, dpisEnabled
        )
        if (warnViewport || warnFont) {
            val warnColor =
                MaterialColors.getColor(statusView, androidx.appcompat.R.attr.colorError)
            statusView.text = AppStatusFormatter.applyConfigSegmentsWarnStyle(
                dialogStatusText, warnColor, warnViewport, warnFont
            )
            return
        }
        statusView.text = dialogStatusText
    }

    fun syncHyperOsNativeProxyAfterSave(
        item: AppListItem?, views: AppConfigDialogViews, state: AppConfigDialogState
    ) {
        if (!host.isHyperOsNativeProxyCandidate(item)) {
            return
        }
        setSaveAndResetButtonsEnabled(views, false)
        val onFinished = Runnable {
            setSaveAndResetButtonsEnabled(views, true)
        }
        if (state.dpisEnabled && hasActiveDialogConfig(views, state)) {
            host.applyHyperOsNativeProxy(item, onFinished)
            return
        }
        host.unmountHyperOsNativeProxy(item, onFinished)
    }

    // State lifecycle is bound to the dialog instance; pending flag cleanup is
    // best-effort since the state object is discarded when the dialog closes.
    fun requestScopeAfterSuccessfulSave(
        dialogView: View,
        item: AppListItem,
        views: AppConfigDialogViews,
        state: AppConfigDialogState,
        style: AppConfigDialogActionStyle,
        systemHooksEnabled: Boolean
    ) {
        if (!state.scopeKnown || state.scopeSelected || state.scopeRequestPending) {
            return
        }
        state.scopeRequestPending = true
        val requestStarted = host.requestScope(
            item,
            Runnable {
                if (!dialogView.isAttachedToWindow) {
                    return@Runnable
                }
                state.scopeSelected = true
                refreshDialogState(views, state, style, systemHooksEnabled, item)
            }
        ) { state.scopeRequestPending = false }
        if (requestStarted) {
            host.showToast(R.string.save_scope_request_notice)
        } else {
            state.scopeRequestPending = false
        }
    }

    fun bindViewportInputHint(viewportInputLayout: TextInputLayout?, viewportTargetType: String?) {
        if (viewportInputLayout == null) {
            return
        }
        viewportInputLayout.setHint(
            if (ViewportTargetType.RELATIVE_SCALE == ViewportTargetType.normalize(viewportTargetType))
                R.string.dialog_viewport_hint_scale
            else
                R.string.dialog_viewport_hint_absolute
        )
    }

    private fun bindScopeButton(
        scopeButton: MaterialButton,
        inScope: Boolean,
        scopeKnown: Boolean,
        defaultBgTint: ColorStateList?,
        defaultStrokeWidth: Int,
        defaultTextColor: Int
    ) {
        val activeBgColor = MaterialColors.getColor(
            scopeButton, com.google.android.material.R.attr.colorSecondaryContainer
        )
        val activeFgColor = MaterialColors.getColor(
            scopeButton, com.google.android.material.R.attr.colorOnSecondaryContainer
        )
        scopeButton.setIcon(null)
        val scopeTextRes = if (scopeKnown && inScope)
            R.string.dialog_scope_in_scope
        else
            R.string.dialog_scope_apply
        scopeButton.setText(scopeTextRes)
        val activeScopeStyle = scopeKnown && inScope
        scopeButton.backgroundTintList = if (activeScopeStyle)
            ColorStateList.valueOf(activeBgColor)
        else
            defaultBgTint
        scopeButton.setTextColor(if (activeScopeStyle) activeFgColor else defaultTextColor)
        scopeButton.strokeWidth = if (activeScopeStyle) 0 else defaultStrokeWidth
        scopeButton.contentDescription = activity.getString(scopeTextRes)
        scopeButton.isEnabled = scopeKnown
        scopeButton.alpha = if (scopeKnown) 1f else 0.6f
    }

    private fun bindDpisToggleButton(
        dpisToggleButton: MaterialButton,
        dpisEnabled: Boolean,
        defaultBgTint: ColorStateList?,
        defaultStrokeWidth: Int,
        defaultTextColor: Int
    ) {
        val buttonText = activity.getString(
            if (dpisEnabled) R.string.dialog_config_disable else R.string.dialog_config_disabled
        )
        dpisToggleButton.text = buttonText
        dpisToggleButton.setIcon(null)
        val activeBgColor = MaterialColors.getColor(
            dpisToggleButton, com.google.android.material.R.attr.colorSecondaryContainer
        )
        val activeFgColor = MaterialColors.getColor(
            dpisToggleButton, com.google.android.material.R.attr.colorOnSecondaryContainer
        )
        dpisToggleButton.backgroundTintList = if (dpisEnabled) defaultBgTint else ColorStateList.valueOf(activeBgColor)
        dpisToggleButton.setTextColor(if (dpisEnabled) defaultTextColor else activeFgColor)
        dpisToggleButton.strokeWidth = if (dpisEnabled) defaultStrokeWidth else 0
        dpisToggleButton.contentDescription = buttonText
        dpisToggleButton.isEnabled = true
        dpisToggleButton.alpha = 1f
    }

    fun bindFontHookDomainsButton(
        button: MaterialButton,
        item: AppListItem?,
        state: AppConfigDialogState?
    ) {
        val buttonText = host.getFontHookDomainsButtonText(item, state)
        button.text = buttonText
        button.setIcon(null)
        button.contentDescription = buttonText
    }

    class ModeToggle(
        container: View, thumb: View, emulationLabel: MaterialTextView,
        replaceLabel: MaterialTextView
    ) : AppConfigDialogModeToggle(container, thumb, emulationLabel, replaceLabel)

    class AppConfigDialogViews internal constructor(
        iconView: ImageView,
        titleView: MaterialTextView,
        packageView: MaterialTextView,
        statusView: MaterialTextView,
        viewportInputLayout: TextInputLayout,
        viewportInputView: TextInputEditText,
        fontInputLayout: TextInputLayout,
        fontInputView: TextInputEditText,
        viewportModeToggle: ModeToggle,
        fontModeToggle: ModeToggle,
        typefaceSelectorButton: MaterialButton,
        scopeButton: MaterialButton,
        startButton: MaterialButton,
        restartButton: MaterialButton,
        stopButton: MaterialButton,
        dpisToggleButton: MaterialButton,
        fontHookDomainsButton: MaterialButton,
        disableButton: MaterialButton,
        saveButton: MaterialButton,
        feedbackDiagnosticButton: MaterialButton
    ) : com.dpis.module.appconfig.AppConfigDialogViews(
        iconView, titleView, packageView, statusView, viewportInputLayout,
        viewportInputView, fontInputLayout, fontInputView, viewportModeToggle,
        fontModeToggle, typefaceSelectorButton, scopeButton, startButton,
        restartButton, stopButton, dpisToggleButton, fontHookDomainsButton,
        disableButton, saveButton, feedbackDiagnosticButton
    )

    class AppConfigDialogState(
        scopeSelected: Boolean,
        scopeKnown: Boolean,
        dpisEnabled: Boolean,
        previewFromGlobalPrefill: Boolean,
        packageName: String,
        draftFontHookDomainsRaw: String?,
        viewportApplyMode: String?,
        selectedTypefaceId: String?,
        initialViewportType: String?,
        initialViewportInput: String?,
        initialViewportScaleInput: String?,
        initialViewportAbsoluteInput: String?
    ) : AppConfigDialogStateModel(
        scopeSelected, scopeKnown, dpisEnabled, previewFromGlobalPrefill, packageName,
        draftFontHookDomainsRaw, viewportApplyMode, selectedTypefaceId,
        initialViewportType, initialViewportInput, initialViewportScaleInput,
        initialViewportAbsoluteInput
    ) {
        @JvmField
        var scopeRequestPending: Boolean = false

        companion object {
            @JvmStatic
            fun fromItem(item: AppListItem): AppConfigDialogState {
                val viewportInput =
                    AppConfigInputValidation.formatViewportInput(item.viewportTargetSpec)
                val viewportTargetType: String = initialViewportTargetType(item)
                val viewportScaleInput = if (item.viewportScaleMilliPercent != null)
                    AppConfigInputValidation.formatScaleMilliPercentInput(item.viewportScaleMilliPercent)
                else
                    (if (item.viewportTargetSpec.isRelativeScale) viewportInput else "")
                val viewportAbsoluteInput =
                    if (item.viewportWidthDp != null) item.viewportWidthDp.toString() else
                        (if (item.viewportTargetSpec.isAbsoluteDp) viewportInput else "")
                return AppConfigDialogState(
                    item.inScope,
                    item.scopeKnown,
                    item.dpisEnabled,
                    item.previewFromGlobalPrefill,
                    item.packageName,
                    item.effectiveFontHookDomainsRaw(),
                    item.viewportMode,
                    item.typefaceId,
                    viewportTargetType,
                    viewportInput,
                    viewportScaleInput,
                    viewportAbsoluteInput
                )
            }
        }
    }

    class AppConfigDialogActionStyle internal constructor(
        defaultActionBgTint: ColorStateList?,
        defaultActionStrokeWidth: Int,
        defaultActionTextColor: Int
    ) : com.dpis.module.appconfig.AppConfigDialogActionStyle(
        defaultActionBgTint,
        defaultActionStrokeWidth,
        defaultActionTextColor
    )

    private class TypefaceOption(id: String?, label: String) : TypefaceOptionModel(id, label)
    companion object {
        // TODO: Move to ModalDialog when this legacy binder no longer exposes AlertDialog handles.
        private const val MODE_TOGGLE_ANIM_DURATION_MS = 200L
        @JvmStatic
        fun stateFor(dialogView: View?): AppConfigDialogState? {
            val tag = dialogView?.getTag(R.id.dialog_save_button)
            return tag as? AppConfigDialogState
        }

        @JvmStatic
        fun viewsFor(dialogView: View?): AppConfigDialogViews? {
            val tag = dialogView?.getTag(R.id.dialog_font_hook_domains_button)
            return tag as? AppConfigDialogViews
        }

        @JvmStatic
        fun captureDialogActionStyle(baseButton: MaterialButton): AppConfigDialogActionStyle {
            val defaultActionBgTint = baseButton.backgroundTintList
            val defaultActionStrokeWidth = baseButton.strokeWidth
            val defaultActionTextColor = MaterialColors.getColor(
                baseButton, androidx.appcompat.R.attr.colorPrimary
            )
            return AppConfigDialogActionStyle(
                defaultActionBgTint,
                defaultActionStrokeWidth, defaultActionTextColor
            )
        }

        private fun containsTypefaceId(
            entries: MutableList<FontLibraryEntry>,
            typefaceId: String?
        ): Boolean {
            if (typefaceId == null) {
                return false
            }
            for (entry in entries) {
                if (typefaceId == entry.id) {
                    return true
                }
            }
            return false
        }

        private fun resolveFontOptionLabel(entry: FontLibraryEntry): String {
            val source = entry.sourceFileName ?: ""
            val display =
                if (entry.displayName != null) entry.displayName.trim { it <= ' ' } else ""
            if (!display.isEmpty() && display != source.trim { it <= ' ' }) {
                return display
            }
            return stripFontExtension(source)
        }

        private fun stripFontExtension(sourceFileName: String?): String {
            if (sourceFileName.isNullOrBlank()) {
                return "Imported font"
            }
            val trimmed = sourceFileName.trim { it <= ' ' }
            val lower = trimmed.lowercase()
            if (lower.endsWith(".ttf") || lower.endsWith(".otf")) {
                return trimmed.substring(0, trimmed.length - 4)
            }
            return trimmed
        }

        private fun formatViewportInput(spec: ViewportTargetSpec?): String? {
            return AppConfigInputValidation.formatViewportInput(spec)
        }

        private fun containsSystemTypeface(
            entries: MutableList<SystemFontEntry>,
            selectedTypefaceId: String?
        ): Boolean {
            for (entry in entries) {
                if (entry.id() == selectedTypefaceId) {
                    return true
                }
            }
            return false
        }

        private fun containsImportedTypeface(
            entries: MutableList<FontLibraryEntry>,
            selectedTypefaceId: String?
        ): Boolean {
            for (entry in entries) {
                if (entry.id == selectedTypefaceId) {
                    return true
                }
            }
            return false
        }

        private fun normalizeTypefaceId(typefaceId: String?): String? {
            return if (!typefaceId.isNullOrBlank()) typefaceId else null
        }

        @JvmStatic
        fun showSaveButtonFeedback(saveButton: MaterialButton?) {
            if (saveButton == null) {
                return
            }
            val restoreText: CharSequence?
            val tag = saveButton.tag as? Array<*>
            if (tag != null && tag[0] is CharSequence) {
                restoreText = tag[0] as CharSequence
                if (tag[1] is Runnable) {
                    saveButton.removeCallbacks(tag[1] as Runnable)
                }
            } else {
                restoreText = saveButton.text
            }
            saveButton.setText(R.string.status_save_success_inline)
            val restore = Runnable {
                if (saveButton.isAttachedToWindow) {
                    saveButton.text = restoreText
                }
            }
            saveButton.tag = arrayOf<Any?>(restoreText, restore)
            saveButton.postDelayed(restore, 1500)
        }

        @JvmStatic
        fun updateSaveButtonState(
            viewportInputLayout: TextInputLayout?,
            viewportInputView: TextInputEditText,
            viewportModeToggle: ModeToggle,
            fontInputLayout: TextInputLayout?,
            fontInputView: TextInputEditText,
            saveButton: MaterialButton
        ): Boolean {
            val viewportRaw = viewportInputView.text?.toString().orEmpty()
            val fontRaw = fontInputView.text?.toString().orEmpty()
            val viewportValid = AppConfigInputValidation.isViewportInputValid(
                viewportRaw, resolveViewportMode(viewportModeToggle)
            )
            val fontValid = AppConfigInputValidation.isFontScaleInputValid(fontRaw)
            ConfigValueInputErrorBinder.bindFullMessage(viewportInputLayout, viewportValid)
            ConfigValueInputErrorBinder.bindFullMessage(fontInputLayout, fontValid)
            val valid = viewportValid && fontValid
            saveButton.isEnabled = valid
            return valid
        }

        @JvmStatic
        fun updateSaveButtonState(dialogView: View?, views: AppConfigDialogViews): Boolean {
            val genericValid: Boolean = updateSaveButtonState(
                views.viewportInputLayout,
                views.viewportInputView,
                views.viewportModeToggle,
                views.fontInputLayout,
                views.fontInputView,
                views.saveButton
            )
            val valid = genericValid && WechatDpiSheetBinder.isInputValid(dialogView)
            views.saveButton.isEnabled = valid
            return valid
        }

        private fun hasActiveDialogConfig(
            views: AppConfigDialogViews,
            state: AppConfigDialogState
        ): Boolean {
            return parseViewportTargetSpecOrNullSafe(
                views.viewportInputView,
                resolveViewportMode(views.viewportModeToggle)
            ).isEnabled
                    || parsePositiveIntOrNullSafe(views.fontInputView) != null || !state.selectedTypefaceId.isNullOrBlank()
        }

        private fun setSaveAndResetButtonsEnabled(views: AppConfigDialogViews, enabled: Boolean) {
            views.saveButton.isEnabled = enabled
            views.disableButton.isEnabled = enabled
        }

        private fun parsePositiveIntOrNullSafe(inputView: TextInputEditText): Int? {
            return parsePositiveIntOrNull(inputView)
        }

        private fun parseViewportTargetSpecOrNullSafe(
            inputView: TextInputEditText,
            viewportTargetType: String?
        ): ViewportTargetSpec {
            return parseViewportTargetSpecOrNull(inputView, viewportTargetType)
        }

        private fun parseFontScalePercentOrNullSafe(inputView: TextInputEditText): Int? {
            return parseFontScalePercentOrNull(inputView)
        }

        @JvmStatic
        fun resolveFontMode(fontModeToggle: ModeToggle): String {
            return AppConfigDialogModeLogic.resolveFontMode(fontModeToggle)
        }

        private fun initialFontMode(fontMode: String?): String {
            return AppConfigDialogInputLogic.initialFontMode(fontMode)
        }

        @JvmStatic
        fun resolveViewportMode(viewportModeToggle: ModeToggle): String {
            return AppConfigDialogModeLogic.resolveViewportMode(viewportModeToggle)
        }

        private fun initialViewportTargetType(item: AppListItem?): String {
            return AppConfigDialogInputLogic.initialViewportTargetType(item)
        }

        @JvmStatic
        fun bindFontModeToggle(
            fontModeToggle: ModeToggle,
            fontMode: String?,
            animate: Boolean
        ) {
            AppConfigDialogModeLogic.bindFontModeToggle(fontModeToggle, fontMode, animate)
        }

        @JvmStatic
        fun toggleFontMode(fontModeToggle: ModeToggle) {
            AppConfigDialogModeLogic.toggleFontMode(fontModeToggle)
        }

        @JvmStatic
        fun bindViewportModeToggle(
            viewportModeToggle: ModeToggle,
            viewportTargetType: String?,
            animate: Boolean
        ) {
            AppConfigDialogModeLogic.bindViewportModeToggle(
                viewportModeToggle,
                viewportTargetType,
                animate
            )
        }

        @JvmStatic
        fun toggleViewportMode(
            viewportModeToggle: ModeToggle,
            viewportInputView: TextInputEditText,
            state: AppConfigDialogState
        ) {
            AppConfigDialogModeLogic.toggleViewportMode(
                viewportModeToggle,
                viewportInputView,
                state
            )
        }

        @JvmStatic
        fun switchViewportTargetType(
            viewportModeToggle: ModeToggle,
            viewportInputView: TextInputEditText,
            state: AppConfigDialogState,
            nextType: String?,
            animate: Boolean
        ) {
            AppConfigDialogModeLogic.switchViewportTargetType(
                viewportModeToggle, viewportInputView, state, nextType, animate
            )
        }

        fun updateModeToggleVisual(
            toggle: ModeToggle,
            emulationActive: Boolean,
            animate: Boolean
        ) {
            toggle.emulationActive = emulationActive
            val activeTextColor = MaterialColors.getColor(
                toggle.container, com.google.android.material.R.attr.colorOnSecondaryContainer
            )
            val inactiveTextColor = MaterialColors.getColor(
                toggle.container, com.google.android.material.R.attr.colorOnSurface
            )
            toggle.emulationLabel.setTextColor(if (emulationActive) activeTextColor else inactiveTextColor)
            toggle.replaceLabel.setTextColor(if (emulationActive) inactiveTextColor else activeTextColor)
            toggle.emulationLabel.alpha = if (emulationActive) 1f else 0.66f
            toggle.replaceLabel.alpha = if (emulationActive) 0.66f else 1f
            toggle.emulationLabel.setTypeface(
                Typeface.DEFAULT,
                if (emulationActive) Typeface.BOLD else Typeface.NORMAL
            )
            toggle.replaceLabel.setTypeface(
                Typeface.DEFAULT,
                if (emulationActive) Typeface.NORMAL else Typeface.BOLD
            )
            toggle.emulationLabel.scaleX = if (emulationActive) 1.04f else 1f
            toggle.emulationLabel.scaleY = if (emulationActive) 1.04f else 1f
            toggle.replaceLabel.scaleX = if (emulationActive) 1f else 1.04f
            toggle.replaceLabel.scaleY = if (emulationActive) 1f else 1.04f
            toggle.container.post(Runnable {
                installModeToggleLayoutObserver(toggle)
                val half: Int = updateModeToggleThumbLayout(toggle)
                if (half <= 0) {
                    return@Runnable
                }
                val target = if (emulationActive) 0f else half.toFloat()
                if (animate) {
                    toggle.thumb.animate().cancel()
                    toggle.thumb.animate()
                        .translationX(target)
                        .translationY(0f)
                        .setDuration(MODE_TOGGLE_ANIM_DURATION_MS)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                } else {
                    toggle.thumb.translationX = target
                    toggle.thumb.translationY = 0f
                }
            })
        }

        /** Keeps the animated thumb half-width even when the landscape parent is measured later.  */
        private fun updateModeToggleThumbLayout(toggle: ModeToggle?): Int {
            if (toggle == null) {
                return 0
            }
            val track: View = modeToggleTrack(toggle)
            val availableWidth = (track.width
                    - track.paddingLeft
                    - track.paddingRight)
            if (availableWidth <= 0) {
                return 0
            }
            val half = availableWidth / 2
            var params = toggle.thumb.layoutParams
            if (params == null) {
                params = ViewGroup.LayoutParams(
                    half,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            if (params.width != half || params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                params.width = half
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                toggle.thumb.layoutParams = params
            }
            toggle.thumb.translationY = 0f
            toggle.thumb.translationX = if (modeUsesStartThumb(toggle)) 0f else half.toFloat()
            return half
        }

        private fun modeToggleTrack(toggle: ModeToggle): View {
            return if (toggle.thumb.parent is View)
                toggle.thumb.parent as View
            else
                toggle.container
        }

        /**
         * Runs after the complete view-tree measure/layout pass. A child LayoutParams mutation made
         * from an OnLayoutChange callback can otherwise leave the thumb's measured width at zero on
         * the first landscape detail creation.
         */
        private fun installModeToggleLayoutObserver(toggle: ModeToggle?) {
            if (toggle == null || !toggle.container.isAttachedToWindow) {
                return
            }
            val existingListener = toggle.container.getTag(R.id.mode_toggle_layout_listener)
            if (existingListener is OnGlobalLayoutListener) {
                return
            }
            val track: View = modeToggleTrack(toggle)
            val listener =
                OnGlobalLayoutListener { updateModeToggleThumbLayout(toggle) }
            toggle.container.setTag(R.id.mode_toggle_layout_listener, listener)
            track.viewTreeObserver.addOnGlobalLayoutListener(listener)
        }

        private fun modeUsesStartThumb(toggle: ModeToggle): Boolean {
            val modeTag = toggle.container.tag
            if (FontApplyMode.SYSTEM_EMULATION == modeTag
                || ViewportTargetType.RELATIVE_SCALE == modeTag
            ) {
                return true
            }
            if (FontApplyMode.FIELD_REWRITE == modeTag
                || ViewportTargetType.ABSOLUTE_DP == modeTag
            ) {
                return false
            }
            return toggle.emulationActive
        }
    }
}
