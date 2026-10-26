package com.prompter.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class AiPromptResult(
    val pixaiPrompt: String,
    val tags: List<String>,
)

class OpenRouterException(
    message: String,
    val statusCode: Int? = null,
    val responseBody: String = "",
) : Exception(message) {
    val canRetryWithoutSchema: Boolean
        get() = statusCode in setOf(400, 404, 422) &&
            RETRY_HINTS.any {
                message.orEmpty().contains(it, ignoreCase = true) ||
                    responseBody.contains(it, ignoreCase = true)
            }

    val isUnauthorized: Boolean
        get() = statusCode == 401

    private companion object {
        val RETRY_HINTS = listOf(
            "json_schema",
            "structured",
            "response_format",
            "not supported",
            "unsupported",
        )
    }
}

/** Result of a lightweight GET /key call used by the settings screen. */
data class OpenRouterKeyInfo(
    val label: String?,
    val usage: Double?,
    val limit: Double?,
    val isFreeTier: Boolean?,
) {
    fun summary(): String = buildString {
        append("接続OK")
        isFreeTier?.let { append(if (it) "（無料枠）" else "（有料枠）") }
        usage?.let { append(" / 使用量 $${String.format(Locale.US, "%.2f", it)}") }
        limit?.let { append(" / 上限 $${String.format(Locale.US, "%.2f", it)}") }
    }
}

/**
 * Minimal OpenRouter / OpenAI-compatible chat-completions client.
 *
 * The key is passed in-memory only and is never logged. Requests use JSON
 * Schema structured output, with a json_object fallback for compatible models
 * that do not support the json_schema response format.
 */
class OpenRouterClient(
    private val baseUrl: String = AiDefaults.BASE_URL,
) {
    fun refine(
        apiKey: String,
        model: String,
        input: String,
        offlineTags: List<String>,
        unmatched: String,
    ): AiPromptResult {
        val system = SYSTEM_PROMPT
        val user = buildUserPrompt(input, offlineTags, unmatched)
        return try {
            requestAndParse(apiKey, model, system, user, useJsonSchema = true)
        } catch (error: OpenRouterException) {
            // Retry with json_object when structured output is unsupported, or
            // when a free-router model returned null/malformed content.
            if (error.statusCode != null && !error.canRetryWithoutSchema) throw error
            requestAndParse(apiKey, model, system, user, useJsonSchema = false)
        }
    }

    /** Validates an API key without generating tokens. */
    fun testKey(apiKey: String): OpenRouterKeyInfo {
        val endpoint = AiDefaults.normalizeBaseUrl(baseUrl) + "/key"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "HTTP-Referer",
                "https://github.com/shiokunn/prompter",
            )
            connection.setRequestProperty("X-Title", "Prompter")

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw OpenRouterException(
                    message = errorMessage(status, body),
                    statusCode = status,
                    responseBody = body,
                )
            }
            val data = JSONObject(body).optJSONObject("data") ?: JSONObject()
            return OpenRouterKeyInfo(
                label = data.optString("label").takeIf { it.isNotBlank() },
                usage = data.optNullableDouble("usage"),
                limit = data.optNullableDouble("limit"),
                isFreeTier = if (data.has("is_free_tier") && !data.isNull("is_free_tier")) {
                    data.optBoolean("is_free_tier")
                } else {
                    null
                },
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name) else null

    private fun requestAndParse(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        useJsonSchema: Boolean,
    ): AiPromptResult {
        val content = request(
            apiKey = apiKey,
            model = model,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            useJsonSchema = useJsonSchema,
        )
        return parseResult(content)
    }

    internal fun buildRequestJson(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        useJsonSchema: Boolean,
    ): JSONObject {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", userPrompt))

        val responseFormat = if (useJsonSchema) {
            JSONObject()
                .put("type", "json_schema")
                .put(
                    "json_schema",
                    JSONObject()
                        .put("name", "prompter_result")
                        .put("strict", true)
                        .put("schema", responseSchema()),
                )
        } else {
            JSONObject().put("type", "json_object")
        }

        return JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.4)
            .put("response_format", responseFormat)
    }

    internal fun parseResult(content: String): AiPromptResult {
        val jsonText = extractJson(content)
        if (jsonText.isBlank() || jsonText.equals("null", ignoreCase = true)) {
            throw OpenRouterException("AIがJSONを返しませんでした。再試行してください。")
        }
        val json = try {
            JSONObject(jsonText)
        } catch (_: Exception) {
            throw OpenRouterException(
                "AIの応答をJSONとして解析できませんでした。再試行してください。",
                responseBody = content,
            )
        }
        val prompt = json.optString("pixai_prompt").trim()
        if (prompt.isEmpty()) {
            throw OpenRouterException("AIが自然文を返しませんでした。")
        }
        val tags = mutableListOf<String>()
        val array = json.optJSONArray("tags")
        if (array != null) {
            for (i in 0 until array.length()) {
                val tag = array.optString(i).trim()
                if (tag.isNotEmpty()) tags.add(tag)
            }
        }
        return AiPromptResult(pixaiPrompt = prompt, tags = tags)
    }

    internal fun extractJson(content: String): String {
        var text = content.trim()
        if (text.startsWith("```")) {
            val firstNewline = text.indexOf('\n')
            if (firstNewline >= 0) text = text.substring(firstNewline + 1)
            if (text.trimEnd().endsWith("```")) {
                text = text.trimEnd().dropLast(3)
            }
        }
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            text.substring(start, end + 1)
        } else {
            text.trim()
        }
    }

    private fun request(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        useJsonSchema: Boolean,
    ): String {
        val endpoint = AiDefaults.normalizeBaseUrl(baseUrl) + "/chat/completions"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "HTTP-Referer",
                "https://github.com/shiokunn/prompter",
            )
            connection.setRequestProperty("X-Title", "Prompter")

            val payload = buildRequestJson(
                model = model,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                useJsonSchema = useJsonSchema,
            ).toString()
            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw OpenRouterException(
                    message = errorMessage(status, body),
                    statusCode = status,
                    responseBody = body,
                )
            }
            return extractContent(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun extractContent(response: String): String {
        val root = try {
            JSONObject(response)
        } catch (_: Exception) {
            throw OpenRouterException(
                "OpenRouterの応答を解析できませんでした。",
                responseBody = response,
            )
        }
        val choices = root.optJSONArray("choices")
            ?: throw OpenRouterException("OpenRouterの応答にchoicesがありません。")
        if (choices.length() == 0) {
            throw OpenRouterException("OpenRouterが空の応答を返しました。")
        }
        val message = choices.optJSONObject(0)?.optJSONObject("message")
            ?: throw OpenRouterException("OpenRouterの応答形式が想定外です。")
        val raw = message.opt("content")
        val content = if (raw == null || raw === JSONObject.NULL) {
            ""
        } else {
            when (raw) {
                is String -> raw
                is JSONArray -> buildString {
                    for (i in 0 until raw.length()) {
                        append(raw.optJSONObject(i)?.optString("text").orEmpty())
                    }
                }
                else -> raw.toString()
            }
        }
        if (content.isBlank() || content.trim().equals("null", ignoreCase = true)) {
            throw OpenRouterException("OpenRouterが空の応答を返しました。再試行してください。")
        }
        return content
    }

    private fun errorMessage(status: Int, body: String): String {
        val apiMessage = try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        } catch (_: Exception) {
            ""
        }
        val base = when (status) {
            401 -> "APIキーが無効です。OpenRouterで新しいキーを発行してください。"
            402 -> "OpenRouterのクレジットが不足しています。"
            403 -> "このモデルへのアクセスが拒否されました。"
            404 -> "モデルが見つかりません。設定のモデルIDを確認してください。"
            429 -> "レート制限に達しました。少し待って再試行してください。"
            in 500..599 -> "OpenRouter側でエラーが発生しました。時間をおいて再試行してください。"
            else -> "OpenRouter APIエラー (HTTP $status)"
        }
        return if (apiMessage.isBlank()) {
            base
        } else {
            "$base ${apiMessage.take(240)}"
        }
    }

    private fun responseSchema(): JSONObject = JSONObject(
        """
        {
          "type": "object",
          "properties": {
            "pixai_prompt": {
              "type": "string",
              "description": "Detailed, precise English natural-language prompt for PixAI DiT. No fixed upper word limit. Stay faithful to the input; do not invent objects or colors to pad."
            },
            "tags": {
              "type": "array",
              "items": { "type": "string" },
              "minItems": 1,
              "maxItems": 40,
              "description": "Canonical Danbooru tag names, lowercase with underscores."
            }
          },
          "required": ["pixai_prompt", "tags"],
          "additionalProperties": false
        }
        """.trimIndent(),
    )

    companion object {
        internal fun buildUserPrompt(
            input: String,
            offlineTags: List<String>,
            unmatched: String,
        ): String = """
            Japanese input:
            $input

            Offline dictionary tags (prefer these exact canonical tags):
            ${offlineTags.joinToString(", ").ifBlank { "(none)" }}

            Unmatched Japanese text:
            ${unmatched.ifBlank { "(none)" }}

            Rewrite the scene as a single detailed, precise English natural-language prompt
            for PixAI DiT.
            Requirements:
            - Write a detailed, precise prompt. There is no upper word limit. A rich input
              should usually produce 180 words or more; a simple input may be shorter.
              Never stop at 150 words just because of a limit.
            - Every sentence must add new visual information. Do not repeat adjectives or
              pad the prompt.
            - Stay faithful to the input. Do not invent a color for hair, eyes, or clothing
              unless it is stated. Do not add new furniture, props, objects, named
              characters, copyrighted works, or setting elements that the input does not
              mention or clearly imply.
            - You may add generic fabric folds, lighting, shadows, a simple background, and
              camera details. If the input is simple, deepen the description of what is
              present (hair, clothing, pose, chair, light, composition) instead of adding
              new objects.
            - Follow Subject + Action + Scene + Mood + Style.
            - Describe the character, face, hair, eyes, clothing, pose, hands, setting,
              lighting, shadows, atmosphere, camera angle/composition, and art style with
              concrete visual detail.
            - Preserve every stated color, hairstyle, clothing, expression, number of
              people, pose, and composition. When one trait applies to several people,
              repeat it for each person.
            - Return only canonical Danbooru tag names in the tags array.
            - Use lowercase tags with underscores instead of spaces.
            - Prefer the offline tags when they are relevant; do not invent tags. If an
              offline tag contradicts the input (for example, the number of people), follow
              the input and omit that tag.
            - Do not filter, censor, or omit adult/NSFW tags. Keep them as canonical tags.
            - Return only a JSON object matching the requested schema. Do not use Markdown.
        """.trimIndent()

        private val SYSTEM_PROMPT = """
            You are an expert prompt engineer for PixAI DiT anime image generation.
            You turn Japanese scene descriptions and Danbooru tags into a polished,
            precise, detailed English prompt. Never shorten the prompt just to meet a
            word limit; include every concrete visual detail without repeating yourself.
            Stay faithful to the input and never invent objects or colors just to pad.
            You always return valid JSON with the keys "pixai_prompt" and "tags".
            Never include the API key or any private data.
        """.trimIndent()
    }
}
