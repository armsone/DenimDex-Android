# DenimDex iOS ↔ Android parity report

## Evidence decision

The current evidence is adequate to verify Android physical-device rendering, default Scan hierarchy, navigation smoke behavior, photo-picker cancellation, and the signed-out ChatGPT login surface. It is not adequate for a full parity claim: only the iOS default Scan state has a current runtime counterpart, the devices have different logical widths, and no authenticated AIBI, tablet, or TV trace exists.

The original two iOS reference PNGs are byte-identical and therefore cannot prove two distinct states. The current iOS source was additionally built and captured on an iPhone 17 Pro simulator. All original and post-change PNGs and SHA-256 values are preserved in `evidence/runtime-manifest.json`.

## Confirmed and corrected visual findings

| Class | State | Finding | Action/result | Confidence |
|---|---|---|---|---|
| Visual | Scan default | Android overline was gray instead of iOS brass | Changed to the shared brass token; verified in post-change phone capture | High |
| Visual | Scan default | Header padding, type hierarchy, radius, and shadow were flatter/smaller | Enlarged hierarchy and introduced shared hero/card shape and elevation tokens | High |
| Visual | All phone tabs | Material bottom navigation was flat and edge-to-edge | Replaced phone path with an inset floating capsule and selected-tab pill; tablet/TV rail untouched | High |
| Visual | Cards/buttons | Android corners and shadows were inconsistent with current iOS | Centralized `DenimShapes` and strengthened card elevation | Medium |
| Visual | Header underline | Exact iOS underline end position is text-metric dependent | Android uses a fixed 64dp trailing rule; exact baseline/width remains 확인 필요 | Medium |
| Forced OS exception candidate | System regions | Status bar, Dynamic Island, Android status icons, system photo picker | OS-owned pixels/actions only; no app-owned mask has been declared complete | High |

## Runtime findings

- Android debug APK installed with data preservation and cold-launched on SM-F968N.
- Eleven final Android states plus a scrolled result-detail state were captured after the last design change.
- Archive list → detail → Back passed with UI Automator text evidence.
- Android system photo picker opened and cancelled back to the unchanged `0 / 30` state.
- ChatGPT signed-out page loaded inside the app login sheet; no credentials were entered.
- iOS Debug built successfully and its default Scan state was captured on one iPhone 17 Pro simulator.

## Open gates

- Authenticated ChatGPT AIBI: ordered 1/20 image attachment, challenge takeover, cancel, timeout, JSON stability, and result import.
- Physical CameraX capture and actual system-picker import ordering.
- Tablet/expanded/Google TV rendering, D-pad focus/back, and 10-foot readability.
- Process-death restoration for selected photos and an active valuation.
- Current iOS runtime pairs for archive, guide, settings, dialogs, result, tablet, and accessibility states.
- Exact app-owned pixel/color/geometry gate after normalized paired captures for every state.

Because these rows remain open, `.parity/ledger.json --gate` must continue to fail; the implementation must not be described as full visual parity.

## Regeneration

```bash
.parity/scripts/capture_android_catalog.sh <adb-serial> <output-dir>
.parity/scripts/capture_ios_default.sh <simulator-udid> <DenimDex.app> <output-png>
python3 /Users/armsone/.codex/skills/matchup/scripts/validate_parity_ledger.py .parity/ledger.json --gate
```
