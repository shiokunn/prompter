package com.prompter.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreenInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun historyDeleteRequiresConfirmationAndReuseDoesNotDelete() {
        val item = HistoryItem(1, "青い服の少女", "A girl", "blue_dress", "", 0, false)
        var deleted = 0
        var loaded = 0
        compose.setContent { PrompterTheme {
            HistoryScreen(listOf(item), false, {}, {}, { loaded++ }, {}, { deleted++ }, {})
        } }
        compose.onNodeWithText("再利用").performClick()
        assertEquals(1, loaded)
        compose.onNodeWithText("削除").performClick()
        assertEquals(0, deleted)
        compose.onNodeWithText("この履歴を削除しますか？").assertIsDisplayed()
        compose.onNodeWithText("キャンセル").performClick()
        assertEquals(0, deleted)
    }

    @Test
    fun typingManualEnglishRequiresExplicitConfirmation() {
        var confirmed = ""
        compose.setContent { PrompterTheme {
            CharacterPanel(true, {}, "人物", "作品", "", false, emptyList(), emptyList(), false, null,
                {}, {}, {}, {}, {}, {}, { confirmed = it }, null, {})
        } }
        compose.onNodeWithText("英語名").performTextInput("Example Name")
        assertEquals("", confirmed)
        compose.onNodeWithText("この英語名を使う").performClick()
        assertEquals("Example Name", confirmed)
    }

    @Test
    fun settingsKeepsSaveVisibleAndProtectsUnsavedEdits() {
        var saved = false
        var left = false
        compose.setContent { PrompterTheme {
            SettingsScreen(false, AiDefaults.MODEL, AiDefaults.BASE_URL, AiBackend.LOCAL,
                true, false, null, LocalModelStatus(LocalModelState.NOT_DOWNLOADED), false, 2, 4,
                { DictionaryUpdateOutcome(true, "最新です", 2, 4) }, { left = true },
                { _, _, _, _ -> saved = true }, {}, { _, _ -> "接続できました" }, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("AI設定を保存").assertIsDisplayed()
        compose.onNodeWithText("クラウド").performClick()
        compose.onNodeWithText("戻る").performClick()
        compose.onNodeWithText("変更を破棄しますか？").assertIsDisplayed()
        assertFalse(left)
        compose.onNodeWithText("キャンセル").performClick()
        assertFalse(saved)
    }
}
