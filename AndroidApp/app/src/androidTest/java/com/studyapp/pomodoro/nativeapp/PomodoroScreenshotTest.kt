package com.studyapp.pomodoro.nativeapp

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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

    @Before
    fun prepareDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        val device = UiDevice.getInstance(instrumentation)
        device.wakeUp()
        device.setOrientationNatural()
        device.waitForIdle()
    }

    @After
    fun resetOrientation() {
        val device = UiDevice.getInstance(instrumentation)
        device.setOrientationNatural()
        device.waitForIdle()
    }

    @Test
    fun pausedTimerKeepsNotificationAndCapturesPortraitScreenshots() {
        val device = UiDevice.getInstance(instrumentation)

        composeRule.onNodeWithText("Start focus").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()
        Thread.sleep(300L)
        capture(device, "01_timer_running.png")

        composeRule.onNodeWithText("Pause").performClick()
        composeRule.onNodeWithText("Resume").assertIsDisplayed()
        composeRule.onNodeWithText("Short").performClick()
        composeRule.onNodeWithText("FOCUS SESSION").assertIsDisplayed()
        capture(device, "02_timer_paused.png")

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

        device.openNotification()
        device.waitForIdle()
        Thread.sleep(1200L)
        capture(device, "03_notification_collapsed.png")
        device.pressBack()
        device.waitForIdle()

        composeRule.onNodeWithText("Reset timer").performClick()
        composeRule.onNodeWithText("Start focus").assertIsDisplayed()
    }

    @Test
    fun landscapeUsesTwoColumnLayoutAndCapturesScreenshot() {
        val device = UiDevice.getInstance(instrumentation)
        device.setOrientationLeft()
        device.waitForIdle()
        composeRule.waitForIdle()
        Thread.sleep(1500L)

        composeRule.onNodeWithText("STUDY / RHYTHM").assertIsDisplayed()
        composeRule.onNodeWithText("FOCUS TIMER").assertIsDisplayed()
        composeRule.onNodeWithText("TODAY'S RHYTHM").assertIsDisplayed()
        composeRule.onNodeWithText("SESSION LENGTHS").assertIsDisplayed()
        capture(device, "04_landscape.png")

        device.setOrientationNatural()
        device.waitForIdle()
    }

    private fun capture(device: UiDevice, name: String) {
        val directory = File(instrumentation.targetContext.filesDir, "screenshots")
        directory.mkdirs()
        assertTrue("Could not capture $name", device.takeScreenshot(File(directory, name)))
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
