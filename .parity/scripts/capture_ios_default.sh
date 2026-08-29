#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "usage: $0 <simulator-udid> <DenimDex.app> [output-png]" >&2
  exit 2
fi

SIMULATOR_UDID="$1"
APP_PATH="$2"
OUTPUT_PNG="${3:-.parity/evidence/ios_runtime/scan_default.png}"

mkdir -p "$(dirname "$OUTPUT_PNG")"
xcrun simctl boot "$SIMULATOR_UDID" 2>/dev/null || true
xcrun simctl bootstatus "$SIMULATOR_UDID" -b
xcrun simctl install "$SIMULATOR_UDID" "$APP_PATH"
xcrun simctl launch --terminate-running-process "$SIMULATOR_UDID" com.armsone.DenimDex
sleep 2
xcrun simctl io "$SIMULATOR_UDID" screenshot "$OUTPUT_PNG"
shasum -a 256 "$OUTPUT_PNG"
