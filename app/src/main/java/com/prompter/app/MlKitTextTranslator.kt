package com.prompter.app

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit on-device translation. The Japanese -> English model is used for
 * unmatched input fragments; the English -> Japanese model powers tag meanings
 * without a network request once downloaded.
 */
class MlKitTextTranslator : TextTranslator {
    private val jaToEn: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.JAPANESE)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build(),
    )
    private val enToJa: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.JAPANESE)
            .build(),
    )
    private val modelManager = RemoteModelManager.getInstance()

    override suspend fun isReady(): Boolean {
        val models = modelManager
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .await()
        val languages = models.map { it.language }.toSet()
        return TranslateLanguage.JAPANESE in languages &&
            TranslateLanguage.ENGLISH in languages
    }

    override suspend fun downloadModels() {
        val conditions = DownloadConditions.Builder().build()
        jaToEn.downloadModelIfNeeded(conditions).awaitCompletion()
        enToJa.downloadModelIfNeeded(conditions).awaitCompletion()
    }

    override suspend fun translateJapaneseToEnglish(text: String): String =
        jaToEn.translate(text).await()

    override suspend fun translateEnglishToJapanese(text: String): String =
        enToJa.translate(text).await()

    override fun close() {
        jaToEn.close()
        enToJa.close()
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { error ->
            if (cont.isActive) cont.resumeWithException(error)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }

    private suspend fun Task<Void>.awaitCompletion() =
        suspendCancellableCoroutine<Unit> { cont ->
            addOnSuccessListener {
                if (cont.isActive) cont.resume(Unit)
            }
            addOnFailureListener { error ->
                if (cont.isActive) cont.resumeWithException(error)
            }
            addOnCanceledListener {
                if (cont.isActive) cont.cancel()
            }
        }
}
