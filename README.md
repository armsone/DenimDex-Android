# DenimDex Android

Zero-backend, on-device vintage denim identification and Korea/Japan market valuation app.
Android port of the canonical iOS reference. Full product/behavioral spec:
[`docs/ios-handoff-v3.0.md`](docs/ios-handoff-v3.0.md).

> **Verification status:** 33 JVM unit tests, debug APK assembly, Android lint, APK metadata,
> and debug v2 signature verification pass. The APK was data-preserving installed and cold
> launched on an SM-F968N physical phone; 12 rendered states, archive navigation, photo-picker
> cancellation, and the signed-out ChatGPT login surface were checked. Authenticated AIBI,
> camera import, tablet/TV, process-death, and full paired visual parity remain open. See
> `.parity/report.md` and `.parity/ledger.json`.

## Module layout

```
app/src/main/java/com/armsone/denimdex/
  DenimDexApp.kt, MainActivity.kt        # entry points
  ui/RootScreen.kt                       # adaptive 4-tab shell (phone/tablet/TV)
  core/design/                           # DenimTheme tokens, buttons, card modifier
  core/model/                            # QuickValueResult, CollectionItem, enums
  core/domain/                           # MarketValueCalculator, CountdownFormatter,
                                          #   QuickValueImagePolicy, QuickValuePromptBuilder,
                                          #   QuickValueResultValidator, PhotoDeduplicator
  core/data/local/                       # SQLiteOpenHelper + repository, SharedPreferences
  core/aibi/                             # AIBI WebView session/media pipeline/login probe
  core/sync/                             # DisabledDenimDexSyncClient (NAS intentionally unbuilt)
  feature/scan/                          # 감정 tab: camera, runner, result card, AIBI sheets
  feature/archive/                       # 아카이브 tab: list, detail, sync invite/disclosure
  feature/guide/                         # 가이드 tab
  feature/settings/                      # 설정 tab
app/src/test/java/...                    # JVM unit tests for deterministic domain logic
```

## Verified build/test/lint commands

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
```

The three commands above passed together on 2026-08-29 with Gradle 9.5.0, AGP 9.3.0,
Kotlin/Compose plugin 2.3.21, compile/target SDK 37, and JDK 17. The generated APK is
`app/build/outputs/apk/debug/app-debug.apk`.

### Runtime verification still required

- Camera capture, actual photo import, and MediaStore behavior on physical hardware.
- Tablet/expanded/Google TV layout and D-pad behavior; the phone path was exercised.
- A live authenticated ChatGPT session, ordered multi-image attachment, takeover, cancel,
  timeout, and stable-result delivery.
- Paired iOS/Android screenshots for every rendered state; only default Scan is currently paired.
- Process-death restoration of selected photos and an active valuation run.

## What was completed in this implementation

- `DenimDexApp` / `MainActivity` entry points and an adaptive `RootScreen` (bottom nav on
  phone, `NavigationRail` on tablet/foldable/TV width, two-pane archive list+detail on
  expanded width, Google TV leanback exit-confirmation on Back at the home tab).
- 가이드 (`LearnScreen`) and 설정 (`SettingsScreen` + `SettingsViewModel`) tabs, completing the
  4-tab product described in handoff §3.4–3.5.
- `SyncInviteSheet` / `SyncDisclosureSheet` / `CollectionItemDetailScreen`, which
  `MyDenimListScreen.kt` referenced but which did not exist in the prior partial draft.
- Fixed a non-existent `Icons.Default.Sparkles` reference (replaced with `AutoAwesome`) that
  would have failed to compile.
- JVM unit tests porting the deterministic vectors from handoff §9.2 for
  `CountdownFormatter`, `MarketValueCalculator` (including the signed cross-market loss
  vectors), `QuickValueImagePolicy`, `QuickValuePhotoRoles`, `QuickValuePromptBuilder`, and
  `QuickValueResultValidator` (flexible JSON normalization + hallucination guard). A real
  `org.json` JVM implementation is included for this test suite.
- `.parity/ledger.json` and `.parity/evidence/ios_reference/manifest.json`.

## What was already present and reviewed (not rewritten)

The AIBI WebView orchestration (`AIBISession`, `AIBIMediaPipeline`, `AIBIProviderRegistry`,
`AIBILoginProbe`), the domain/valuation math, the SQLite persistence layer, and the 감정/아카이브
screens were already implemented in the prior draft and were read in full during this pass.
They were left in place where sound; only the compile-breaking icon reference was fixed and a
few missing collaborators (sync sheets, item detail screen) were added so the existing
references resolve.

## Explicitly out of scope (matches iOS, per handoff §11)

- Deep Inspect / precise research runner and prompt.
- A real NAS sync backend — `DisabledDenimDexSyncClient` always fails with `notConfigured`.
- Non-ChatGPT AIBI providers (Gemini/Claude/Grok selectors exist in `aibi-providers.json` but
  are not wired into any UI).
- Dark mode — `DenimTheme` forces a light `ColorScheme` unconditionally.
