package com.studyapp.decaycharge

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.studyapp.decaycharge.domain.ActiveChargeController
import com.studyapp.decaycharge.navigation.LocalWindowSizeClass
import com.studyapp.decaycharge.ui.DecayChargeApp
import com.studyapp.decaycharge.ui.theme.DecayChargeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var controller: ActiveChargeController

    private val autoLaunchChapterId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
            val chapterId by autoLaunchChapterId.collectAsState()
            CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                DecayChargeTheme {
                    DecayChargeApp(
                        autoLaunchChapterId = chapterId,
                        onAutoLaunchHandled = { autoLaunchChapterId.value = null },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        controller.restore()
        lifecycleScope.launch {
            controller.awaitRestore()
            controller.ensureForegroundService()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val chapterId = intent?.getLongExtra(EXTRA_AUTO_LAUNCH_CHAPTER_ID, -1L) ?: -1L
        if (chapterId > 0) {
            autoLaunchChapterId.value = chapterId
            intent?.removeExtra(EXTRA_AUTO_LAUNCH_CHAPTER_ID)
        }
    }

    companion object {
        const val EXTRA_AUTO_LAUNCH_CHAPTER_ID = "com.studyapp.decaycharge.extra.AUTO_LAUNCH_CHAPTER_ID"
    }
}
