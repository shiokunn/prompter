package com.prompter.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/**
 * One row of the bundled character database. Alternate forms, costumes and
 * units are separate rows (`category == "variant"`); they are never folded into
 * the base character or offered as that character's alias.
 */
data class CharacterEntry(
    val id: Long,
    val nameJa: String,
    val nameEn: String,
    val series: String,
    val category: String,
    val note: String,
)

/**
 * Read-only character dictionary built from `character.md` by
 * `tools/build_character_db.py` and shipped as assets/characterdb.sqlite.
 *
 * Lookup matches every stored surface form (canonical name, short name,
 * reading, same-form alias) so うまる, UMR, 綾華 and 神里 all resolve.
 */
object CharacterDb {
    private const val ASSET_NAME = "characterdb.sqlite"
    private const val PREFS_NAME = "characterdb"
    private const val KEY_HASH = "asset_sha256"

    private val ignored = Regex("[\\s・·'’\\-]")

    @Volatile
    private var cached: SQLiteDatabase? = null

    fun search(context: Context, query: String, limit: Int = 8): List<CharacterEntry> {
        val normalized = normalize(query)
        if (normalized.isEmpty()) return emptyList()
        val db = try {
            open(context.applicationContext)
        } catch (failure: Exception) {
            return emptyList()
        }
        val sql = """
            SELECT DISTINCT c.id, c.name_ja, c.name_en, s.name_ja, c.category, c.note
            FROM aliases a
            JOIN characters c ON c.id = a.character_id
            JOIN series s ON s.id = c.series_id
            WHERE a.text_norm LIKE ?
            ORDER BY
                CASE WHEN a.text_norm = ? THEN 0 WHEN a.text_norm LIKE ? THEN 1 ELSE 2 END,
                c.id
            LIMIT ?
        """.trimIndent()
        return try {
            db.rawQuery(sql, arrayOf("%$normalized%", normalized, "$normalized%", limit.toString())).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            CharacterEntry(
                                id = cursor.getLong(0),
                                nameJa = cursor.getString(1),
                                nameEn = cursor.getString(2),
                                series = cursor.getString(3),
                                category = cursor.getString(4),
                                note = cursor.getString(5),
                            ),
                        )
                    }
                }
            }
        } catch (failure: Exception) {
            emptyList()
        }
    }

    internal fun normalize(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(ignored, "")

    @Synchronized
    private fun open(context: Context): SQLiteDatabase {
        cached?.let { return it }
        val file = context.getDatabasePath(ASSET_NAME)
        file.parentFile?.mkdirs()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hash = assetHash(context)
        val length = runCatching { context.assets.openFd(ASSET_NAME).use { it.length } }.getOrDefault(-1L)
        val stale = !file.exists() ||
            (length >= 0 && file.length() != length) ||
            prefs.getString(KEY_HASH, null) != hash
        if (stale) {
            context.assets.open(ASSET_NAME).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            prefs.edit().putString(KEY_HASH, hash).commit()
            cached?.close()
            cached = null
        }
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).also { cached = it }
    }

    private fun assetHash(context: Context): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.assets.open(ASSET_NAME).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
