package com.studyapp.decaycharge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyapp.decaycharge.navigation.AnalyticsRoute
import com.studyapp.decaycharge.navigation.ChargerRoute
import com.studyapp.decaycharge.navigation.LocalWindowSizeClass
import com.studyapp.decaycharge.navigation.SyllabusRoute
import com.studyapp.decaycharge.navigation.TopLevelDestination
import com.studyapp.decaycharge.navigation.TriageRoute
import com.studyapp.decaycharge.domain.ActiveChargeState
import com.studyapp.decaycharge.ui.components.CalmIconButton
import com.studyapp.decaycharge.ui.components.Hairline
import com.studyapp.decaycharge.ui.screens.AnalyticsScreen
import com.studyapp.decaycharge.ui.screens.ChargerScreen
import com.studyapp.decaycharge.ui.screens.SyllabusScreen
import com.studyapp.decaycharge.ui.screens.TriageScreen
import com.studyapp.decaycharge.ui.theme.Border
import com.studyapp.decaycharge.ui.theme.Forest
import com.studyapp.decaycharge.ui.theme.Ivory
import com.studyapp.decaycharge.ui.theme.MintLight
import com.studyapp.decaycharge.ui.theme.Muted
import com.studyapp.decaycharge.ui.theme.Paper
import com.studyapp.decaycharge.ui.theme.Stone
import com.studyapp.decaycharge.ui.theme.ZoneGreen

@Composable
fun DecayChargeApp() {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val activeSession by sessionViewModel.activeSession.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull {
        destination?.hasRoute(it.routeClass) == true
    }
    val isCharger = destination?.hasRoute(ChargerRoute::class) == true
    val sizeClass = LocalWindowSizeClass.current
    val useRail = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact ||
        sizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    val showNavigation = currentTab != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ivory)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
            )
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (showNavigation && useRail) {
                        Modifier.navigationBarsPadding()
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (showNavigation && useRail) {
                DecayNavigationRail(
                    selected = currentTab,
                    onSelect = { navController.switchTab(it) },
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                DecayNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (!isCharger) {
            activeSession?.let { session ->
                MiniChargerBar(
                    session = session,
                    onOpen = {
                        navController.navigate(ChargerRoute(session.chapterId)) {
                            launchSingleTop = true
                        }
                    },
                    onToggle = sessionViewModel::togglePause,
                    modifier = if (showNavigation && useRail) {
                        Modifier.navigationBarsPadding()
                    } else {
                        Modifier
                    },
                )
            }
        }
        if (showNavigation && !useRail) {
            DecayBottomBar(
                selected = currentTab,
                onSelect = { navController.switchTab(it) },
                modifier = Modifier.navigationBarsPadding(),
            )
        } else if (!showNavigation && !isCharger) {
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun DecayNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TriageRoute,
        modifier = modifier,
        enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) },
        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) },
        popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) },
        popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) },
    ) {
        composable<TriageRoute> {
            val viewModel: TriageViewModel = hiltViewModel()
            TriageScreen(
                viewModel = viewModel,
                onOpenCharger = { navController.navigate(ChargerRoute(it)) },
            )
        }
        composable<SyllabusRoute> {
            val viewModel: SyllabusViewModel = hiltViewModel()
            SyllabusScreen(
                viewModel = viewModel,
                onBackToTriage = { navController.switchTab(TopLevelDestination.Triage) },
                onOpenCharger = { navController.navigate(ChargerRoute(it)) },
            )
        }
        composable<AnalyticsRoute> {
            val viewModel: AnalyticsViewModel = hiltViewModel()
            AnalyticsScreen(
                viewModel = viewModel,
                onBackToTriage = { navController.switchTab(TopLevelDestination.Triage) },
            )
        }
        composable<ChargerRoute> {
            val viewModel: ChargerViewModel = hiltViewModel()
            ChargerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun NavHostController.switchTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MiniChargerBar(
    session: ActiveChargeState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Paper),
    ) {
        Hairline(color = Border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(StoneLight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(session.progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(if (session.isComplete) ZoneGreen else Forest),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clickable(onClick = onOpen)
                .padding(start = 18.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(Color(session.subjectColorHex), CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (session.isComplete) {
                        "Ready to review · ${session.chapterTitle}"
                    } else {
                        "Charging · ${session.chapterTitle}"
                    },
                    color = Ink,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                )
                Text(
                    text = if (session.isComplete) {
                        "${session.projectedBattery.toInt()}% held · choose a recall rating"
                    } else {
                        "${session.subjectName} · ${formatSeconds(session.remainingSeconds)} remaining"
                    },
                    color = Muted,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                )
            }
            Spacer(Modifier.width(6.dp))
            if (session.isComplete) {
                CalmIconButton(
                    icon = Icons.Rounded.Check,
                    contentDescription = "Review completed charge",
                    onClick = onOpen,
                    size = 32.dp,
                    iconSize = 15.dp,
                    tint = ZoneGreen,
                    filled = false,
                )
            } else {
                CalmIconButton(
                    icon = if (session.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (session.isRunning) "Pause charge" else "Resume charge",
                    onClick = onToggle,
                    size = 32.dp,
                    iconSize = 15.dp,
                    filled = false,
                )
                CalmIconButton(
                    icon = Icons.Rounded.ArrowForward,
                    contentDescription = "Open charger",
                    onClick = onOpen,
                    size = 32.dp,
                    iconSize = 15.dp,
                    filled = false,
                )
            }
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

@Composable
private fun DecayBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Paper.copy(alpha = 0.98f)),
    ) {
        Hairline(color = Border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                BottomDestination(
                    destination = destination,
                    active = selected == destination,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomDestination(
    destination: TopLevelDestination,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics {
                selected = active
                contentDescription = destination.label
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(if (active) MintLight else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = if (active) Forest else Muted,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = destination.label.uppercase(),
            color = if (active) Forest else Muted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp,
                letterSpacing = 1.2.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun DecayNavigationRail(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(94.dp)
            .fillMaxHeight()
            .background(Paper)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Forest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "dc",
                color = Ivory,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        Spacer(Modifier.height(34.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                RailDestination(
                    destination = destination,
                    active = selected == destination,
                    onClick = { onSelect(destination) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "DECAY\nCHARGE",
            color = Muted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp,
                lineHeight = 12.sp,
                letterSpacing = 1.8.sp,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Hairline(modifier = Modifier.width(18.dp), color = Stone)
    }
}

@Composable
private fun RailDestination(
    destination: TopLevelDestination,
    active: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics {
                selected = active
                contentDescription = destination.label
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(if (active) MintLight else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = if (active) Forest else Muted,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = destination.label.uppercase(),
            color = if (active) Forest else Muted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp,
                letterSpacing = 1.0.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}
