package com.studyapp.pomodoro.nativeapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyapp.pomodoro.nativeapp.ui.PomodoroScreen
import com.studyapp.pomodoro.nativeapp.ui.theme.PomodoroTheme

class MainActivity : ComponentActivity() {
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
                    onToggle = viewModel::toggle,
                    onReset = viewModel::reset,
                    onSelectPhase = viewModel::selectPhase,
                    onUpdateSetting = viewModel::updateSetting,
                    onDismissCompletion = viewModel::dismissCompletion,
                    onStartNext = viewModel::startNext,
                )
            }
        }
    }
}
