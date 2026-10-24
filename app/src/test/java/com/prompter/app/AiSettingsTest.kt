package com.prompter.app

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AiSettingsTest {
    @Test
    fun defaultAndPresetsUseTheFreeRouter() {
        assertEquals("openrouter/free", AiDefaults.MODEL)
        assertTrue(AiDefaults.PRESETS.any { it.model == AiDefaults.MODEL })
        assertFalse(AiDefaults.PRESETS.any { it.model == AiDefaults.LEGACY_FREE_MODEL })
    }

    @Test
    fun migratesLegacyModelsToFreeRouterOnce() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            val settings = AiSettings(context)
            settings.model = AiDefaults.LEGACY_FREE_MODEL
            settings.migrateLegacyDefaultModel()
            assertEquals(AiDefaults.MODEL, settings.model)

            // A deliberate later choice must not be overwritten by the
            // one-time migration marker.
            settings.model = AiDefaults.LEGACY_FREE_MODEL
            settings.migrateLegacyDefaultModel()
            assertEquals(AiDefaults.LEGACY_FREE_MODEL, settings.model)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun preservesCustomModelsDuringMigration() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            val settings = AiSettings(context)
            settings.model = "custom/provider-model"
            settings.migrateLegacyDefaultModel()
            assertEquals("custom/provider-model", settings.model)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test
    fun backendDefaultsToCloudAndPersistsLocal() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            val settings = AiSettings(context)
            assertEquals(AiBackend.CLOUD, settings.backend)

            settings.backend = AiBackend.LOCAL
            assertEquals(AiBackend.LOCAL, AiSettings(context).backend)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    private companion object {
        const val PREFS_NAME = "prompter_ai_settings"
    }
}
