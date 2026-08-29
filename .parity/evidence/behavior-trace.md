# Android physical-device behavior trace

- Captured: 2026-08-29 KST
- Device: SM-F968N (`R3KYB061JTZ`), Android 16 / SDK 36
- Artifact: `app-debug.apk`, SHA-256 `ab00e0b4aadce7d3b8997a61861afbf34e5faf2f54204bfc6c5f529a7abdd3d2`
- Install: `adb install -r` succeeded; package `com.armsone.denimdex` launched cold.

## Archive list → detail → Back

1. Launched debug fixture `archive_list`.
2. Tapped the first deterministic item.
3. UI Automator observed `아카이브 상세`, `내 첫 빈티지 501`, and `KRW 80,000 ~ 180,000`.
4. Sent Android Back.
5. UI Automator again observed `내 아카이브`, `내 첫 빈티지 501`, and `Lee 101Z`.

Result: list-to-detail navigation and Back restoration passed. Editing, deletion confirmation, database relaunch persistence, and expanded master-detail remain separate open checks.

## Photo picker open → cancel

1. Launched debug fixture `scan_empty`.
2. Tapped `사진 선택`.
3. Top resumed activity changed to `com.google.android.photopicker/com.android.photopicker.MainActivity`.
4. Sent Android Back without selecting or changing a photo.
5. Top resumed activity returned to `com.armsone.denimdex/.MainActivity`; UI Automator observed `감정 사진`, `사진 선택`, and `0 / 30`.

Result: Android system picker launch and non-destructive cancel passed. Actual image import ordering and CameraX capture remain open.

## ChatGPT login surface

1. Launched debug fixture `scan_login`.
2. The attached in-app WebView loaded the official ChatGPT signed-out surface and its cookie-consent prompt inside the DenimDex login sheet.
3. App process and `MainActivity` remained foreground.

Result: visible login takeover and web content loading passed. Login credentials were not entered; authenticated attachment transfer, challenge handling, cancellation, timeout, and stable result extraction remain open.

## Authenticated ChatGPT result delivery

1. Reused the user's authenticated ChatGPT WebView session without reading cookies, prompt text, or returned private content.
2. Selected one synthetic denim image and started valuation.
3. UI Automator observed the media-attachment phase and then the host-side progress state.
4. The browser generation stabilized and DenimDex rendered one validated V3 result card, including the visible section labels `한눈에 보는 결론`, `보수적 희귀도`, and `적정 매입가 (한국)`.
5. The attached WebView was cleaned up after commit; no browser page remained attached to the result screen.

Result: authenticated one-photo attachment, submission, stable extraction, schema validation, host commit, and browser cleanup passed on SM-F968N. Challenge, cancellation, timeout, and 20-image completion remain open.

## iOS 0.2.1 collector usability sync

1. Selected four photos and observed `4 / 30` with square collector tiles.
2. The screen automatically scrolled far enough to expose `가치 확인하기`.
3. Cleared the test selection, selected one known-valid PNG, and started valuation.
4. UI Automator observed `Attaching 1 photos...` and the progress panel in the visible viewport.
5. Opened Settings and observed the exact combined row `현재 버전` → `0.2.1 (202608291110)`.

Result: the photo-addition scroll, run-start progress scroll, square collector geometry, and exact version display passed on the physical phone. Current paired iOS runtime captures remain open.

## Invalid or unsupported photo recovery

1. Selected four recent motion-photo items whose bytes could not be decoded by the normalization stage.
2. The pre-fix build terminated with `AIBIMediaPreparationException: IMAGE_DECODE_FAILED` escaping the task coroutine.
3. Expanded the runner's error boundary to include deduplication, normalization, prompt construction, and session startup.
4. Repeated the same selection on the replacement build.
5. The app process remained alive and rendered `선택한 사진을 읽지 못했습니다. 다른 사진으로 다시 시도해주세요.` with a retry action.

Result: media-preparation failure is now recoverable and no longer crashes the app.

## Existing ChatGPT composer text safety

1. Started another authenticated run while the ChatGPT composer already contained draft state.
2. The Android runtime preserved the existing composer value instead of overwriting it.
3. The host rendered the same recoverable terminal message as iOS: `ChatGPT 입력창에 이미 다른 내용이 있어 자동 입력을 건너뛰었습니다. 직접 확인해주세요.`

Result: existing user text is terminal and preserved. Live forced DOM-replacement retry remains a separate open fixture; deterministic retry-policy unit tests pass.
