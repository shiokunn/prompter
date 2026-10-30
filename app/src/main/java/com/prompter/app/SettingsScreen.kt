package com.prompter.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    hasApiKey: Boolean,
    initialModel: String,
    initialBaseUrl: String,
    initialBackend: AiBackend,
    translationReady: Boolean,
    translationDownloading: Boolean,
    translationError: String?,
    localModelStatus: LocalModelStatus,
    localEngineLoaded: Boolean,
    initialDictionaryVersion: Long,
    initialDictionaryAliasCount: Int,
    onCheckDictionary: suspend () -> DictionaryUpdateOutcome,
    onBack: () -> Unit,
    onSave: (apiKey: String, model: String, baseUrl: String, backend: AiBackend) -> Unit,
    onDeleteKey: () -> Unit,
    onTestConnection: suspend (apiKey: String, baseUrl: String) -> String,
    onDownloadTranslation: () -> Unit,
    onDownloadLocalModel: () -> Unit,
    onCancelLocalModelDownload: () -> Unit,
    onVerifyLocalModel: () -> Unit,
    onDeleteLocalModel: () -> Unit,
    onUnloadLocalEngine: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    // Secrets are intentionally not saved into Activity saved state.
    var apiKey by remember { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf(initialModel) }
    var baseUrl by rememberSaveable { mutableStateOf(initialBaseUrl) }
    var backendName by rememberSaveable { mutableStateOf(initialBackend.name) }
    val backend = AiBackend.valueOf(backendName)
    var showKey by remember { mutableStateOf(false) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var testOk by remember { mutableStateOf(false) }
    var confirmDeleteKey by remember { mutableStateOf(false) }
    var confirmDeleteModel by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var dictionaryVersion by remember { mutableStateOf(initialDictionaryVersion) }
    var dictionaryAliasCount by remember { mutableStateOf(initialDictionaryAliasCount) }
    var dictionaryUpdating by remember { mutableStateOf(false) }
    var dictionaryMessage by remember { mutableStateOf<String?>(null) }
    var dictionaryOk by remember { mutableStateOf(false) }
    val dirty = apiKey.isNotEmpty() || model != initialModel || baseUrl != initialBaseUrl || backend != initialBackend
    fun leave() { if (dirty) confirmDiscard = true else onBack() }
    BackHandler { leave() }

    fun clearTest() { testMessage = null; localError = null }
    fun save() {
        val url = AiDefaults.normalizeBaseUrl(baseUrl)
        localError = when {
            backend == AiBackend.CLOUD && !AiDefaults.isValidBaseUrl(url) -> "Base URLは https:// で始まるURLを入力してください。"
            backend == AiBackend.CLOUD && model.isBlank() -> "モデルIDを入力してください。"
            backend == AiBackend.CLOUD && !hasApiKey && apiKey.isBlank() -> "OpenRouterのAPIキーを入力してください。"
            else -> null
        }
        if (localError == null) onSave(apiKey.trim(), model.trim(), url, backend)
    }
    fun testConnection() {
        val url = AiDefaults.normalizeBaseUrl(baseUrl)
        if (!AiDefaults.isValidBaseUrl(url) || (!hasApiKey && apiKey.isBlank())) {
            localError = "APIキーと https:// から始まるBase URLを確認してください。"
            return
        }
        clearTest(); testing = true
        scope.launch {
            try {
                testMessage = onTestConnection(apiKey.trim(), url); testOk = true
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) {
                testMessage = failure.message ?: "接続できませんでした。"; testOk = false
            } finally { testing = false }
        }
    }
    fun checkDictionary() {
        if (dictionaryUpdating) return
        dictionaryUpdating = true; dictionaryMessage = null
        scope.launch {
            try {
                val outcome = onCheckDictionary()
                // A network error must not make an installed version appear to disappear.
                if (outcome.ok) {
                    dictionaryVersion = outcome.version
                    dictionaryAliasCount = outcome.aliasCount
                }
                dictionaryMessage = outcome.message; dictionaryOk = outcome.ok
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) {
                dictionaryMessage = failure.message ?: "更新できませんでした。"; dictionaryOk = false
            } finally { dictionaryUpdating = false }
        }
    }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { ScreenHeader(title = "設定") },
        bottomBar = {
            Surface(color = Paper) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding(), contentAlignment = Alignment.Center) {
                    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        localError?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium) }
                        PrimaryAction("AI設定を保存", ::save, enabled = !testing && !dictionaryUpdating)
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("生成方法", style = MaterialTheme.typography.titleLarge, color = Ink)
                        SegmentedControl(AiBackend.entries, backend,
                            { backendName = it.name; clearTest() },
                            { when (it) {
                                AiBackend.CLOUD -> "クラウド"
                                AiBackend.LOCAL -> "CPU"
                                AiBackend.LOCAL_GPU -> "GPU"
                            } })
                    }
                }
                if (backend == AiBackend.CLOUD) {
                    item {
                        SettingsGroup("OpenRouter") {
                            OutlinedTextField(
                                value = apiKey, onValueChange = { apiKey = it; clearTest() },
                                label = { Text(if (hasApiKey) "APIキー（設定済み）" else "APIキー") },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "隠す" else "表示") } },
                                shape = RoundedCornerShape(12.dp),
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = ::testConnection, enabled = !testing) {
                                    Text(if (testing) "接続中…" else "接続テスト")
                                }
                                TextButton(onClick = {
                                    runCatching { uriHandler.openUri("https://openrouter.ai/keys") }
                                        .onFailure { localError = "リンクを開けませんでした。" }
                                }) { Text("キーを取得") }
                                if (hasApiKey) TextButton(onClick = { confirmDeleteKey = true }) { Text("キーを削除", color = ErrorRed) }
                            }
                            testMessage?.let { StatusMessage(it, testOk) }
                            HorizontalDivider(color = Hairline)
                            Text("モデル", style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AiDefaults.PRESETS.forEach { preset ->
                                    FilterChip(selected = model == preset.model,
                                        onClick = { model = preset.model; clearTest() },
                                        label = { Text(preset.label) }, modifier = Modifier.heightIn(min = 48.dp))
                                }
                            }
                            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                                Text(if (showAdvanced) "詳細を閉じる" else "モデルID・接続先")
                            }
                            if (showAdvanced) {
                                OutlinedTextField(model, { model = it; clearTest() },
                                    label = { Text("モデルID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(baseUrl, { baseUrl = it; clearTest() },
                                    label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            }
                        }
                    }
                } else {
                    item {
                        SettingsGroup(LocalModelSpec.DISPLAY_NAME) {
                            LocalModelControls(localModelStatus, localEngineLoaded,
                                onDownloadLocalModel, onCancelLocalModelDownload, onVerifyLocalModel,
                                { confirmDeleteModel = true }, onUnloadLocalEngine)
                        }
                    }
                }
                item {
                    SettingsGroup("辞書") {
                        Text(if (dictionaryVersion > 0) "署名辞書 v$dictionaryVersion · 別名 $dictionaryAliasCount 件" else "同梱辞書を使用中",
                            style = MaterialTheme.typography.bodyLarge, color = Ink)
                        OutlinedButton(onClick = ::checkDictionary, enabled = !dictionaryUpdating) {
                            Text(if (dictionaryUpdating) "更新を確認中…" else "更新を確認")
                        }
                        if (dictionaryUpdating) LinearProgressIndicator(Modifier.fillMaxWidth())
                        dictionaryMessage?.let { StatusMessage(it, dictionaryOk) }
                    }
                }
                item {
                    SettingsGroup("日本語・英語の翻訳") {
                        Text(if (translationReady) "翻訳モデル準備完了" else "翻訳モデル未準備",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (translationReady) SuccessGreen else Ink)
                        if (!translationReady) {
                            OutlinedButton(onClick = onDownloadTranslation, enabled = !translationDownloading) {
                                Text(if (translationDownloading) "ダウンロード中…" else "ダウンロード（約60MB）")
                            }
                            if (translationDownloading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        translationError?.let { StatusMessage(it, false) }
                    }
                }
            }
        }
    }
    if (confirmDeleteKey || confirmDeleteModel || confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDeleteKey = false; confirmDeleteModel = false; confirmDiscard = false },
            title = { Text(when {
                confirmDiscard -> "変更を破棄しますか？"
                confirmDeleteKey -> "APIキーを削除しますか？"
                else -> "モデルを削除しますか？"
            }) },
            text = { Text(when {
                confirmDiscard -> "AI設定の変更は保存されません。辞書やモデルの更新は保持されます。"
                confirmDeleteKey -> "保存されているOpenRouterのAPIキーを削除します。"
                else -> "約${formatBytes(LocalModelSpec.SIZE_BYTES)}のファイルを削除します。再利用にはダウンロードが必要です。"
            }) },
            confirmButton = { TextButton(onClick = {
                when {
                    confirmDiscard -> onBack()
                    confirmDeleteKey -> { apiKey = ""; onDeleteKey() }
                    else -> onDeleteLocalModel()
                }
                confirmDeleteKey = false; confirmDeleteModel = false; confirmDiscard = false
            }) { Text(if (confirmDiscard) "破棄して戻る" else "削除", color = ErrorRed) } },
            dismissButton = { TextButton(onClick = {
                confirmDeleteKey = false; confirmDeleteModel = false; confirmDiscard = false
            }) { Text("キャンセル") } },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    PaperCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun StatusMessage(text: String, ok: Boolean) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = if (ok) SuccessGreen else ErrorRed)
}

/** Plain, state-driven content; no file or network operations during composition. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalModelControls(
    status: LocalModelStatus,
    loaded: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onVerify: () -> Unit,
    onDelete: () -> Unit,
    onUnload: () -> Unit,
) {
    Text("${formatBytes(LocalModelSpec.SIZE_BYTES)} · ${localStateLabel(status.state)}",
        style = MaterialTheme.typography.bodyLarge, color = Ink)
    when (status.state) {
        LocalModelState.READY -> {
            Text("SHA-256検証済み", color = SuccessGreen, style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loaded) OutlinedButton(onClick = onUnload) { Text("メモリを解放") }
                TextButton(onClick = onDelete) { Text("モデルを削除", color = ErrorRed) }
            }
        }
        LocalModelState.DOWNLOADING -> {
            Text("${(status.progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = onCancel) { Text("キャンセル") }
        }
        LocalModelState.NEEDS_VERIFICATION -> OutlinedButton(onClick = onVerify) { Text("SHA-256を検証") }
        LocalModelState.VERIFYING -> LinearProgressIndicator(Modifier.fillMaxWidth())
        LocalModelState.ERROR -> {
            status.error?.let { StatusMessage(it, false) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDownload) { Text("再ダウンロード") }
                TextButton(onClick = onVerify) { Text("再検証") }
            }
        }
        LocalModelState.NOT_DOWNLOADED -> OutlinedButton(onClick = onDownload) {
            Text("ダウンロード（${formatBytes(LocalModelSpec.SIZE_BYTES)}）")
        }
    }
}

private fun localStateLabel(state: LocalModelState): String = when (state) {
    LocalModelState.NOT_DOWNLOADED -> "未ダウンロード"
    LocalModelState.DOWNLOADING -> "ダウンロード中"
    LocalModelState.NEEDS_VERIFICATION -> "検証待ち"
    LocalModelState.VERIFYING -> "検証中"
    LocalModelState.READY -> "準備完了"
    LocalModelState.ERROR -> "エラー"
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> String.format(Locale.US, "%.2f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> String.format(Locale.US, "%.0f MB", bytes / 1_000_000.0)
    else -> "$bytes B"
}
