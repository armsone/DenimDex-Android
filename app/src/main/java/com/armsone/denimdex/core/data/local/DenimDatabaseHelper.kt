package com.armsone.denimdex.core.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DenimDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "denimdex.db"
        const val DATABASE_VERSION = 2

        const val TABLE_ITEMS = "collection_items"
        const val COL_ID = "id"
        const val COL_USER_TITLE = "user_title"
        const val COL_BRAND_GUESS = "brand_guess"
        const val COL_MODEL_GUESS = "model_guess"
        const val COL_ERA_GUESS = "era_guess"
        const val COL_VARIANT_GUESS = "variant_guess"
        const val COL_ESTIMATED_PRODUCTION_YEAR = "estimated_production_year"
        const val COL_ESTIMATED_FACTORY = "estimated_factory"
        const val COL_SUMMARY = "summary"
        const val COL_CONFIDENCE = "confidence"
        const val COL_CONDITION = "condition"
        const val COL_RARITY_LEVEL = "rarity_level"
        const val COL_RARITY_SUMMARY = "rarity_summary"
        const val COL_RARITY_REASONS = "rarity_reasons"
        const val COL_KOREA_FAIR_PURCHASE_LOW = "korea_fair_purchase_low"
        const val COL_KOREA_FAIR_PURCHASE_HIGH = "korea_fair_purchase_high"
        const val COL_JAPAN_FAIR_PURCHASE_LOW = "japan_fair_purchase_low"
        const val COL_JAPAN_FAIR_PURCHASE_HIGH = "japan_fair_purchase_high"
        const val COL_KOREA_SALE_LOW = "korea_sale_low"
        const val COL_KOREA_SALE_HIGH = "korea_sale_high"
        const val COL_JAPAN_SALE_LOW = "japan_sale_low"
        const val COL_JAPAN_SALE_HIGH = "japan_sale_high"
        const val COL_JPY_TO_KRW_RATE = "jpy_to_krw_rate"
        const val COL_VERIFICATION_STATE = "verification_state"
        const val COL_SYNC_ELIGIBILITY_STATE = "sync_eligibility_state"
        const val COL_USER_NOTES = "user_notes"
        const val COL_CREATED_AT = "created_at"
        const val COL_UPDATED_AT = "updated_at"
        const val COL_RAW_AI_RESPONSE_JSON = "raw_ai_response_json"

        const val TABLE_PHOTOS = "collection_photos"
        const val COL_PHOTO_ID = "id"
        const val COL_PHOTO_ITEM_ID = "item_id"
        const val COL_PHOTO_URI = "photo_uri"
        const val COL_PHOTO_SORT_ORDER = "sort_order"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createItemsTable = """
            CREATE TABLE $TABLE_ITEMS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_USER_TITLE TEXT,
                $COL_BRAND_GUESS TEXT,
                $COL_MODEL_GUESS TEXT,
                $COL_ERA_GUESS TEXT,
                $COL_VARIANT_GUESS TEXT,
                $COL_ESTIMATED_PRODUCTION_YEAR TEXT,
                $COL_ESTIMATED_FACTORY TEXT,
                $COL_SUMMARY TEXT,
                $COL_CONFIDENCE TEXT,
                $COL_CONDITION TEXT,
                $COL_RARITY_LEVEL TEXT,
                $COL_RARITY_SUMMARY TEXT,
                $COL_RARITY_REASONS TEXT,
                $COL_KOREA_FAIR_PURCHASE_LOW INTEGER,
                $COL_KOREA_FAIR_PURCHASE_HIGH INTEGER,
                $COL_JAPAN_FAIR_PURCHASE_LOW INTEGER,
                $COL_JAPAN_FAIR_PURCHASE_HIGH INTEGER,
                $COL_KOREA_SALE_LOW INTEGER,
                $COL_KOREA_SALE_HIGH INTEGER,
                $COL_JAPAN_SALE_LOW INTEGER,
                $COL_JAPAN_SALE_HIGH INTEGER,
                $COL_JPY_TO_KRW_RATE REAL,
                $COL_VERIFICATION_STATE TEXT,
                $COL_SYNC_ELIGIBILITY_STATE TEXT,
                $COL_USER_NOTES TEXT,
                $COL_CREATED_AT INTEGER,
                $COL_UPDATED_AT INTEGER,
                $COL_RAW_AI_RESPONSE_JSON TEXT
            )
        """.trimIndent()

        val createPhotosTable = """
            CREATE TABLE $TABLE_PHOTOS (
                $COL_PHOTO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PHOTO_ITEM_ID TEXT NOT NULL,
                $COL_PHOTO_URI TEXT NOT NULL,
                $COL_PHOTO_SORT_ORDER INTEGER NOT NULL,
                FOREIGN KEY ($COL_PHOTO_ITEM_ID) REFERENCES $TABLE_ITEMS($COL_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        val createIndexPhotos = "CREATE INDEX idx_photos_item ON $TABLE_PHOTOS($COL_PHOTO_ITEM_ID)"

        db.execSQL(createItemsTable)
        db.execSQL(createPhotosTable)
        db.execSQL(createIndexPhotos)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $COL_VARIANT_GUESS TEXT")
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $COL_ESTIMATED_PRODUCTION_YEAR TEXT")
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $COL_ESTIMATED_FACTORY TEXT")
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $COL_RARITY_LEVEL TEXT")
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $COL_RARITY_SUMMARY TEXT")
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $COL_RARITY_REASONS TEXT")
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $COL_KOREA_FAIR_PURCHASE_LOW INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $COL_KOREA_FAIR_PURCHASE_HIGH INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $COL_JAPAN_FAIR_PURCHASE_LOW INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $COL_JAPAN_FAIR_PURCHASE_HIGH INTEGER DEFAULT 0")
        }
    }
}
