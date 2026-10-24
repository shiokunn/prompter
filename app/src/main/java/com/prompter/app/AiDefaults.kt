package com.prompter.app

import java.net.URI

object AiDefaults {
    const val BASE_URL = "https://openrouter.ai/api/v1"

    /** OpenRouter's free-model router; avoids depending on one free model. */
    const val MODEL = "openrouter/free"
    const val BALANCED_MODEL = "google/gemini-3.1-flash-lite"
    const val BUDGET_MODEL = "google/gemini-2.5-flash-lite"
    const val QUALITY_MODEL = "openai/gpt-5.6-luna"
    const val LEGACY_FREE_MODEL = "google/gemma-4-31b-it:free"

    /** Old defaults that should migrate once to the free router. */
    val LEGACY_DEFAULT_MODELS = setOf(BALANCED_MODEL, LEGACY_FREE_MODEL)

    data class ModelPreset(
        val label: String,
        val model: String,
    )

    val PRESETS = listOf(
        ModelPreset("無料（自動）", MODEL),
        ModelPreset("バランス", BALANCED_MODEL),
        ModelPreset("低コスト", BUDGET_MODEL),
        ModelPreset("高品質", QUALITY_MODEL),
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
