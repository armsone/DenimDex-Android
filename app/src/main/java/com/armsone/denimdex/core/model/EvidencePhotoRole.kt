package com.armsone.denimdex.core.model

enum class EvidencePhotoRole(
    val roleKey: String,
    val title: String,
    val description: String
) {
    FRONT_SILHOUETTE(
        "front_silhouette",
        "전체 앞면",
        "전체적인 핏과 실루엣, 페이딩 상태"
    ),
    BACK_SILHOUETTE(
        "back_silhouette",
        "전체 뒷면",
        "뒷모습 핏, 백포켓 위치 및 형태"
    ),
    RED_TAB(
        "red_tab",
        "레드탭",
        "포켓 옆 탭의 글자(Big E/small e), 자수 상태"
    ),
    TOP_BUTTON_FRONT(
        "top_button_front",
        "상단 버튼 앞면",
        "브랜드 각인, 녹 및 광택 상태"
    ),
    TOP_BUTTON_BACK(
        "top_button_back",
        "상단 버튼 뒷면 각인",
        "공장 식별 번호 숫자 각인(예: 555, 524 등)"
    ),
    CARE_LABEL(
        "care_label",
        "케어라벨",
        "생산지, 세탁 표기, 생산 주차/연도 숫자 코드"
    ),
    PATCH(
        "patch",
        "가죽·종이 패치",
        "허리 패치의 로고, 로트 번호, 사이즈 표기"
    ),
    RIVET(
        "rivet",
        "리벳",
        "앞주머니와 코인포켓 리벳의 각인 및 소재(구리/알루미늄)"
    ),
    FLY(
        "fly",
        "지퍼·버튼 플라이",
        "버튼 플라이 개수 또는 지퍼 브랜드(Talon, Scovill, YKK 등)"
    ),
    SELVEDGE(
        "selvedge",
        "셀비지",
        "밑단 롤업 시 보이는 레드라인/스티칭 마감"
    ),
    STITCHING(
        "stitching",
        "봉제·아큐에이트",
        "백포켓 스티치 곡선, 밑단 체인스티치 유무"
    ),
    DAMAGE_OR_REPAIR(
        "damage_or_repair",
        "오염·수선·마모",
        "크러시, 덧댐 수선, 옐로우/오렌지 실 사용 여부"
    ),
    SCALE_REFERENCE(
        "scale_reference",
        "크기 비교 기준",
        "동전, 줄자 등 실측 보조 도구와 함께 촬영"
    );

    companion object {
        fun fromKey(key: String): EvidencePhotoRole? =
            entries.find { it.roleKey.equals(key.trim(), ignoreCase = true) }
    }
}
