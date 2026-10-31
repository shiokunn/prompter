package com.prompter.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
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
    fun circularCharacterToggleCanEnableAndDisableThePanel() {
        val active = mutableStateOf(false)
        compose.setContent { PrompterTheme {
            CharacterPanel(active.value, { active.value = it }, "", "", "", false,
                emptyList(), emptyList(), {}, {}, {})
        } }
        val toggle = compose.onNodeWithContentDescription("版権キャラクターモード")
        toggle.assertIsOff()
        compose.onNodeWithText("キャラクター名").assertDoesNotExist()
        toggle.performClick().assertIsOn()
        compose.onNodeWithText("キャラクター名").assertIsDisplayed()
        toggle.performClick().assertIsOff()
        compose.onNodeWithText("キャラクター名").assertDoesNotExist()
    }

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
    fun characterCandidateMustBeSelected() {
        val entry = CharacterEntry(1, "初音ミク", "Hatsune Miku", "VOCALOID", "character", "")
        var selected: CharacterEntry? = null
        compose.setContent { PrompterTheme {
            CharacterPanel(
                active = true,
                onActiveChange = {},
                name = "ミク",
                series = "",
                english = "",
                variant = false,
                dbResults = listOf(entry),
                mentionResults = emptyList(),
                onNameChange = {},
                onSelect = { selected = it },
                onClearSelection = {},
            )
        } }
        compose.onNodeWithText("キャラクター名").assertIsDisplayed()
        compose.onNodeWithText("登場作品・ゲーム・サービス名（任意）").assertDoesNotExist()
        compose.onNodeWithText("英語名").assertDoesNotExist()
        assertNull(selected)
        compose.onNodeWithText("初音ミク").performClick()
        assertEquals(entry, selected)
    }

    @Test
    fun sceneMentionCandidateRequiresTap() {
        val entry = CharacterEntry(2, "胡桃", "Hu Tao", "原神", "base", "")
        var selected: CharacterEntry? = null
        compose.setContent { PrompterTheme {
            CharacterPanel(
                active = true,
                onActiveChange = {},
                name = "",
                series = "",
                english = "",
                variant = false,
                dbResults = emptyList(),
                mentionResults = listOf(entry),
                onNameChange = {},
                onSelect = { selected = it },
                onClearSelection = {},
            )
        } }
        compose.onNodeWithText("情景の候補").assertIsDisplayed()
        assertNull(selected)
        compose.onNodeWithText("胡桃").performClick()
        assertEquals(entry, selected)
    }

    @Test
    fun settingsKeepsSaveVisibleWithoutHeaderBack() {
        var saved = false
        compose.setContent { PrompterTheme {
            SettingsScreen(false, AiDefaults.MODEL, AiDefaults.BASE_URL, AiBackend.LOCAL,
                true, false, null, LocalModelStatus(LocalModelState.NOT_DOWNLOADED), false, 2, 4,
                { DictionaryUpdateOutcome(true, "最新です", 2, 4) }, {},
                { _, _, _, _ -> saved = true }, {}, { _, _ -> "接続できました" }, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("AI設定を保存").assertIsDisplayed()
        compose.onNodeWithText("戻る").assertDoesNotExist()
        compose.onNodeWithText("クラウド").performClick()
        assertFalse(saved)
    }
}
