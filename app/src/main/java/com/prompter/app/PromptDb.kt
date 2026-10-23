package com.prompter.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Opens the read-only M1 dictionary that is shipped in assets/promptdb.sqlite.
 *
 * The asset is marked uncompressed in build.gradle.kts, so openFd() can be used
 * to detect when an app update replaced the database.
 */
object PromptDb {
    private const val ASSET_NAME = "promptdb.sqlite"

    fun open(context: Context): SQLiteDatabase {
        val dbFile: File = context.getDatabasePath(ASSET_NAME)
        dbFile.parentFile?.mkdirs()

        val assetLength = context.assets.openFd(ASSET_NAME).use { it.length }
        if (!dbFile.exists() || dbFile.length() != assetLength) {
            context.assets.open(ASSET_NAME).use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return SQLiteDatabase.openDatabase(
            dbFile.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
    }
}
