package com.prompter.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    webResults: List<CharacterCandidate>,
    searching: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onSeriesChange: (String) -> Unit,
    onSelect: (CharacterEntry) -> Unit,
    onSelectWeb: (CharacterCandidate) -> Unit,
    onSearchWeb: () -> Unit,
    onClearSelection: () -> Unit,
    onEnglishChange: (String) -> Unit,
    sourceUrl: String?,
    onOpenSource: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialLabel(
            text = "キャラクター",
            trailing = {
                Switch(
                    checked = active,
                    onCheckedChange = onActiveChange,
                    modifier = Modifier.height(30.dp),
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
                    onEnglishChange = onEnglishChange,
                    onClearSelection = onClearSelection,
                    sourceUrl = sourceUrl,
                    onOpenSource = onOpenSource,
                )
            } else {
                CharacterSearch(
                    name = name,
                    series = series,
                    english = english,
                    dbResults = dbResults,
                    webResults = webResults,
                    searching = searching,
                    error = error,
                    onNameChange = onNameChange,
                    onSeriesChange = onSeriesChange,
                    onSelect = onSelect,
                    onSelectWeb = onSelectWeb,
                    onSearchWeb = onSearchWeb,
                    onEnglishChange = onEnglishChange,
                )
            }
        }
    }
}

@Composable
private fun CharacterSearch(
    name: String,
    series: String,
    english: String,
    dbResults: List<CharacterEntry>,
    webResults: List<CharacterCandidate>,
    searching: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onSeriesChange: (String) -> Unit,
    onSelect: (CharacterEntry) -> Unit,
    onSelectWeb: (CharacterCandidate) -> Unit,
    onSearchWeb: () -> Unit,
    onEnglishChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("名前") },
            placeholder = { Text("シンデレラ") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = series,
            onValueChange = onSeriesChange,
            label = { Text("作品名（任意）") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = onSearchWeb,
                enabled = name.isNotBlank() && !searching,
            ) {
                if (searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Ink,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("検索中")
                } else {
                    Text("Webで探す")
                }
            }
        }

        if (dbResults.isNotEmpty()) {
            CandidateCard {
                dbResults.take(6).forEachIndexed { index, entry ->
                    CandidateRow(
                        title = entry.nameJa,
                        detail = "${entry.series} · ${entry.nameEn}",
                        badge = if (entry.category == "variant") "別形態" else null,
                        onClick = { onSelect(entry) },
                    )
                    if (index < dbResults.take(6).lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = Hairline)
                    }
                }
            }
        }

        if (webResults.isNotEmpty()) {
            CandidateCard {
                webResults.take(4).forEachIndexed { index, found ->
                    CandidateRow(
                        title = found.name,
                        detail = buildString {
                            if (found.nativeName.isNotBlank()) append(found.nativeName).append(" · ")
                            append(found.source)
                        },
                        badge = null,
                        onClick = { onSelectWeb(found) },
                    )
                    if (index < webResults.take(4).lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = Hairline)
                    }
                }
            }
        }

        error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }

        if (name.isNotBlank() || series.isNotBlank()) {
            OutlinedTextField(
                value = english,
                onValueChange = onEnglishChange,
                label = { Text("英語名") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SelectedCharacter(
    name: String,
    series: String,
    english: String,
    variant: Boolean,
    onEnglishChange: (String) -> Unit,
    onClearSelection: () -> Unit,
    sourceUrl: String?,
    onOpenSource: (String) -> Unit,
) {
    PaperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        maxLines = 1,
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
        OutlinedTextField(
            value = english,
            onValueChange = onEnglishChange,
            label = { Text("英語名") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClearSelection) { Text("変更") }
            if (sourceUrl != null) {
                TextButton(onClick = { onOpenSource(sourceUrl) }) { Text("出典") }
            }
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
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    maxLines = 1,
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
                maxLines = 1,
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
