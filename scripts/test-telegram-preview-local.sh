#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/test-telegram-preview-local.sh [options]

Options:
  --run-number N   Use N as the preview run number. Default: 123.
  --sha SHA        Use SHA as the preview commit. Default: current git HEAD.
  --release        Build release APKs instead of debug APKs.
  --send           Send built release APKs to Telegram. Requires --release.
  --skip-build     Only calculate and print the preview version.
  -h, --help       Show this help.

Default behavior is a safe local dry-run:
  - calculate VERSION_NAME and VERSION_CODE like telegram-preview.yml
  - run Gradle with DPIS_VERSION_NAME / DPIS_VERSION_CODE
  - build modern101 + compat100 debug APKs

Release build requires local signing env vars:
  DPIS_RELEASE_STORE_FILE
  DPIS_RELEASE_STORE_PASSWORD
  DPIS_RELEASE_KEY_ALIAS
  DPIS_RELEASE_KEY_PASSWORD

Telegram send requires:
  TELEGRAM_BOT_TOKEN
  TELEGRAM_CHAT_ID or TELEGRAM_CHAT_IDS

Optional Telegram env vars:
  TELEGRAM_MESSAGE_THREAD_ID
  TELEGRAM_PROTECT_CONTENT=true
EOF
}

RUN_NUMBER=123
SHA=""
BUILD_RELEASE=false
SEND_TELEGRAM=false
SKIP_BUILD=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --run-number)
      RUN_NUMBER="${2:-}"
      shift 2
      ;;
    --sha)
      SHA="${2:-}"
      shift 2
      ;;
    --release)
      BUILD_RELEASE=true
      shift
      ;;
    --send)
      SEND_TELEGRAM=true
      shift
      ;;
    --skip-build)
      SKIP_BUILD=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ ! "$RUN_NUMBER" =~ ^[0-9]+$ ]] || (( RUN_NUMBER < 1 || RUN_NUMBER > 9999 )); then
  echo "Run number must be an integer in 1..9999: $RUN_NUMBER" >&2
  exit 1
fi

if [[ "$SEND_TELEGRAM" == true && "$BUILD_RELEASE" != true ]]; then
  echo "--send requires --release so the sent APKs match the preview workflow." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -z "$SHA" ]]; then
  SHA="$(git rev-parse HEAD)"
fi
SHORT_SHA="${SHA:0:7}"

BASE_VERSION="$(
  python - <<'PY'
import json
from pathlib import Path

manifest = json.loads(Path(".github/.release-please-manifest.json").read_text(encoding="utf-8"))
print(manifest["."])
PY
)"

if [[ ! "$BASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Base version must be semantic version x.y.z: $BASE_VERSION" >&2
  exit 1
fi

IFS='.' read -r MAJOR MINOR PATCH <<< "$BASE_VERSION"
BASE_VERSION_CODE=$((10#$MAJOR * 10000 + 10#$MINOR * 100 + 10#$PATCH))
PREVIEW_VERSION_CODE=$((BASE_VERSION_CODE * 10000 + RUN_NUMBER))
if (( PREVIEW_VERSION_CODE > 2100000000 )); then
  echo "Preview versionCode exceeds Android limit: $PREVIEW_VERSION_CODE" >&2
  exit 1
fi
PREVIEW_VERSION_NAME="${BASE_VERSION}-${RUN_NUMBER}.${SHORT_SHA}"

echo "Preview version:"
echo "  baseVersion=$BASE_VERSION"
echo "  runNumber=$RUN_NUMBER"
echo "  shortSha=$SHORT_SHA"
echo "  versionName=$PREVIEW_VERSION_NAME"
echo "  versionCode=$PREVIEW_VERSION_CODE"

if [[ "$SKIP_BUILD" == true ]]; then
  exit 0
fi

export DPIS_VERSION_NAME="$PREVIEW_VERSION_NAME"
export DPIS_VERSION_CODE="$PREVIEW_VERSION_CODE"

if [[ "$BUILD_RELEASE" == true ]]; then
  required_release_vars=(
    DPIS_RELEASE_STORE_FILE
    DPIS_RELEASE_STORE_PASSWORD
    DPIS_RELEASE_KEY_ALIAS
    DPIS_RELEASE_KEY_PASSWORD
  )
  for key in "${required_release_vars[@]}"; do
    if [[ -z "${!key:-}" ]]; then
      echo "Missing required release signing env var: $key" >&2
      exit 1
    fi
  done
  echo "Building release APKs..."
  ./gradlew :app:assembleRelease
  APKS=(
    "app/build/outputs/apk/modern101/release/DPIS_${PREVIEW_VERSION_NAME}.apk"
    "app/build/outputs/apk/compat100/release/DPIS_${PREVIEW_VERSION_NAME}_legacy.apk"
  )
else
  echo "Building debug APKs..."
  ./gradlew :app:assembleModern101Debug :app:assembleCompat100Debug
  APKS=(
    "app/build/outputs/apk/modern101/debug/app-modern101-debug.apk"
    "app/build/outputs/apk/compat100/debug/app-compat100-debug.apk"
  )
fi

for apk in "${APKS[@]}"; do
  if [[ ! -f "$apk" ]]; then
    echo "APK not found after build: $apk" >&2
    exit 1
  fi
  ls -lh "$apk"
done

if [[ "$SEND_TELEGRAM" != true ]]; then
  echo "Local test complete. Add --release --send to send release APKs to Telegram."
  exit 0
fi

if [[ -z "${TELEGRAM_BOT_TOKEN:-}" ]]; then
  echo "TELEGRAM_BOT_TOKEN is required for --send" >&2
  exit 1
fi

CHAT_TARGETS="${TELEGRAM_CHAT_IDS:-${TELEGRAM_CHAT_ID:-}}"
if [[ -z "$CHAT_TARGETS" ]]; then
  echo "TELEGRAM_CHAT_ID or TELEGRAM_CHAT_IDS is required for --send" >&2
  exit 1
fi

CAPTION=$'DPIS local preview test\nVersion: '"${PREVIEW_VERSION_NAME}"$'\nVersionCode: '"${PREVIEW_VERSION_CODE}"$'\nCommit: '"${SHORT_SHA}"
IFS=',' read -ra CHAT_IDS <<< "$CHAT_TARGETS"
for chat_id in "${CHAT_IDS[@]}"; do
  chat_id="$(echo "$chat_id" | xargs)"
  [[ -z "$chat_id" ]] && continue
  for apk in "${APKS[@]}"; do
    args=(
      -F "chat_id=${chat_id}"
      -F "caption=${CAPTION}"
      -F "document=@${apk}"
    )
    if [[ -n "${TELEGRAM_MESSAGE_THREAD_ID:-}" ]]; then
      args+=(-F "message_thread_id=${TELEGRAM_MESSAGE_THREAD_ID}")
    fi
    if [[ "${TELEGRAM_PROTECT_CONTENT:-}" == "true" ]]; then
      args+=(-F "protect_content=true")
    fi
    curl --fail --show-error --silent \
      "${args[@]}" \
      "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendDocument"
  done
done

echo "Telegram send complete."
