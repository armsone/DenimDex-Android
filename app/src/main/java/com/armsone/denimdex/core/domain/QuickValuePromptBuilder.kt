package com.armsone.denimdex.core.domain

import com.armsone.denimdex.core.model.QuickValuePhotoRoles

object QuickValuePromptBuilder {
    fun buildPrompt(photoCount: Int): String {
        val boundedCount = photoCount.coerceIn(1, QuickValuePhotoRoles.maxCount)
        val photoListString = (1..boundedCount).joinToString("\n") { index ->
            "${index}번 사진: photo_${index}"
        }

        return buildString {
            appendLine("너는 빈티지 데님 감정을 돕는 조사 보조원이다. 첨부된 사진 ${boundedCount}장을 보고 아래 JSON 스키마 하나만 출력해라. 설명 문장, 인사말, 마크다운 제목을 붙이지 말고 JSON 코드 블록 하나만 응답해라.")
            appendLine()
            appendLine("사진 순서와 식별자 (관찰 근거를 적을 때 이 식별자를 evidencePhotoRole에 그대로 사용해라):")
            appendLine(photoListString)
            appendLine()
            appendLine("규칙:")
            appendLine("- 이것은 빠른 참고용 추정이며 정품 감정이나 실제 매입가가 아니다. 이 사실을 caveats에 반드시 포함해라.")
            appendLine("- 적정 매입가(fairPurchaseRange)와 예상 판매가(saleRange)를 구분하여 한국(KRW)과 일본(JPY) 두 시장의 범위를 각각 넓게 추정해라. 적정 매입가는 구매자가 지불할 만한 합리적인 가격대이며 예상 판매가보다 낮아야 한다.")
            appendLine("- 근거가 부족한 경우 희귀도(rarityLevel)는 보수적으로 낮게 추정하고 검증된 희소성이 아님을 유의해라 (unknown, common, uncommon, rare, extremely_rare 중 선택).")
            appendLine("- variant는 사진에서 판별 근거가 있을 때만 적고, 확신할 수 없으면 빈 문자열로 두어라.")
            appendLine("- estimatedProductionYear에는 케어라벨, 로트 번호, 탭, 지퍼, 리벳 등 사진 단서로 추정한 생산연도 또는 연도 범위를 적어라. estimatedFactory에는 공장 코드나 원산지 표기 등 사진 근거로 추정한 제조공장 또는 생산지를 적어라. 근거가 부족하면 각각 빈 문자열로 두고, 근거가 있으면 observations에 해당 사진과 certainty를 남겨라.")
            appendLine("- 실시간 거래 데이터베이스는 연결되어 있지 않으므로, 이는 일반 지식에 기반한 넓은 참고 범위이며 가격과 환율 모두 실시간으로 검증되지 않았다는 사실을 caveats에 명시해라.")
            appendLine("- 웹 검색, 외부 도구 호출, 추가 조사나 장시간 추론을 하지 말고 첨부 사진과 일반 지식만으로 즉시 응답해라.")
            appendLine("- jpyToKrwRate는 \"엔화 1엔당 원화\" 환율로, 반드시 0보다 큰 값을 제시해라 (예: 9.1).")
            appendLine("- 사진에서 직접 보이지 않는 특징을 관찰된 사실처럼 적지 마라.")
            appendLine("- 판단이 어려우면 무리하게 브랜드나 모델을 단정하지 말고 confidence를 낮춰라.")
            appendLine("- 판단에 도움이 될 사진이 한 장 더 있으면 좋겠다면 nextPhotoInstruction에 한 문장으로 안내해라. 필요 없으면 생략해라.")
            appendLine()
            appendLine("정확히 이 스키마를 따르는 JSON 코드 블록만 출력해라:")
            appendLine("```json")
            appendLine("{")
            appendLine("  \"schemaVersion\": 3,")
            appendLine("  \"task\": \"quick_value\",")
            appendLine("  \"productGuess\": { \"brand\": \"string\", \"model\": \"string\", \"era\": \"string\", \"variant\": \"string\", \"estimatedProductionYear\": \"string\", \"estimatedFactory\": \"string\" },")
            appendLine("  \"summary\": \"string, 두 문장 이내\",")
            appendLine("  \"confidence\": \"high | medium | low | unknown\",")
            appendLine("  \"condition\": \"excellent | good | fair | poor | unknown\",")
            appendLine("  \"rarityLevel\": \"unknown | common | uncommon | rare | extremely_rare\",")
            appendLine("  \"raritySummary\": \"string\",")
            appendLine("  \"rarityReasons\": [\"string\"],")
            appendLine("  \"koreaFairPurchaseRange\": { \"low\": 0, \"high\": 0 },")
            appendLine("  \"japanFairPurchaseRange\": { \"low\": 0, \"high\": 0 },")
            appendLine("  \"koreaSaleRange\": { \"low\": 0, \"high\": 0 },")
            appendLine("  \"japanSaleRange\": { \"low\": 0, \"high\": 0 },")
            appendLine("  \"jpyToKrwRate\": 9.1,")
            appendLine("  \"observations\": [")
            appendLine("    { \"feature\": \"string\", \"value\": \"string\", \"evidencePhotoRole\": \"photo_1\", \"certainty\": \"observed | reported | inferred\" }")
            appendLine("  ],")
            appendLine("  \"valueReasons\": [\"string\"],")
            appendLine("  \"nextPhotoInstruction\": \"string\",")
            appendLine("  \"caveats\": [\"string\"]")
            appendLine("}")
            append("```")
        }
    }
}
