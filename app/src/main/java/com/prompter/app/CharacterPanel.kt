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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    evidence: List<CharacterSearchResult> = emptyList(),
    evidenceSearching: Boolean = false,
    evidenceError: String? = null,
    onSearchEvidence: () -> Unit = {},
    onBrowserSearch: () -> Unit = {},
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
                    evidence = evidence,
                    evidenceSearching = evidenceSearching,
                    evidenceError = evidenceError,
                    onSearchEvidence = onSearchEvidence,
                    onBrowserSearch = onBrowserSearch,
                    onOpenSource = onOpenSource,
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
    evidence: List<CharacterSearchResult>,
    evidenceSearching: Boolean,
    evidenceError: String?,
    onSearchEvidence: () -> Unit,
    onBrowserSearch: () -> Unit,
    onOpenSource: (String) -> Unit,
) {
    var manualEnglish by rememberSaveable(name, series) { mutableStateOf("") }
    var externalConfirmation by remember { mutableStateOf<String?>(null) }
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
                onClick = { externalConfirmation = "catalog" },
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
                    Text("外部候補")
                }
            }
            TextButton(onClick = { externalConfirmation = "evidence" },
                enabled = name.isNotBlank() && !evidenceSearching) {
                Text(if (evidenceSearching) "資料を検索中…" else "関連資料")
            }
        }

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

        if (webResults.isNotEmpty()) {
            CandidateCard {
                webResults.forEachIndexed { index, found ->
                    CandidateRow(
                        title = found.name,
                        detail = buildString {
                            if (found.nativeName.isNotBlank()) append(found.nativeName).append(" · ")
                            if (found.series.isNotBlank()) append(found.series).append(" · ")
                            append(found.source)
                        },
                        badge = null,
                        onClick = { onSelectWeb(found) },
                    )
                    if (index < webResults.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = Hairline)
                    }
                }
            }
        }

        error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }
        evidenceError?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium) }
        evidence.forEach { result ->
            TextButton(onClick = { onOpenSource(result.url) }) {
                Text(result.title, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        if (evidenceError != null || evidence.isNotEmpty()) {
            TextButton(onClick = onBrowserSearch) { Text("DuckDuckGoをブラウザで開く") }
        }

        if (name.isNotBlank() || series.isNotBlank()) {
            OutlinedTextField(
                value = manualEnglish,
                onValueChange = { manualEnglish = it },
                label = { Text("英語名") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { onEnglishChange(manualEnglish.trim()) }, enabled = manualEnglish.isNotBlank()) {
                Text("この英語名を使う")
            }
        }
    }
    externalConfirmation?.let { target ->
        AlertDialog(onDismissRequest = { externalConfirmation = null },
            title = { Text(if (target == "catalog") "外部候補を検索" else "関連資料を検索") },
            text = { Text(if (target == "catalog") "名前をAniList・Wikidataへ送信します。作品名で候補を絞り込みます。候補は正式表記を保証しません。"
                else "名前と作品名をDuckDuckGoへ送信します。検索結果のページ名をキャラクター名として自動採用しません。") },
            confirmButton = { TextButton(onClick = {
                externalConfirmation = null
                if (target == "catalog") onSearchWeb() else onSearchEvidence()
            }) { Text("検索") } },
            dismissButton = { TextButton(onClick = { externalConfirmation = null }) { Text("キャンセル") } },
        )
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
