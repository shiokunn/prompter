package com.prompter.app

/**
 * Small abstraction around the on-device translation engine. Keeping this
 * interface free of ML Kit types lets the UI and merge logic stay testable and
 * lets the app degrade gracefully when the model is unavailable.
 */
interface TextTranslator {
    suspend fun isReady(): Boolean

    suspend fun downloadModels()

    suspend fun translateJapaneseToEnglish(text: String): String

    suspend fun translateEnglishToJapanese(text: String): String

    fun close()
}
