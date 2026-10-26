package com.prompter.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Compact tag editor shown only in tag-output mode. Tapping the weight pill
 * cycles 1.0 -> 1.2 -> 1.4; the arrows reorder and the cross removes.
 */
@Composable
fun TagEditor(
    tags: List<EditableTag>,
    meanings: Map<String, String>,
    onCycleWeight: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        tags.forEachIndexed { index, editable ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = editable.tag,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    meanings[editable.tag]?.takeIf { it.isNotBlank() }?.let { meaning ->
                        Text(
                            text = meaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                WeightPill(
                    weight = editable.weight,
                    onClick = { onCycleWeight(index) },
                )
                MiniAction(
                    label = "↑",
                    onClick = { onMove(index, -1) },
                    enabled = index > 0,
                )
                MiniAction(
                    label = "↓",
                    onClick = { onMove(index, 1) },
                    enabled = index < tags.lastIndex,
                )
                MiniAction(
                    label = "×",
                    onClick = { onRemove(index) },
                )
            }
            if (index != tags.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = Hairline)
            }
        }
    }
}

@Composable
private fun WeightPill(weight: Float, onClick: () -> Unit) {
    val emphasized = weight > 1.0f
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (emphasized) TerracottaSoft else CardCream,
        border = BorderStroke(
            1.dp,
            if (emphasized) Terracotta.copy(alpha = 0.45f) else Hairline,
        ),
    ) {
        Text(
            text = String.format(Locale.US, "%.1f", weight),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
            ),
            color = if (emphasized) Terracotta else MutedInk,
        )
    }
}
