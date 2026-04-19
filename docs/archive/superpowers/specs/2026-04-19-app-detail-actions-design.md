# App Detail Sheet Actions Design (2026-04-19)

## Scope
Redesign the action area in the app detail bottom sheet (`dialog_app_config`) to support:

- process operations: `启动` / `重启` / `停止`
- scope action: `加入作用域` / `移除作用域` (reuse current logic)
- DPIS effective toggle: `启用` / `禁用`
- parameter reset action: rename existing `禁用参数` semantics to `重置参数`
- keep `保存` as the primary full-width bottom action

Out of scope:

- changing existing scope-request backend logic
- introducing request-pending disable/debounce behavior
- changing global/system settings page behavior

## UX Goals
- Keep all non-save actions as one visual class in one action zone.
- Keep `保存` separate and dominant to avoid confusion between instant actions and parameter persistence.
- Preserve current mental model: scope/DPIS are independent from `保存`.
- Reduce accidental risk for system apps via explicit confirmation on process operations only.

## Layout
Within the existing sheet structure:

1. Parameter inputs (existing viewport/font fields + mode toggles).
2. Action row (same visual class):  
   `加入/移除作用域` | `启动` | `重启` | `停止` | `DPIS 启用/禁用`
3. Bottom row:  
   `保存` (primary, long/full-width dominant) + `重置参数` (secondary)

Notes:
- `重置参数` sits with `保存` in the bottom band but keeps secondary style.
- Existing button radius/spacing rhythm should stay consistent with current sheet style.

## Behavioral Contract

### 1) 加入/移除作用域
- Keep current `toggleScope` behavior and backend calls unchanged.
- Only align button placement/style and toast entry point.

### 2) 启动 / 重启 / 停止
- Execute immediately for normal apps.
- For system apps, show danger confirmation dialog before execution.
- Confirmation is required only for process actions.

### 3) DPIS 启用/禁用
- Instant toggle.
- No processing/loading state.
- Does not modify parameter values.

### 4) 重置参数
- Always available.
- Always resets only parameter domain:
  - clear two input fields
  - set both mode selectors to default `替换`
- Must not alter:
  - scope state
  - DPIS enable/disable state

### 5) 保存
- Saves only parameter domain:
  - input values
  - `伪装/替换` state
- Does not control:
  - scope state
  - DPIS enable/disable state

## Validation Rules
- `保存` enabled only when both inputs are valid (or empty per existing semantics).
- Invalid input must:
  - disable `保存`
  - set corresponding input border to warn/error color

## Status Text Rules
- When DPIS is `禁用`: status segment after `已注入 |` for width/font fields renders as `已禁用`.
- When DPIS is switched back to `启用`: status must show current actual configuration values.

## Technical Design

### UI Layer
- Update `dialog_app_config.xml` action area to new arrangement.
- Rename visible label from `禁用参数` to `重置参数`.

### Controller Layer (`MainActivity`)
- Keep existing scope logic paths.
- Add dedicated handlers:
  - process actions (start/restart/stop)
  - DPIS toggle state
  - reset-parameters action
- `saveAppConfig` remains parameter-only.

### State/Store Layer
- Introduce/consume per-app DPIS enabled flag in config store (if not already present in current branch implementation).
- Keep parameter values persisted even when DPIS is disabled.

### Formatting Layer
- Extend app status formatter rules to render `已禁用` view when DPIS is off.
- Preserve existing status structure; only substitute width/font value segments by DPIS state.

## Error Handling
- Any backend failure for action commands shows toast through unified toast helper.
- System-app process action cancel path must be silent (no mutation).
- Reset action is idempotent; repeated reset keeps empty/default state.

## Testing Strategy

### Unit tests
- status formatter:
  - DPIS off => width/font segments show `已禁用`
  - DPIS on => width/font segments show actual values
- reset behavior:
  - clears width/font values
  - sets modes to `替换`
  - does not mutate scope/DPIS flag

### Interaction checks (manual)
- Scope action behavior unchanged (join/remove + current notification behavior).
- Save disabled + input error border when invalid.
- Save does not alter scope/DPIS.
- Reset does not alter scope/DPIS.
- System app process actions require confirmation; normal app actions do not.

## Migration/Compatibility
- Existing saved parameter values remain valid.
- If DPIS flag is newly introduced, default should be `启用` for backward-compatible behavior.

## Acceptance Criteria
- New action layout appears in app detail sheet with specified grouping.
- `保存` and `重置参数` semantics are parameter-only.
- Scope logic behavior is unchanged.
- DPIS off/on status display follows defined rules.
- System app process actions require explicit confirmation.
