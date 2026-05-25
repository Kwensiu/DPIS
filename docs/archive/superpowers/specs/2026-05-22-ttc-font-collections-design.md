# TTC Font Collections Design

## Context

DPIS currently treats each imported font file as one selectable face. That is valid for `.ttf` and `.otf`, but incomplete for TrueType Collection (`.ttc`) files because one physical file can contain multiple faces selected by `ttcIndex`.

Issue: https://github.com/Kwensiu/DPIS/issues/61

The current import validation rejects TTC files. Supporting TTC needs changes across import validation, font library metadata, preview loading, runtime loading, duplicate detection, and deletion safety.

## Goals

- Keep existing `.ttf` and `.otf` import behavior unchanged.
- Add `.ttc` import as an experimental feature gated by the Laboratory page.
- Avoid ambiguous default-index behavior. Users must choose which TTC face entries to import.
- Store `ttcIndex` per imported font library entry.
- Load previews and runtime replacements with the selected `ttcIndex`.
- Keep shared TTC files on disk while any imported face entry still references them.
- Preserve old font library metadata and saved app configs.

## Non-Goals

- Do not parse TTC name tables for human-readable face names in the first version.
- Do not add a global runtime kill switch for already imported TTC entries.
- Do not rework unrelated font hook domains or HyperOS native font behavior.
- Do not change existing `.ttf` or `.otf` ids, labels, or import flow.

Reading TTC name table family/subfamily names remains a follow-up enhancement. The first version intentionally uses index labels because name-table parsing adds encoding and localization choices that are not needed to make TTC face selection unambiguous.

## Platform Assumptions

DPIS currently has `minSdk = 26`. `Typeface.Builder` is available at this minimum API level, so TTC face validation and loading do not need a lower-API fallback.

## Experimental Gate

TTC support is controlled by a new global preference in `DpiConfigStore`, for example `font.ttc_import_enabled`.

The Laboratory page is currently an empty state. This feature will turn it into a real settings page with a switch row:

- Title: `TTC font collections`
- Hint: `Import TrueType Collection files as separate faces. Experimental.`
- Default: off

When the switch is off:

- The font picker and import validation do not accept `.ttc`.
- Existing `.ttc` entries remain visible and usable if they were imported while the switch was on.
- Runtime loading for already configured TTC entries continues to work.

When the switch is on:

- `.ttc` files are accepted by the import entry point.
- Importing a `.ttc` opens a face-selection flow before metadata is registered.

This makes the switch an experimental entry gate for new imports, not a destructive compatibility kill switch.

Import classification is based on file signature after copying to the temporary file, not only on extension or MIME type. A renamed TTC file with a `.ttf` or `.otf` suffix is treated as TTC. If the experiment is off, that file is rejected instead of being imported as a single non-TTC face.

## TTC Parsing And Validation

DPIS will add a small TTC container parser, such as `TtcFontCollectionParser`.

The parser only reads the TTC container layer:

- Validate the `ttcf` signature.
- Read the TTC version.
- Read `numFonts`.
- Read each face table directory offset.
- Reject malformed files, zero face count, unreasonable face count, negative/overflowing offsets, or offsets outside the file.

Use an explicit upper bound for face count, initially 128, to avoid expensive parsing of hostile or corrupt files.

After parsing, DPIS validates each candidate face by loading it with Android's public API:

```java
new Typeface.Builder(file)
        .setTtcIndex(index)
        .build();
```

Faces that parse but fail Android loading are filtered out of the selectable list. The UI reports how many faces were filtered. If no face is loadable, import fails.

The parser returns a detected font kind (`TTF`, `OTF`, or `TTC`) to the import flow. The store uses the detected kind, not the source filename extension, when choosing the managed file extension and id strategy.

## Import Flow

`.ttf` and `.otf` keep the existing one-step import flow.

For `.ttc`:

1. Copy the selected URI to a temporary file.
2. Parse the TTC header and validate candidate face indexes.
3. Validate each face through `Typeface.Builder#setTtcIndex`.
4. Show a multi-select dialog titled `选择导入 face`.
5. Use the source file name as the subtitle.
6. Keep a fixed summary area at the top of the list with two small rounded capsules:
   - `已选择 N`
   - `M 个 face 无法导入`
7. Hide failed faces from the list.
8. Show loadable faces as fallback labels: `<file name> (TTC <index>)`.
9. Default to no selected faces.
10. Provide compact select-all and deselect-all actions in the dialog.
11. Disable the import action until the user selects at least one face.
12. Register all selected faces as one batch.

After a successful multi-face import, show a success toast that reports the number of imported faces.

The temporary file created from the selected URI is deleted in every path: parse failure, validation failure, user cancellation, successful registration, and unexpected import errors. If process death occurs after publishing a shared TTC file but before metadata commit, the next font-library cleanup can remove the orphaned published file through the existing orphan-file cleanup path or an equivalent public-directory cleanup pass.

## Font Library Metadata

`FontLibraryEntry` gains an `int ttcIndex` field.

Backward compatibility:

- Old JSON entries without `ttcIndex` are valid.
- Missing `ttcIndex` defaults to `0`.
- Existing `.ttf` and `.otf` entries keep their current ids and behavior.

New id rules:

- `.ttf` and `.otf`: keep `font_<sha256 first 16 chars>`.
- `.ttc`: use `font_<sha256 first 16 chars>_ttc_<index>`.

This lets multiple faces from the same TTC file share the same physical file but remain independently selectable by id.

Metadata serialization writes `ttcIndex` for all new entries.

## Storage And Deletion

TTC entries from the same physical file share the same `storedPath`, `storedFileName`, and `sha256`, but have different `id`, `displayName`, and `ttcIndex`.

Duplicate handling:

- `.ttf` and `.otf` remain deduplicated by `sha256`.
- `.ttc` is deduplicated by `sha256 + ttcIndex`.
- Re-importing the same TTC face reuses the existing entry.
- Re-importing a different face from the same TTC creates or reuses that face's entry.

Registration uses a batch method for TTC imports. The method computes all new entries, publishes/copies the physical file once, and commits metadata once. If metadata persistence fails, it leaves no partial entry set.

For TTC imports, the physical file is copied or published once. Every entry created for selected faces references that same managed file path.

Deletion changes:

- Deleting a font entry still refuses when that entry id is referenced by app config.
- After removing metadata for one entry, only delete the physical file if no remaining entry references the same `storedPath`.
- If another entry still references the path, keep the physical file.
- `purgeOrphanedFiles()` continues to build a known-path set and therefore naturally preserves shared TTC files while any entry references them.

## Typeface Loading

Introduce one small loader helper, such as `FontTypefaceLoader`, to centralize imported font loading:

```java
Typeface load(File file, int ttcIndex)
```

The helper uses `Typeface.Builder(file).setTtcIndex(ttcIndex).build()` for TTC entries. Non-TTC entries keep the existing `Typeface.createFromFile(file)` path unless implementation testing shows the builder path is needed. All TTC consumers honor `ttcIndex`.

Consumers to update:

- Font library detail preview.
- App config font-option preview.
- Runtime replacement in `TypefaceOverrideHookInstaller`.

Runtime replacement must resolve the `FontLibraryEntry` first, not only the file, because the entry carries `ttcIndex`. The `/data/local/tmp` published-file fallback can locate the file, but it cannot infer the selected face without metadata.

## Published File Resolution

`PublishedFontFileResolver` recognizes `.ttc` files in addition to `.ttf` and `.otf`.

The resolver still maps by `typefaceId` to a physical published file. For TTC, the file path only identifies the shared collection; runtime code must use the matching `FontLibraryEntry.ttcIndex` to choose the face.

Multiple TTC `typefaceId` values can resolve to the same published `.ttc` file because their entries share `storedPath`; the id difference selects metadata, not a distinct physical file.

## Error Handling

- Unsupported extension or MIME while the experiment is off: reject as today.
- `ttcf` signature while the experiment is off, even with `.ttf` or `.otf` suffix: reject as TTC.
- Invalid TTC signature or malformed header: import fails.
- Face count is zero or above the configured maximum: import fails.
- Face offset is outside the file: import fails.
- Some parsed faces fail Android loading: filter them and show the failed-face count capsule.
- All parsed faces fail Android loading: import fails.
- User selects no faces: import action stays disabled.
- Batch metadata write fails: do not leave partial entries or orphaned published files.
- Temporary import files are deleted on every success, failure, and cancellation path.
- Delete fails after metadata update: restore metadata as existing delete handling does.

## Testing

Unit tests:

- `TtcFontCollectionParserTest`
  - Parses a minimal valid TTC header.
  - Rejects non-`ttcf` signatures.
  - Rejects zero count.
  - Rejects oversized count.
  - Rejects offsets outside the file.
  - Classifies renamed TTC files by `ttcf` signature rather than extension.

- `FontLibraryStoreTest`
  - Reads old entries without `ttcIndex` as index `0`.
  - Writes and reads `ttcIndex`.
  - Creates different ids for same TTC hash with different indexes.
  - Reuses same entry for duplicate `sha256 + ttcIndex`.
  - Deletes one TTC face without deleting the shared file.
  - Deletes the shared file when the last referencing face is deleted.
  - Batch TTC registration does not leave partial metadata on commit failure.
  - Batch TTC registration publishes one shared file for multiple selected faces.

- `PublishedFontFileResolverTest`
  - Resolves `.ttc` published files.
  - Allows multiple TTC ids to reference one shared physical file through metadata.

- Source smoke tests
  - `FontLibraryActivity` gates `.ttc` input behind the Laboratory switch.
  - Preview loading uses the centralized loader and `ttcIndex`.
  - `TypefaceOverrideHookInstaller` resolves entry metadata before loading imported TTC files.
  - `ExperimentalSettingsActivity` exposes the TTC switch row and stores it in `DpiConfigStore`.

Manual validation:

- With the switch off, `.ttc` is not accepted for new import.
- With the switch on, a real TTC opens the face-selection dialog.
- Loadable faces are listed, failed faces are counted but hidden.
- Default selection is empty and the import action is disabled.
- Select-all and deselect-all actions work for large face counts.
- Importing multiple faces creates multiple selectable entries.
- Applying different TTC indexes to a target app loads the corresponding face after app restart.
- Deleting one imported face does not break other faces from the same collection.
