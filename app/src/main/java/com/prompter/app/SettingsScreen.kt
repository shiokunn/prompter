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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    hasApiKey: Boolean,
    initialModel: String,
    initialBaseUrl: String,
    onBack: () -> Unit,
    onSave: (apiKey: String, model: String, baseUrl: String) -> Unit,
    onDeleteKey: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(initialModel) }
    var baseUrl by remember { mutableStateOf(initialBaseUrl) }
    var showKey by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun save() {
        val normalizedBaseUrl = AiDefaults.normalizeBaseUrl(baseUrl)
        val trimmedModel = model.trim()
        when {
            !AiDefaults.isValidBaseUrl(normalizedBaseUrl) -> {
                localError = "Base URLは https:// で始まるURLを入力してください。"
            }
            trimmedModel.isEmpty() -> {
                localError = "モデルIDを入力してください。"
            }
            !hasApiKey && apiKey.isBlank() -> {
                localError = "OpenRouterのAPIキーを入力してください。"
            }
            else -> {
                localError = null
                onSave(apiKey.trim(), trimmedModel, normalizedBaseUrl)
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
                            "APIキーはAndroid Keystoreの鍵で暗号化して端末内に保存します。履歴・ログ・APKには含めません。",
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
}
