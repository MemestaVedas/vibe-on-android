package moe.memesta.vibeon.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        val packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: "moe.memesta.vibeon"

        baselineProfileRule.collect(
            packageName = packageName,
            includeInStartupProfile = true,
            maxIterations = 3
        ) {
            pressHome()
            startActivityAndWait()

            // Let first composition and startup work settle.
            device.waitForIdle()

            // Exercise main navigation tabs when present.
            tapIfExists("Home")
            tapIfExists("Library")
            tapIfExists("Search")
            tapIfExists("Torrents")
            tapIfExists("Settings")
        }
    }

    private fun tapIfExists(label: String) {
        val obj = device.wait(Until.findObject(By.text(label)), 1200)
            ?: device.wait(Until.findObject(By.desc(label)), 600)
        obj?.click()
        if (obj != null) {
            device.waitForIdle()
            Thread.sleep(250)
        }
    }
}
