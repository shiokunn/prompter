package com.prompter.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Character picker. A tap on a candidate is the explicit confirmation step:
 * it fills the confirmed English name, series and form. Alternate forms stay
 * separate rows and are labelled, never merged into the base character.
 */
@Composable
fun CharacterPanel(
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    name: String,
    series: String,
    english: String,
    variant: Boolean,
    dbResults: List<CharacterEntry>,
    onNameChange: (String) -> Unit,
    onSelect: (CharacterEntry) -> Unit,
    onClearSelection: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialLabel(
            text = "キャラクター",
            trailing = {
                Switch(
                    checked = active,
                    onCheckedChange = onActiveChange,
                    modifier = Modifier.heightIn(min = 48.dp)
                        .semantics { contentDescription = "版権キャラクターモード" },
                )
            },
        )

        if (active) {
            if (english.isNotBlank()) {
                SelectedCharacter(
                    name = name,
                    series = series,
                    english = english,
                    variant = variant,
                    onClearSelection = onClearSelection,
                )
            } else {
                CharacterSearch(
                    name = name,
                    dbResults = dbResults,
                    onNameChange = onNameChange,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun CharacterSearch(
    name: String,
    dbResults: List<CharacterEntry>,
    onNameChange: (String) -> Unit,
    onSelect: (CharacterEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("キャラクター名") },
            placeholder = { Text("初音ミク") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        if (dbResults.isNotEmpty()) {
            CandidateCard {
                dbResults.forEachIndexed { index, entry ->
                    CandidateRow(
                        title = entry.nameJa,
                        detail = "${entry.series} · ${entry.nameEn}",
                        badge = if (entry.category == "variant") "別形態" else null,
                        onClick = { onSelect(entry) },
                    )
                    if (index < dbResults.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = Hairline)
                    }
                }
            }
        }

        if (name.isNotBlank() && dbResults.isEmpty()) {
            Text("候補が見つかりません", style = MaterialTheme.typography.bodyMedium, color = MutedInk)
        }
    }
}

@Composable
private fun SelectedCharacter(
    name: String,
    series: String,
    english: String,
    variant: Boolean,
    onClearSelection: () -> Unit,
) {
    PaperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (variant) {
                        Spacer(Modifier.width(8.dp))
                        Badge("別形態")
                    }
                }
                if (series.isNotBlank()) {
                    Text(
                        text = series,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = TerracottaSoft,
                border = BorderStroke(1.dp, Hairline),
            ) {
                Text(
                    text = "確認済み",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Terracotta,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(english, style = MaterialTheme.typography.bodyLarge, color = Ink)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClearSelection) { Text("変更") }
        }
    }
}

@Composable
private fun CandidateCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardCream,
        border = BorderStroke(1.dp, Hairline),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
            content()
        }
    }
}

@Composable
private fun CandidateRow(
    title: String,
    detail: String,
    badge: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Badge(badge)
                }
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("選択", style = MaterialTheme.typography.labelMedium, color = Terracotta)
    }
}

@Composable
private fun Badge(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = TerracottaSoft,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Terracotta,
        )
    }
}
