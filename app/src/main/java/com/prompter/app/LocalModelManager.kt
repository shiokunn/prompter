package com.prompter.app

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.security.MessageDigest

enum class LocalModelState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    NEEDS_VERIFICATION,
    VERIFYING,
    READY,
    ERROR,
}

data class LocalModelStatus(
    val state: LocalModelState,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = LocalModelSpec.SIZE_BYTES,
    val error: String? = null,
) {
    val progress: Float
        get() = if (totalBytes <= 0L) 0f
        else (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

    val isReady: Boolean
        get() = state == LocalModelState.READY
}

/**
 * Downloads and verifies the optional Gemma 4 E2B model.
 *
 * DownloadManager handles Wi-Fi-only transfer, notifications, and resumption.
 * The file lives in the app-specific external files directory, so it is removed
 * on uninstall and needs no storage permission.
 */
class LocalModelManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val downloadManager =
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val modelFile: File = LocalModelSpec.modelFile(appContext)

    fun status(): LocalModelStatus {
        val file = modelFile
        val downloadId = prefs.getLong(KEY_DOWNLOAD_ID, NO_ID)
        if (downloadId != NO_ID) {
            val download = queryDownload(downloadId)
            if (download != null) {
                when (download.status) {
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PAUSED,
                    -> return LocalModelStatus(
                        state = LocalModelState.DOWNLOADING,
                        bytesDownloaded = download.bytesDownloaded,
                        totalBytes = download.totalBytes.takeIf { it > 0L }
                            ?: LocalModelSpec.SIZE_BYTES,
                    )

                    DownloadManager.STATUS_SUCCESSFUL -> {
                        if (file.isFile && file.length() == LocalModelSpec.SIZE_BYTES) {
                            return if (isVerified(file)) {
                                clearDownloadId()
                                LocalModelStatus(LocalModelState.READY)
                            } else {
                                LocalModelStatus(LocalModelState.NEEDS_VERIFICATION)
                            }
                        }
                        clearDownloadId()
                        return error("ダウンロードは完了しましたが、モデルファイルが見つかりません。")
                    }

                    DownloadManager.STATUS_FAILED -> {
                        val reason = download.reason
                        clearDownloadId()
                        return error("モデルのダウンロードに失敗しました ($reason)。再試行してください。")
                    }
                }
            } else {
                // The DownloadManager row can disappear after a reboot or an
                // app-data restore. Fall through to the file checks.
                clearDownloadId()
            }
        }

        if (file.isFile) {
            if (file.length() != LocalModelSpec.SIZE_BYTES) {
                return error(
                    "モデルファイルのサイズが不正です " +
                        "(${file.length()} / ${LocalModelSpec.SIZE_BYTES} bytes)。削除して再取得してください。",
                )
            }
            return if (isVerified(file)) {
                LocalModelStatus(LocalModelState.READY)
            } else {
                LocalModelStatus(LocalModelState.NEEDS_VERIFICATION)
            }
        }
        return LocalModelStatus(LocalModelState.NOT_DOWNLOADED)
    }

    fun startDownload(): LocalModelStatus {
        val current = status()
        if (current.state == LocalModelState.DOWNLOADING) return current
        if (current.state == LocalModelState.READY) return current

        val memory = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        memory.getMemoryInfo(memoryInfo)
        if (memoryInfo.totalMem < LocalModelSpec.MIN_RAM_BYTES) {
            return error("ローカルLLMには8GB以上のRAMが必要です。この端末では利用できません。")
        }

        val destinationDir = modelFile.parentFile
            ?: return error("モデルの保存先を作成できませんでした。")
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            return error("モデルの保存先を作成できませんでした。")
        }

        // A previous partial/mismatched file would make DownloadManager append
        // to an unknown state. Remove it before checking free space so its
        // bytes count toward the available capacity.
        if (modelFile.exists()) {
            modelFile.delete()
        }
        prefs.edit()
            .remove(KEY_VERIFIED_SIZE)
            .remove(KEY_VERIFIED_MTIME)
            .apply()

        val freeBytes = runCatching { StatFs(destinationDir.absolutePath).availableBytes }
            .getOrDefault(0L)
        if (freeBytes < LocalModelSpec.SIZE_BYTES + LocalModelSpec.EXTRA_FREE_BYTES) {
            return error("空き容量が不足しています。約3.6GBの空き容量を確保してください。")
        }

        val request = DownloadManager.Request(Uri.parse(LocalModelSpec.DOWNLOAD_URL))
            .setTitle("${LocalModelSpec.DISPLAY_NAME} モデル")
            .setDescription("PrompterのローカルLLM（約2.59GB）")
            .setMimeType("application/octet-stream")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
            )
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .addRequestHeader("User-Agent", "Prompter/${appVersionName()}")
            .setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                "models/${LocalModelSpec.FILE_NAME}",
            )

        return try {
            val id = downloadManager.enqueue(request)
            prefs.edit().putLong(KEY_DOWNLOAD_ID, id).apply()
            LocalModelStatus(
                state = LocalModelState.DOWNLOADING,
                bytesDownloaded = 0L,
                totalBytes = LocalModelSpec.SIZE_BYTES,
            )
        } catch (t: Throwable) {
            error("ダウンロードを開始できませんでした: ${t.message ?: "不明なエラー"}")
        }
    }

    fun cancelDownload(): LocalModelStatus {
        val id = prefs.getLong(KEY_DOWNLOAD_ID, NO_ID)
        if (id != NO_ID) {
            runCatching { downloadManager.remove(id) }
            clearDownloadId()
        }
        if (modelFile.exists()) modelFile.delete()
        prefs.edit()
            .remove(KEY_VERIFIED_SIZE)
            .remove(KEY_VERIFIED_MTIME)
            .apply()
        return LocalModelStatus(LocalModelState.NOT_DOWNLOADED)
    }

    fun verify(): LocalModelStatus {
        if (!modelFile.isFile) {
            return LocalModelStatus(LocalModelState.NOT_DOWNLOADED)
        }
        if (modelFile.length() != LocalModelSpec.SIZE_BYTES) {
            return error(
                "モデルファイルのサイズが不正です。削除して再ダウンロードしてください。",
            )
        }
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            modelFile.inputStream().use { input ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            if (!actual.equals(LocalModelSpec.SHA256, ignoreCase = true)) {
                modelFile.delete()
                prefs.edit()
                    .remove(KEY_VERIFIED_SIZE)
                    .remove(KEY_VERIFIED_MTIME)
                    .remove(KEY_DOWNLOAD_ID)
                    .apply()
                return error("モデルのSHA-256が一致しません。ファイルを削除しました。再取得してください。")
            }
            prefs.edit()
                .putLong(KEY_VERIFIED_SIZE, modelFile.length())
                .putLong(KEY_VERIFIED_MTIME, modelFile.lastModified())
                .remove(KEY_DOWNLOAD_ID)
                .apply()
            LocalModelStatus(LocalModelState.READY)
        } catch (t: Throwable) {
            error("モデルの検証に失敗しました: ${t.message ?: "不明なエラー"}")
        }
    }

    fun deleteModel(): LocalModelStatus {
        val id = prefs.getLong(KEY_DOWNLOAD_ID, NO_ID)
        if (id != NO_ID) {
            runCatching { downloadManager.remove(id) }
        }
        if (modelFile.exists()) modelFile.delete()
        prefs.edit()
            .remove(KEY_DOWNLOAD_ID)
            .remove(KEY_VERIFIED_SIZE)
            .remove(KEY_VERIFIED_MTIME)
            .apply()
        return LocalModelStatus(LocalModelState.NOT_DOWNLOADED)
    }

    private fun isVerified(file: File): Boolean =
        prefs.getLong(KEY_VERIFIED_SIZE, -1L) == file.length() &&
            prefs.getLong(KEY_VERIFIED_MTIME, -1L) == file.lastModified()

    private fun clearDownloadId() {
        prefs.edit().remove(KEY_DOWNLOAD_ID).apply()
    }

    private fun error(message: String) = LocalModelStatus(
        state = LocalModelState.ERROR,
        error = message,
    )

    private fun queryDownload(id: Long): DownloadInfo? {
        val cursor = runCatching {
            downloadManager.query(DownloadManager.Query().setFilterById(id))
        }.getOrNull() ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = it.getLong(
                it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
            )
            val total = it.getLong(
                it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
            )
            val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            return DownloadInfo(status, downloaded, total, reason)
        }
    }

    private data class DownloadInfo(
        val status: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val reason: Int,
    )

    private fun appVersionName(): String = runCatching {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
    }.getOrNull().orEmpty().ifBlank { "dev" }

    private companion object {
        const val PREFS_NAME = "prompter_local_model"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_VERIFIED_SIZE = "verified_size"
        const val KEY_VERIFIED_MTIME = "verified_mtime"
        const val NO_ID = -1L
    }
}
