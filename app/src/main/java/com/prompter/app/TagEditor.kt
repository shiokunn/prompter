package com.prompter.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditor(
    tags: List<EditableTag>,
    meanings: Map<String, String>,
    onCycleWeight: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("タグ編集", style = MaterialTheme.typography.titleMedium)
            Text(
                "チップをタップすると強調（1.2 → 1.4 → 1.0）。↑↓で並び替え、×で削除。重みはタグ出力に反映されます。チップ下の日本語は辞書の意味、辞書にない場合はML Kitの機械訳です。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tags.forEachIndexed { index, editable ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = editable.weight > 1.0f,
                            onClick = { onCycleWeight(index) },
                            label = {
                                Column {
                                    Text(
                                        if (editable.weight > 1.0f) {
                                            "${editable.tag}  ${editable.weight}"
                                        } else {
                                            editable.tag
                                        },
                                    )
                                    meanings[editable.tag]?.let { meaning ->
                                        Text(
                                            text = meaning,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                        )
                                    }
                                }
                            },
                        )
                        TextButton(
                            onClick = { onMove(index, -1) },
                            enabled = index > 0,
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text("↑")
                        }
                        TextButton(
                            onClick = { onMove(index, 1) },
                            enabled = index < tags.lastIndex,
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text("↓")
                        }
                        TextButton(
                            onClick = { onRemove(index) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text("×")
                        }
                    }
                }
            }
        }
    }
}
