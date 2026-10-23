package com.prompter.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
    var engine by remember { mutableStateOf<PromptEngine?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        try {
            engine = withContext(Dispatchers.IO) { PromptEngine(context) }
        } catch (t: Throwable) {
            error = "辞書を開けませんでした: ${t.message}"
        } finally {
            loading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { engine?.close() }
    }

    fun convert() {
        val current = engine ?: return
        if (input.isBlank()) return
        scope.launch {
            loading = true
            error = null
            try {
                result = withContext(Dispatchers.Default) { current.convert(input) }
            } catch (t: Throwable) {
                error = "変換に失敗しました: ${t.message}"
            } finally {
                loading = false
            }
        }
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
                    },
                )
            }

            item {
                ModeRow(mode = mode, onMode = { modeName = it.name })
            }

            item {
                Button(
                    onClick = ::convert,
                    enabled = input.isNotBlank() && engine != null && !loading,
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
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            result?.let { res ->
                if (res.tags.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "辞書に一致するタグがありません",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "髪色・服装・ポーズ・場所など、具体的な言葉を入れてください。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                if (res.unmatched.isNotBlank()) {
                                    Text(
                                        "未変換: ${res.unmatched}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (mode != OutputMode.TAGS) {
                        item {
                            OutputCard(
                                title = "PixAI DiT 自然文",
                                subtitle = "${res.tags.size} tags",
                                text = res.pixaiPrompt,
                            )
                        }
                    }
                    if (mode != OutputMode.PIXAI) {
                        item {
                            OutputCard(
                                title = "Danbooru タグ",
                                subtitle = "${res.tags.size} tags",
                                text = res.tagText,
                            )
                        }
                    }
                    if (res.unmatched.isNotBlank()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "辞書外: ${res.unmatched}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                )
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
                TextButton(onClick = { shareText(context, text) }) {
                    Text("共有")
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

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(intent, "プロンプトを共有")
    if (context !is android.app.Activity) {
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
