package com.studyapp.pomodoro.nativeapp

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PomodoroScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val engine
        get() = PomodoroTimerEngine.getInstance(instrumentation.targetContext)

    @Before
    fun prepareDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        instrumentation.runOnMainSync {
            engine.stopAlarm()
            engine.reset()
            engine.dismissCompletion()
            engine.selectPhase(Phase.Focus)
            engine.resetDailySessions()
            engine.updateSetting(Phase.Focus, 25)
            engine.updateAlarmEnabled(true)
            engine.updateAlarmSound(DEFAULT_ALARM_SOUND)
            engine.updateAlarmVibration(true)
            engine.updateAlarmDuration(10)
            engine.ensureDailyReset()
        }
        val device = UiDevice.getInstance(instrumentation)
        device.wakeUp()
        device.setOrientationNatural()
        dismissSystemDialog(device)
        device.waitForIdle()
        composeRule.waitForIdle()
    }

    @After
    fun resetDevice() {
        instrumentation.runOnMainSync { engine.stopAlarm() }
        val device = UiDevice.getInstance(instrumentation)
        device.setOrientationNatural()
        device.waitForIdle()
    }

    @Test
    fun capturesPortraitTimerDailyAndNotification() {
        val device = UiDevice.getInstance(instrumentation)

        composeRule.onNodeWithText("Start focus").performScrollTo().performClick()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()
        Thread.sleep(500L)
        scrollToTop()
        capture(device, "01_timer_running.png")

        composeRule.onNodeWithContentDescription("Reset today's sessions")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("AUTO RESET · 12:00 AM").assertExists()
        capture(device, "02_daily_rhythm.png")

        composeRule.onNodeWithText("Pause").performScrollTo().performClick()
        composeRule.onNodeWithText("Resume").assertIsDisplayed()
        scrollToTop()
        capture(device, "03_timer_paused.png")

        val pausedText = "Paused. Return when you are ready."
        val pausedNotification = waitForOngoingNotification(pausedText)
        assertEquals(
            pausedText,
            pausedNotification.extras
                .getCharSequence(Notification.EXTRA_TEXT)
                ?.toString(),
        )
        assertTrue(
            "Paused notification should remain ongoing",
            (pausedNotification.flags and Notification.FLAG_ONGOING_EVENT) != 0,
        )

        device.executeShellCommand("cmd statusbar expand-notifications")
        assertTrue(
            "Notification shade did not open",
            device.wait(Until.hasObject(By.text(pausedText)), 5_000L),
        )
        device.waitForIdle()
        Thread.sleep(3000L)
        capture(device, "04_notification_collapsed.png")
        device.pressBack()
        device.waitForIdle()

        composeRule.onNodeWithText("Resume").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun capturesAlarmSettingsCompletionAndDailyReset() {
        val device = UiDevice.getInstance(instrumentation)

        scrollToTop()
        composeRule.onNodeWithContentDescription("Alarm settings").performClick()
        composeRule.onNodeWithText("Alarm settings").assertIsDisplayed()
        composeRule.onNodeWithText("Ring when a session ends").assertIsDisplayed()
        composeRule.onNodeWithText("Alarm sound").assertIsDisplayed()
        capture(device, "05_alarm_settings.png")

        composeRule.onNodeWithText("RING FOR").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Preview alarm").assertExists()
        capture(device, "06_alarm_settings_durations.png")

        composeRule.onNodeWithContentDescription("Close alarm settings")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Pomodoro").assertIsDisplayed()

        composeRule.onNodeWithText("Start focus").performScrollTo().performClick()
        instrumentation.runOnMainSync { engine.completeCurrentPhaseForTesting() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Focus complete").assertIsDisplayed()
        capture(device, "07_session_complete.png")

        instrumentation.runOnMainSync { engine.stopAlarm() }
        composeRule.onNodeWithText("Later").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("TODAY'S RHYTHM").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 OF 4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reset today's sessions")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("0 OF 4").performScrollTo().assertIsDisplayed()
        capture(device, "08_daily_reset.png")
    }

    @Test
    fun capturesLandscapeTimerAndAlarmSettings() {
        val device = UiDevice.getInstance(instrumentation)
        device.setOrientationLeft()
        device.waitForIdle()
        composeRule.waitForIdle()
        Thread.sleep(1200L)

        composeRule.onNodeWithText("Start focus").performScrollTo().performClick()
        composeRule.onNodeWithText("TODAY'S RHYTHM").assertExists()
        composeRule.onNodeWithText("SESSION LENGTHS").performScrollTo().assertIsDisplayed()
        runCatching {
            composeRule.onNodeWithText("STUDY / RHYTHM").performScrollTo()
        }
        Thread.sleep(500L)
        capture(device, "09_landscape.png")

        val settingsButton = composeRule.onNodeWithContentDescription("Alarm settings")
        runCatching { settingsButton.performScrollTo() }
        settingsButton.performClick()
        composeRule.onNodeWithText("Alarm settings").assertIsDisplayed()
        capture(device, "10_landscape_settings.png")

        composeRule.onNodeWithContentDescription("Close alarm settings")
            .performScrollTo()
            .performClick()
        device.setOrientationNatural()
        device.waitForIdle()
    }

    private fun scrollToTop() {
        composeRule.onNodeWithText("STUDY / RHYTHM").performScrollTo()
        composeRule.waitForIdle()
    }

    private fun capture(device: UiDevice, name: String) {
        dismissSystemDialog(device)
        val directory = File(instrumentation.targetContext.filesDir, "screenshots")
        directory.mkdirs()
        assertTrue("Could not capture $name", device.takeScreenshot(File(directory, name)))
    }

    private fun dismissSystemDialog(device: UiDevice) {
        val wait = device.findObject(By.text("Wait"))
        if (wait != null) {
            wait.click()
            device.waitForIdle()
            Thread.sleep(400L)
        }
    }

    private fun waitForOngoingNotification(expectedText: String): Notification {
        val manager = instrumentation.targetContext.getSystemService(NotificationManager::class.java)
        val deadline = System.currentTimeMillis() + 10_000L
        while (System.currentTimeMillis() < deadline) {
            manager.activeNotifications
                .firstOrNull { it.id == PomodoroNotifications.ONGOING_NOTIFICATION_ID }
                ?.notification
                ?.let { notification ->
                    val text = notification.extras
                        .getCharSequence(Notification.EXTRA_TEXT)
                        ?.toString()
                    if (text == expectedText) {
                        return notification
                    }
                }
            Thread.sleep(200L)
        }
        throw AssertionError("Notification did not reach text: $expectedText")
    }
}
