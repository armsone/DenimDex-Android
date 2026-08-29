#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "usage: $0 <adb-serial> [output-dir]" >&2
  exit 2
fi

ADB_SERIAL="$1"
OUTPUT_DIR="${2:-.parity/evidence/android_runtime/catalog}"
PACKAGE="com.armsone.denimdex"
ACTIVITY="${PACKAGE}/.MainActivity"
EXTRA="com.armsone.denimdex.extra.CATALOG_FIXTURE"

fixtures=(
  scan_empty scan_six scan_result archive_empty archive_list archive_detail
  guide settings_logged_out scan_consent_dialog settings_clear_session_dialog
  scan_login
)

mkdir -p "$OUTPUT_DIR"
adb -s "$ADB_SERIAL" get-state >/dev/null

for fixture in "${fixtures[@]}"; do
  remote="/sdcard/denimdex_${fixture}.png"
  adb -s "$ADB_SERIAL" shell am force-stop "$PACKAGE"
  adb -s "$ADB_SERIAL" shell am start -W -n "$ACTIVITY" --es "$EXTRA" "$fixture" >/dev/null
  sleep 1
  adb -s "$ADB_SERIAL" shell screencap -p "$remote" >/dev/null
  adb -s "$ADB_SERIAL" pull "$remote" "$OUTPUT_DIR/${fixture}.png" >/dev/null
  adb -s "$ADB_SERIAL" shell rm "$remote"
done

shasum -a 256 "$OUTPUT_DIR"/*.png
