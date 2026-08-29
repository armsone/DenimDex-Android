package com.armsone.denimdex.core.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.armsone.denimdex.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

class CollectionRepository(private val context: Context) {

    private val dbHelper = DenimDatabaseHelper(context)

    private val _items = MutableStateFlow<List<CollectionItem>>(emptyList())
    val items: StateFlow<List<CollectionItem>> = _items.asStateFlow()

    init {
        refreshItems()
    }

    fun refreshItems() {
        val list = loadAllFromDatabase()
        _items.value = list
    }

    suspend fun getAllItems(): List<CollectionItem> = withContext(Dispatchers.IO) {
        loadAllFromDatabase()
    }

    suspend fun getItemById(id: String): CollectionItem? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DenimDatabaseHelper.TABLE_ITEMS,
            null,
            "${DenimDatabaseHelper.COL_ID} = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val item = mapCursorToItem(it)
                val photos = loadPhotosForItem(db, item.id)
                item.copy(photoUris = photos)
            } else {
                null
            }
        }
    }

    suspend fun saveItem(
        item: CollectionItem,
        photoDataList: List<ByteArray> = emptyList()
    ): CollectionItem = withContext(Dispatchers.IO) {
        val savedPhotoUris = mutableListOf<String>()

        // 1. Save photo binaries to app-private storage if provided
        if (photoDataList.isNotEmpty()) {
            val itemDir = File(context.filesDir, "photos/${item.id}").apply { mkdirs() }
            photoDataList.forEachIndexed { index, bytes ->
                val photoFile = File(itemDir, "photo_${index + 1}.jpg")
                FileOutputStream(photoFile).use { fos ->
                    fos.write(bytes)
                }
                savedPhotoUris.add(Uri.fromFile(photoFile).toString())
            }
        } else {
            savedPhotoUris.addAll(item.photoUris)
        }

        val itemToSave = item.copy(photoUris = savedPhotoUris)

        // 2. Insert into SQLite
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(DenimDatabaseHelper.COL_ID, itemToSave.id)
                put(DenimDatabaseHelper.COL_USER_TITLE, itemToSave.userTitle)
                put(DenimDatabaseHelper.COL_BRAND_GUESS, itemToSave.brandGuess)
                put(DenimDatabaseHelper.COL_MODEL_GUESS, itemToSave.modelGuess)
                put(DenimDatabaseHelper.COL_ERA_GUESS, itemToSave.eraGuess)
                put(DenimDatabaseHelper.COL_VARIANT_GUESS, itemToSave.variantGuess)
                put(DenimDatabaseHelper.COL_ESTIMATED_PRODUCTION_YEAR, itemToSave.estimatedProductionYear)
                put(DenimDatabaseHelper.COL_ESTIMATED_FACTORY, itemToSave.estimatedFactory)
                put(DenimDatabaseHelper.COL_SUMMARY, itemToSave.summary)
                put(DenimDatabaseHelper.COL_CONFIDENCE, itemToSave.confidence.rawValue)
                put(DenimDatabaseHelper.COL_CONDITION, itemToSave.condition.rawValue)
                put(DenimDatabaseHelper.COL_RARITY_LEVEL, itemToSave.rarityLevel.rawValue)
                put(DenimDatabaseHelper.COL_RARITY_SUMMARY, itemToSave.raritySummary)
                put(DenimDatabaseHelper.COL_RARITY_REASONS, serializeReasons(itemToSave.rarityReasons))
                put(DenimDatabaseHelper.COL_KOREA_FAIR_PURCHASE_LOW, itemToSave.koreaFairPurchaseLow)
                put(DenimDatabaseHelper.COL_KOREA_FAIR_PURCHASE_HIGH, itemToSave.koreaFairPurchaseHigh)
                put(DenimDatabaseHelper.COL_JAPAN_FAIR_PURCHASE_LOW, itemToSave.japanFairPurchaseLow)
                put(DenimDatabaseHelper.COL_JAPAN_FAIR_PURCHASE_HIGH, itemToSave.japanFairPurchaseHigh)
                put(DenimDatabaseHelper.COL_KOREA_SALE_LOW, itemToSave.koreaSaleLow)
                put(DenimDatabaseHelper.COL_KOREA_SALE_HIGH, itemToSave.koreaSaleHigh)
                put(DenimDatabaseHelper.COL_JAPAN_SALE_LOW, itemToSave.japanSaleLow)
                put(DenimDatabaseHelper.COL_JAPAN_SALE_HIGH, itemToSave.japanSaleHigh)
                put(DenimDatabaseHelper.COL_JPY_TO_KRW_RATE, itemToSave.jpyToKrwRate)
                put(DenimDatabaseHelper.COL_VERIFICATION_STATE, itemToSave.verificationState.rawValue)
                put(DenimDatabaseHelper.COL_SYNC_ELIGIBILITY_STATE, itemToSave.syncEligibilityState.rawValue)
                put(DenimDatabaseHelper.COL_USER_NOTES, itemToSave.userNotes)
                put(DenimDatabaseHelper.COL_CREATED_AT, itemToSave.createdAt)
                put(DenimDatabaseHelper.COL_UPDATED_AT, itemToSave.updatedAt)
                put(DenimDatabaseHelper.COL_RAW_AI_RESPONSE_JSON, itemToSave.rawAiResponseJson)
            }
            db.insertWithOnConflict(
                DenimDatabaseHelper.TABLE_ITEMS,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )

            // Insert photos
            savedPhotoUris.forEachIndexed { index, uriStr ->
                val pValues = ContentValues().apply {
                    put(DenimDatabaseHelper.COL_PHOTO_ITEM_ID, itemToSave.id)
                    put(DenimDatabaseHelper.COL_PHOTO_URI, uriStr)
                    put(DenimDatabaseHelper.COL_PHOTO_SORT_ORDER, index)
                }
                db.insert(DenimDatabaseHelper.TABLE_PHOTOS, null, pValues)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshItems()
        itemToSave
    }

    suspend fun updateItem(item: CollectionItem) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DenimDatabaseHelper.COL_USER_TITLE, item.userTitle)
            put(DenimDatabaseHelper.COL_USER_NOTES, item.userNotes)
            put(DenimDatabaseHelper.COL_VERIFICATION_STATE, item.verificationState.rawValue)
            put(DenimDatabaseHelper.COL_SYNC_ELIGIBILITY_STATE, item.syncEligibilityState.rawValue)
            put(DenimDatabaseHelper.COL_UPDATED_AT, System.currentTimeMillis())
        }
        db.update(
            DenimDatabaseHelper.TABLE_ITEMS,
            values,
            "${DenimDatabaseHelper.COL_ID} = ?",
            arrayOf(item.id)
        )
        refreshItems()
    }

    suspend fun deleteItem(id: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            DenimDatabaseHelper.TABLE_ITEMS,
            "${DenimDatabaseHelper.COL_ID} = ?",
            arrayOf(id)
        )

        // Delete photo files
        val itemDir = File(context.filesDir, "photos/$id")
        if (itemDir.exists()) {
            itemDir.deleteRecursively()
        }

        refreshItems()
    }

    suspend fun clearAllItems() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(DenimDatabaseHelper.TABLE_PHOTOS, null, null)
        db.delete(DenimDatabaseHelper.TABLE_ITEMS, null, null)

        val photosDir = File(context.filesDir, "photos")
        if (photosDir.exists()) {
            photosDir.deleteRecursively()
        }

        refreshItems()
    }

    suspend fun seedItems(seedList: List<CollectionItem>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(DenimDatabaseHelper.TABLE_PHOTOS, null, null)
        db.delete(DenimDatabaseHelper.TABLE_ITEMS, null, null)

        for (item in seedList) {
            val values = ContentValues().apply {
                put(DenimDatabaseHelper.COL_ID, item.id)
                put(DenimDatabaseHelper.COL_USER_TITLE, item.userTitle)
                put(DenimDatabaseHelper.COL_BRAND_GUESS, item.brandGuess)
                put(DenimDatabaseHelper.COL_MODEL_GUESS, item.modelGuess)
                put(DenimDatabaseHelper.COL_ERA_GUESS, item.eraGuess)
                put(DenimDatabaseHelper.COL_VARIANT_GUESS, item.variantGuess)
                put(DenimDatabaseHelper.COL_ESTIMATED_PRODUCTION_YEAR, item.estimatedProductionYear)
                put(DenimDatabaseHelper.COL_ESTIMATED_FACTORY, item.estimatedFactory)
                put(DenimDatabaseHelper.COL_SUMMARY, item.summary)
                put(DenimDatabaseHelper.COL_CONFIDENCE, item.confidence.rawValue)
                put(DenimDatabaseHelper.COL_CONDITION, item.condition.rawValue)
                put(DenimDatabaseHelper.COL_RARITY_LEVEL, item.rarityLevel.rawValue)
                put(DenimDatabaseHelper.COL_RARITY_SUMMARY, item.raritySummary)
                put(DenimDatabaseHelper.COL_RARITY_REASONS, serializeReasons(item.rarityReasons))
                put(DenimDatabaseHelper.COL_KOREA_FAIR_PURCHASE_LOW, item.koreaFairPurchaseLow)
                put(DenimDatabaseHelper.COL_KOREA_FAIR_PURCHASE_HIGH, item.koreaFairPurchaseHigh)
                put(DenimDatabaseHelper.COL_JAPAN_FAIR_PURCHASE_LOW, item.japanFairPurchaseLow)
                put(DenimDatabaseHelper.COL_JAPAN_FAIR_PURCHASE_HIGH, item.japanFairPurchaseHigh)
                put(DenimDatabaseHelper.COL_KOREA_SALE_LOW, item.koreaSaleLow)
                put(DenimDatabaseHelper.COL_KOREA_SALE_HIGH, item.koreaSaleHigh)
                put(DenimDatabaseHelper.COL_JAPAN_SALE_LOW, item.japanSaleLow)
                put(DenimDatabaseHelper.COL_JAPAN_SALE_HIGH, item.japanSaleHigh)
                put(DenimDatabaseHelper.COL_JPY_TO_KRW_RATE, item.jpyToKrwRate)
                put(DenimDatabaseHelper.COL_VERIFICATION_STATE, item.verificationState.rawValue)
                put(DenimDatabaseHelper.COL_SYNC_ELIGIBILITY_STATE, item.syncEligibilityState.rawValue)
                put(DenimDatabaseHelper.COL_USER_NOTES, item.userNotes)
                put(DenimDatabaseHelper.COL_CREATED_AT, item.createdAt)
                put(DenimDatabaseHelper.COL_UPDATED_AT, item.updatedAt)
                put(DenimDatabaseHelper.COL_RAW_AI_RESPONSE_JSON, item.rawAiResponseJson)
            }
            db.insert(DenimDatabaseHelper.TABLE_ITEMS, null, values)
        }
        refreshItems()
    }

    fun setInMemoryItems(items: List<CollectionItem>) {
        _items.value = items
    }

    private fun loadAllFromDatabase(): List<CollectionItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DenimDatabaseHelper.TABLE_ITEMS,
            null,
            null,
            null,
            null,
            null,
            "${DenimDatabaseHelper.COL_CREATED_AT} DESC"
        )
        val list = mutableListOf<CollectionItem>()
        cursor.use {
            while (it.moveToNext()) {
                val item = mapCursorToItem(it)
                val photos = loadPhotosForItem(db, item.id)
                list.add(item.copy(photoUris = photos))
            }
        }
        return list
    }

    private fun loadPhotosForItem(db: android.database.sqlite.SQLiteDatabase, itemId: String): List<String> {
        val cursor = db.query(
            DenimDatabaseHelper.TABLE_PHOTOS,
            arrayOf(DenimDatabaseHelper.COL_PHOTO_URI),
            "${DenimDatabaseHelper.COL_PHOTO_ITEM_ID} = ?",
            arrayOf(itemId),
            null,
            null,
            "${DenimDatabaseHelper.COL_PHOTO_SORT_ORDER} ASC"
        )
        val photos = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                photos.add(it.getString(0))
            }
        }
        return photos
    }

    private fun mapCursorToItem(cursor: Cursor): CollectionItem {
        val variantIndex = cursor.getColumnIndex(DenimDatabaseHelper.COL_VARIANT_GUESS)
        val variantGuess = if (variantIndex != -1) cursor.getString(variantIndex) ?: "" else ""

        val productionYearIndex = cursor.getColumnIndex(DenimDatabaseHelper.COL_ESTIMATED_PRODUCTION_YEAR)
        val estimatedProductionYear = if (productionYearIndex != -1) cursor.getString(productionYearIndex) ?: "" else ""

        val factoryIndex = cursor.getColumnIndex(DenimDatabaseHelper.COL_ESTIMATED_FACTORY)
        val estimatedFactory = if (factoryIndex != -1) cursor.getString(factoryIndex) ?: "" else ""

        val rarityLevelIndex = cursor.getColumnIndex(DenimDatabaseHelper.COL_RARITY_LEVEL)
        val rarityLevel = if (rarityLevelIndex != -1) {
            RarityLevel.fromString(cursor.getString(rarityLevelIndex) ?: "")
        } else RarityLevel.UNKNOWN

        val raritySummaryIndex = cursor.getColumnIndex(DenimDatabaseHelper.COL_RARITY_SUMMARY)
        val raritySummary = if (raritySummaryIndex != -1) cursor.getString(raritySummaryIndex) ?: "" else ""

        val rarityReasonsIndex = cursor.getColumnIndex(DenimDatabaseHelper.COL_RARITY_REASONS)
        val rarityReasons = if (rarityReasonsIndex != -1) {
            parseReasons(cursor.getString(rarityReasonsIndex) ?: "")
        } else emptyList()

        val kfpLowIndex = cursor.getColumnIndex(DenimDatabaseHelper.COL_KOREA_FAIR_PURCHASE_LOW)
        val koreaFairPurchaseLow = if (kfpLowIndex != -1) cursor.getLong(kfpLowIndex) else 0L

        val kfpHighIndex = cursor.getColumnIndex(DenimDatabaseHelper.COL_KOREA_FAIR_PURCHASE_HIGH)
        val koreaFairPurchaseHigh = if (kfpHighIndex != -1) cursor.getLong(kfpHighIndex) else 0L

        val jfpLowIndex = cursor.getColumnIndex(DenimDatabaseHelper.COL_JAPAN_FAIR_PURCHASE_LOW)
        val japanFairPurchaseLow = if (jfpLowIndex != -1) cursor.getLong(jfpLowIndex) else 0L

        val jfpHighIndex = cursor.getColumnIndex(DenimDatabaseHelper.COL_JAPAN_FAIR_PURCHASE_HIGH)
        val japanFairPurchaseHigh = if (jfpHighIndex != -1) cursor.getLong(jfpHighIndex) else 0L

        return CollectionItem(
            id = cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_ID)),
            userTitle = cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_USER_TITLE)) ?: "",
            brandGuess = cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_BRAND_GUESS)) ?: "",
            modelGuess = cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_MODEL_GUESS)) ?: "",
            eraGuess = cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_ERA_GUESS)) ?: "",
            variantGuess = variantGuess,
            estimatedProductionYear = estimatedProductionYear,
            estimatedFactory = estimatedFactory,
            summary = cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_SUMMARY)) ?: "",
            confidence = QuickValueConfidence.fromString(
                cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_CONFIDENCE)) ?: ""
            ),
            condition = QuickValueCondition.fromString(
                cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_CONDITION)) ?: ""
            ),
            rarityLevel = rarityLevel,
            raritySummary = raritySummary,
            rarityReasons = rarityReasons,
            koreaFairPurchaseLow = koreaFairPurchaseLow,
            koreaFairPurchaseHigh = koreaFairPurchaseHigh,
            japanFairPurchaseLow = japanFairPurchaseLow,
            japanFairPurchaseHigh = japanFairPurchaseHigh,
            koreaSaleLow = cursor.getLong(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_KOREA_SALE_LOW)),
            koreaSaleHigh = cursor.getLong(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_KOREA_SALE_HIGH)),
            japanSaleLow = cursor.getLong(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_JAPAN_SALE_LOW)),
            japanSaleHigh = cursor.getLong(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_JAPAN_SALE_HIGH)),
            jpyToKrwRate = cursor.getDouble(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_JPY_TO_KRW_RATE)),
            verificationState = VerificationState.fromString(
                cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_VERIFICATION_STATE)) ?: ""
            ),
            syncEligibilityState = SyncEligibilityState.fromString(
                cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_SYNC_ELIGIBILITY_STATE)) ?: ""
            ),
            userNotes = cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_USER_NOTES)) ?: "",
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_UPDATED_AT)),
            rawAiResponseJson = cursor.getString(cursor.getColumnIndexOrThrow(DenimDatabaseHelper.COL_RAW_AI_RESPONSE_JSON)) ?: ""
        )
    }

    private fun serializeReasons(reasons: List<String>): String {
        return JSONArray(reasons).toString()
    }

    private fun parseReasons(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            (0 until jsonArray.length()).mapNotNull { jsonArray.optString(it).takeIf { str -> str.isNotBlank() } }
        } catch (_: Exception) {
            raw.split("\n").filter { it.isNotBlank() }
        }
    }
}
