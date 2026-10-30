package com.prompter.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrompterTheme {
                Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
                    PrompterApp()
                }
            }
        }
    }
}

@Composable
private fun PrompterApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var input by rememberSaveable { mutableStateOf("") }
    var characterMode by rememberSaveable { mutableStateOf(false) }
    var characterName by rememberSaveable { mutableStateOf("") }
    var characterSeries by rememberSaveable { mutableStateOf("") }
    var characterEnglish by rememberSaveable { mutableStateOf("") }
    var characterDbResults by remember { mutableStateOf<List<CharacterEntry>>(emptyList()) }
    var characterVariant by remember { mutableStateOf(false) }
    var dictionaryVersion by remember { mutableStateOf(0L) }
    var dictionaryAliasCount by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf<ConversionResult?>(null) }
    var editableTags by remember { mutableStateOf<List<EditableTag>>(emptyList()) }
    var engine by remember { mutableStateOf<PromptEngine?>(null) }
    var historyStore by remember { mutableStateOf<HistoryStore?>(null) }
    var historyItems by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var currentHistoryId by remember { mutableStateOf<Long?>(null) }
    var aiSettings by remember { mutableStateOf<AiSettings?>(null) }
    var aiCache by remember { mutableStateOf<AiCache?>(null) }
    var apiKeyConfigured by remember { mutableStateOf(false) }
    var aiModel by remember { mutableStateOf(AiDefaults.MODEL) }
    var aiBaseUrl by remember { mutableStateOf(AiDefaults.BASE_URL) }
    var aiBackend by remember { mutableStateOf(AiBackend.CLOUD) }
    var localModelStatus by remember {
        mutableStateOf(LocalModelStatus(LocalModelState.NOT_DOWNLOADED))
    }
    var localEngineLoaded by remember { mutableStateOf(false) }
    val localModelManager = remember { LocalModelManager(context.applicationContext) }
    val localLlm = remember { LocalLlmEngine(context.applicationContext) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var aiLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiNotice by remember { mutableStateOf<String?>(null) }
    var naturalPromptOverride by remember { mutableStateOf<String?>(null) }
    var translator by remember { mutableStateOf<TextTranslator?>(null) }
    var translationReady by remember { mutableStateOf(false) }
    var translationDownloading by remember { mutableStateOf(false) }
    var translationError by remember { mutableStateOf<String?>(null) }
    var translatedUnmatched by remember { mutableStateOf<String?>(null) }
    var translationNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(characterMode, characterName, characterSeries, characterEnglish) {
        characterDbResults = emptyList()
        if (characterMode && characterName.isNotBlank() && characterEnglish.isBlank()) {
            delay(120)
            characterDbResults = withContext(Dispatchers.IO) {
                CharacterDb.search(context, characterName, series = characterSeries)
            }
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        try {
            engine = withContext(Dispatchers.IO) { PromptEngine(context) }
            val dictionary = withContext(Dispatchers.IO) { DictionaryUpdates.snapshot(context) }
            dictionaryVersion = dictionary.version
            dictionaryAliasCount = dictionary.aliasCount
            historyStore = withContext(Dispatchers.IO) { HistoryStore(context) }
            val loadedAiSettings = withContext(Dispatchers.IO) { AiSettings(context) }
            aiSettings = loadedAiSettings
            withContext(Dispatchers.IO) { loadedAiSettings.migrateLegacyDefaultModel() }
            aiCache = withContext(Dispatchers.IO) { AiCache(context) }
            apiKeyConfigured = withContext(Dispatchers.IO) { loadedAiSettings.hasApiKey() }
            aiModel = loadedAiSettings.model
            aiBaseUrl = loadedAiSettings.baseUrl
            aiBackend = loadedAiSettings.backend
            localModelStatus = withContext(Dispatchers.IO) { localModelManager.status() }
            val loadedTranslator = withContext(Dispatchers.IO) {
                runCatching { MlKitTextTranslator() }.getOrNull()
            }
            translator = loadedTranslator
            translationReady = if (loadedTranslator == null) {
                false
            } else {
                withContext(Dispatchers.IO) {
                    runCatching { loadedTranslator.isReady() }.getOrDefault(false)
                }
            }
            if (loadedTranslator == null) {
                translationError = "ML Kit翻訳を初期化できませんでした。辞書のみで動作します。"
            }
        } catch (t: Throwable) {
            error = "初期化に失敗しました: ${t.message}"
        } finally {
            loading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // The native engine may still be generating. Close it off the main
            // thread so activity teardown cannot trigger an ANR.
            localLlm.closeAsync()
            translator?.close()
            engine?.close()
            historyStore?.close()
        }
    }

    val isDownloading = localModelStatus.state == LocalModelState.DOWNLOADING
    LaunchedEffect(isDownloading) {
        if (!isDownloading) return@LaunchedEffect
        while (true) {
            delay(1000)
            val next = withContext(Dispatchers.IO) { localModelManager.status() }
            localModelStatus = next
            if (next.state != LocalModelState.DOWNLOADING) break
        }
    }

    // Keep verification in an effect whose key never changes. If the effect
    // were keyed on the state itself, setting VERIFYING would cancel the
    // in-flight hash and could leave the UI stuck at VERIFYING forever.
    LaunchedEffect(Unit) {
        snapshotFlow { localModelStatus.state }
            .collect { state ->
                if (state == LocalModelState.NEEDS_VERIFICATION) {
                    localModelStatus = localModelStatus.copy(state = LocalModelState.VERIFYING)
                    localModelStatus = withContext(Dispatchers.IO) {
                        localModelManager.verify()
                    }
                }
            }
    }

    LaunchedEffect(showSettings) {
        if (showSettings && localModelStatus.state != LocalModelState.VERIFYING) {
            val refreshed = withContext(Dispatchers.IO) { localModelManager.status() }
            // Verification may have started while status() was running.
            if (localModelStatus.state != LocalModelState.VERIFYING) {
                localModelStatus = refreshed
            }
        }
    }

    LaunchedEffect(showHistory, onlyFavorites, historyStore) {
        if (showHistory) {
            val store = historyStore ?: return@LaunchedEffect
            historyItems = withContext(Dispatchers.IO) { store.recent(onlyFavorites) }
        }
    }

    val editedPixaiPrompt = remember(editableTags, naturalPromptOverride, translatedUnmatched) {
        naturalPromptOverride ?: PromptComposer.compose(
            tags = editableTags.map { it.tag },
            extraContext = translatedUnmatched,
        )
    }
    val savedTagText = result?.tagText.orEmpty()

    LaunchedEffect(
        editableTags,
        currentHistoryId,
        historyStore,
        editedPixaiPrompt,
        savedTagText,
    ) {
        val id = currentHistoryId ?: return@LaunchedEffect
        val store = historyStore ?: return@LaunchedEffect
        if (editableTags.isEmpty()) return@LaunchedEffect
        delay(300)
        withContext(Dispatchers.IO) {
            store.update(id, editedPixaiPrompt, savedTagText)
        }
    }

    fun refreshHistory() {
        val store = historyStore ?: return
        scope.launch {
            historyItems = withContext(Dispatchers.IO) { store.recent(onlyFavorites) }
        }
    }

    fun convert() {
        val current = engine ?: return
        if (input.isBlank()) return
        if (characterMode && characterEnglish.isBlank()) {
            error = "キャラクターの英語名を確認して入力してください。"
            return
        }
        val confirmedName = if (characterMode) characterEnglish.trim() else ""
        val savedCharacterName = if (characterMode) characterName else ""
        val savedCharacterSeries = if (characterMode) characterSeries else ""
        scope.launch {
            loading = true
            error = null
            aiError = null
            aiNotice = null
            translatedUnmatched = null
            translationNotice = null
            try {
                var converted = withContext(Dispatchers.Default) { current.convert(input) }
                val currentTranslator = translator
                if (translationReady && currentTranslator != null &&
                    converted.unmatched.isNotBlank()
                ) {
                    try {
                        val english = withContext(Dispatchers.IO) {
                            currentTranslator.translateJapaneseToEnglish(converted.unmatched)
                        }
                        if (english.isNotBlank()) {
                            val normalized = withContext(Dispatchers.Default) {
                                current.normalizeEnglishTags(english)
                            }
                            val addedCount = normalized.count { it !in converted.tags }
                            converted = current.applyTranslation(converted, english, normalized)
                            translatedUnmatched = english
                            translationNotice = if (addedCount == 0) {
                                "辞書外をML Kitで英訳しました"
                            } else {
                                "辞書外をML Kitで英訳し、タグ${addedCount}件を追加しました"
                            }
                        }
                    } catch (t: Throwable) {
                        translationNotice = "ML Kit翻訳に失敗しました: ${t.message ?: "不明なエラー"}"
                    }
                }
                if (confirmedName.isNotBlank()) {
                    converted = converted.copy(
                        pixaiPrompt = CharacterIdentity.prefix(converted.pixaiPrompt, confirmedName, savedCharacterSeries),
                    )
                }
                naturalPromptOverride = if (confirmedName.isNotBlank()) converted.pixaiPrompt else null
                result = converted
                editableTags = converted.tags.map { EditableTag(it) }
                currentHistoryId = null
                val store = historyStore
                if (store != null && converted.pixaiPrompt.isNotBlank()) {
                    currentHistoryId = withContext(Dispatchers.IO) {
                        store.add(
                            input = input,
                            pixaiPrompt = converted.pixaiPrompt,
                            tagText = converted.tagText,
                            unmatched = converted.unmatched,
                            characterName = savedCharacterName,
                            characterSeries = savedCharacterSeries,
                            characterEnglish = confirmedName,
                        )
                    }
                }
            } catch (t: Throwable) {
                error = "変換に失敗しました: ${t.message}"
            } finally {
                loading = false
            }
        }
    }

    fun loadHistory(item: HistoryItem) {
        val tags = parseTagText(item.tagText)
        input = item.input
        characterName = item.characterName
        characterSeries = item.characterSeries
        characterEnglish = item.characterEnglish
        characterMode = item.characterEnglish.isNotBlank()
        characterDbResults = emptyList()
        characterVariant = false
        result = ConversionResult(
            pixaiPrompt = item.pixaiPrompt,
            tagText = item.tagText,
            tags = tags.map { it.tag },
            unmatched = item.unmatched,
        )
        editableTags = tags
        currentHistoryId = item.id
        naturalPromptOverride = item.pixaiPrompt
        translatedUnmatched = null
        translationNotice = null
        aiError = null
        aiNotice = null
        showHistory = false
    }

    fun toggleFavorite(item: HistoryItem) {
        val store = historyStore ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                store.setFavorite(item.id, !item.isFavorite)
            }
            refreshHistory()
        }
    }

    fun deleteHistory(item: HistoryItem) {
        val store = historyStore ?: return
        scope.launch {
            withContext(Dispatchers.IO) { store.delete(item.id) }
            if (currentHistoryId == item.id) currentHistoryId = null
            refreshHistory()
        }
    }

    fun clearHistory() {
        val store = historyStore ?: return
        scope.launch {
            withContext(Dispatchers.IO) { store.clear() }
            currentHistoryId = null
            historyItems = emptyList()
        }
    }

    fun refreshAiSettings() {
        val settings = aiSettings ?: return
        scope.launch {
            apiKeyConfigured = withContext(Dispatchers.IO) { settings.hasApiKey() }
            aiModel = settings.model
            aiBaseUrl = settings.baseUrl
            aiBackend = settings.backend
        }
    }

    suspend fun testOpenRouterConnection(apiKey: String, baseUrl: String): String {
        val settings = aiSettings ?: throw OpenRouterException("AI設定を読み込めませんでした。")
        val key = apiKey.trim().ifBlank { settings.getApiKey().orEmpty().trim() }
        if (key.isBlank()) throw OpenRouterException("APIキーが入力されていません。")
        return withContext(Dispatchers.IO) {
            OpenRouterClient(baseUrl).testKey(key).summary()
        }
    }

    fun saveAiSettings(
        apiKey: String,
        model: String,
        baseUrl: String,
        backend: AiBackend,
    ) {
        val settings = aiSettings ?: return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (apiKey.isNotBlank()) settings.setApiKey(apiKey)
                    settings.model = model
                    settings.baseUrl = baseUrl
                    settings.backend = backend
                }
                refreshAiSettings()
                aiError = null
                showSettings = false
            } catch (t: Throwable) {
                aiError = "AI設定の保存に失敗しました: ${t.message}"
            }
        }
    }

    fun deleteAiKey() {
        val settings = aiSettings ?: return
        scope.launch {
            withContext(Dispatchers.IO) { settings.clearApiKey() }
            refreshAiSettings()
            aiError = null
            aiNotice = null
            showSettings = false
        }
    }

    fun downloadLocalModel() {
        if (
            localModelStatus.state == LocalModelState.DOWNLOADING ||
            localModelStatus.state == LocalModelState.VERIFYING ||
            localModelStatus.state == LocalModelState.READY
        ) {
            return
        }
        // Move to DOWNLOADING immediately so a double tap cannot enqueue two
        // DownloadManager requests to the same destination.
        localModelStatus = LocalModelStatus(LocalModelState.DOWNLOADING)
        scope.launch {
            localModelStatus = withContext(Dispatchers.IO) { localModelManager.startDownload() }
        }
    }

    fun cancelLocalModelDownload() {
        scope.launch {
            localModelStatus = withContext(Dispatchers.IO) { localModelManager.cancelDownload() }
        }
    }

    fun verifyLocalModel() {
        if (
            localModelStatus.state == LocalModelState.VERIFYING ||
            localModelStatus.state == LocalModelState.DOWNLOADING
        ) {
            return
        }
        localModelStatus = localModelStatus.copy(state = LocalModelState.VERIFYING)
        scope.launch {
            localModelStatus = withContext(Dispatchers.IO) { localModelManager.verify() }
        }
    }

    fun deleteLocalModel() {
        localEngineLoaded = false
        scope.launch {
            val status = withContext(Dispatchers.IO) {
                // close() waits for any in-flight generation; never block the
                // main thread while the native engine is still busy.
                localLlm.close()
                localModelManager.deleteModel()
            }
            localModelStatus = status
        }
    }

    fun unloadLocalEngine() {
        localEngineLoaded = false
        scope.launch {
            withContext(Dispatchers.IO) { localLlm.close() }
            aiNotice = "ローカルLLMのエンジンを解放しました"
        }
    }

    fun downloadTranslationModels() {
        val current = translator
        if (current == null) {
            translationError = "ML Kit翻訳を初期化できませんでした。辞書のみで動作します。"
            return
        }
        scope.launch {
            translationDownloading = true
            translationError = null
            try {
                withContext(Dispatchers.IO) { current.downloadModels() }
                translationReady = withContext(Dispatchers.IO) {
                    runCatching { current.isReady() }.getOrDefault(false)
                }
                if (!translationReady) {
                    translationError = "翻訳モデルの準備を確認できませんでした。再試行してください。"
                }
            } catch (t: Throwable) {
                translationError = "翻訳モデルのダウンロードに失敗しました: " +
                    (t.message ?: "不明なエラー")
            } finally {
                translationDownloading = false
            }
        }
    }

    fun refineWithAi() {
        val current = result ?: return
        val currentEngine = engine ?: return
        val settings = aiSettings ?: return
        val backend = aiBackend
        if (backend == AiBackend.CLOUD && !apiKeyConfigured) {
            showSettings = true
            return
        }
        if (backend == AiBackend.LOCAL && !localModelStatus.isReady) {
            showSettings = true
            return
        }

        val sourceInput = input
        val confirmedName = if (characterMode) characterEnglish.trim() else ""
        val confirmedSeries = if (characterMode) characterSeries.trim() else ""
        if (characterMode && confirmedName.isEmpty()) {
            aiError = "キャラクターの英語名を確認して入力してください。"
            return
        }
        val identityDetails = listOf(confirmedName, confirmedSeries)
            .filter(String::isNotBlank)
            .joinToString(" from ")
        val generationInput = if (confirmedName.isEmpty()) sourceInput else
            "$sourceInput\nCharacter identity: $identityDetails. Use this exact identity; do not say resembling or like."
        val offlineTags = editableTags.map { it.tag }
        val historyId = currentHistoryId
        scope.launch {
            aiLoading = true
            aiError = null
            aiNotice = null
            try {
                val cloud = if (backend == AiBackend.CLOUD) {
                    withContext(Dispatchers.IO) {
                        Triple(settings.getApiKey(), settings.model, settings.baseUrl)
                    }
                } else {
                    null
                }
                val cache = aiCache
                val cacheBase = if (backend == AiBackend.LOCAL) {
                    "local://litertlm"
                } else {
                    checkNotNull(cloud).third
                }
                val cacheModel = if (backend == AiBackend.LOCAL) {
                    LocalModelSpec.CACHE_KEY
                } else {
                    checkNotNull(cloud).second
                }
                val cacheKey = cache?.key(
                    baseUrl = cacheBase,
                    model = cacheModel,
                    input = generationInput,
                    offlineTags = offlineTags,
                    unmatched = current.unmatched,
                )
                val cached = if (cache != null && cacheKey != null) {
                    withContext(Dispatchers.IO) { cache.get(cacheKey) }
                } else {
                    null
                }
                val fromCache = cached != null
                var loadMillis = 0L
                var generateMillis = 0L
                val aiResult = cached ?: if (backend == AiBackend.LOCAL) {
                    val generation = withContext(Dispatchers.IO) {
                        localLlm.generate(generationInput, offlineTags, current.unmatched)
                    }
                    localEngineLoaded = localLlm.isLoaded
                    loadMillis = generation.loadMillis
                    generateMillis = generation.generateMillis
                    generation.result
                } else {
                    val cloudSettings = checkNotNull(cloud)
                    val apiKey = cloudSettings.first
                    if (apiKey.isNullOrBlank()) {
                        throw OpenRouterException(
                            "APIキーを読み込めませんでした。設定で再登録してください。",
                        )
                    }
                    withContext(Dispatchers.IO) {
                        OpenRouterClient(cloudSettings.third).refine(
                            apiKey = apiKey,
                            model = cloudSettings.second,
                            input = generationInput,
                            offlineTags = offlineTags,
                            unmatched = current.unmatched,
                        )
                    }
                }
                if (!fromCache && cache != null && cacheKey != null) {
                    withContext(Dispatchers.IO) { cache.put(cacheKey, aiResult) }
                }

                val local = backend == AiBackend.LOCAL
                val minWords = if (local) {
                    LocalLlmPrompt.MIN_WORDS
                } else {
                    0
                }
                val maxWords: Int? = if (local) {
                    LocalLlmPrompt.MAX_WORDS
                } else {
                    null
                }
                val guardedTags = FidelityGuard.correctTags(
                    input = sourceInput,
                    tags = aiResult.tags,
                    isKnownTag = currentEngine::isKnownTag,
                    statedTags = offlineTags,
                ).tags
                val guardedPrompt = FidelityGuard.sanitizePrompt(
                    input = sourceInput,
                    prompt = aiResult.pixaiPrompt,
                    offlineTags = offlineTags,
                )
                val validation = AiPromptValidator.validate(
                    result = aiResult.copy(
                        pixaiPrompt = CharacterIdentity.prefix(guardedPrompt, confirmedName, confirmedSeries),
                        tags = guardedTags,
                    ),
                    minWords = minWords,
                    maxWords = maxWords,
                ) { tag ->
                    currentEngine.isKnownTag(tag)
                }
                if (validation.prompt.isBlank()) {
                    if (backend == AiBackend.LOCAL) {
                        throw LocalLlmException("ローカルLLMが空の自然文を返しました。")
                    }
                    throw OpenRouterException("AIが空の自然文を返しました。")
                }

                val tags = validation.tags.ifEmpty { offlineTags }
                val editable = tags.map { EditableTag(it) }
                val tagText = formatTagText(editable)
                result = ConversionResult(
                    pixaiPrompt = validation.prompt,
                    tagText = tagText,
                    tags = tags,
                    unmatched = current.unmatched,
                )
                editableTags = editable
                naturalPromptOverride = validation.prompt
                translatedUnmatched = null
                translationNotice = null

                aiNotice = buildString {
                    append(
                        when {
                            fromCache -> "キャッシュから反映"
                            backend == AiBackend.LOCAL -> "端末内AIで更新"
                            else -> "AIで更新"
                        },
                    )
                    append(" / ${validation.wordCount}語")
                    if (!validation.wordCountOk) {
                        val target = if (maxWords == null) {
                            "${minWords}語以上"
                        } else {
                            "${minWords}〜${maxWords}語"
                        }
                        append("（目標${target}）")
                    }
                    if (backend == AiBackend.LOCAL && !fromCache) {
                        append(" / 生成${formatDuration(generateMillis)}")
                        if (loadMillis > 0L) {
                            append("（読み込み${formatDuration(loadMillis)}）")
                        }
                    }
                    if (validation.removedTags.isNotEmpty()) {
                        append(" / 辞書外タグ${validation.removedTags.size}件を除去")
                    }
                    if (validation.tags.isEmpty()) {
                        append(
                            if (offlineTags.isEmpty()) {
                                " / 有効タグなし"
                            } else {
                                " / 有効タグがないためオフラインタグを使用"
                            },
                        )
                    }
                }

                val store = historyStore
                if (store != null) {
                    if (historyId != null) {
                        withContext(Dispatchers.IO) {
                            store.update(historyId, validation.prompt, tagText)
                        }
                    } else {
                        currentHistoryId = withContext(Dispatchers.IO) {
                            store.add(
                                input = sourceInput,
                                pixaiPrompt = validation.prompt,
                                tagText = tagText,
                                unmatched = current.unmatched,
                                characterName = if (confirmedName.isNotEmpty()) characterName else "",
                                characterSeries = if (confirmedName.isNotEmpty()) characterSeries else "",
                                characterEnglish = confirmedName,
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                aiError = if (backend == AiBackend.LOCAL) {
                    "端末内AIで整えられませんでした: ${t.message ?: "不明なエラー"}"
                } else {
                    t.message ?: "AI補完に失敗しました。"
                }
            } finally {
                localEngineLoaded = localLlm.isLoaded
                aiLoading = false
            }
        }
    }

    if (showHistory) {
        HistoryScreen(
            items = historyItems,
            onlyFavorites = onlyFavorites,
            onFilter = { onlyFavorites = it },
            onBack = { showHistory = false },
            onLoad = ::loadHistory,
            onToggleFavorite = ::toggleFavorite,
            onDelete = ::deleteHistory,
            onClear = ::clearHistory,
        )
        return
    }

    if (showSettings) {
        SettingsScreen(
            hasApiKey = apiKeyConfigured,
            initialModel = aiModel,
            initialBaseUrl = aiBaseUrl,
            initialBackend = aiBackend,
            translationReady = translationReady,
            translationDownloading = translationDownloading,
            translationError = translationError,
            localModelStatus = localModelStatus,
            localEngineLoaded = localEngineLoaded,
            initialDictionaryVersion = dictionaryVersion,
            initialDictionaryAliasCount = dictionaryAliasCount,
            onCheckDictionary = {
                val outcome = withContext(Dispatchers.IO) {
                    try {
                        val current = DictionaryUpdates.snapshot(context)
                        val release = DictionaryUpdateClient.latestRelease()
                        if (release.version <= current.version) {
                            DictionaryUpdateOutcome(
                                ok = true,
                                message = "最新です（v${current.version}）",
                                version = current.version,
                                aliasCount = current.aliasCount,
                            )
                        } else {
                            val bytes = DictionaryUpdateClient.download(release)
                            val verified = DictionaryUpdateVerifier.verify(bytes, current.version)
                            val installed = DictionaryUpdates.install(context, verified, release.tag)
                            DictionaryUpdateOutcome(
                                ok = true,
                                message = "v${installed.version} に更新しました",
                                version = installed.version,
                                aliasCount = installed.aliasCount,
                            )
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        DictionaryUpdateOutcome(
                            ok = false,
                            message = failure.message ?: "更新できませんでした。",
                        )
                    }
                }
                if (outcome.ok && outcome.version > dictionaryVersion) {
                    withContext(Dispatchers.IO) { engine?.close() }
                    engine = withContext(Dispatchers.IO) { PromptEngine(context) }
                    dictionaryVersion = outcome.version
                    dictionaryAliasCount = outcome.aliasCount
                }
                outcome
            },
            onBack = { showSettings = false },
            onSave = ::saveAiSettings,
            onDeleteKey = ::deleteAiKey,
            onTestConnection = ::testOpenRouterConnection,
            onDownloadTranslation = ::downloadTranslationModels,
            onDownloadLocalModel = ::downloadLocalModel,
            onCancelLocalModelDownload = ::cancelLocalModelDownload,
            onVerifyLocalModel = ::verifyLocalModel,
            onDeleteLocalModel = ::deleteLocalModel,
            onUnloadLocalEngine = ::unloadLocalEngine,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ScreenHeader(
            title = "Prompter",
            actions = {
                QuietAction("履歴", onClick = { showHistory = true })
                QuietAction("設定", onClick = { showSettings = true })
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 4.dp,
                bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                Column {
                    Text("イメージを、", style = MaterialTheme.typography.displaySmall)
                    Text("プロンプトに。", style = MaterialTheme.typography.displaySmall)
                }
            }

            item {
                CharacterPanel(
                    active = characterMode,
                    onActiveChange = { enabled ->
                        characterMode = enabled
                        if (!enabled) {
                            characterName = ""
                            characterSeries = ""
                            characterEnglish = ""
                            characterVariant = false
                            characterDbResults = emptyList()
                        }
                    },
                    name = characterName,
                    series = characterSeries,
                    english = characterEnglish,
                    variant = characterVariant,
                    dbResults = characterDbResults,
                    onNameChange = {
                        characterName = it
                        characterEnglish = ""
                        characterVariant = false
                    },
                    onSeriesChange = {
                        characterSeries = it
                        characterEnglish = ""
                        characterVariant = false
                    },
                    onSelect = { entry ->
                        characterName = entry.nameJa
                        characterSeries = entry.series
                        characterEnglish = entry.nameEn
                        characterVariant = entry.category == "variant"
                    },
                    onClearSelection = {
                        characterEnglish = ""
                        characterVariant = false
                    },
                    onEnglishChange = {
                        characterEnglish = it
                        if (it.isBlank()) characterVariant = false
                    },
                )
            }

            item {
                Column {
                    EditorialLabel("情景")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "長い黒髪の少女が、青いドレスで椅子に座っている",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        minLines = 5,
                        maxLines = 10,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Ink,
                            unfocusedBorderColor = Hairline,
                            focusedContainerColor = CardCream,
                            unfocusedContainerColor = CardCream,
                            cursorColor = Terracotta,
                            focusedPlaceholderColor = MutedInk.copy(alpha = 0.6f),
                            unfocusedPlaceholderColor = MutedInk.copy(alpha = 0.6f),
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    ExampleRow(
                        onPick = {
                            input = it
                            result = null
                            editableTags = emptyList()
                            currentHistoryId = null
                            naturalPromptOverride = null
                            translatedUnmatched = null
                            translationNotice = null
                            aiError = null
                            aiNotice = null
                            error = null
                        },
                    )
                }
            }

            item {
                PrimaryAction(
                    text = if (loading) "作成中…" else "プロンプトを作成",
                    onClick = ::convert,
                    enabled = input.isNotBlank() && engine != null && !aiLoading,
                    loading = loading,
                )
            }

            error?.let { message ->
                item {
                    Text(
                        text = message,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            result?.let { res ->
                val hasAiPrompt = !naturalPromptOverride.isNullOrBlank()
                val hasTranslatedContext = !translatedUnmatched.isNullOrBlank()
                val hasContent = editedPixaiPrompt.isNotBlank() || hasAiPrompt || hasTranslatedContext

                if (!hasContent) {
                    item {
                        EmptyResult(unmatched = res.unmatched)
                    }
                } else {
                    item {
                        NaturalPromptBlock(
                            text = editedPixaiPrompt,
                            onCopy = { copyText(context, "プロンプト", editedPixaiPrompt) },
                        )
                    }
                }

                item {
                    AiRefineBlock(
                        needsSetup = (aiBackend == AiBackend.CLOUD && !apiKeyConfigured) ||
                            (aiBackend == AiBackend.LOCAL && !localModelStatus.isReady),
                        loading = aiLoading,
                        error = aiError,
                        notice = aiNotice,
                        onRefine = ::refineWithAi,
                    )
                }

                if (res.unmatched.isNotBlank() && !hasAiPrompt) {
                    item {
                        UnmatchedBlock(
                            unmatched = res.unmatched,
                            translated = translatedUnmatched,
                            notice = translationNotice,
                            translationReady = translationReady,
                            translationDownloading = translationDownloading,
                            translationError = translationError,
                            onOpenSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String =
    String.format(Locale.US, "%.1f秒", millis / 1000.0)

@Composable
private fun ExampleRow(onPick: (String) -> Unit) {
    val examples = listOf(
        "黒髪の少女" to "長い黒髪の少女が青いドレスを着て、椅子に座っている",
        "白いドレス" to "白いドレスを着た少女が野原に立って、こちらを見ている",
        "二人の少女" to "二人の少女がベッドに座って、軽く微笑んでいる",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        examples.forEach { (label, full) ->
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onPick(full) },
                shape = RoundedCornerShape(20.dp),
                color = CardCream,
                border = BorderStroke(1.dp, Hairline),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedInk,
                )
            }
        }
    }
}

@Composable
private fun EmptyResult(unmatched: String) {
    PaperCard {
        Text("辞書に一致しませんでした", style = MaterialTheme.typography.titleMedium)
        if (unmatched.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "辞書外: $unmatched",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk,
            )
        }
    }
}

@Composable
private fun NaturalPromptBlock(text: String, onCopy: () -> Unit) {
    Column {
        EditorialLabel(
            text = "プロンプト",
            trailing = { QuietAction("コピー", onCopy) },
        )
        Spacer(Modifier.height(10.dp))
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
            )
        }
    }
}

@Composable
private fun AiRefineBlock(
    needsSetup: Boolean,
    loading: Boolean,
    error: String?,
    notice: String?,
    onRefine: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onRefine,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Ink),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Terracotta,
                )
                Spacer(Modifier.width(10.dp))
                Text("AIで整えています…", style = MaterialTheme.typography.labelLarge)
            } else {
                SparkleMark(Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("AIで整える", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (needsSetup) {
            Text("AIの準備が必要です。タップして設定してください。",
                style = MaterialTheme.typography.bodySmall, color = MutedInk)
        }
        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Terracotta,
                trackColor = PaperDeep,
            )
        }
        error?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        notice?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = MutedInk,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SparkleMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val primary = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            lineTo(size.width * 0.64f, size.height * 0.36f)
            lineTo(size.width, size.height * 0.5f)
            lineTo(size.width * 0.64f, size.height * 0.64f)
            lineTo(size.width * 0.5f, size.height)
            lineTo(size.width * 0.36f, size.height * 0.64f)
            lineTo(0f, size.height * 0.5f)
            lineTo(size.width * 0.36f, size.height * 0.36f)
            close()
        }
        drawPath(primary, color = Terracotta)
        val secondary = Path().apply {
            moveTo(size.width * 0.88f, size.height * 0.02f)
            lineTo(size.width * 0.94f, size.height * 0.18f)
            lineTo(size.width, size.height * 0.24f)
            lineTo(size.width * 0.94f, size.height * 0.30f)
            lineTo(size.width * 0.88f, size.height * 0.46f)
            lineTo(size.width * 0.82f, size.height * 0.30f)
            lineTo(size.width * 0.76f, size.height * 0.24f)
            lineTo(size.width * 0.82f, size.height * 0.18f)
            close()
        }
        drawPath(secondary, color = Terracotta)
    }
}

@Composable
private fun UnmatchedBlock(
    unmatched: String,
    translated: String?,
    notice: String?,
    translationReady: Boolean,
    translationDownloading: Boolean,
    translationError: String?,
    onOpenSettings: () -> Unit,
) {
    Column {
        EditorialLabel("辞書外")
        Spacer(Modifier.height(8.dp))
        Text(
            text = unmatched,
            style = MaterialTheme.typography.bodySmall,
            color = MutedInk,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        translated?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "ML Kit: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk,
            )
        }
        notice?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MutedInk,
            )
        }
        if (!translationReady) {
            Spacer(Modifier.height(4.dp))
            if (translationDownloading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Terracotta,
                    trackColor = PaperDeep,
                )
            } else {
                QuietAction("翻訳モデルを準備", onOpenSettings)
            }
        }
        translationError?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
            )
        }
    }
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
}
