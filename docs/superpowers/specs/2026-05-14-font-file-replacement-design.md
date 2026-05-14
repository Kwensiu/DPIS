# Per-App Font File Replacement Design

Date: 2026-05-14

## Goal

Add a minimum viable per-app font file replacement feature to DPIS.

Users can import `.ttf` or `.otf` files into a global DPIS font library, then choose one imported font for a target app. The first version targets ordinary Android Java text paths only. HyperOS Flutter/native font file replacement remains a later experimental path.

## Non-Goals

- Do not replace HyperOS Gallery/Weather Flutter/native text fonts in this MVP.
- Do not remap Android system font family names globally.
- Do not add downloadable fonts or font provider integration.
- Do not manage multi-weight font families as a family set.
- Do not bundle third-party fonts in the APK.
- Do not include font binary files in config backup.

## Key Design Constraint

Font file replacement is a separate runtime feature from DP/viewport override and font-size override.

The existing DP/font-size modes, including "emulation" and "replacement", continue to describe viewport and font scale behavior. Font file replacement must use its own configuration key, package-plan flag, and hook installer. It must not be folded into `FontApplyMode`, `FontScaleOverride`, or field-rewrite text-size logic. This keeps `Configuration.fontScale`, `Paint` text-size rewriting, and `Typeface` replacement from fighting each other.

## Evidence and References

Android provides official APIs for this chain:

- `Typeface.createFromFile(File/String)` can create a `Typeface` from a font file.
- `TextView#setTypeface(...)` applies a typeface to ordinary Android text views.
- `Paint#setTypeface(...)` controls the typeface used for text drawing and measuring.

Official API references:

- https://developer.android.com/reference/android/graphics/Typeface
- https://developer.android.com/reference/android/widget/TextView
- https://developer.android.com/reference/android/graphics/Paint

Project references already support the boundary:

- Existing Java font-size replacement lives in app-process hooks such as `ForceTextSizeHookInstaller`.
- Existing HyperOS docs distinguish ordinary Java text from Flutter/Rust/native text paths.
- Existing app-process hook orchestration is centralized in `AppProcessHookInstaller`.

## User Flow

### Font Library

The DPIS settings screen adds a "Font library" entry. This is inside DPIS's own settings UI, not Android system Settings.

The font library supports:

- Import a `.ttf` or `.otf` file with `ACTION_OPEN_DOCUMENT`.
- List imported fonts.
- Delete imported fonts that are not used by any app.

Deletion is blocked when any package references the font. The user must clear those app selections first.

### App Configuration

The per-app configuration dialog adds a font selector separate from the existing font-size input and font-size mode toggle.

Choices:

- System default
- Each imported font from the DPIS font library

Selecting "System default" clears the app's font file replacement config. Selecting an imported font writes the selected font ID.

## Data Model

### Font Library Store

Add a `FontLibraryStore` responsible for imported font metadata and files.

Each font record contains:

- `id`: stable ID derived from the font file hash, for example a short sha256 prefix.
- `displayName`: user-facing name, initially based on the source file name.
- `sourceFileName`: original file name.
- `storedFileName`: managed internal file name.
- `sha256`: full file hash.
- `importedAtEpochMs`: import timestamp.

The store owns the font directory, for example `files/fonts/`.

Import behavior:

- Read the selected URI through `ContentResolver.openInputStream`.
- Copy into a temporary file.
- Hash while copying or immediately after copying.
- Validate by calling `Typeface.createFromFile(...)`.
- Move or rename the file to a stable managed filename.
- Add or reuse the metadata record.

### Per-Package Config

Extend `DpiConfigStore` with a package-level key:

```text
font.<package>.typeface_id
```

Required store behavior:

- `getTargetTypefaceId(packageName)`
- `setTargetTypefaceId(packageName, typefaceId)`
- `clearTargetTypefaceId(packageName)`
- `hasPrimaryTargetTypefaceId(packageName)`

Package membership must count typeface config as a real target config. An app with only `font.<package>.typeface_id` must remain in `target_packages` and receive app-process hooks.

`clearTargetPackageConfig(packageName)` must remove the typeface ID as well as viewport and font-size settings.

## Runtime Plan

### Package Planning

Extend `ModulePackagePlan` with a typeface flag, separate from `fontScaleActive` and `fontEnabled`.

The package plan should consider an app active when:

- DPIS is enabled for the target package, and
- at least one of these exists:
  - viewport width config
  - font-scale config
  - valid typeface ID config

Typeface replacement should not require a font-scale percentage.

### Hook Installation

Add `TypefaceOverrideHookInstaller`.

`AppProcessHookInstaller` installs it when the current package has an enabled, valid typeface ID.

The installer:

- Resolves the selected typeface ID through `FontLibraryStore`.
- Loads the font with `Typeface.createFromFile(path)`.
- Caches the loaded typeface.
- Hooks ordinary Java text paths with libxposed protective mode.

Initial hook targets:

- `TextView#setTypeface(Typeface)`
- `TextView#setTypeface(Typeface, int)`
- `Paint#setTypeface(Typeface)`

`TextPaint` extends `Paint`, so the first implementation can rely on `Paint#setTypeface` and add a direct `TextPaint` hook only if real-device evidence shows gaps.

### Replacement Strategy

Default behavior is strong replacement with style preservation:

- If the app passes `null`, use the imported base typeface.
- If the app passes a typeface with bold or italic style, create a styled typeface from the imported font and the original style.
- If `TextView#setTypeface(Typeface, int)` supplies a style argument, prefer that explicit style.
- If styled creation fails, fall back to the imported base typeface.
- If loading or replacement fails, leave the original typeface untouched.

Alternative policies are reserved for later:

- Replace only default fonts and preserve app custom fonts.
- Force the exact imported typeface without preserving style.

## File Access Boundary

The largest technical risk is target-process readability.

The hook runs inside the target app process, but the imported file is owned by DPIS. The MVP should first copy fonts to a DPIS-managed path and try to make the managed font file readable by target processes. Real-device validation must confirm whether target apps can call `Typeface.createFromFile(path)` on that path under the current LSPosed/Android environment.

If the target process cannot read the file:

- Skip replacement.
- Log a clear `DPIS_FONT_STYLE` message once per package/font.
- Keep the target app running normally.

Future fallback options, not in MVP:

- Root-assisted copy into a target-readable location.
- Xposed service mediated file staging.
- Target-app sibling file staging for selected native-like cases.

## Error Handling

Import failure:

- Invalid extension or unreadable URI: show import failure.
- `Typeface.createFromFile(...)` validation failure: do not register the font.
- Duplicate sha256: reuse the existing record or report that it is already imported.
- Copy failure: remove partial files and do not update metadata.

Runtime failure:

- Missing typeface ID: skip hook and log.
- Missing font file: skip hook and log.
- Unreadable font file in target process: skip hook and log.
- Hook exception: protective mode proceeds with the app's original call.
- Styled typeface creation failure: fall back to base imported font, then original typeface if needed.

Delete failure:

- Referenced font: block deletion and show a message.
- File delete failure: keep metadata and show failure.
- Metadata delete failure: keep file to avoid orphaned config.

Backup and restore:

- Existing JSON config backup may include typeface IDs and library metadata only if those preferences are included.
- The MVP does not include font binary files in backups.
- Restored configs that reference missing font files are treated as inactive at runtime and reported in logs/UI where practical.

## UI Changes

### Settings Screen

Add a "Font library" row to `SystemServerSettingsActivity`.

The font library dialog or bottom sheet contains:

- Import button.
- Imported font list.
- Delete action for unused fonts.
- Empty state when no fonts are imported.

### App Config Dialog

Add a compact font selector below or near the existing font-size controls.

The selector is visually and semantically separate from:

- Font-size percent input.
- Font-size emulation/replacement toggle.

This reinforces that font file replacement is not another `FontApplyMode`.

## Testing

### Unit Tests

`DpiConfigStoreTest`:

- Save, read, and clear `typeface_id`.
- A package with only `typeface_id` remains in `target_packages`.
- Clearing the last config removes the package from `target_packages`.
- `clearTargetPackageConfig` clears the typeface ID.

`FontLibraryStoreTest`:

- Stable ID generation from hash.
- Duplicate hash import behavior.
- Delete unused font.
- Reject delete when referenced by `DpiConfigStore`.
- Missing file and malformed metadata handling.

`TypefaceOverrideHookInstallerTest`:

- Resolve style from original typeface.
- Resolve style from explicit `TextView#setTypeface(..., style)` argument.
- Null original typeface uses imported base typeface.
- Failure returns the original typeface without throwing.

### Source Smoke Tests

- `AppProcessHookInstallerTest`: typeface-only config installs typeface hook.
- `ModulePackagePlanTest`: typeface-only config makes the package active.
- `SystemServerSettingsActivity` source smoke: settings page contains font library entry and import picker flow.
- `AppConfigDialogBinderSourceSmokeTest`: app dialog exposes typeface selection and save wiring.

### Real Device Validation

1. Import a `.ttf` or `.otf`.
2. Assign it to a normal Java Android app without changing DP or font size.
3. Restart the target app.
4. Confirm ordinary `TextView` text changes font.
5. Confirm target app does not crash.
6. Check logcat for:
   - font library resolution
   - hook installation
   - readable or unreadable file result
7. Try a self-drawn text app or screen as an exploratory check, not a hard MVP pass criterion.
8. Record that Gallery/Weather Flutter/native text is out of scope for MVP.

## Acceptance Criteria

- A user can import at least one valid `.ttf` or `.otf` into the DPIS font library.
- The imported font appears in the per-app config selector.
- A package can be configured with only a typeface ID and no DP/font-size setting.
- The target package receives app-process typeface hooks.
- Ordinary Java `TextView` text in a target app visibly uses the selected imported font on a real device, assuming the font file is readable by the target process.
- Deleting a font referenced by any app is blocked.
- Clearing an app's font selection returns it to system default font behavior after target app restart.
- Font file replacement does not alter existing DP/font-size mode behavior.
