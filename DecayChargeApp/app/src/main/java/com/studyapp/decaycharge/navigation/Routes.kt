package com.studyapp.decaycharge.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object TriageRoute

@Serializable
data object SyllabusRoute

@Serializable
data object AnalyticsRoute

@Serializable
data object ImportRoute

@Serializable
data class ChargerRoute(val chapterId: Long)

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
) {
    Triage(TriageRoute, TriageRoute::class, "Triage", Icons.Rounded.BatteryChargingFull),
    Syllabus(SyllabusRoute, SyllabusRoute::class, "Syllabus", Icons.Rounded.AutoStories),
    Analytics(AnalyticsRoute, AnalyticsRoute::class, "Analytics", Icons.Rounded.QueryStats),
}

val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("LocalWindowSizeClass must be provided by MainActivity")
}
