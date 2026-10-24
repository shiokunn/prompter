package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Test

class TagEditingTest {
    @Test
    fun formatsAndParsesWeights() {
        val tags = listOf(
            EditableTag("blue_dress"),
            EditableTag("smile", 1.2f),
            EditableTag("bow_(weapon)", 1.4f),
        )
        val text = formatTagText(tags)
        assertEquals("blue_dress, (smile:1.2), (bow_(weapon):1.4)", text)
        assertEquals(tags, parseTagText(text))
    }

    @Test
    fun parsesPlainTagText() {
        assertEquals(
            listOf(EditableTag("1girl"), EditableTag("long_hair")),
            parseTagText("1girl, long_hair"),
        )
    }

    @Test
    fun cyclesWeights() {
        assertEquals(1.2f, cycleTagWeight(1.0f), 0.0001f)
        assertEquals(1.4f, cycleTagWeight(1.2f), 0.0001f)
        assertEquals(1.0f, cycleTagWeight(1.4f), 0.0001f)
    }
}
