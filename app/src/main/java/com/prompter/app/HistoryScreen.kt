package com.prompter.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    Scaffold(
        containerColor = Paper,
        topBar = {
            ScreenHeader(
                title = "履歴",
                onBack = onBack,
                actions = {
                    if (items.isNotEmpty()) {
                        QuietAction("全削除") { confirmClear = true }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 40.dp,
            ),
        ) {
            item {
                Column {
                    SegmentedControl(
                        options = listOf(false, true),
                        selected = onlyFavorites,
                        onSelect = onFilter,
                        label = { if (it) "お気に入り" else "すべて" },
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }

            if (items.isEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 40.dp)) {
                        Text(
                            text = if (onlyFavorites) {
                                "お気に入りはまだありません"
                            } else {
                                "履歴はまだありません"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "変換した内容がここに残ります。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk,
                        )
                    }
                }
            }

            items(items, key = { it.id }) { item ->
                HistoryRow(
                    item = item,
                    onLoad = { onLoad(item) },
                    onToggleFavorite = { onToggleFavorite(item) },
                    onDelete = { onDelete(item) },
                )
                HorizontalDivider(thickness = 1.dp, color = Hairline)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("履歴を全削除") },
            text = { Text("保存されている変換履歴をすべて削除します。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    },
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("キャンセル")
                }
            },
        )
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onLoad: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLoad)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatHistoryDate(item.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MutedInk,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.input,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.tagText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = MutedInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${parseTagText(item.tagText).size} tags",
                style = MaterialTheme.typography.labelSmall,
                color = MutedInk,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MiniAction(
                label = if (item.isFavorite) "★" else "☆",
                onClick = onToggleFavorite,
            )
            MiniAction(label = "×", onClick = onDelete)
        }
    }
}

private fun formatHistoryDate(timestamp: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(timestamp))
