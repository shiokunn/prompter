package com.prompter.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cross-version smoke test: the app must start and open its bundled
 * databases on every Android version and ABI we ship.
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @Test
    fun applicationStartsWithBundledResources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.assets.open("promptdb.sqlite").use { it.read() > 0 })
        assertTrue(context.assets.open("characterdb.sqlite").use { it.read() > 0 })

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> assertFalse(activity.isFinishing) }
        }
    }
}
