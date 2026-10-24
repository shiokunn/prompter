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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var aiLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiNotice by remember { mutableStateOf<String?>(null) }
    var naturalPromptOverride by remember { mutableStateOf<String?>(null) }

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
        } catch (t: Throwable) {
            error = "初期化に失敗しました: ${t.message}"
        } finally {
            loading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            engine?.close()
            historyStore?.close()
        }
    }

    LaunchedEffect(showHistory, onlyFavorites, historyStore) {
        if (showHistory) {
            val store = historyStore ?: return@LaunchedEffect
            historyItems = withContext(Dispatchers.IO) { store.recent(onlyFavorites) }
        }
    }

    val editedTagText = remember(editableTags) { formatTagText(editableTags) }
    val editedPixaiPrompt = remember(editableTags, naturalPromptOverride) {
        naturalPromptOverride ?: PromptComposer.compose(editableTags.map { it.tag })
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
            try {
                val converted = withContext(Dispatchers.Default) { current.convert(input) }
                naturalPromptOverride = null
                result = converted
                editableTags = converted.tags.map { EditableTag(it) }
                currentHistoryId = null
                val store = historyStore
                if (store != null && converted.tags.isNotEmpty()) {
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
        }
    }

    fun saveAiSettings(apiKey: String, model: String, baseUrl: String) {
        val settings = aiSettings ?: return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (apiKey.isNotBlank()) settings.setApiKey(apiKey)
                    settings.model = model
                    settings.baseUrl = baseUrl
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

    fun refineWithAi() {
        val current = result ?: return
        val currentEngine = engine ?: return
        val settings = aiSettings ?: return
        if (!apiKeyConfigured) {
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
                val (apiKey, model, baseUrl) = withContext(Dispatchers.IO) {
                    Triple(settings.getApiKey(), settings.model, settings.baseUrl)
                }
                if (apiKey.isNullOrBlank()) {
                    throw OpenRouterException(
                        "APIキーを読み込めませんでした。設定で再登録してください。",
                    )
                }

                val cache = aiCache
                val cacheKey = cache?.key(
                    baseUrl = baseUrl,
                    model = model,
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
                val aiResult = cached ?: withContext(Dispatchers.IO) {
                    OpenRouterClient(baseUrl).refine(
                        apiKey = apiKey,
                        model = model,
                        input = sourceInput,
                        offlineTags = offlineTags,
                        unmatched = current.unmatched,
                    )
                }
                if (!fromCache && cache != null && cacheKey != null) {
                    withContext(Dispatchers.IO) { cache.put(cacheKey, aiResult) }
                }

                val validation = AiPromptValidator.validate(aiResult) { tag ->
                    currentEngine.isKnownTag(tag)
                }
                if (validation.prompt.isBlank()) {
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

                aiNotice = buildString {
                    append(if (fromCache) "キャッシュから反映" else "AIで更新")
                    append(" / ${validation.wordCount}語")
                    if (!validation.wordCountOk) {
                        append("（目標80〜150語）")
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
                aiError = t.message ?: "AI補完に失敗しました。"
            } finally {
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
            onBack = { showSettings = false },
            onSave = ::saveAiSettings,
            onDeleteKey = ::deleteAiKey,
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
                when {
                    res.tags.isEmpty() && !hasAiPrompt -> {
                        item { NoMatchCard(res.unmatched) }
                        item {
                            AiCard(
                                hasApiKey = apiKeyConfigured,
                                model = aiModel,
                                loading = aiLoading,
                                error = aiError,
                                notice = aiNotice,
                                enabled = !aiLoading,
                                onOpenSettings = { showSettings = true },
                                onRefine = ::refineWithAi,
                            )
                        }
                    }
                    editableTags.isEmpty() && !hasAiPrompt -> {
                        item { EmptyTagsCard() }
                        item {
                            AiCard(
                                hasApiKey = apiKeyConfigured,
                                model = aiModel,
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
                            editableTags.isEmpty() -> "AI prompt / no valid tags"
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
                                hasApiKey = apiKeyConfigured,
                                model = aiModel,
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
                                UnmatchedCard(res.unmatched)
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
                "AIは辞書に存在するタグを返しませんでした。自然文を確認するか、入力文を変えて再試行してください。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AiCard(
    hasApiKey: Boolean,
    model: String,
    loading: Boolean,
    error: String?,
    notice: String?,
    enabled: Boolean,
    onOpenSettings: () -> Unit,
    onRefine: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("AI補完（任意）", style = MaterialTheme.typography.titleMedium)
            Text(
                "オフライン結果をOpenRouterのモデルで整えます。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (!hasApiKey) {
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
            } else {
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
private fun UnmatchedCard(unmatched: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "辞書外: $unmatched",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
        )
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
