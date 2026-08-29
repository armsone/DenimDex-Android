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
