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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.sp

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
            // On the workout screen, toasts sit above the Log next set bar so they never cover it.
            SnackbarHost(snackbar, Modifier.padding(bottom = if (route == "workout") 84.dp else 0.dp)) { data ->
                Box(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                        .drawBehind { chunkyBlock(GameColors.Parchment, 14.dp.toPx(), shadow = 3.dp.toPx(), lip = 3.dp.toPx(), gloss = 0f) }
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                ) {
                    Text(data.visuals.message, color = Color(0xFF4A3216), style = MaterialTheme.typography.titleSmall.copy(shadow = null))
                }
            }
        },
        bottomBar = {
            if (showBar) {
                GameTabBar(tabs, route) { r ->
                    nav.navigate(r) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
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
                    onOpenBadges = { nav.navigate("badges") },
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
            composable("badges") { BadgesScreen(vm, onBack = { nav.popBackStack() }) }
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
            containerColor = GameColors.Panel,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            modifier = Modifier.border(3.dp, GameColors.Gold, androidx.compose.foundation.shape.RoundedCornerShape(22.dp)),
            onDismissRequest = { levelUp = null },
            confirmButton = { Button(onClick = { levelUp = null }, modifier = Modifier.fillMaxWidth()) { Text("Onward!") } },
            title = {
                OutlinedText(
                    "LEVEL UP!", modifier = Modifier.fillMaxWidth(), color = GameColors.Gold, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall, outlineWidth = 5.dp,
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


/** Bottom tabs: the chosen tab rises as a lit tile with its name; the others rest as dim icons. */
@Composable
private fun GameTabBar(tabs: List<Tab>, route: String?, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A4C93), Color(0xFF1A3269), Color(0xFF101F47))))
                drawRect(GameColors.Ink, Offset.Zero, androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx()))
                drawRect(Color.White.copy(alpha = 0.18f), Offset(0f, 3.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()))
            }
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        tabs.forEach { tab ->
            val selected = route == tab.route
            Box(
                Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable { onSelect(tab.route) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Canvas(Modifier.fillMaxSize().padding(horizontal = 2.dp)) {
                        chunkyBlock(GameColors.PanelLight, 14.dp.toPx(), shadow = 3.dp.toPx(), lip = 4.dp.toPx(), rim = GameColors.Gold, gloss = 0.18f)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = if (selected) 6.dp else 0.dp)) {
                    androidx.compose.foundation.Image(
                        painterResource(tab.icon), contentDescription = if (selected) null else tab.label,
                        modifier = Modifier.size(if (selected) 34.dp else 30.dp).alpha(if (selected) 1f else 0.6f),
                    )
                    if (selected) OutlinedText(tab.label, style = MaterialTheme.typography.titleSmall, outlineWidth = 2.5.dp, fontSize = 12.sp)
                    else Text(tab.label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp, shadow = null), color = GameColors.Muted, fontSize = 11.sp)
                }
            }
        }
    }
}
