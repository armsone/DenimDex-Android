package com.armsone.denimdex.core.catalog

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.armsone.denimdex.core.aibi.AIBIPhase
import com.armsone.denimdex.core.aibi.LoginStatus
import com.armsone.denimdex.core.model.*
import com.armsone.denimdex.feature.archive.ArchiveViewModel
import com.armsone.denimdex.feature.scan.QuickValueRunState
import com.armsone.denimdex.feature.scan.ScanViewModel
import com.armsone.denimdex.feature.settings.SettingsViewModel
import com.armsone.denimdex.ui.DenimDexTab
import java.io.ByteArrayOutputStream

/**
 * Deterministic catalog and fixture provider for DenimDex.
 * Enables zero-credential, deterministic UI states and test captures matching iOS handoff 9.3.
 */
object DeterministicCatalog {

    // Fixture Identifiers matching handoff 9.3
    const val FIXTURE_SCAN_EMPTY = "scan_empty"
    const val FIXTURE_SCAN_SIX = "scan_six"
    const val FIXTURE_SCAN_RUNNING_45 = "scan_running_45"
    const val FIXTURE_SCAN_LOGIN = "scan_login"
    const val FIXTURE_SCAN_ERROR = "scan_error"
    const val FIXTURE_SCAN_RESULT = "scan_result"
    const val FIXTURE_ARCHIVE_EMPTY = "archive_empty"
    const val FIXTURE_ARCHIVE_LIST = "archive_list"
    const val FIXTURE_ARCHIVE_DETAIL = "archive_detail"
    const val FIXTURE_GUIDE = "guide"
    const val FIXTURE_SETTINGS_LOGGED_OUT = "settings_logged_out"
    const val FIXTURE_SETTINGS_LOGGED_IN = "settings_logged_in"
    const val FIXTURE_SYNC_INVITE = "sync_invite"
    const val FIXTURE_SYNC_DISCLOSURE = "sync_disclosure"

    // Dialog-specific fixtures
    const val FIXTURE_SCAN_CONSENT_DIALOG = "scan_consent_dialog"
    const val FIXTURE_SCAN_CLEAR_ALL_DIALOG = "scan_clear_all_dialog"
    const val FIXTURE_SCAN_PHOTO_SAVE_ALERT = "scan_photo_save_alert"
    const val FIXTURE_ARCHIVE_DELETE_DIALOG = "archive_delete_dialog"
    const val FIXTURE_SETTINGS_CLEAR_SESSION_DIALOG = "settings_clear_session_dialog"
    const val FIXTURE_SETTINGS_SESSION_CLEARED_ALERT = "settings_session_cleared_alert"
    const val FIXTURE_SETTINGS_CLEAR_ARCHIVE_DIALOG = "settings_clear_archive_dialog"
    const val FIXTURE_TV_EXIT_DIALOG = "tv_exit_dialog"

    val ALL_FIXTURES = listOf(
        FIXTURE_SCAN_EMPTY,
        FIXTURE_SCAN_SIX,
        FIXTURE_SCAN_RUNNING_45,
        FIXTURE_SCAN_LOGIN,
        FIXTURE_SCAN_ERROR,
        FIXTURE_SCAN_RESULT,
        FIXTURE_ARCHIVE_EMPTY,
        FIXTURE_ARCHIVE_LIST,
        FIXTURE_ARCHIVE_DETAIL,
        FIXTURE_GUIDE,
        FIXTURE_SETTINGS_LOGGED_OUT,
        FIXTURE_SETTINGS_LOGGED_IN,
        FIXTURE_SYNC_INVITE,
        FIXTURE_SYNC_DISCLOSURE,
        FIXTURE_SCAN_CONSENT_DIALOG,
        FIXTURE_SCAN_CLEAR_ALL_DIALOG,
        FIXTURE_SCAN_PHOTO_SAVE_ALERT,
        FIXTURE_ARCHIVE_DELETE_DIALOG,
        FIXTURE_SETTINGS_CLEAR_SESSION_DIALOG,
        FIXTURE_SETTINGS_SESSION_CLEARED_ALERT,
        FIXTURE_SETTINGS_CLEAR_ARCHIVE_DIALOG,
        FIXTURE_TV_EXIT_DIALOG
    )

    /**
     * Generates a deterministic JPEG byte array with given label and background color.
     */
    fun createSamplePhoto(label: String, bgColor: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Draw label text wrapped if needed
        val lines = label.split("\n")
        val startY = 80f - ((lines.size - 1) * 12f)
        lines.forEachIndexed { i, line ->
            canvas.drawText(line, 80f, startY + (i * 24f), paint)
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    /**
     * Set of 6 distinct deterministic sample photos:
     * 1. Silhouette Front
     * 2. Silhouette Back
     * 3. Red Tab
     * 4. Top Button
     * 5. Care Label
     * 6. Paper Patch
     */
    fun sampleSixPhotos(): List<ByteArray> {
        return listOf(
            createSamplePhoto("실루엣 앞면\n(Front)", 0xFF0B2940.toInt()), // Indigo
            createSamplePhoto("실루엣 뒷면\n(Back)", 0xFF1A5780.toInt()),  // Cool Blue
            createSamplePhoto("레드탭\n(Red Tab)", 0xFFE41E25.toInt()),  // Red Tab
            createSamplePhoto("탑 버튼 각인\n(Button)", 0xFF997340.toInt()), // Brass
            createSamplePhoto("케어 라벨\n(Care Label)", 0xFF4A4D4F.toInt()), // Care Label
            createSamplePhoto("백 패치\n(Patch)", 0xFF5C3D29.toInt())     // Leather
        )
    }

    /**
     * Canonical QuickValueResult V3 matching iOS V3 contract.
     */
    val sampleQuickValueResult: QuickValueResult by lazy {
        QuickValueResult(
            schemaVersion = 3,
            task = "quick_value",
            productGuess = ProductGuess(
                brand = "Levi's",
                model = "501",
                era = "1990s 판단 어려움",
                variant = "미국제 00501-0000 슈링크 투 핏"
            ),
            summary = "Levi's 501 레귤러 스트레이트 데님으로 추정되며 자연스러운 페이딩이 있는 상태입니다.",
            confidence = QuickValueConfidence.MEDIUM,
            condition = QuickValueCondition.FAIR,
            rarityLevel = RarityLevel.UNCOMMON,
            raritySummary = "90년대 미국 생산 501로 현재 유통 시장에서 점차 줄어들고 있는 준희귀 개체입니다.",
            rarityReasons = listOf(
                "미국 생산 종료(2003년) 이전 90년대 후기 개체",
                "오리지널 버튼 플라이 및 원단 보존"
            ),
            koreaFairPurchaseRange = KoreaFairPurchaseRange(low = 60000, high = 130000),
            japanFairPurchaseRange = JapanFairPurchaseRange(low = 6000, high = 13000),
            koreaSaleRange = KoreaSaleRange(low = 80000, high = 180000),
            japanSaleRange = JapanSaleRange(low = 8000, high = 18000),
            jpyToKrwRate = 9.1,
            observations = listOf(
                Observation(
                    feature = "fly_type",
                    value = "button_fly",
                    evidencePhotoRole = "photo_1",
                    certainty = Certainty.OBSERVED
                )
            ),
            valueReasons = listOf(
                "90년대 미국 생산 501 특유의 버튼 플라이 구조",
                "무릎 및 밑단 사용감 반영"
            ),
            nextPhotoInstruction = "더 정확한 연대 특정을 위해 상단 버튼 뒷면 각인을 촬영해주세요.",
            caveats = listOf(
                "실시간 거래 데이터베이스가 연결되지 않은 빠른 AI 추정치입니다.",
                "정품 감정서나 실제 매입가가 아닙니다."
            ),
            rawJson = """
                {
                  "schemaVersion": 3,
                  "task": "quick_value",
                  "productGuess": { "brand": "Levi's", "model": "501", "era": "1990s 판단 어려움", "variant": "미국제 00501-0000 슈링크 투 핏" },
                  "summary": "Levi's 501 레귤러 스트레이트 데님으로 추정되며 자연스러운 페이딩이 있는 상태입니다.",
                  "confidence": "medium",
                  "condition": "fair",
                  "rarityLevel": "uncommon",
                  "raritySummary": "90년대 미국 생산 501로 현재 유통 시장에서 점차 줄어들고 있는 준희귀 개체입니다.",
                  "rarityReasons": [
                    "미국 생산 종료(2003년) 이전 90년대 후기 개체",
                    "오리지널 버튼 플라이 및 원단 보존"
                  ],
                  "koreaFairPurchaseRange": { "low": 60000, "high": 130000 },
                  "japanFairPurchaseRange": { "low": 6000, "high": 13000 },
                  "koreaSaleRange": { "low": 80000, "high": 180000 },
                  "japanSaleRange": { "low": 8000, "high": 18000 },
                  "jpyToKrwRate": 9.1,
                  "observations": [
                    { "feature": "fly_type", "value": "button_fly", "evidencePhotoRole": "photo_1", "certainty": "observed" }
                  ],
                  "valueReasons": [
                    "90년대 미국 생산 501 특유의 버튼 플라이 구조",
                    "무릎 및 밑단 사용감 반영"
                  ],
                  "nextPhotoInstruction": "더 정확한 연대 특정을 위해 상단 버튼 뒷면 각인을 촬영해주세요.",
                  "caveats": [
                    "실시간 거래 데이터베이스가 연결되지 않은 빠른 AI 추정치입니다.",
                    "정품 감정서나 실제 매입가가 아닙니다."
                  ]
                }
            """.trimIndent()
        )
    }

    /**
     * Canonical sample archive items.
     */
    val sampleArchiveItems: List<CollectionItem> by lazy {
        listOf(
            CollectionItem(
                id = "item-levis-501-1990s",
                userTitle = "내 첫 빈티지 501",
                brandGuess = "Levi's",
                modelGuess = "501",
                eraGuess = "1990s",
                variantGuess = "미국제 00501-0000 슈링크 투 핏",
                summary = "Levi's 501 레귤러 스트레이트 데님으로 추정되며 자연스러운 페이딩이 있는 상태입니다.",
                confidence = QuickValueConfidence.MEDIUM,
                condition = QuickValueCondition.FAIR,
                rarityLevel = RarityLevel.UNCOMMON,
                raritySummary = "90년대 미국 생산 501로 현재 유통 시장에서 점차 줄어들고 있는 준희귀 개체입니다.",
                rarityReasons = listOf("미국 생산 종료(2003년) 이전 90년대 후기 개체", "오리지널 버튼 플라이 및 원단 보존"),
                koreaFairPurchaseLow = 60000,
                koreaFairPurchaseHigh = 130000,
                japanFairPurchaseLow = 6000,
                japanFairPurchaseHigh = 13000,
                koreaSaleLow = 80000,
                koreaSaleHigh = 180000,
                japanSaleLow = 8000,
                japanSaleHigh = 18000,
                jpyToKrwRate = 9.1,
                verificationState = VerificationState.USER_CONFIRMED,
                syncEligibilityState = SyncEligibilityState.PENDING_CONSENT,
                userNotes = "신사동 빈티지샵에서 구매. 실착용.",
                photoUris = emptyList(),
                createdAt = 1772236800000L, // 2026-02-28
                updatedAt = 1772236800000L,
                rawAiResponseJson = sampleQuickValueResult.rawJson
            ),
            CollectionItem(
                id = "item-lee-101z-1970s",
                userTitle = "",
                brandGuess = "Lee",
                modelGuess = "101Z",
                eraGuess = "1970s",
                variantGuess = "",
                summary = "Lee 101Z 센터 레드 라벨 셀비지 데님입니다.",
                confidence = QuickValueConfidence.HIGH,
                condition = QuickValueCondition.GOOD,
                rarityLevel = RarityLevel.UNKNOWN,
                raritySummary = "",
                rarityReasons = emptyList(),
                koreaFairPurchaseLow = 0,
                koreaFairPurchaseHigh = 0,
                japanFairPurchaseLow = 0,
                japanFairPurchaseHigh = 0,
                koreaSaleLow = 250000,
                koreaSaleHigh = 450000,
                japanSaleLow = 28000,
                japanSaleHigh = 50000,
                jpyToKrwRate = 9.1,
                verificationState = VerificationState.AI_ESTIMATE,
                syncEligibilityState = SyncEligibilityState.NOT_ELIGIBLE,
                userNotes = "보관 상태 양호.",
                photoUris = emptyList(),
                createdAt = 1772150400000L, // 2026-02-27
                updatedAt = 1772150400000L,
                rawAiResponseJson = ""
            )
        )
    }

    /**
     * Applies a fixture ID to the application state deterministically.
     */
    fun applyFixture(
        fixtureId: String,
        scanViewModel: ScanViewModel,
        archiveViewModel: ArchiveViewModel,
        settingsViewModel: SettingsViewModel,
        onTabSelected: (DenimDexTab) -> Unit,
        onShowExitConfirm: ((Boolean) -> Unit)? = null
    ) {
        when (fixtureId) {
            FIXTURE_SCAN_EMPTY -> {
                onTabSelected(DenimDexTab.SCAN)
                scanViewModel.clearAllPhotos()
                scanViewModel.resetValuation()
                scanViewModel.dismissLoginSheet()
                scanViewModel.dismissConsentDialog()
                scanViewModel.dismissClearAllConfirm()
                scanViewModel.dismissPhotoSaveAlert()
                scanViewModel.closeCamera()
            }

            FIXTURE_SCAN_SIX -> {
                onTabSelected(DenimDexTab.SCAN)
                scanViewModel.resetValuation()
                scanViewModel.setPhotos(sampleSixPhotos())
                scanViewModel.dismissLoginSheet()
                scanViewModel.dismissConsentDialog()
                scanViewModel.dismissClearAllConfirm()
            }

            FIXTURE_SCAN_RUNNING_45 -> {
                onTabSelected(DenimDexTab.SCAN)
                scanViewModel.setPhotos(sampleSixPhotos())
                scanViewModel.runner.injectRunningState(
                    phase = AIBIPhase.GENERATING,
                    elapsedSeconds = 45.0,
                    statusMessage = "답변을 생성하고 있습니다...",
                    sentPhotoCount = 6,
                    excludedSimilarCount = 0,
                    excludedLimitCount = 0
                )
            }

            FIXTURE_SCAN_LOGIN -> {
                onTabSelected(DenimDexTab.SCAN)
                scanViewModel.setPhotos(sampleSixPhotos())
                scanViewModel.openLoginSheet()
            }

            FIXTURE_SCAN_ERROR -> {
                onTabSelected(DenimDexTab.SCAN)
                scanViewModel.setPhotos(sampleSixPhotos())
                scanViewModel.runner.injectErrorState(
                    "가치 확인을 완료하지 못했어요. 잠시 후 다시 시도해주세요."
                )
            }

            FIXTURE_SCAN_RESULT -> {
                onTabSelected(DenimDexTab.SCAN)
                scanViewModel.setPhotos(sampleSixPhotos())
                scanViewModel.setLatestResult(sampleQuickValueResult)
                scanViewModel.runner.injectSuccessState(
                    result = sampleQuickValueResult,
                    sentPhotoCount = 6,
                    excludedSimilarCount = 0,
                    excludedLimitCount = 0
                )
            }

            FIXTURE_ARCHIVE_EMPTY -> {
                onTabSelected(DenimDexTab.ARCHIVE)
                archiveViewModel.onSearchQueryChanged("")
                archiveViewModel.clearSelectedItem()
                archiveViewModel.setCatalogItems(emptyList())
                archiveViewModel.dismissSyncInvite()
                archiveViewModel.dismissSyncDisclosure()
            }

            FIXTURE_ARCHIVE_LIST -> {
                onTabSelected(DenimDexTab.ARCHIVE)
                archiveViewModel.onSearchQueryChanged("")
                archiveViewModel.clearSelectedItem()
                archiveViewModel.setCatalogItems(sampleArchiveItems)
                archiveViewModel.dismissSyncInvite()
                archiveViewModel.dismissSyncDisclosure()
            }

            FIXTURE_ARCHIVE_DETAIL -> {
                onTabSelected(DenimDexTab.ARCHIVE)
                archiveViewModel.onSearchQueryChanged("")
                archiveViewModel.setCatalogItems(sampleArchiveItems)
                archiveViewModel.selectItem(sampleArchiveItems.first())
                archiveViewModel.dismissSyncInvite()
                archiveViewModel.dismissSyncDisclosure()
            }

            FIXTURE_GUIDE -> {
                onTabSelected(DenimDexTab.GUIDE)
            }

            FIXTURE_SETTINGS_LOGGED_OUT -> {
                onTabSelected(DenimDexTab.SETTINGS)
                settingsViewModel.setLoginStatus(LoginStatus.LOGIN_REQUIRED)
                settingsViewModel.dismissLoginSheet()
                settingsViewModel.dismissClearSessionConfirm()
                settingsViewModel.dismissSessionClearedAlert()
                settingsViewModel.dismissClearArchiveConfirm()
            }

            FIXTURE_SETTINGS_LOGGED_IN -> {
                onTabSelected(DenimDexTab.SETTINGS)
                settingsViewModel.setLoginStatus(LoginStatus.LOGGED_IN)
                settingsViewModel.dismissLoginSheet()
                settingsViewModel.dismissClearSessionConfirm()
                settingsViewModel.dismissSessionClearedAlert()
                settingsViewModel.dismissClearArchiveConfirm()
            }

            FIXTURE_SYNC_INVITE -> {
                onTabSelected(DenimDexTab.ARCHIVE)
                archiveViewModel.setCatalogItems(sampleArchiveItems)
                archiveViewModel.openSyncInvite()
            }

            FIXTURE_SYNC_DISCLOSURE -> {
                onTabSelected(DenimDexTab.ARCHIVE)
                archiveViewModel.setCatalogItems(sampleArchiveItems)
                archiveViewModel.openSyncDisclosure()
            }

            FIXTURE_SCAN_CONSENT_DIALOG -> {
                onTabSelected(DenimDexTab.SCAN)
                scanViewModel.setPhotos(sampleSixPhotos())
                scanViewModel.openConsentDialog()
            }

            FIXTURE_SCAN_CLEAR_ALL_DIALOG -> {
                onTabSelected(DenimDexTab.SCAN)
                scanViewModel.setPhotos(sampleSixPhotos())
                scanViewModel.openClearAllConfirm()
            }

            FIXTURE_SCAN_PHOTO_SAVE_ALERT -> {
                onTabSelected(DenimDexTab.SCAN)
                scanViewModel.setPhotos(sampleSixPhotos())
                scanViewModel.onPhotoSaveIssue()
            }

            FIXTURE_ARCHIVE_DELETE_DIALOG -> {
                onTabSelected(DenimDexTab.ARCHIVE)
                archiveViewModel.setCatalogItems(sampleArchiveItems)
                archiveViewModel.selectItem(sampleArchiveItems.first())
                // Detail screen has showDeleteConfirm state
            }

            FIXTURE_SETTINGS_CLEAR_SESSION_DIALOG -> {
                onTabSelected(DenimDexTab.SETTINGS)
                settingsViewModel.requestClearSession()
            }

            FIXTURE_SETTINGS_SESSION_CLEARED_ALERT -> {
                onTabSelected(DenimDexTab.SETTINGS)
                settingsViewModel.showSessionClearedAlertDirectly()
            }

            FIXTURE_SETTINGS_CLEAR_ARCHIVE_DIALOG -> {
                onTabSelected(DenimDexTab.SETTINGS)
                settingsViewModel.showClearArchiveConfirmDirectly()
            }

            FIXTURE_TV_EXIT_DIALOG -> {
                onTabSelected(DenimDexTab.SCAN)
                onShowExitConfirm?.invoke(true)
            }
        }
    }
}
