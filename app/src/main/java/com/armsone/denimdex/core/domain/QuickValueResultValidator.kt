package com.armsone.denimdex.core.domain

import com.armsone.denimdex.core.model.*
import org.json.JSONArray
import org.json.JSONObject

sealed class QuickValueValidationError(message: String) : Exception(message) {
    object EmptyResult : QuickValueValidationError("AI response is empty.")
    object InvalidJson : QuickValueValidationError("Could not extract valid JSON from AI response.")
    data class SchemaVersionMismatch(val version: Int) : QuickValueValidationError("Schema version mismatch: expected 2, got $version.")
    data class TaskMismatch(val task: String) : QuickValueValidationError("Task mismatch: expected quick_value, got $task.")
    data class DisallowedEnumValue(val field: String, val value: String) : QuickValueValidationError("Disallowed enum value for $field: $value.")
    object NegativeValue : QuickValueValidationError("Price range cannot contain negative values.")
    object LowGreaterThanHigh : QuickValueValidationError("Minimum price cannot exceed maximum price.")
    object InvalidExchangeRate : QuickValueValidationError("Exchange rate must be greater than 0.")
    data class UnobservedPhotoRoleUsed(val role: String) : QuickValueValidationError("AI referenced an unobserved photo role ($role) as an observed fact.")
}

object QuickValueResultValidator {

    private val JSON_BLOCK_REGEX = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)

    fun validate(rawText: String, sentPhotoRoles: List<String>): Result<QuickValueResult> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(QuickValueValidationError.EmptyResult)
        }

        val jsonString = extractJsonBlock(trimmed)
            ?: return Result.failure(QuickValueValidationError.InvalidJson)

        return try {
            val jsonObject = JSONObject(jsonString)
            val result = parseAndValidateJson(jsonObject, sentPhotoRoles, rawText)
            Result.success(result)
        } catch (e: QuickValueValidationError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(QuickValueValidationError.InvalidJson)
        }
    }

    private fun extractJsonBlock(text: String): String? {
        val match = JSON_BLOCK_REGEX.find(text)
        if (match != null && match.groupValues.size > 1) {
            val content = match.groupValues[1].trim()
            if (content.isNotEmpty()) return content
        }

        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1).trim()
        }
        return null
    }

    private fun parseAndValidateJson(
        json: JSONObject,
        sentPhotoRoles: List<String>,
        originalRawText: String
    ): QuickValueResult {
        // 1. Schema version check
        val schemaVersion = json.optInt("schemaVersion", -1)
        if (schemaVersion != 2) {
            throw QuickValueValidationError.SchemaVersionMismatch(schemaVersion)
        }

        // 2. Task check
        val task = json.optString("task", "")
        if (task != "quick_value") {
            throw QuickValueValidationError.TaskMismatch(task)
        }

        // 3. Confidence enum check
        val confidenceRaw = json.optString("confidence", "").trim().lowercase()
        val validConfidenceValues = setOf("high", "medium", "low", "unknown")
        if (confidenceRaw !in validConfidenceValues) {
            throw QuickValueValidationError.DisallowedEnumValue("confidence", confidenceRaw)
        }
        val confidence = QuickValueConfidence.fromString(confidenceRaw)

        // 4. Condition enum check
        val conditionRaw = json.optString("condition", "").trim().lowercase()
        val validConditionValues = setOf("excellent", "good", "fair", "poor", "unknown")
        if (conditionRaw !in validConditionValues) {
            throw QuickValueValidationError.DisallowedEnumValue("condition", conditionRaw)
        }
        val condition = QuickValueCondition.fromString(conditionRaw)

        // 5. Product guess
        val productGuessObj = json.optJSONObject("productGuess")
        val productGuess = ProductGuess(
            brand = productGuessObj?.optString("brand", "") ?: "",
            model = productGuessObj?.optString("model", "") ?: "",
            era = productGuessObj?.optString("era", "") ?: ""
        )

        val summary = json.optString("summary", "")

        // 6. Price ranges with flexible number recovery
        val koreaObj = json.optJSONObject("koreaSaleRange")
        val koreaLow = parseFlexibleLong(koreaObj?.opt("low"))
        val koreaHigh = parseFlexibleLong(koreaObj?.opt("high"))

        if (koreaLow < 0L || koreaHigh < 0L) {
            throw QuickValueValidationError.NegativeValue
        }
        if (koreaLow > koreaHigh) {
            throw QuickValueValidationError.LowGreaterThanHigh
        }
        val koreaSaleRange = KoreaSaleRange(low = koreaLow, high = koreaHigh)

        val japanObj = json.optJSONObject("japanSaleRange")
        val japanLow = parseFlexibleLong(japanObj?.opt("low"))
        val japanHigh = parseFlexibleLong(japanObj?.opt("high"))

        if (japanLow < 0L || japanHigh < 0L) {
            throw QuickValueValidationError.NegativeValue
        }
        if (japanLow > japanHigh) {
            throw QuickValueValidationError.LowGreaterThanHigh
        }
        val japanSaleRange = JapanSaleRange(low = japanLow, high = japanHigh)

        // 7. Exchange rate
        val jpyToKrwRate = parseFlexibleDouble(json.opt("jpyToKrwRate"))
        if (jpyToKrwRate <= 0.0) {
            throw QuickValueValidationError.InvalidExchangeRate
        }

        // 8. Observations & Unobserved Photo Role Verification
        val observationsArray = json.optJSONArray("observations") ?: JSONArray()
        val observations = mutableListOf<Observation>()
        val validCertaintyValues = setOf("observed", "reported", "inferred")

        for (i in 0 until observationsArray.length()) {
            val obsObj = observationsArray.optJSONObject(i) ?: continue
            val feature = obsObj.optString("feature", "")
            val value = obsObj.optString("value", "")
            val evidencePhotoRole = obsObj.optString("evidencePhotoRole", "").trim()
            val certaintyRaw = obsObj.optString("certainty", "observed").trim().lowercase()

            if (certaintyRaw !in validCertaintyValues) {
                throw QuickValueValidationError.DisallowedEnumValue("certainty", certaintyRaw)
            }
            val certainty = Certainty.fromString(certaintyRaw) ?: Certainty.OBSERVED

            // Hallucination Guard: only observed certainty must be strictly present in sentPhotoRoles
            if (certainty == Certainty.OBSERVED) {
                if (evidencePhotoRole.isNotEmpty() && !sentPhotoRoles.contains(evidencePhotoRole)) {
                    throw QuickValueValidationError.UnobservedPhotoRoleUsed(evidencePhotoRole)
                }
            }

            observations.add(
                Observation(
                    feature = feature,
                    value = value,
                    evidencePhotoRole = evidencePhotoRole,
                    certainty = certainty
                )
            )
        }

        // 9. Value reasons, caveats, nextPhotoInstruction
        val valueReasonsArray = json.optJSONArray("valueReasons") ?: JSONArray()
        val valueReasons = (0 until valueReasonsArray.length()).mapNotNull {
            valueReasonsArray.optString(it).takeIf { str -> str.isNotBlank() }
        }

        val caveatsArray = json.optJSONArray("caveats") ?: JSONArray()
        val caveats = (0 until caveatsArray.length()).mapNotNull {
            caveatsArray.optString(it).takeIf { str -> str.isNotBlank() }
        }

        val nextPhotoInstruction = json.optString("nextPhotoInstruction").takeIf { it.isNotBlank() }

        return QuickValueResult(
            schemaVersion = schemaVersion,
            task = task,
            productGuess = productGuess,
            summary = summary,
            confidence = confidence,
            condition = condition,
            koreaSaleRange = koreaSaleRange,
            japanSaleRange = japanSaleRange,
            jpyToKrwRate = jpyToKrwRate,
            observations = observations,
            valueReasons = valueReasons,
            nextPhotoInstruction = nextPhotoInstruction,
            caveats = caveats,
            rawJson = originalRawText
        )
    }

    private fun parseFlexibleLong(value: Any?): Long {
        if (value == null) return 0L
        if (value is Number) return value.toLong()
        val str = value.toString().trim()
        val filtered = str.filter { it.isDigit() || it == '-' }
        return filtered.toLongOrNull() ?: 0L
    }

    private fun parseFlexibleDouble(value: Any?): Double {
        if (value == null) return 0.0
        if (value is Number) return value.toDouble()
        val str = value.toString().trim()
        val filtered = str.filter { it.isDigit() || it == '.' || it == '-' }
        return filtered.toDoubleOrNull() ?: 0.0
    }
}
