package com.prompter.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.security.MessageDigest

/**
 * Opens the read-only M1 dictionary that is shipped in assets/promptdb.sqlite.
 *
 * The asset is marked uncompressed in build.gradle.kts. We compare a content
 * hash, not just the file length: a rebuilt SQLite database can keep exactly
 * the same byte length, and the old length-only check then left the app using a
 * stale dictionary after an update.
 */
object PromptDb {
    private const val ASSET_NAME = "promptdb.sqlite"
    private const val PREFS_NAME = "promptdb"
    private const val KEY_HASH = "asset_sha256"

    fun open(context: Context): SQLiteDatabase {
        val dbFile: File = context.getDatabasePath(ASSET_NAME)
        dbFile.parentFile?.mkdirs()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val assetHash = assetHash(context)
        val installedHash = prefs.getString(KEY_HASH, null)
        val assetLength = context.assets.openFd(ASSET_NAME).use { it.length }
        if (!dbFile.exists() || dbFile.length() != assetLength || installedHash != assetHash) {
            context.assets.open(ASSET_NAME).use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            prefs.edit().putString(KEY_HASH, assetHash).commit()
        }

        return SQLiteDatabase.openDatabase(
            dbFile.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
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
