package com.prompter.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Locale

private const val OPENROUTER_KEYS_URL = "https://openrouter.ai/keys"

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

    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(initialModel) }
    var baseUrl by remember { mutableStateOf(initialBaseUrl) }
    var backend by remember { mutableStateOf(initialBackend) }
    var showKey by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var testOk by remember { mutableStateOf<Boolean?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDeleteModel by remember { mutableStateOf(false) }

    fun clearTest() {
        testMessage = null
        testOk = null
    }

    fun save() {
        val normalizedBaseUrl = AiDefaults.normalizeBaseUrl(baseUrl)
        val trimmedModel = model.trim()
        when {
            backend == AiBackend.CLOUD && !AiDefaults.isValidBaseUrl(normalizedBaseUrl) -> {
                localError = "Base URLは https:// で始まるURLを入力してください。"
            }
            backend == AiBackend.CLOUD && trimmedModel.isEmpty() -> {
                localError = "モデルIDを入力してください。"
            }
            backend == AiBackend.CLOUD && !hasApiKey && apiKey.isBlank() -> {
                localError = "OpenRouterのAPIキーを入力してください。"
            }
            else -> {
                localError = null
                onSave(apiKey.trim(), trimmedModel, normalizedBaseUrl, backend)
            }
        }
    }

    fun testConnection() {
        val normalizedBaseUrl = AiDefaults.normalizeBaseUrl(baseUrl)
        if (!AiDefaults.isValidBaseUrl(normalizedBaseUrl)) {
            localError = "Base URLは https:// で始まるURLを入力してください。"
            return
        }
        if (!hasApiKey && apiKey.isBlank()) {
            localError = "先にAPIキーを入力してください。"
            return
        }
        localError = null
        clearTest()
        testing = true
        scope.launch {
            try {
                testMessage = onTestConnection(apiKey.trim(), normalizedBaseUrl)
                testOk = true
            } catch (t: Throwable) {
                testMessage = t.message ?: "接続に失敗しました。"
                testOk = false
            } finally {
                testing = false
            }
        }
    }

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ScreenHeader(title = "設定", onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Column {
                    EditorialLabel("AI")
                    Spacer(Modifier.height(10.dp))
                    SegmentedControl(
                        options = AiBackend.entries,
                        selected = backend,
                        onSelect = {
                            backend = it
                            localError = null
                        },
                        label = { if (it == AiBackend.CLOUD) "クラウド" else "ローカル" },
                    )
                }
            }

            if (backend == AiBackend.CLOUD) {
                item {
                    Column {
                        EditorialLabel("APIキー")
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it
                                localError = null
                                clearTest()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    if (hasApiKey) {
                                        "設定済み（変更する場合のみ入力）"
                                    } else {
                                        "sk-or-v1-..."
                                    },
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (showKey) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                TextButton(onClick = { showKey = !showKey }) {
                                    Text(
                                        text = if (showKey) "隠す" else "表示",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row {
                            OutlinedButton(
                                onClick = ::testConnection,
                                enabled = !testing,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Ink),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                            ) {
                                if (testing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Ink,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("テスト中", style = MaterialTheme.typography.labelLarge)
                                } else {
                                    Text("接続テスト", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            QuietAction(
                                text = "キーを再発行",
                                onClick = {
                                    runCatching { uriHandler.openUri(OPENROUTER_KEYS_URL) }
                                },
                            )
                        }
                        testMessage?.let { message ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = message,
                                color = if (testOk == true) SuccessGreen else ErrorRed,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (hasApiKey) {
                            Spacer(Modifier.height(4.dp))
                            QuietAction("APIキーを削除", onClick = { confirmDelete = true })
                        }
                    }
                }

                item {
                    Column {
                        EditorialLabel("モデル")
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AiDefaults.PRESETS.forEach { preset ->
                                FilterChip(
                                    selected = model == preset.model,
                                    onClick = {
                                        model = preset.model
                                        localError = null
                                    },
                                    label = {
                                        Text(
                                            preset.label,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = CardCream,
                                        labelColor = MutedInk,
                                        selectedContainerColor = Ink,
                                        selectedLabelColor = CardCream,
                                    ),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = model,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = MutedInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        QuietAction(
                            text = if (showAdvanced) "詳細を閉じる" else "モデルID・Base URL",
                            onClick = { showAdvanced = !showAdvanced },
                        )
                        if (showAdvanced) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = model,
                                onValueChange = {
                                    model = it
                                    localError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("モデルID") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors(),
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = baseUrl,
                                onValueChange = {
                                    baseUrl = it
                                    localError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Base URL") },
                                placeholder = { Text(AiDefaults.BASE_URL) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors(),
                            )
                        }
                    }
                }
            } else {
                item {
                    Column {
                        EditorialLabel("ローカルLLM")
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = LocalModelSpec.DISPLAY_NAME,
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${formatBytes(LocalModelSpec.SIZE_BYTES)} / ${localStateLabel(localModelStatus.state)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk,
                        )

                        when (localModelStatus.state) {
                            LocalModelState.READY -> {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "SHA-256検証済み",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SuccessGreen,
                                )
                                if (localEngineLoaded) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "エンジン読み込み済み / CPU 4 threads",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedInk,
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Row {
                                    OutlinedButton(
                                        onClick = { confirmDeleteModel = true },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Hairline),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                                    ) {
                                        Text("モデルを削除")
                                    }
                                    if (localEngineLoaded) {
                                        Spacer(Modifier.width(8.dp))
                                        QuietAction("エンジンを解放", onUnloadLocalEngine)
                                    }
                                }
                            }

                            LocalModelState.DOWNLOADING -> {
                                Spacer(Modifier.height(12.dp))
                                val percent = (localModelStatus.progress * 100).toInt()
                                Text(
                                    "ダウンロード中 $percent%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk,
                                )
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { localModelStatus.progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Terracotta,
                                    trackColor = PaperDeep,
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = onCancelLocalModelDownload,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Ink),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                                ) {
                                    Text("キャンセル")
                                }
                            }

                            LocalModelState.NEEDS_VERIFICATION -> {
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = onVerifyLocalModel,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Ink),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                                ) {
                                    Text("SHA-256を検証")
                                }
                            }

                            LocalModelState.VERIFYING -> {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "検証中…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk,
                                )
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Terracotta,
                                    trackColor = PaperDeep,
                                )
                            }

                            LocalModelState.ERROR -> {
                                localModelStatus.error?.let { message ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ErrorRed,
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onDownloadLocalModel,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Ink),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                                ) {
                                    Text("再ダウンロード")
                                }
                                QuietAction("再検証", onVerifyLocalModel)
                            }

                            LocalModelState.NOT_DOWNLOADED -> {
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = onDownloadLocalModel,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Ink),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                                ) {
                                    Text("ダウンロード（${formatBytes(LocalModelSpec.SIZE_BYTES)}）")
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Wi-Fiのみ / 8GB RAM / 約3.6GB空き",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedInk,
                        )
                    }
                }
            }

            item {
                Column {
                    EditorialLabel("翻訳")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (translationReady) {
                            "ML Kit / 準備完了"
                        } else {
                            "ML Kit / 未ダウンロード"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (translationReady) SuccessGreen else MutedInk,
                    )
                    if (!translationReady) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onDownloadTranslation,
                            enabled = !translationDownloading,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Ink),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                        ) {
                            Text(
                                if (translationDownloading) {
                                    "ダウンロード中…"
                                } else {
                                    "翻訳モデル（約60MB）"
                                },
                            )
                        }
                    }
                    translationError?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed,
                        )
                    }
                }
            }

            localError?.let { message ->
                item {
                    Text(
                        text = message,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Column {
                    PrimaryAction("保存", ::save)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "APIキーはAndroid Keystoreで暗号化して保存します。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedInk,
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("APIキーを削除") },
            text = { Text("保存されているOpenRouter APIキーを削除します。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteKey()
                    },
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("キャンセル")
                }
            },
        )
    }

    if (confirmDeleteModel) {
        AlertDialog(
            onDismissRequest = { confirmDeleteModel = false },
            title = { Text("ローカルLLMモデルを削除") },
            text = {
                Text(
                    "モデルファイル（約${formatBytes(LocalModelSpec.SIZE_BYTES)}）を削除します。" +
                        "次回利用時に再ダウンロードが必要です。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteModel = false
                        onDeleteLocalModel()
                    },
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteModel = false }) {
                    Text("キャンセル")
                }
            },
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Ink,
    unfocusedBorderColor = Hairline,
    focusedContainerColor = CardCream,
    unfocusedContainerColor = CardCream,
    cursorColor = Terracotta,
)

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
    bytes >= 1_000L -> String.format(Locale.US, "%.0f KB", bytes / 1_000.0)
    else -> "$bytes B"
}
