package com.prompter.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun filterHistory(items: List<HistoryItem>, query: String): List<HistoryItem> {
    val q = query.trim()
    if (q.isEmpty()) return items
    return items.filter { item ->
        listOf(item.input, item.pixaiPrompt, item.tagText, item.characterName,
            item.characterEnglish, item.characterSeries).any { it.contains(q, ignoreCase = true) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    onlyFavorites: Boolean,
    onFilter: (Boolean) -> Unit,
    onBack: () -> Unit,
    onLoad: (HistoryItem) -> Unit,
    onToggleFavorite: (HistoryItem) -> Unit,
    onDelete: (HistoryItem) -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HistoryItem?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(items, query) { filterHistory(items, query) }
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = Paper,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ScreenHeader(title = "履歴", onBack = onBack, actions = {
                if (items.isNotEmpty()) QuietAction("全削除", { confirmClear = true })
            })
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().navigationBarsPadding().imePadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SegmentedControl(listOf(false, true), onlyFavorites, onFilter,
                            { if (it) "お気に入り" else "すべて" })
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            label = { Text("表示中の履歴を検索") }, singleLine = true,
                            trailingIcon = {
                                if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("クリア") }
                            },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
                if (visible.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(when {
                                query.isNotBlank() -> "一致する履歴がありません"
                                onlyFavorites -> "お気に入りはまだありません"
                                else -> "履歴はまだありません"
                            }, style = MaterialTheme.typography.titleMedium, color = Ink)
                            OutlinedButton(onClick = when {
                                query.isNotBlank() -> { { query = "" } }
                                onlyFavorites -> { { onFilter(false) } }
                                else -> onBack
                            }) { Text(when {
                                query.isNotBlank() -> "検索をクリア"
                                onlyFavorites -> "すべての履歴へ"
                                else -> "プロンプトを作る"
                            }) }
                        }
                    }
                }
                items(visible, key = { it.id }) { item ->
                    Surface(shape = RoundedCornerShape(16.dp), color = CardCream,
                        border = BorderStroke(1.dp, Hairline)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(item.createdAt)),
                                style = MaterialTheme.typography.labelMedium, color = MutedInk)
                            if (item.characterEnglish.isNotBlank()) {
                                Text(listOf(item.characterName.ifBlank { item.characterEnglish }, item.characterSeries)
                                    .filter(String::isNotBlank).joinToString(" · "),
                                    style = MaterialTheme.typography.titleSmall, color = Terracotta)
                            }
                            Text(item.input, style = MaterialTheme.typography.bodyLarge, color = Ink,
                                maxLines = 3, overflow = TextOverflow.Ellipsis)
                            val preview = item.pixaiPrompt.ifBlank { item.tagText }
                            if (preview.isNotBlank()) Text(preview,
                                style = MaterialTheme.typography.bodyMedium, color = MutedInk,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            HorizontalDivider(color = Hairline)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onLoad(item) }) { Text("再利用") }
                                FilterChip(
                                    selected = item.isFavorite, onClick = { onToggleFavorite(item) },
                                    label = { Text(if (item.isFavorite) "お気に入り済み" else "お気に入り") },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                                TextButton(onClick = { deleteTarget = item }) { Text("削除", color = ErrorRed) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmClear || deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { confirmClear = false; deleteTarget = null },
            title = { Text(if (confirmClear) "履歴をすべて削除しますか？" else "この履歴を削除しますか？") },
            text = { Text(if (confirmClear) "お気に入りを含むすべての履歴が削除されます。元に戻せません。"
                else deleteTarget?.input.orEmpty(), maxLines = 4, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                TextButton(onClick = {
                    if (confirmClear) onClear() else deleteTarget?.let(onDelete)
                    confirmClear = false; deleteTarget = null
                }) { Text("削除", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false; deleteTarget = null }) { Text("キャンセル") }
            },
        )
    }
}
