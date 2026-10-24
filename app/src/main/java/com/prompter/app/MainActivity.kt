package com.prompter.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PrompterApp()
                }
            }
        }
    }
}

private enum class OutputMode(val label: String) {
    BOTH("両方"),
    PIXAI("PixAI自然文"),
    TAGS("Danbooruタグ"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrompterApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var input by rememberSaveable { mutableStateOf("") }
    var modeName by rememberSaveable { mutableStateOf(OutputMode.BOTH.name) }
    val mode = OutputMode.valueOf(modeName)
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
    var tagMeanings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var mlTagMeanings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        loading = true
        try {
            engine = withContext(Dispatchers.IO) { PromptEngine(context) }
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

    LaunchedEffect(editableTags, engine, translationReady, translator) {
        val current = engine ?: return@LaunchedEffect
        val tags = editableTags.map { it.tag }
        val dictionaryMeanings: Map<String, String> = if (tags.isEmpty()) {
            emptyMap()
        } else {
            withContext(Dispatchers.IO) { current.japaneseMeanings(tags) }
        }
        tagMeanings = dictionaryMeanings + mlTagMeanings.filterKeys { it in tags }

        val currentTranslator = translator
        if (translationReady && currentTranslator != null) {
            val missing = tags
                .filter { it !in dictionaryMeanings && it !in mlTagMeanings }
                .distinct()
            for (batch in missing.chunked(8)) {
                val translated = withContext(Dispatchers.IO) {
                    batch.mapNotNull { tag ->
                        runCatching {
                            currentTranslator.translateEnglishToJapanese(tag.replace('_', ' '))
                        }.getOrNull()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?.let { tag to it }
                    }.toMap()
                }
                if (translated.isNotEmpty()) {
                    mlTagMeanings = mlTagMeanings + translated
                    tagMeanings = dictionaryMeanings + mlTagMeanings.filterKeys { it in tags }
                }
            }
        }
    }

    LaunchedEffect(showHistory, onlyFavorites, historyStore) {
        if (showHistory) {
            val store = historyStore ?: return@LaunchedEffect
            historyItems = withContext(Dispatchers.IO) { store.recent(onlyFavorites) }
        }
    }

    val editedTagText = remember(editableTags) { formatTagText(editableTags) }
    val editedPixaiPrompt = remember(editableTags, naturalPromptOverride, translatedUnmatched) {
        naturalPromptOverride ?: PromptComposer.compose(
            tags = editableTags.map { it.tag },
            extraContext = translatedUnmatched,
        )
    }

    LaunchedEffect(
        editableTags,
        currentHistoryId,
        historyStore,
        editedPixaiPrompt,
        editedTagText,
    ) {
        val id = currentHistoryId ?: return@LaunchedEffect
        val store = historyStore ?: return@LaunchedEffect
        if (editableTags.isEmpty()) return@LaunchedEffect
        delay(300)
        withContext(Dispatchers.IO) {
            store.update(id, editedPixaiPrompt, editedTagText)
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
                naturalPromptOverride = null
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
                    input = sourceInput,
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
                        localLlm.generate(sourceInput, offlineTags, current.unmatched)
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
                            input = sourceInput,
                            offlineTags = offlineTags,
                            unmatched = current.unmatched,
                        )
                    }
                }
                if (!fromCache && cache != null && cacheKey != null) {
                    withContext(Dispatchers.IO) { cache.put(cacheKey, aiResult) }
                }

                val validation = AiPromptValidator.validate(aiResult) { tag ->
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
                            backend == AiBackend.LOCAL -> "ローカルLLMで更新"
                            else -> "AIで更新"
                        },
                    )
                    append(" / ${validation.wordCount}語")
                    if (!validation.wordCountOk) {
                        append("（目標80〜150語）")
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
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                aiError = if (backend == AiBackend.LOCAL) {
                    "ローカルLLMで失敗しました: ${t.message ?: "不明なエラー"}"
                } else {
                    t.message ?: "AI補完に失敗しました。"
                }
            } finally {
                localEngineLoaded = localLlm.isLoaded
                aiLoading = false
            }
        }
    }

    fun cycleWeight(index: Int) {
        if (index !in editableTags.indices) return
        val next = editableTags.toMutableList()
        next[index] = next[index].copy(weight = cycleTagWeight(next[index].weight))
        editableTags = next
    }

    fun moveTag(index: Int, delta: Int) {
        val target = index + delta
        if (index !in editableTags.indices || target !in editableTags.indices) return
        val next = editableTags.toMutableList()
        val item = next.removeAt(index)
        next.add(target, item)
        editableTags = next
    }

    fun removeTag(index: Int) {
        if (index !in editableTags.indices) return
        naturalPromptOverride = null
        aiNotice = null
        editableTags = editableTags.filterIndexed { i, _ -> i != index }
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
            onBack = { showSettings = false },
            onSave = ::saveAiSettings,
            onDeleteKey = ::deleteAiKey,
            onDownloadTranslation = ::downloadTranslationModels,
            onDownloadLocalModel = ::downloadLocalModel,
            onCancelLocalModelDownload = ::cancelLocalModelDownload,
            onVerifyLocalModel = ::verifyLocalModel,
            onDeleteLocalModel = ::deleteLocalModel,
            onUnloadLocalEngine = ::unloadLocalEngine,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Prompter", fontWeight = FontWeight.SemiBold)
                        Text(
                            "日本語 → PixAI DiT / Danbooru tags",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showSettings = true }) {
                        Text("設定")
                    }
                    TextButton(onClick = { showHistory = true }) {
                        Text("履歴")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                IntroCard()
            }

            item {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("日本語の情景") },
                    placeholder = {
                        Text("例: 長い黒髪の少女が青いドレスを着て、椅子に座っている")
                    },
                    minLines = 4,
                    maxLines = 8,
                )
            }

            item {
                ExampleChips(
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

            item {
                ModeRow(mode = mode, onMode = { modeName = it.name })
            }

            item {
                Button(
                    onClick = ::convert,
                    enabled = input.isNotBlank() && engine != null && !loading && !aiLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (loading) "変換中…" else "変換する")
                }
            }

            if (loading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    }
                }
            }

            error?.let { message ->
                item {
                    ErrorCard(message)
                }
            }

            result?.let { res ->
                val hasAiPrompt = !naturalPromptOverride.isNullOrBlank()
                val hasTranslatedContext = !translatedUnmatched.isNullOrBlank()
                when {
                    res.tags.isEmpty() && !hasAiPrompt && !hasTranslatedContext -> {
                        item { NoMatchCard(res.unmatched) }
                        item {
                            AiCard(
                                backend = aiBackend,
                                hasApiKey = apiKeyConfigured,
                                model = aiModel,
                                localModelStatus = localModelStatus,
                                loading = aiLoading,
                                error = aiError,
                                notice = aiNotice,
                                enabled = !aiLoading,
                                onOpenSettings = { showSettings = true },
                                onRefine = ::refineWithAi,
                            )
                        }
                        if (res.unmatched.isNotBlank() && !translationReady) {
                            item {
                                TranslationSetupCard(
                                    downloading = translationDownloading,
                                    error = translationError,
                                    onOpenSettings = { showSettings = true },
                                )
                            }
                        }
                    }
                    editableTags.isEmpty() && !hasAiPrompt && !hasTranslatedContext -> {
                        item { EmptyTagsCard() }
                        item {
                            AiCard(
                                backend = aiBackend,
                                hasApiKey = apiKeyConfigured,
                                model = aiModel,
                                localModelStatus = localModelStatus,
                                loading = aiLoading,
                                error = aiError,
                                notice = aiNotice,
                                enabled = !aiLoading,
                                onOpenSettings = { showSettings = true },
                                onRefine = ::refineWithAi,
                            )
                        }
                    }
                    else -> {
                        val emphasized = editableTags.count { it.weight > 1.0f }
                        val subtitle = when {
                            editableTags.isEmpty() -> "ML Kit翻訳 / タグなし"
                            emphasized > 0 -> "${editableTags.size} tags / $emphasized emphasized"
                            else -> "${editableTags.size} tags"
                        }
                        if (mode != OutputMode.TAGS) {
                            item {
                                OutputCard(
                                    title = "PixAI DiT 自然文",
                                    subtitle = subtitle,
                                    text = editedPixaiPrompt,
                                )
                            }
                        }
                        if (mode != OutputMode.PIXAI && editableTags.isNotEmpty()) {
                            item {
                                OutputCard(
                                    title = "Danbooru タグ",
                                    subtitle = subtitle,
                                    text = editedTagText,
                                )
                            }
                        }
                        if (editableTags.isNotEmpty()) {
                            item {
                                TagEditor(
                                    tags = editableTags,
                                    meanings = tagMeanings,
                                    onCycleWeight = ::cycleWeight,
                                    onMove = ::moveTag,
                                    onRemove = ::removeTag,
                                )
                            }
                        } else {
                            item { AiNoTagsCard() }
                        }
                        item {
                            AiCard(
                                backend = aiBackend,
                                hasApiKey = apiKeyConfigured,
                                model = aiModel,
                                localModelStatus = localModelStatus,
                                loading = aiLoading,
                                error = aiError,
                                notice = aiNotice,
                                enabled = !aiLoading,
                                onOpenSettings = { showSettings = true },
                                onRefine = ::refineWithAi,
                            )
                        }
                        if (res.unmatched.isNotBlank()) {
                            item {
                                UnmatchedCard(
                                    unmatched = res.unmatched,
                                    translated = translatedUnmatched,
                                    notice = translationNotice,
                                )
                            }
                            if (!translationReady && !hasAiPrompt) {
                                item {
                                    TranslationSetupCard(
                                        downloading = translationDownloading,
                                        error = translationError,
                                        onOpenSettings = { showSettings = true },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Footer(engine)
            }
        }
    }
}

private fun formatDuration(millis: Long): String =
    String.format(Locale.US, "%.1f秒", millis / 1000.0)

@Composable
private fun IntroCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("オフラインで下書き", style = MaterialTheme.typography.titleMedium)
            Text(
                "日本語の情景を端末内の辞書でタグに変換し、PixAI DiT 向けの英語プロンプトを組み立てます。ネット接続は不要です。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ExampleChips(onPick: (String) -> Unit) {
    val examples = listOf(
        "長い黒髪の少女が青いドレスを着て、椅子に座っている",
        "白いドレスを着た少女が野原に立って、こちらを見ている",
        "二人の少女がベッドに座って、軽く微笑んでいる",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        examples.forEach { example ->
            AssistChip(
                onClick = { onPick(example) },
                label = { Text(example.take(18) + "…", style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
private fun ModeRow(mode: OutputMode, onMode: (OutputMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutputMode.entries.forEach { entry ->
            FilterChip(
                selected = mode == entry,
                onClick = { onMode(entry) },
                label = { Text(entry.label) },
            )
        }
    }
}

@Composable
private fun OutputCard(title: String, subtitle: String, text: String) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = { copyText(context, title, text) }) {
                    Text("コピー")
                }
            }
            SelectionContainer {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun AiNoTagsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("有効なタグなし", style = MaterialTheme.typography.titleMedium)
            Text(
                "辞書に存在するタグがありません。自然文を確認するか、入力文を変えて再試行してください。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AiCard(
    backend: AiBackend,
    hasApiKey: Boolean,
    model: String,
    localModelStatus: LocalModelStatus,
    loading: Boolean,
    error: String?,
    notice: String?,
    enabled: Boolean,
    onOpenSettings: () -> Unit,
    onRefine: () -> Unit,
) {
    val local = backend == AiBackend.LOCAL
    val localStateLabel = when (localModelStatus.state) {
        LocalModelState.NOT_DOWNLOADED -> "未ダウンロード"
        LocalModelState.DOWNLOADING -> "ダウンロード中"
        LocalModelState.NEEDS_VERIFICATION -> "検証待ち"
        LocalModelState.VERIFYING -> "検証中"
        LocalModelState.READY -> "準備完了"
        LocalModelState.ERROR -> "エラー"
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (local) "ローカルLLM補完（任意）" else "AI補完（任意）",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (local) {
                    "オフライン結果を端末内のGemma 4 E2Bで整えます。機内モードでも使えます。初回はモデルの読み込みに時間がかかります。"
                } else {
                    "オフライン結果をOpenRouterのモデルで整えます。"
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            when {
                local && !localModelStatus.isReady -> {
                    Text(
                        "モデルの状態: $localStateLabel。設定から約2.59GBのモデルを準備してください。",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    localModelStatus.error?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text("AI設定を開く")
                    }
                }

                local -> {
                    Text(
                        "モデル: ${LocalModelSpec.DISPLAY_NAME} / CPU",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(
                        onClick = onRefine,
                        enabled = enabled && !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text(
                            if (loading) {
                                "ローカルLLMで整え中…"
                            } else {
                                "ローカルLLMで整える"
                            },
                        )
                    }
                }

                !hasApiKey -> {
                    Text(
                        "OpenRouter APIキーを設定すると使えます。キーは端末内で暗号化して保存します。",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text("AI設定を開く")
                    }
                }

                else -> {
                    Text(
                        "モデル: $model",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(
                        onClick = onRefine,
                        enabled = enabled && !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text(if (loading) "AIで整え中…" else "AIで整える")
                    }
                }
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            notice?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun NoMatchCard(unmatched: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("辞書に一致するタグがありません", style = MaterialTheme.typography.titleMedium)
            Text(
                "髪色・服装・ポーズ・場所など、具体的な言葉を入れてください。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (unmatched.isNotBlank()) {
                Text(
                    "未変換: $unmatched",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyTagsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("タグをすべて削除しました", style = MaterialTheme.typography.titleMedium)
            Text(
                "入力文を変えて、もう一度変換してください。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun UnmatchedCard(
    unmatched: String,
    translated: String?,
    notice: String?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "辞書外: $unmatched",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!translated.isNullOrBlank()) {
                Text(
                    text = "ML Kit翻訳: $translated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (!notice.isNullOrBlank()) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TranslationSetupCard(
    downloading: Boolean,
    error: String?,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("辞書外を翻訳できます", style = MaterialTheme.typography.titleSmall)
            Text(
                "ML Kitの翻訳モデル（約60MB）をダウンロードすると、辞書にない日本語も英訳して自然文とタグに反映します。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(
                onClick = onOpenSettings,
                enabled = !downloading,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(if (downloading) "ダウンロード中…" else "設定で翻訳モデルを準備")
            }
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Footer(engine: PromptEngine?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = engine?.let { "オフライン辞書: ${it.tagCount} タグ / ${it.aliasCount} 別名" }
                ?: "辞書を読み込み中…",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "M1 fresh holdout: Recall 51.9% / Precision 76.2%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
}
