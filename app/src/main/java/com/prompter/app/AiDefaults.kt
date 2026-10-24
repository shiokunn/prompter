package com.prompter.app

import java.net.URI

object AiDefaults {
    const val BASE_URL = "https://openrouter.ai/api/v1"
    const val MODEL = "google/gemini-3.1-flash-lite"

    data class ModelPreset(
        val label: String,
        val model: String,
    )

    val PRESETS = listOf(
        ModelPreset("既定", "google/gemini-3.1-flash-lite"),
        ModelPreset("低コスト", "google/gemini-2.5-flash-lite"),
        ModelPreset("高品質", "openai/gpt-5.6-luna"),
        ModelPreset("無料テスト", "google/gemma-4-31b-it:free"),
    )

    fun normalizeBaseUrl(value: String): String =
        value.trim().trimEnd('/').ifBlank { BASE_URL }

    fun isValidBaseUrl(value: String): Boolean {
        val normalized = normalizeBaseUrl(value)
        return try {
            val uri = URI(normalized)
            uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
