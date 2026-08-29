package com.armsone.denimdex.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ports handoff 9.2 #3 (QuickValueResultValidatorTests.swift, 15 cases; explicit vectors condensed to 9 here).
 *
 * NOTE FOR CODEX: [QuickValueResultValidator] parses JSON via `org.json.JSONObject`/`JSONArray`.
 * Those classes ship inside the Android SDK's "mockable android.jar" for local JVM unit tests,
 * where method bodies are stubbed to throw ("not mocked") instead of running real parsing logic.
 * This suite will fail at runtime under plain `./gradlew test` until either:
 *   1. `testImplementation("org.json:json:<version>")` is added (real org.json impl shadows the
 *      stub on the unit test classpath), or
 *   2. Robolectric is added and these tests are annotated `@RunWith(RobolectricTestRunner::class)`.
 * Neither dependency exists in gradle/libs.versions.toml today; adding one was out of scope for
 * this pass per the "catalog-only, no network" constraint. Codex must add the dependency before
 * this file can pass.
 */
class QuickValueResultValidatorTest {

    private fun validJson(
        schemaVersion: Int = 2,
        task: String = "quick_value",
        confidence: String = "medium",
        koreaLow: String = "80000",
        koreaHigh: String = "180000",
        jpyToKrwRate: String = "9.1",
        observations: String = """[{"feature":"fly_type","value":"button_fly","evidencePhotoRole":"photo_1","certainty":"observed"}]"""
    ): String = """
        {
          "schemaVersion": $schemaVersion,
          "task": "$task",
          "productGuess": { "brand": "Levi's", "model": "501", "era": "1990s" },
          "summary": "test summary",
          "confidence": "$confidence",
          "condition": "fair",
          "koreaSaleRange": { "low": $koreaLow, "high": $koreaHigh },
          "japanSaleRange": { "low": 8000, "high": 18000 },
          "jpyToKrwRate": $jpyToKrwRate,
          "observations": $observations,
          "valueReasons": ["reason"],
          "caveats": ["caveat"]
        }
    """.trimIndent()

    @Test
    fun `schema version mismatch is rejected`() {
        val result = QuickValueResultValidator.validate(validJson(schemaVersion = 1), listOf("photo_1"))
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.SchemaVersionMismatch)
    }

    @Test
    fun `task mismatch is rejected`() {
        val result = QuickValueResultValidator.validate(validJson(task = "deep_inspect"), listOf("photo_1"))
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.TaskMismatch)
    }

    @Test
    fun `disallowed confidence enum value is rejected`() {
        val result = QuickValueResultValidator.validate(validJson(confidence = "extreme"), listOf("photo_1"))
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.DisallowedEnumValue)
    }

    @Test
    fun `negative korea low is rejected`() {
        val result = QuickValueResultValidator.validate(validJson(koreaLow = "-1"), listOf("photo_1"))
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.NegativeValue)
    }

    @Test
    fun `korea low greater than high is rejected`() {
        val result = QuickValueResultValidator.validate(
            validJson(koreaLow = "200000", koreaHigh = "100000"),
            listOf("photo_1")
        )
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.LowGreaterThanHigh)
    }

    @Test
    fun `non positive exchange rate is rejected`() {
        val result = QuickValueResultValidator.validate(validJson(jpyToKrwRate = "0"), listOf("photo_1"))
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.InvalidExchangeRate)
    }

    @Test
    fun `observed certainty referencing an unsent photo role is rejected`() {
        val json = validJson(
            observations = """[{"feature":"fly_type","value":"button_fly","evidencePhotoRole":"photo_5","certainty":"observed"}]"""
        )
        val result = QuickValueResultValidator.validate(json, listOf("photo_1"))
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.UnobservedPhotoRoleUsed)
    }

    @Test
    fun `inferred certainty referencing an unsent photo role is allowed`() {
        val json = validJson(
            observations = """[{"feature":"era_guess","value":"1990s","evidencePhotoRole":"photo_7","certainty":"inferred"}]"""
        )
        val result = QuickValueResultValidator.validate(json, listOf("photo_1"))
        assertTrue(result.isSuccess)
    }

    @Test
    fun `string prices and exchange rate are recovered via flexible normalization`() {
        val json = validJson(koreaLow = "\"80,000원\"", jpyToKrwRate = "\"9.1\"")
        val result = QuickValueResultValidator.validate(json, listOf("photo_1"))
        assertTrue(result.isSuccess)
        assertEquals(80_000L, result.getOrThrow().koreaSaleRange.low)
        assertEquals(9.1, result.getOrThrow().jpyToKrwRate, 0.001)
    }
}
