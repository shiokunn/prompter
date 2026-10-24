package com.prompter.app

import java.util.Locale

data class EditableTag(
    val tag: String,
    val weight: Float = 1.0f,
)

fun cycleTagWeight(weight: Float): Float = when {
    weight < 1.1f -> 1.2f
    weight < 1.3f -> 1.4f
    else -> 1.0f
}

fun formatTagText(tags: List<EditableTag>): String =
    tags.joinToString(", ") { editable ->
        if (editable.weight > 1.0f) {
            "(${editable.tag}:${formatWeight(editable.weight)})"
        } else {
            editable.tag
        }
    }

fun parseTagText(text: String): List<EditableTag> {
    if (text.isBlank()) return emptyList()
    return text.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { part ->
            val weighted = WEIGHTED_TAG.matchEntire(part)
            if (weighted != null) {
                EditableTag(
                    tag = weighted.groupValues[1],
                    weight = weighted.groupValues[2].toFloatOrNull() ?: 1.0f,
                )
            } else {
                EditableTag(part)
            }
        }
}

private fun formatWeight(weight: Float): String =
    String.format(Locale.US, "%.1f", weight)

private val WEIGHTED_TAG = Regex("^\\((.*):([0-9]+(?:\\.[0-9]+)?)\\)$")
