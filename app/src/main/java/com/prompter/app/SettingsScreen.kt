package com.prompter.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onDownloadTranslation: () -> Unit,
    onDownloadLocalModel: () -> Unit,
    onCancelLocalModelDownload: () -> Unit,
    onVerifyLocalModel: () -> Unit,
    onDeleteLocalModel: () -> Unit,
    onUnloadLocalEngine: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(initialModel) }
    var baseUrl by remember { mutableStateOf(initialBaseUrl) }
    var backend by remember { mutableStateOf(initialBackend) }
    var showKey by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDeleteModel by remember { mutableStateOf(false) }

    fun save() {
        val normalizedBaseUrl = AiDefaults.normalizeBaseUrl(baseUrl)
        val trimmedModel = model.trim()
        when {
            !AiDefaults.isValidBaseUrl(normalizedBaseUrl) -> {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI設定") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("戻る")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AIバックエンド", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "クラウドのOpenRouterか、端末内で動くGemma 4 E2Bを選べます。",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        FlowRow(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            AiBackend.entries.forEach { entry ->
                                FilterChip(
                                    selected = backend == entry,
                                    onClick = {
                                        backend = entry
                                        localError = null
                                    },
                                    label = { Text(entry.label) },
                                )
                            }
                        }
                        Text(
                            if (backend == AiBackend.LOCAL) {
                                "ローカルLLM使用時はOpenRouterのAPIキーは不要です。"
                            } else {
                                "クラウド使用時はOpenRouterのAPIキーが必要です。"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            if (backend == AiBackend.CLOUD) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("OpenRouter APIキー", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (hasApiKey) {
                                    "設定済みです。変更しない場合は空欄のまま保存してください。"
                                } else {
                                    "AI補完を使うにはOpenRouterのAPIキーが必要です。"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = {
                                    apiKey = it
                                    localError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                label = {
                                    Text(
                                        if (hasApiKey) "新しいAPIキー（変更する場合のみ）" else "APIキー",
                                    )
                                },
                                placeholder = {
                                    Text(if (hasApiKey) "設定済み" else "sk-or-v1-...")
                                },
                                singleLine = true,
                                visualTransformation = if (showKey) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    TextButton(onClick = { showKey = !showKey }) {
                                        Text(if (showKey) "隠す" else "表示")
                                    }
                                },
                            )
                            if (hasApiKey) {
                                TextButton(
                                    onClick = { confirmDelete = true },
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    Text("APIキーを削除")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("モデル", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "OpenRouterのモデルIDを指定できます。",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            FlowRow(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                AiDefaults.PRESETS.forEach { preset ->
                                    FilterChip(
                                        selected = model == preset.model,
                                        onClick = {
                                            model = preset.model
                                            localError = null
                                        },
                                        label = { Text(preset.label) },
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = model,
                                onValueChange = {
                                    model = it
                                    localError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                label = { Text("モデルID") },
                                singleLine = true,
                            )
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("API Base URL", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = baseUrl,
                                onValueChange = {
                                    baseUrl = it
                                    localError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                label = { Text("Base URL") },
                                placeholder = { Text(AiDefaults.BASE_URL) },
                                singleLine = true,
                            )
                            Text(
                                "OpenAI互換APIのURLを指定できます。通常はOpenRouterの既定値のままにしてください。",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ローカルLLM（${LocalModelSpec.DISPLAY_NAME}）",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "任意ダウンロード。端末内で日本語から英語プロンプトを生成します。ダウンロード後は機内モードでも動きます。",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "モデル: ${LocalModelSpec.FILE_NAME} / " +
                                "${formatBytes(LocalModelSpec.SIZE_BYTES)} / " +
                                LocalModelSpec.LICENSE,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )

                        when (localModelStatus.state) {
                            LocalModelState.READY -> {
                                Text(
                                    "状態: 準備完了（SHA-256検証済み）",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                if (localEngineLoaded) {
                                    Text(
                                        "エンジン: 読み込み済み（CPU / 4 threads）",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                                TextButton(
                                    onClick = { confirmDeleteModel = true },
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    Text("モデルを削除")
                                }
                                if (localEngineLoaded) {
                                    TextButton(onClick = onUnloadLocalEngine) {
                                        Text("エンジンを解放")
                                    }
                                }
                            }

                            LocalModelState.DOWNLOADING -> {
                                val percent = (localModelStatus.progress * 100).toInt()
                                Text(
                                    "状態: ダウンロード中 $percent% " +
                                        "(${formatBytes(localModelStatus.bytesDownloaded)} / " +
                                        "${formatBytes(localModelStatus.totalBytes)})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                LinearProgressIndicator(
                                    progress = { localModelStatus.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                )
                                Button(
                                    onClick = onCancelLocalModelDownload,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                ) {
                                    Text("ダウンロードをキャンセル")
                                }
                            }

                            LocalModelState.NEEDS_VERIFICATION -> {
                                Text(
                                    "状態: 検証が必要（SHA-256未確認）",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Button(
                                    onClick = onVerifyLocalModel,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                ) {
                                    Text("モデルを検証")
                                }
                            }

                            LocalModelState.VERIFYING -> {
                                Text(
                                    "状態: SHA-256を検証中…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                )
                            }

                            LocalModelState.ERROR -> {
                                Text(
                                    "状態: エラー",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                localModelStatus.error?.let { message ->
                                    Text(
                                        text = message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                                Button(
                                    onClick = onDownloadLocalModel,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                ) {
                                    Text("モデルを再ダウンロード（約2.59GB）")
                                }
                            }

                            LocalModelState.NOT_DOWNLOADED -> {
                                Text(
                                    "状態: 未ダウンロード",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Button(
                                    onClick = onDownloadLocalModel,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                ) {
                                    Text("モデルをダウンロード（約2.59GB）")
                                }
                            }
                        }

                        localModelStatus.error?.takeIf {
                            localModelStatus.state != LocalModelState.ERROR
                        }?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        Text(
                            "Wi-Fi接続時のみダウンロードします。8GB以上のRAMと約3.6GBの空き容量が必要です。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("オフライン翻訳（ML Kit）", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "辞書にない日本語を英訳し、英語からDanbooruタグを補完します。ダウンロード後は機内モードでも使えます。",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            when {
                                translationReady -> "状態: 準備完了（日本語→英語 / 英語→日本語）"
                                translationDownloading -> "状態: ダウンロード中…"
                                else -> "状態: 未ダウンロード"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        if (!translationReady) {
                            Button(
                                onClick = onDownloadTranslation,
                                enabled = !translationDownloading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            ) {
                                Text(
                                    if (translationDownloading) {
                                        "ダウンロード中…"
                                    } else {
                                        "翻訳モデルをダウンロード（約60MB）"
                                    },
                                )
                            }
                        }
                        translationError?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        Text(
                            "モデルのダウンロードにはネット接続が必要です。Google Play Servicesが使えない端末では辞書のみで動作します。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            localError?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                Button(
                    onClick = ::save,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("保存")
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("セキュリティ", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "APIキーはAndroid Keystoreの鍵で暗号化して端末内に保存します。履歴・ログ・APKには含めません。ローカルLLMのモデルと生成結果も端末外へ送信しません。",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
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

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> String.format(Locale.US, "%.2f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> String.format(Locale.US, "%.0f MB", bytes / 1_000_000.0)
    bytes >= 1_000L -> String.format(Locale.US, "%.0f KB", bytes / 1_000.0)
    else -> "$bytes B"
}
