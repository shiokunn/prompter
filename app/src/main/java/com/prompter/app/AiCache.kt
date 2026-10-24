package com.prompter.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Small persistent cache for AI results. It contains only prompts/tags, never
 * the API key. Cache keys include the endpoint, model, input, and offline tags.
 */
class AiCache(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun key(
        baseUrl: String,
        model: String,
        input: String,
        offlineTags: List<String>,
        unmatched: String,
    ): String {
        val raw = buildString {
            append(CACHE_VERSION).append('\n')
            append(baseUrl).append('\n')
            append(model).append('\n')
            append(input).append('\n')
            append(offlineTags.joinToString("\n")).append('\n')
            append(unmatched)
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun get(key: String): AiPromptResult? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val prompt = json.optString("pixai_prompt").trim()
            if (prompt.isEmpty()) {
                prefs.edit().remove(key).apply()
                return null
            }
            val tags = mutableListOf<String>()
            val array = json.optJSONArray("tags")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val tag = array.optString(i).trim()
                    if (tag.isNotEmpty()) tags.add(tag)
                }
            }
            AiPromptResult(pixaiPrompt = prompt, tags = tags)
        } catch (_: Exception) {
            prefs.edit().remove(key).apply()
            null
        }
    }

    fun put(key: String, result: AiPromptResult) {
        val tags = JSONArray()
        result.tags.forEach { tags.put(it) }
        val json = JSONObject()
            .put("pixai_prompt", result.pixaiPrompt)
            .put("tags", tags)
            .put("created_at", System.currentTimeMillis())
        prefs.edit().putString(key, json.toString()).apply()
        trim()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun trim() {
        val entries = prefs.all.entries.mapNotNull { (key, value) ->
            val createdAt = try {
                JSONObject(value as? String ?: return@mapNotNull null)
                    .optLong("created_at", 0L)
            } catch (_: Exception) {
                return@mapNotNull null
            }
            key to createdAt
        }
        if (entries.size <= MAX_ENTRIES) return
        val editor = prefs.edit()
        entries.sortedBy { it.second }
            .take(entries.size - MAX_ENTRIES)
            .forEach { (key, _) -> editor.remove(key) }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "prompter_ai_cache"
        private const val CACHE_VERSION = 1
        private const val MAX_ENTRIES = 50
    }
}
