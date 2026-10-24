package com.studyapp.pomodoro.nativeapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyapp.pomodoro.nativeapp.ui.PomodoroScreen
import com.studyapp.pomodoro.nativeapp.ui.theme.PomodoroTheme

class MainActivity : ComponentActivity() {

    private var notificationPermissionRequested = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            PomodoroTheme {
                val viewModel: PomodoroViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                PomodoroScreen(
                    state = state,
                    onToggle = {
                        requestNotificationPermissionIfNeeded()
                        viewModel.toggle()
                    },
                    onReset = viewModel::reset,
                    onSelectPhase = viewModel::selectPhase,
                    onUpdateSetting = viewModel::updateSetting,
                    onResetDaily = viewModel::resetDailySessions,
                    onUpdateAlarmEnabled = viewModel::updateAlarmEnabled,
                    onUpdateAlarmSound = viewModel::updateAlarmSound,
                    onUpdateAlarmVibration = viewModel::updateAlarmVibration,
                    onUpdateAlarmDuration = viewModel::updateAlarmDuration,
                    onPreviewAlarm = viewModel::previewAlarm,
                    onStopAlarm = viewModel::stopAlarm,
                    onDismissCompletion = viewModel::dismissCompletion,
                    onStartNext = {
                        requestNotificationPermissionIfNeeded()
                        viewModel.startNext()
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        PomodoroTimerEngine.getInstance(this).apply {
            ensureDailyReset()
            ensureServiceIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (notificationPermissionRequested) return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionRequested = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
