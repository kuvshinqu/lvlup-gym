package com.levelup.gym.ui

import com.levelup.gym.R
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
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
import androidx.compose.material3.Snackbar
import androidx.compose.foundation.border
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

private data class Tab(val route: String, val label: String, @DrawableRes val icon: Int)

private val tabs = listOf(
    Tab("hero", "Hero", R.drawable.ic_hero_helmet),
    Tab("program", "Quests", R.drawable.ic_quest_scroll),
    Tab("history", "Chronicle", R.drawable.ic_campaign_map),
    Tab("exercises", "Armory", R.drawable.ic_mission_swords),
    Tab("treasury", "Treasury", R.drawable.ic_loot_chest),
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
                is GameEvent.ChestFound -> toast("${e.tier.label} found · ${e.reason}")
            }
        }
    }

    // First launch: the Hero's Trial picks a starting program.
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    LaunchedEffect(route) { if (route == "hero" && !vm.quizDone.value) nav.navigate("quiz") }
    val showBar = tabs.any { it.route == route }

    Scaffold(
        modifier = Modifier.stoneBackground(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    data,
                    containerColor = Color(0xFF1C2233),
                    contentColor = GameColors.Gold,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                )
            }
        },
        bottomBar = {
            if (showBar) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    modifier = Modifier.drawBehind {
                        // iron bar along the bottom with grain and a lit top edge
                        drawRect(Brush.verticalGradient(listOf(Color(0xFF2B2F38), Color(0xFF15171C), Color(0xFF0B0C0F))))
                        drawRect(Materials.grainBrush, alpha = 0.2f)
                        drawLine(Color.Black, Offset(0f, 0f), Offset(size.width, 0f), 2.dp.toPx())
                        drawLine(Materials.SteelLight.copy(alpha = 0.35f), Offset(0f, 2.dp.toPx()), Offset(size.width, 2.dp.toPx()), 1.dp.toPx())
                    },
                ) {
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
                            icon = {
                                androidx.compose.foundation.Image(
                                    painterResource(tab.icon), contentDescription = null,
                                    modifier = Modifier.size(28.dp).alpha(if (route == tab.route) 1f else 0.55f),
                                )
                            },
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
                ProgramScreen(
                    vm, onOpenWorkout = { nav.navigate("workout") }, onOpenExercise = { nav.navigate("exercise/$it") },
                    onChangeProgram = { nav.navigate("programs") }, onOpenQuiz = { nav.navigate("quiz") },
                )
            }
            composable("programs") {
                ProgramsScreen(
                    vm, onBack = { nav.popBackStack() }, onOpenQuiz = { nav.navigate("quiz") },
                    onBuild = { id -> nav.navigate(if (id == null) "builder" else "builder?id=$id") },
                )
            }
            composable("treasury") { TreasuryScreen(vm) }
            composable("quiz") { QuizScreen(vm, onDone = { nav.popBackStack() }) }
            composable("library") { LibraryScreen(vm, onBack = { nav.popBackStack() }) }
            composable(
                "builder?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                ProgramBuilderScreen(vm, entry.arguments?.getString("id"), onDone = { nav.popBackStack() })
            }
            composable("history") { HistoryScreen(vm, onOpen = { nav.navigate("history/$it") }) }
            composable("exercises") { ExercisesScreen(vm, onOpenExercise = { nav.navigate("exercise/$it") }, onOpenLibrary = { nav.navigate("library") }) }
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
            containerColor = Color(0xFF1A1F30),
            onDismissRequest = { levelUp = null },
            confirmButton = { TextButton(onClick = { levelUp = null }) { Text("Onward!") } },
            title = {
                Text(
                    "LEVEL UP!", modifier = Modifier.fillMaxWidth(), color = GameColors.Gold, textAlign = TextAlign.Center,
                    fontFamily = Fonts.Display, fontWeight = FontWeight.Black,
                )
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
