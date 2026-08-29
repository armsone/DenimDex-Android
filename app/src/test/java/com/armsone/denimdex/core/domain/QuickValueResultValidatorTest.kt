package com.armsone.denimdex.core.domain

import com.armsone.denimdex.core.model.RarityLevel
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
        schemaVersion: Int = 3,
        task: String = "quick_value",
        confidence: String = "medium",
        condition: String = "fair",
        rarityLevel: String = "uncommon",
        variant: String = "501-0000",
        koreaFairLow: String = "60000",
        koreaFairHigh: String = "130000",
        japanFairLow: String = "6000",
        japanFairHigh: String = "13000",
        koreaLow: String = "80000",
        koreaHigh: String = "180000",
        japanLow: String = "8000",
        japanHigh: String = "18000",
        jpyToKrwRate: String = "9.1",
        observations: String = """[{"feature":"fly_type","value":"button_fly","evidencePhotoRole":"photo_1","certainty":"observed"}]"""
    ): String = """
        {
          "schemaVersion": $schemaVersion,
          "task": "$task",
          "productGuess": { "brand": "Levi's", "model": "501", "era": "1990s", "variant": "$variant" },
          "summary": "test summary",
          "confidence": "$confidence",
          "condition": "$condition",
          "rarityLevel": "$rarityLevel",
          "raritySummary": "test rarity summary",
          "rarityReasons": ["reason 1"],
          "koreaFairPurchaseRange": { "low": $koreaFairLow, "high": $koreaFairHigh },
          "japanFairPurchaseRange": { "low": $japanFairLow, "high": $japanFairHigh },
          "koreaSaleRange": { "low": $koreaLow, "high": $koreaHigh },
          "japanSaleRange": { "low": $japanLow, "high": $japanHigh },
          "jpyToKrwRate": $jpyToKrwRate,
          "observations": $observations,
          "valueReasons": ["reason"],
          "caveats": ["caveat"]
        }
    """.trimIndent()

    @Test
    fun `schema version mismatch is rejected`() {
        val result = QuickValueResultValidator.validate(validJson(schemaVersion = 2), listOf("photo_1"))
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
    fun `disallowed rarityLevel enum value is rejected`() {
        val result = QuickValueResultValidator.validate(validJson(rarityLevel = "mythic"), listOf("photo_1"))
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.DisallowedEnumValue)
    }

    @Test
    fun `negative korea sale low is rejected`() {
        val result = QuickValueResultValidator.validate(validJson(koreaLow = "-1"), listOf("photo_1"))
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.NegativeValue)
    }

    @Test
    fun `negative korea fair purchase low is rejected`() {
        val result = QuickValueResultValidator.validate(validJson(koreaFairLow = "-1"), listOf("photo_1"))
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.NegativeValue)
    }

    @Test
    fun `negative japan fair purchase low is rejected`() {
        val result = QuickValueResultValidator.validate(validJson(japanFairLow = "-1"), listOf("photo_1"))
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.NegativeValue)
    }

    @Test
    fun `korea sale low greater than high is rejected`() {
        val result = QuickValueResultValidator.validate(
            validJson(koreaLow = "200000", koreaHigh = "100000"),
            listOf("photo_1")
        )
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.LowGreaterThanHigh)
    }

    @Test
    fun `korea fair purchase low greater than high is rejected`() {
        val result = QuickValueResultValidator.validate(
            validJson(koreaFairLow = "150000", koreaFairHigh = "50000"),
            listOf("photo_1")
        )
        assertTrue(result.exceptionOrNull() is QuickValueValidationError.LowGreaterThanHigh)
    }

    @Test
    fun `japan fair purchase low greater than high is rejected`() {
        val result = QuickValueResultValidator.validate(
            validJson(japanFairLow = "15000", japanFairHigh = "5000"),
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
        val json = validJson(
            koreaFairLow = "\"60,000원\"",
            koreaLow = "\"80,000원\"",
            jpyToKrwRate = "\"9.1\""
        )
        val result = QuickValueResultValidator.validate(json, listOf("photo_1"))
        assertTrue(result.isSuccess)
        assertEquals(60_000L, result.getOrThrow().koreaFairPurchaseRange.low)
        assertEquals(80_000L, result.getOrThrow().koreaSaleRange.low)
        assertEquals(9.1, result.getOrThrow().jpyToKrwRate, 0.001)
    }

    @Test
    fun `omitted optional arrays and variant are tolerated`() {
        val json = """
            {
              "schemaVersion": 3,
              "task": "quick_value",
              "productGuess": { "brand": "Levi's", "model": "501", "era": "1990s" },
              "summary": "test summary",
              "confidence": "medium",
              "condition": "fair",
              "rarityLevel": "common",
              "raritySummary": "common denim",
              "koreaFairPurchaseRange": { "low": 50000, "high": 100000 },
              "japanFairPurchaseRange": { "low": 5000, "high": 10000 },
              "koreaSaleRange": { "low": 80000, "high": 150000 },
              "japanSaleRange": { "low": 8000, "high": 15000 },
              "jpyToKrwRate": 9.1
            }
        """.trimIndent()
        val result = QuickValueResultValidator.validate(json, listOf("photo_1"))
        assertTrue(result.isSuccess)
        val item = result.getOrThrow()
        assertEquals(RarityLevel.COMMON, item.rarityLevel)
        assertEquals("", item.productGuess.variant)
        assertEquals("", item.productGuess.estimatedProductionYear)
        assertEquals("", item.productGuess.estimatedFactory)
        assertTrue(item.observations.isEmpty())
        assertTrue(item.valueReasons.isEmpty())
        assertTrue(item.caveats.isEmpty())
    }
}
