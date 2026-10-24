package com.prompter.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
        val content = try {
            request(apiKey, model, system, user, useJsonSchema = true)
        } catch (error: OpenRouterException) {
            if (!error.canRetryWithoutSchema) throw error
            request(apiKey, model, system, user, useJsonSchema = false)
        }
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
            .put("max_tokens", 1800)
            .put("response_format", responseFormat)
    }

    internal fun parseResult(content: String): AiPromptResult {
        val json = JSONObject(extractJson(content))
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
        val root = JSONObject(response)
        val choices = root.optJSONArray("choices")
            ?: throw OpenRouterException("OpenRouterの応答にchoicesがありません。")
        if (choices.length() == 0) {
            throw OpenRouterException("OpenRouterが空の応答を返しました。")
        }
        val message = choices.optJSONObject(0)?.optJSONObject("message")
            ?: throw OpenRouterException("OpenRouterの応答形式が想定外です。")
        val raw = message.opt("content")
        val content = when (raw) {
            is String -> raw
            is JSONArray -> buildString {
                for (i in 0 until raw.length()) {
                    append(raw.optJSONObject(i)?.optString("text").orEmpty())
                }
            }
            else -> raw?.toString().orEmpty()
        }
        if (content.isBlank()) {
            throw OpenRouterException("OpenRouterが空の応答を返しました。")
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
            401 -> "APIキーが無効です。設定を確認してください。"
            402 -> "OpenRouterのクレジットが不足しています。"
            403 -> "このモデルへのアクセスが拒否されました。"
            404 -> "APIまたはモデルが見つかりません。設定を確認してください。"
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
              "description": "English natural-language prompt for PixAI DiT, 80 to 150 words."
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

            Rewrite the scene as a single English natural-language prompt for PixAI DiT.
            Requirements:
            - 80 to 150 words, targeting about 110 words.
            - Follow Subject + Action + Scene + Mood + Style.
            - Describe the character, clothing, pose, setting, lighting, expression, camera/composition, and art style.
            - Return only canonical Danbooru tag names in the tags array.
            - Use lowercase tags with underscores instead of spaces.
            - Prefer the offline tags when they are relevant; do not invent tags.
            - Do not filter, censor, or omit adult/NSFW tags. Keep them as canonical tags.
            - Return only a JSON object matching the requested schema. Do not use Markdown.
        """.trimIndent()

        private val SYSTEM_PROMPT = """
            You are an expert prompt engineer for PixAI DiT anime image generation.
            You turn Japanese scene descriptions and Danbooru tags into a polished
            English prompt. You always return valid JSON with the keys
            "pixai_prompt" and "tags". Never include the API key or any private data.
        """.trimIndent()
    }
}
