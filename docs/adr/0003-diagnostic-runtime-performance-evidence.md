---
status: accepted
---

# Diagnostic runtime performance evidence

DPIS feedback diagnostics use a one-action capture lifecycle: starting a
diagnostic starts runtime evidence and Perfetto capture, and returning to DPIS
stops both and packages the result immediately. The default Perfetto budget is
approximately 60 seconds, but an earlier return is always a hard stop and never
requires a second user action.

Runtime performance evidence belongs to the injected target process (or
`system_server` for system routes), not to the DPIS UI process. Target processes
aggregate route callbacks, decisions, mutations, skip reasons, and latency
locally, then send periodic snapshots plus limited slow-call samples to the
diagnostic transport. The exported report may classify evidence as observed,
correlated, likely contributor, or not supported; it must not claim that DPIS
caused a jank solely because a hook mutation was observed.

Background accuracy is intentionally unresolved. The implementation must expose
transport and capture completeness rather than treating missing background
snapshots as proof that no route executed.
