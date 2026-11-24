package com.levelup.gym.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("hero", "Hero", Icons.Filled.Face),
    Tab("program", "Program", Icons.Filled.DateRange),
    Tab("history", "History", Icons.AutoMirrored.Filled.List),
    Tab("exercises", "Exercises", Icons.Filled.Star),
)

@Composable
fun GymAppUi(vm: GymViewModel = viewModel()) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    var levelUp by remember { mutableStateOf<GameEvent.LevelUp?>(null) }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        // Show toasts without blocking, so a level-up never waits behind a snackbar.
        fun toast(text: String) {
            snackbar.currentSnackbarData?.dismiss()
            scope.launch { snackbar.showSnackbar(text) }
        }
        vm.events.collect { e ->
            when (e) {
                is GameEvent.XpGained ->
                    toast("+${e.xp} XP · ${e.label}" + if (e.personalRecord) " · NEW BEST!" else "")
                is GameEvent.LevelUp -> levelUp = e
                is GameEvent.Message -> toast(e.text)
            }
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "hero", modifier = Modifier.padding(padding)) {
            composable("hero") {
                HeroScreen(
                    vm,
                    onOpenWorkout = { nav.navigate("workout") },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
            composable("program") {
                ProgramScreen(vm, onOpenWorkout = { nav.navigate("workout") }, onOpenExercise = { nav.navigate("exercise/$it") })
            }
            composable("history") { HistoryScreen(vm, onOpen = { nav.navigate("history/$it") }) }
            composable("exercises") { ExercisesScreen(vm, onOpenExercise = { nav.navigate("exercise/$it") }) }
            composable("workout") {
                WorkoutScreen(vm, onClose = { nav.popBackStack() }, onOpenExercise = { nav.navigate("exercise/$it") })
            }
            composable(
                "exercise/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                ExerciseDetailScreen(vm, entry.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
            composable(
                "history/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                WorkoutDetailScreen(vm, entry.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
        }
    }

    levelUp?.let { e ->
        AlertDialog(
            onDismissRequest = { levelUp = null },
            confirmButton = { TextButton(onClick = { levelUp = null }) { Text("Onward!") } },
            title = {
                Text("LEVEL UP!", modifier = Modifier, color = GameColors.Gold, textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LevelBadge(e.level, size = 96.dp)
                    Text("Your hero reached level ${e.level}.", style = MaterialTheme.typography.titleMedium)
                    Text("Rank: ${e.title}", color = GameColors.Gold)
                }
            },
        )
    }
}
