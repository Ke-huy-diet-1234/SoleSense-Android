package com.example.solesense.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cơ sở dữ liệu SQLite cục bộ lưu trữ Lịch sử tìm kiếm sản phẩm (Recent Searches - Ảnh 7)
 */
class SearchHistoryDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "solesense_search_history.db"
        private const val DB_VERSION = 1
        private const val TABLE_NAME = "search_history"
        private const val COL_KEYWORD = "keyword"
        private const val COL_TIMESTAMP = "searched_at"

        @Volatile
        private var instance: SearchHistoryDatabase? = null

        private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
        val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

        fun getInstance(context: Context): SearchHistoryDatabase {
            return instance ?: synchronized(this) {
                instance ?: SearchHistoryDatabase(context).also { db ->
                    instance = db
                    db.refreshState()
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_KEYWORD TEXT PRIMARY KEY,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // Khởi tạo sẵn một số từ khóa gợi ý ban đầu nếu bảng trống
        val initialKeywords = listOf(
            "Nike Pegasus 41",
            "Adidas Ultraboost 5",
            "Asics Gel-Nimbus",
            "New Balance 574",
            "Converse Chuck 70",
            "Puma Velocity Nitro"
        )
        val now = System.currentTimeMillis()
        initialKeywords.forEachIndexed { index, kw ->
            val values = ContentValues().apply {
                put(COL_KEYWORD, kw)
                put(COL_TIMESTAMP, now - index * 1000L)
            }
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addSearchKeyword(rawKeyword: String) {
        val keyword = rawKeyword.trim()
        if (keyword.isBlank()) return
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_KEYWORD, keyword)
                put(COL_TIMESTAMP, System.currentTimeMillis())
            }
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshState()
        } catch (_: Exception) {}
    }

    fun deleteSearchKeyword(keyword: String) {
        try {
            val db = writableDatabase
            db.delete(TABLE_NAME, "$COL_KEYWORD = ?", arrayOf(keyword))
            refreshState()
        } catch (_: Exception) {}
    }

    fun clearAllHistory() {
        try {
            val db = writableDatabase
            db.delete(TABLE_NAME, null, null)
            refreshState()
        } catch (_: Exception) {}
    }

    fun refreshState() {
        try {
            val db = readableDatabase
            val list = mutableListOf<String>()
            val cursor = db.query(
                TABLE_NAME,
                arrayOf(COL_KEYWORD),
                null,
                null,
                null,
                null,
                "$COL_TIMESTAMP DESC",
                "15"
            )
            cursor.use { c ->
                val idx = c.getColumnIndex(COL_KEYWORD)
                while (c.moveToNext()) {
                    if (idx >= 0) {
                        list.add(c.getString(idx))
                    }
                }
            }
            _recentSearches.value = list
        } catch (_: Exception) {}
    }
}
