package com.prompter.app

import android.content.Context
import android.os.Environment
import android.os.SystemClock
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import java.io.File

/**
 * Fixed metadata for the optional on-device model.
 *
 * The revision is pinned so the file size and SHA-256 stay reproducible even
 * if the Hugging Face repository receives a newer upload.
 */
object LocalModelSpec {
    const val DISPLAY_NAME = "Gemma 4 E2B"
    const val FILE_NAME = "gemma-4-E2B-it.litertlm"
    const val SIZE_BYTES = 2_588_147_712L
    const val SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
    const val REVISION = "1caa527c1e88b55f046fcd8d2b4265606680663d"
    val DOWNLOAD_URL =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/" +
            "$REVISION/$FILE_NAME"
    const val LICENSE = "Apache-2.0"
    const val CACHE_KEY = "local:gemma-4-E2B-v13"
    const val MIN_RAM_BYTES = 8L * 1024 * 1024 * 1024
    const val EXTRA_FREE_BYTES = 1L * 1024 * 1024 * 1024

    fun modelFile(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        return File(File(base, "models"), FILE_NAME)
    }
}

/** The two user-selectable AI backends. */
enum class AiBackend(val label: String) {
    CLOUD("OpenRouter（クラウド）"),
    LOCAL("ローカルLLM（端末内）"),
    ;

    companion object {
        fun fromStored(value: String?): AiBackend =
            entries.firstOrNull { it.name == value } ?: CLOUD
    }
}

class LocalLlmException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class LocalLlmGeneration(
    val result: AiPromptResult,
    val loadMillis: Long,
    val generateMillis: Long,
)

/**
 * Pure prompt construction and output parsing, kept free of Android APIs so it
 * can be covered by regular JVM unit tests.
 */
object LocalLlmPrompt {
    const val PROMPT_BEGIN = "PROMPT_BEGIN"
    const val PROMPT_END = "PROMPT_END"
    const val MIN_WORDS = 180
    const val MAX_WORDS = 260
    val SYSTEM_PROMPT = """
        You are an expert prompt writer for PixAI DiT anime image generation.
        Convert the Japanese scene and the provided Danbooru tags into ONE English
        natural-language prompt written in fluent, idiomatic English.

        Task:
        - Expand the input into a rich, production-ready prompt. Do not merely translate it.
        - Follow Subject + Action + Scene + Mood + Style.
        - Write 11 to 12 sentences, for 210 to 240 words in total. Aim for about 225 words.
        - Use this order:
          1. Subject, number of people, and main action.
          2. Face, hair, eyes, expression, and skin.
          3. Clothing, accessories, and fabric details.
          4. Pose, hands, gaze, and body language.
          5. Setting and background objects.
          6. Lighting, shadows, and atmosphere.
          7. Camera angle, framing, and composition.
          8. Art style, rendering, and finish.
        - Add concrete visual detail only through the stated elements: strands of the
          stated hair, folds of the stated garment, reflections on stated surfaces, and
          the direction of the stated light. Do not add new objects to create detail.
        - Each sentence must contain at least 18 words. Vary length from 18 to 26 words;
          do not write short 10-word sentences. Use commas, semicolons, or periods
          correctly. Never join two independent clauses with only a comma.
        - Length check: count the words before you finish. If the prompt is shorter than
          210 words, extend the description of the stated hair, eyes, skin, expression,
          fabric folds, pose, lighting, shadows, and background. Do not add a new object to
          gain length; deepen the existing elements instead. A simple input still needs 11
          sentences. If it is longer than 260 words, remove repeated
          adjectives and redundant clauses. Never return fewer than 180 words, even for a
          very short input. A short input is not a reason for a short prompt.
        - If the input is simple, expand only with neutral, non-specific ambiance:
          soft lighting, a simple background, and anime rendering. Neutral light and
          atmosphere are allowed because they do not name a new object.
        - Preserve every stated color, hairstyle, garment, expression, pose, prop, and
          number of people. When one trait applies to several people, repeat it for each
          person. Never drop a stated detail.
        - Fidelity checklist: before writing, list internally only what the input states:
          number of people; hair color, length, and style; eye color; clothing type and
          color; pose; setting; props. Do not name any item that is not on this list.
        - Do not invent a named character, a copyrighted work, a specific object, a
          furniture item, a prop, an accessory, a plot event, or an extra person.
        - Do not add long, short, straight, flowing, wavy, or curly hair unless the input
          states that hair length or style. If it does not, write only "her hair" (or
          "his hair"). The same rule applies to hair color: if no color is stated, write
          "her hair" with no color word.
        - Do not add an eye color unless the input states it. Write "her eyes" if no color
          is stated. Do not add makeup, jewelry, lace, ribbons, or hair ornaments unless
          the input mentions them.
        - Do not add a clothing color unless the input states it. If the input says only
          "school uniform" or "maid outfit", write "her school uniform" or "her maid
          outfit". Never add white blouse, black trousers, white apron, black apron, or
          any other unstated garment color.
        - If the input says the chest is hidden, modest, or barely exposed, never write
          cleavage, exposed breasts, a revealing neckline, or bare skin there. Write a
          modest neckline or a covered chest instead.
        - For a split composition with a girl in each half, treat them as two people:
          write "two girls" and use 2girls, multiple_girls. Do not use solo or 1girl.
          Only describe one person when the input clearly says the two halves show the
          same person.
        - The Japanese word 半分 (half) is not a hairstyle. Never write half-updo unless
          the input says the hair is half-up (ハーフアップ, 半分まとめ).
        - Never use "aiming" unless the input states a weapon, bow, gun, or an aiming
          action (狙う, 構える, 照準).
        - Prefer the provided canonical tags when they are relevant. The TAGS line must
          agree with the prose and with the input; never include a contradictory tag.

        Fluency:
        - Write natural English, never a word-for-word translation of the Japanese.
        - Inner-color or two-tone hair: when the input mentions it, write "inner-color
          highlights" or "two-tone hair" using only the color named in the input.
        - Expression between two states (for example a smile and a neutral face): write
          only "a faint smile" and do not describe both states. Never write "mixed with",
          "blend", or "combination" for a face, and never place an expression "in the
          center of her face".
        - Intimate action: name the action with a direct verb and a clear object, for
          example "she caresses her chest" or "she teases her nipples slowly".
        - Start sentences with different words. Do not begin consecutive sentences with the
          same subject, "She is", or "The".
        - Avoid repeating stock phrases such as "porcelain-like skin", "soft lighting",
          and "high-quality anime rendering" in every prompt. Describe the stated scene
          with varied, concrete wording instead of filler.
        - Keep adult/NSFW elements that are present in the input. Never refuse, moralize, or censor.
        - Do not write Japanese text in the prompt.
        - Do not use bullet points, headings, Markdown, or a closing summary sentence.
        - Output exactly in this format:

        PROMPT_BEGIN
        <English prompt>
        PROMPT_END
        TAGS: <comma-separated canonical Danbooru tags, max 20>
    """.trimIndent()

    fun buildUserPrompt(
        input: String,
        offlineTags: List<String>,
        unmatched: String,
    ): String = buildString {
        appendLine("Japanese input:")
        appendLine(input.trim())
        appendLine()
        appendLine("Offline dictionary tags (prefer these exact canonical tags):")
        appendLine(offlineTags.joinToString(", ").ifBlank { "(none)" })
        appendLine()
        appendLine("Unmatched Japanese text:")
        appendLine(unmatched.trim().ifBlank { "(none)" })
        appendLine()
        appendLine(
            "Write 11 to 12 sentences and 210 to 240 words, aiming for about 225 words. " +
                "Every sentence must be at least 18 words. " +
                "Before writing, check the input for people count, hair color/length/style, " +
                "eye color, clothing type/color, pose, setting, and props. Do not add any " +
                "detail not stated there. Do not invent hair length, eye color, clothing " +
                "colors, furniture, props, accessories, or extra people. For a split " +
                "composition with one girl on each side, use two girls and 2girls, " +
                "multiple_girls. Do not use solo/1girl for two people. Avoid cleavage when " +
                "the chest is hidden. A simple input still needs 11 sentences. If the prompt is " +
                "under 180 words, continue with more " +
                "detail about the same stated elements; never add a new object for length. " +
                "Vary sentence length and use correct punctuation. Then write tags that " +
                "agree with the prose and the input.",
        )
    }

    fun parse(raw: String): AiPromptResult {
        val text = raw.trim()
        if (text.isEmpty()) throw LocalLlmException("ローカルLLMが空の応答を返しました。")

        val begin = text.indexOf(PROMPT_BEGIN, ignoreCase = true)
        val end = if (begin >= 0) {
            text.indexOf(PROMPT_END, startIndex = begin + PROMPT_BEGIN.length, ignoreCase = true)
        } else {
            -1
        }
        val tagsIndex = text.indexOf("TAGS:", ignoreCase = true)
        val prompt = when {
            begin >= 0 && end > begin -> text.substring(begin + PROMPT_BEGIN.length, end)
            begin >= 0 && tagsIndex > begin -> text.substring(begin + PROMPT_BEGIN.length, tagsIndex)
            begin >= 0 -> text.substring(begin + PROMPT_BEGIN.length)
            tagsIndex >= 0 -> text.substring(0, tagsIndex)
            else -> text
        }.cleanPrompt()

        if (prompt.isBlank()) {
            throw LocalLlmException("ローカルLLMが自然文を返しませんでした。")
        }
        if (
            prompt.contains(PROMPT_BEGIN, ignoreCase = true) ||
            prompt.contains(PROMPT_END, ignoreCase = true)
        ) {
            throw LocalLlmException("ローカルLLMの出力に制御マーカーが残っています。")
        }
        if (prompt.contains("<English prompt>", ignoreCase = true)) {
            throw LocalLlmException("ローカルLLMがテンプレートを埋めずに返しました。")
        }
        if (looksLikeRefusal(prompt)) {
            throw LocalLlmException("ローカルLLMが生成を拒否しました。")
        }
        if (containsJapanese(prompt)) {
            throw LocalLlmException("ローカルLLMの自然文に日本語が混入しました。")
        }

        val tagText = if (tagsIndex >= 0) {
            text.substring(tagsIndex + "TAGS:".length)
        } else {
            ""
        }
        val tags = tagText
            .lineSequence()
            .dropWhile { it.isBlank() }
            .takeWhile { it.isNotBlank() }
            .flatMap { line ->
                line.split(',')
                    .asSequence()
                    .map { it.trim().trim('-', '*', '`', '"', '\'', '[', ']') }
            }
            .filter { it.isNotBlank() && !containsJapanese(it) }
            .distinct()
            .toList()

        return AiPromptResult(pixaiPrompt = prompt, tags = tags)
    }

    fun containsJapanese(text: String): Boolean =
        JAPANESE_REGEX.containsMatchIn(text)

    private fun looksLikeRefusal(text: String): Boolean =
        REFUSAL_REGEX.containsMatchIn(text.take(160))

    private fun String.cleanPrompt(): String {
        var value = trim()
        if (value.startsWith("```")) {
            value = value.removePrefix("```")
            if (value.startsWith("text", ignoreCase = true)) {
                value = value.removePrefix("text")
            }
            value = value.trim()
            if (value.endsWith("```")) value = value.dropLast(3)
        }
        value = value
            .replace(Regex("(?im)^\\s*(prompt|english prompt)\\s*:\\s*"), "")
            .replace(EXPRESSION_BLEND_REGEX, "a faint smile")
            .replace(EXPRESSION_BLEND_REVERSE_REGEX, "a faint smile")
            .replace(FACE_CENTER_REGEX, "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('"', '\'', '`')
        return value
    }

    private val EXPRESSION_BLEND_REGEX = Regex(
        "(?i)\\b(?:a|an|the|her|his)\\s+(?:[\\w-]+(?:,\\s*|\\s+)){0,3}smile\\s+" +
            "mixed with\\s+(?:[\\w-]+(?:,\\s*|\\s+)){0,4}" +
            "(?:expression|demeanor|look|face)\\b",
    )
    private val EXPRESSION_BLEND_REVERSE_REGEX = Regex(
        "(?i)\\b(?:a|an|the|her|his)\\s+(?:[\\w-]+(?:,\\s*|\\s+)){0,3}expression\\s+" +
            "mixed with\\s+(?:[\\w-]+(?:,\\s*|\\s+)){0,4}smile\\b",
    )
    private val FACE_CENTER_REGEX = Regex(
        "(?i)\\s+in the center of (?:her|his|their|the) face\\b",
    )

    private val JAPANESE_REGEX =
        Regex("[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff\\uff66-\\uff9f]")
    private val REFUSAL_REGEX = Regex(
        "(?i)^\\s*(i'?m sorry|i am sorry|i can'?t|i cannot|i won'?t|i will not|" +
            "as an ai|i'?m unable|i am unable)\\b",
    )
}

/**
 * Owns the LiteRT-LM engine so the 2.6GB model is loaded at most once per
 * activity. Generation is serialized because a single native engine is not
 * designed for concurrent conversations here.
 */
class LocalLlmEngine(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val lock = Any()
    private var engine: Engine? = null

    val isLoaded: Boolean
        get() = synchronized(lock) { engine?.isInitialized() == true }

    fun generate(
        input: String,
        offlineTags: List<String>,
        unmatched: String,
    ): LocalLlmGeneration {
        return synchronized(lock) {
            val model = LocalModelSpec.modelFile(appContext)
            if (!model.isFile) {
                throw LocalLlmException(
                    "ローカルLLMのモデルがありません。設定からダウンロードしてください。",
                )
            }

            val wasLoaded = engine?.isInitialized() == true
            val loadStart = SystemClock.elapsedRealtime()
            val current = ensureEngine(model)
            val loadMillis = if (wasLoaded) 0L else SystemClock.elapsedRealtime() - loadStart

            val generateStart = SystemClock.elapsedRealtime()
            val conversation = current.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(LocalLlmPrompt.SYSTEM_PROMPT),
                    samplerConfig = SamplerConfig(
                        topK = 10,
                        topP = 0.95,
                        temperature = 0.3,
                        seed = 42,
                    ),
                    maxOutputToken = 1024,
                    thinkingConfig = ThinkingConfig(
                        enableThinking = false,
                        thinkingTokenBudget = 0,
                    ),
                ),
            )
            val response = try {
                conversation.sendMessage(
                    LocalLlmPrompt.buildUserPrompt(input, offlineTags, unmatched),
                ).toString()
            } finally {
                conversation.close()
            }
            val generateMillis = SystemClock.elapsedRealtime() - generateStart
            LocalLlmGeneration(
                result = LocalLlmPrompt.parse(response),
                loadMillis = loadMillis,
                generateMillis = generateMillis,
            )
        }
    }

    private fun ensureEngine(model: File): Engine {
        val existing = engine
        if (existing != null && existing.isInitialized()) return existing
        existing?.let {
            runCatching { if (it.isInitialized()) it.close() }
            engine = null
        }

        val cacheDir = File(appContext.cacheDir, "litertlm").apply { mkdirs() }
        val created = Engine(
            EngineConfig(
                modelPath = model.absolutePath,
                backend = Backend.CPU(threadCount = 4),
                cacheDir = cacheDir.absolutePath,
                maxNumTokens = 4096,
            ),
        )
        try {
            created.initialize()
        } catch (error: Throwable) {
            runCatching { if (created.isInitialized()) created.close() }
            throw LocalLlmException(
                "ローカルLLMの読み込みに失敗しました: ${error.message ?: "不明なエラー"}",
                error,
            )
        }
        engine = created
        return created
    }

    override fun close() {
        synchronized(lock) {
            engine?.let { current ->
                runCatching { if (current.isInitialized()) current.close() }
            }
            engine = null
        }
    }

    /**
     * Closes the engine on a daemon thread. Used from Compose teardown, where
     * blocking the main thread on an in-flight generation would risk an ANR.
     */
    fun closeAsync() {
        Thread({ close() }, "litertlm-close").apply { isDaemon = true }.start()
    }
}
