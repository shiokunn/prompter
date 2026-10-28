package com.prompter.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class HistoryItem(
    val id: Long,
    val input: String,
    val pixaiPrompt: String,
    val tagText: String,
    val unmatched: String,
    val createdAt: Long,
    val isFavorite: Boolean,
    val characterName: String = "",
    val characterSeries: String = "",
    val characterEnglish: String = "",
)

/**
 * Small local history store. The dictionary DB is read-only and separate;
 * this DB only contains conversions the user has made.
 */
class HistoryStore(context: Context) : SQLiteOpenHelper(
    context,
    DB_NAME,
    null,
    DB_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                input TEXT NOT NULL,
                pixai_prompt TEXT NOT NULL,
                tag_text TEXT NOT NULL,
                unmatched TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                character_name TEXT NOT NULL DEFAULT '',
                character_series TEXT NOT NULL DEFAULT '',
                character_english TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_history_created ON $TABLE(created_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN character_name TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN character_series TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN character_english TEXT NOT NULL DEFAULT ''")
        }
    }

    fun add(
        input: String,
        pixaiPrompt: String,
        tagText: String,
        unmatched: String,
        characterName: String = "",
        characterSeries: String = "",
        characterEnglish: String = "",
    ): Long {
        val values = ContentValues().apply {
            put("input", input)
            put("pixai_prompt", pixaiPrompt)
            put("tag_text", tagText)
            put("unmatched", unmatched)
            put("character_name", characterName)
            put("character_series", characterSeries)
            put("character_english", characterEnglish)
            put("created_at", System.currentTimeMillis())
            put("is_favorite", 0)
        }
        return writableDatabase.insertOrThrow(TABLE, null, values)
    }

    fun update(
        id: Long,
        pixaiPrompt: String,
        tagText: String,
    ) {
        val values = ContentValues().apply {
            put("pixai_prompt", pixaiPrompt)
            put("tag_text", tagText)
        }
        writableDatabase.update(TABLE, values, "id = ?", arrayOf(id.toString()))
    }

    fun recent(onlyFavorites: Boolean = false, limit: Int = 100): List<HistoryItem> {
        val selection = if (onlyFavorites) "is_favorite = 1" else null
        readableDatabase.query(
            TABLE,
            null,
            selection,
            null,
            null,
            null,
            "is_favorite DESC, created_at DESC",
            limit.toString(),
        ).use { cursor ->
            val items = mutableListOf<HistoryItem>()
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val inputIndex = cursor.getColumnIndexOrThrow("input")
            val pixaiIndex = cursor.getColumnIndexOrThrow("pixai_prompt")
            val tagIndex = cursor.getColumnIndexOrThrow("tag_text")
            val unmatchedIndex = cursor.getColumnIndexOrThrow("unmatched")
            val createdIndex = cursor.getColumnIndexOrThrow("created_at")
            val favoriteIndex = cursor.getColumnIndexOrThrow("is_favorite")
            while (cursor.moveToNext()) {
                items.add(
                    HistoryItem(
                        id = cursor.getLong(idIndex),
                        input = cursor.getString(inputIndex),
                        pixaiPrompt = cursor.getString(pixaiIndex),
                        tagText = cursor.getString(tagIndex),
                        unmatched = cursor.getString(unmatchedIndex),
                        createdAt = cursor.getLong(createdIndex),
                        isFavorite = cursor.getInt(favoriteIndex) != 0,
                        characterName = cursor.getString(cursor.getColumnIndexOrThrow("character_name")),
                        characterSeries = cursor.getString(cursor.getColumnIndexOrThrow("character_series")),
                        characterEnglish = cursor.getString(cursor.getColumnIndexOrThrow("character_english")),
                    ),
                )
            }
            return items
        }
    }

    fun setFavorite(id: Long, favorite: Boolean) {
        val values = ContentValues().apply {
            put("is_favorite", if (favorite) 1 else 0)
        }
        writableDatabase.update(TABLE, values, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
    }

    fun clear() {
        writableDatabase.delete(TABLE, null, null)
    }

    companion object {
        private const val DB_NAME = "prompter_history.db"
        private const val DB_VERSION = 2
        private const val TABLE = "history"
    }
}
