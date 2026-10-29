package com.prompter.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

/**
 * A verified alias from a signed dictionary release. Updates are authoritative:
 * the matcher gives them priority over every bundled alias with the same
 * normalized reading, while bundled aliases for other readings stay untouched.
 */
data class UpdateAlias(
    val surface: String,
    val norm: String,
    val tag: String,
    val source: String,
    val confidence: Double,
    val priority: Int,
)

data class DictionarySnapshot(
    val version: Long,
    val installedAt: Long,
    val aliasCount: Int,
    val sourceTag: String,
    val aliases: List<UpdateAlias>,
) {
    companion object {
        val EMPTY = DictionarySnapshot(0L, 0L, 0, "", emptyList())
    }
}

data class DictionaryUpdateOutcome(
    val ok: Boolean,
    val message: String,
    val version: Long = 0L,
    val aliasCount: Int = 0,
)

@Entity(tableName = "dictionary_meta")
data class DictionaryMetaEntity(
    @PrimaryKey val id: Int = 1,
    val version: Long,
    val installedAt: Long,
    val aliasCount: Int,
    val sourceTag: String,
)

@Entity(tableName = "dictionary_aliases")
data class UpdateAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val surface: String,
    val norm: String,
    val tag: String,
    val source: String,
    val confidence: Double,
    val priority: Int,
)

@Dao
abstract class DictionaryUpdateDao {
    @Query("SELECT * FROM dictionary_meta WHERE id = 1")
    abstract fun meta(): DictionaryMetaEntity?

    @Query("SELECT * FROM dictionary_aliases")
    abstract fun aliases(): List<UpdateAliasEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun putMeta(meta: DictionaryMetaEntity)

    @Insert
    abstract fun insertAliases(aliases: List<UpdateAliasEntity>)

    @Query("DELETE FROM dictionary_aliases")
    abstract fun deleteAliases()

    @Query("DELETE FROM dictionary_meta")
    abstract fun deleteMeta()

    /** One transaction: a crash cannot leave a new version with partial rows. */
    @Transaction
    open fun replace(meta: DictionaryMetaEntity, aliases: List<UpdateAliasEntity>) {
        deleteAliases()
        insertAliases(aliases)
        putMeta(meta)
    }

    @Transaction
    open fun clear() {
        deleteAliases()
        deleteMeta()
    }
}

@Database(
    entities = [DictionaryMetaEntity::class, UpdateAliasEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class DictionaryUpdateDatabase : RoomDatabase() {
    abstract fun dao(): DictionaryUpdateDao
}

/**
 * Room-backed store for signed dictionary updates. Room is deliberately kept
 * separate from the read-only bundled SQLite asset so a broken update can never
 * damage the shipped dictionary; clearing these tables restores it.
 */
object DictionaryUpdates {
    private const val DB_NAME = "dictionary-updates.db"

    @Volatile
    private var database: DictionaryUpdateDatabase? = null

    private fun open(context: Context): DictionaryUpdateDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                DictionaryUpdateDatabase::class.java,
                DB_NAME,
            ).build().also { database = it }
        }

    fun snapshot(context: Context): DictionarySnapshot {
        val dao = open(context).dao()
        val meta = dao.meta() ?: return DictionarySnapshot.EMPTY
        return DictionarySnapshot(
            version = meta.version,
            installedAt = meta.installedAt,
            aliasCount = meta.aliasCount,
            sourceTag = meta.sourceTag,
            aliases = dao.aliases().map { it.toUpdateAlias() },
        )
    }

    fun install(
        context: Context,
        update: VerifiedDictionaryUpdate,
        sourceTag: String,
        now: Long = System.currentTimeMillis(),
    ): DictionarySnapshot {
        val aliases = update.aliases.map { alias ->
            UpdateAliasEntity(
                surface = alias.surface,
                norm = M1Matcher.normalize(alias.surface),
                tag = alias.tag,
                source = "signed-update",
                confidence = 1.0,
                priority = UPDATE_PRIORITY,
            )
        }
        open(context).dao().replace(
            DictionaryMetaEntity(
                version = update.version,
                installedAt = now,
                aliasCount = aliases.size,
                sourceTag = sourceTag,
            ),
            aliases,
        )
        return DictionarySnapshot(
            version = update.version,
            installedAt = now,
            aliasCount = aliases.size,
            sourceTag = sourceTag,
            aliases = aliases.map { it.toUpdateAlias() },
        )
    }

    fun clear(context: Context) {
        open(context).dao().clear()
    }

    private fun UpdateAliasEntity.toUpdateAlias() = UpdateAlias(
        surface = surface,
        norm = norm,
        tag = tag,
        source = source,
        confidence = confidence,
        priority = priority,
    )

    private const val UPDATE_PRIORITY = 10_000
}
