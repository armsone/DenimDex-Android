package com.armsone.denimdex.feature.scan

import androidx.annotation.DrawableRes
import com.armsone.denimdex.R
import com.armsone.denimdex.core.domain.QuickValuePromptBuilder

/**
 * Capture modes offered on the Scan screen.
 * PANTS/JACKET run the nine-slot guided flow; FREE keeps the existing free capture.
 */
enum class ScanCaptureMode(val displayName: String) {
    PANTS("팬츠"),
    JACKET("재킷"),
    FREE("자유 촬영");

    val isGuided: Boolean
        get() = this != FREE
}

/**
 * One ordered guided-capture slot definition with its stable role string
 * and the bundled real reference images (small thumbnail + high-resolution preview).
 */
data class GuidedCaptureStep(
    val role: String,
    val title: String,
    val instruction: String,
    @DrawableRes val thumbnailRes: Int,
    @DrawableRes val previewRes: Int
)

/**
 * Mutable per-slot capture state: captured photo bytes and skipped flag.
 */
data class GuidedSlotState(
    val photo: ByteArray? = null,
    val isSkipped: Boolean = false
) {
    val isCaptured: Boolean
        get() = photo != null

    val isResolved: Boolean
        get() = isCaptured || isSkipped

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GuidedSlotState) return false
        if (isSkipped != other.isSkipped) return false
        val a = photo
        val b = other.photo
        if (a === b) return true
        if (a == null || b == null) return false
        return a.contentEquals(b)
    }

    override fun hashCode(): Int {
        var result = photo?.contentHashCode() ?: 0
        result = 31 * result + isSkipped.hashCode()
        return result
    }
}

object GuidedCapturePresets {

    val pantsSteps: List<GuidedCaptureStep> = listOf(
        GuidedCaptureStep(
            role = "pants_front",
            title = "전체 앞면",
            instruction = "바지 앞모습 전체가 한눈에 다 들어오게 바닥에 펴고 찍어주세요.",
            thumbnailRes = R.drawable.guided_pants_front,
            previewRes = R.drawable.guided_pants_front_preview
        ),
        GuidedCaptureStep(
            role = "pants_inside_hem_selvedge",
            title = "안쪽 밑단·아웃심 (셀비지)",
            instruction = "바지 밑단을 살짝 접어 올려 안쪽 재봉선과 셀비지 실선이 보이게 찍어주세요.",
            thumbnailRes = R.drawable.guided_pants_hem,
            previewRes = R.drawable.guided_pants_hem_preview
        ),
        GuidedCaptureStep(
            role = "pants_back",
            title = "전체 뒷면",
            instruction = "바지 뒷모습 전체와 뒷주머니가 잘 보이게 펴고 찍어주세요.",
            thumbnailRes = R.drawable.guided_pants_back,
            previewRes = R.drawable.guided_pants_back_preview
        ),
        GuidedCaptureStep(
            role = "pants_patch",
            title = "가죽·종이 패치",
            instruction = "허리 오른쪽 뒤에 붙은 패치의 글자와 숫자가 잘 보이게 찍어주세요.",
            thumbnailRes = R.drawable.guided_pants_patch,
            previewRes = R.drawable.guided_pants_patch_preview
        ),
        GuidedCaptureStep(
            role = "pants_red_tab",
            title = "레드탭",
            instruction = "뒷주머니 옆에 달린 빨간색 탭의 글자가 또렷하게 보이게 가까이서 찍어주세요.",
            thumbnailRes = R.drawable.guided_pants_red_tab,
            previewRes = R.drawable.guided_pants_red_tab_preview
        ),
        GuidedCaptureStep(
            role = "pants_waist_button_back",
            title = "허리 상단 버튼 뒷면 각인",
            instruction = "허리 제일 위 버튼을 뒤집어서 뒷면에 찍힌 숫자나 영문 각인을 찍어주세요.",
            thumbnailRes = R.drawable.guided_pants_button_back,
            previewRes = R.drawable.guided_pants_button_back_preview
        ),
        GuidedCaptureStep(
            role = "pants_care_tag",
            title = "케어라벨 앞뒤",
            instruction = "바지 안쪽에 달린 세탁 라벨의 글자와 숫자가 선명하게 보이게 찍어주세요.",
            thumbnailRes = R.drawable.guided_pants_care_tag,
            previewRes = R.drawable.guided_pants_care_tag_preview
        ),
        GuidedCaptureStep(
            role = "pants_inside_back_pocket",
            title = "뒷포켓 안쪽 (히든리벳·바택)",
            instruction = "뒷주머니 안쪽 윗부분을 벌려서 숨은 쇠 리벳이나 보강 박음질을 찍어주세요.",
            thumbnailRes = R.drawable.guided_pants_pocket_inside,
            previewRes = R.drawable.guided_pants_pocket_inside_preview
        ),
        GuidedCaptureStep(
            role = "pants_fly",
            title = "플라이 전체 (V스티치·지퍼·버튼)",
            instruction = "앞 여밈 부분의 단추/지퍼와 허리 단추 옆 V자 박음질이 잘 보이게 찍어주세요.",
            thumbnailRes = R.drawable.guided_pants_fly,
            previewRes = R.drawable.guided_pants_fly_preview
        )
    )

    val jacketSteps: List<GuidedCaptureStep> = listOf(
        GuidedCaptureStep(
            role = "jacket_front",
            title = "전체 앞면",
            instruction = "재킷 앞모습 전체가 한눈에 다 들어오게 바닥에 펴고 찍어주세요.",
            thumbnailRes = R.drawable.guided_jacket_front,
            previewRes = R.drawable.guided_jacket_front_preview
        ),
        GuidedCaptureStep(
            role = "jacket_interior",
            title = "내부 전체",
            instruction = "재킷 앞을 활짝 열고 안쪽 전체 모습과 안감 상태를 찍어주세요.",
            thumbnailRes = R.drawable.guided_jacket_interior,
            previewRes = R.drawable.guided_jacket_interior_preview
        ),
        GuidedCaptureStep(
            role = "jacket_back",
            title = "전체 뒷면",
            instruction = "재킷 뒷모습 전체가 잘 보이게 반듯하게 펴고 찍어주세요.",
            thumbnailRes = R.drawable.guided_jacket_back,
            previewRes = R.drawable.guided_jacket_back_preview
        ),
        GuidedCaptureStep(
            role = "jacket_neck_label_patch",
            title = "목 라벨·패치",
            instruction = "목 안쪽에 붙은 라벨이나 가죽 패치의 글자가 잘 보이게 찍어주세요.",
            thumbnailRes = R.drawable.guided_jacket_neck_patch,
            previewRes = R.drawable.guided_jacket_neck_patch_preview
        ),
        GuidedCaptureStep(
            role = "jacket_red_tab",
            title = "레드탭",
            instruction = "가슴 주머니 옆에 달린 빨간색 탭의 글자가 선명하게 보이게 찍어주세요.",
            thumbnailRes = R.drawable.guided_jacket_red_tab,
            previewRes = R.drawable.guided_jacket_red_tab_preview
        ),
        GuidedCaptureStep(
            role = "jacket_chest_pocket",
            title = "가슴 포켓",
            instruction = "가슴 주머니 덮개 모양과 주머니 옆 주름(플리츠) 박음질을 찍어주세요.",
            thumbnailRes = R.drawable.guided_jacket_chest_pocket,
            previewRes = R.drawable.guided_jacket_chest_pocket_preview
        ),
        GuidedCaptureStep(
            role = "jacket_button_back",
            title = "버튼 뒷면 각인",
            instruction = "앞단추를 뒤집어서 뒷면에 새겨진 공장 번호나 각인을 찍어주세요.",
            thumbnailRes = R.drawable.guided_jacket_button_back,
            previewRes = R.drawable.guided_jacket_button_back_preview
        ),
        GuidedCaptureStep(
            role = "jacket_waist_adjuster",
            title = "허리 조절기 (신치백·버튼)",
            instruction = "허리 양옆의 조절 버튼이나 뒤쪽 허리 버클(신치백)을 찍어주세요.",
            thumbnailRes = R.drawable.guided_jacket_waist_adjuster,
            previewRes = R.drawable.guided_jacket_waist_adjuster_preview
        ),
        GuidedCaptureStep(
            role = "jacket_care_tag",
            title = "케어라벨 앞뒤",
            instruction = "재킷 안쪽에 달린 세탁 라벨의 글자와 숫자가 또렷하게 보이게 찍어주세요.",
            thumbnailRes = R.drawable.guided_jacket_care_tag,
            previewRes = R.drawable.guided_jacket_care_tag_preview
        )
    )

    fun steps(mode: ScanCaptureMode): List<GuidedCaptureStep> = when (mode) {
        ScanCaptureMode.PANTS -> pantsSteps
        ScanCaptureMode.JACKET -> jacketSteps
        ScanCaptureMode.FREE -> emptyList()
    }

    fun heading(mode: ScanCaptureMode): String = when (mode) {
        ScanCaptureMode.PANTS -> "팬츠 9컷 가이드"
        ScanCaptureMode.JACKET -> "재킷 9컷 가이드"
        ScanCaptureMode.FREE -> "감정 사진"
    }

    fun emptySlots(mode: ScanCaptureMode): List<GuidedSlotState> =
        List(steps(mode).size) { GuidedSlotState() }

    /** First slot that is neither captured nor skipped; falls back to the first slot. */
    fun firstUnresolvedIndex(slots: List<GuidedSlotState>): Int {
        val index = slots.indexOfFirst { !it.isResolved }
        return if (index >= 0) index else 0
    }

    /** Captured photos in slot order paired with their stable roles. */
    fun capturedPhotosWithRoles(
        mode: ScanCaptureMode,
        slots: List<GuidedSlotState>
    ): Pair<List<ByteArray>, List<String>> {
        val steps = steps(mode)
        val photos = mutableListOf<ByteArray>()
        val roles = mutableListOf<String>()
        slots.forEachIndexed { index, slot ->
            val photo = slot.photo
            if (photo != null && index < steps.size) {
                photos.add(photo)
                roles.add(steps[index].role)
            }
        }
        return photos to roles
    }

    /** Guided analysis prompt: sent slots in order plus explicitly listed missing roles. */
    fun buildGuidedPromptText(mode: ScanCaptureMode, slots: List<GuidedSlotState>): String {
        val steps = steps(mode)
        val sent = mutableListOf<QuickValuePromptBuilder.GuidedSlotDescriptor>()
        val missing = mutableListOf<QuickValuePromptBuilder.GuidedSlotDescriptor>()
        steps.forEachIndexed { index, step ->
            val descriptor = QuickValuePromptBuilder.GuidedSlotDescriptor(step.role, step.title)
            if (slots.getOrNull(index)?.isCaptured == true) {
                sent.add(descriptor)
            } else {
                missing.add(descriptor)
            }
        }
        return QuickValuePromptBuilder.buildGuidedPrompt(sent, missing)
    }
}
