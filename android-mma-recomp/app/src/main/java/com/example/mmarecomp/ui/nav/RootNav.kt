package com.example.mmarecomp.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mmarecomp.data.AuthRepository
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.ui.dashboard.DashboardScreen
import com.example.mmarecomp.ui.nutrition.MealLogScreen
import com.example.mmarecomp.ui.plan.ImportTrainingPlanScreen
import com.example.mmarecomp.ui.plan.TrainingPlanEditScreen
import com.example.mmarecomp.ui.progress.ProgressScreen
import com.example.mmarecomp.ui.settings.CalorieGoalScreen
import com.example.mmarecomp.ui.settings.SettingsScreen
import com.example.mmarecomp.ui.weighin.WeighInScreen
import com.example.mmarecomp.ui.workout.MmaSessionScreen
import com.example.mmarecomp.ui.workout.WorkoutLogScreen
import com.example.mmarecomp.viewmodel.CalorieGoalViewModel
import com.example.mmarecomp.viewmodel.DashboardViewModel
import com.example.mmarecomp.viewmodel.ImportTrainingPlanViewModel
import com.example.mmarecomp.viewmodel.MealLogViewModel
import com.example.mmarecomp.viewmodel.MmaSessionViewModel
import com.example.mmarecomp.viewmodel.ProfileViewModel
import com.example.mmarecomp.viewmodel.ProgressViewModel
import com.example.mmarecomp.viewmodel.TrainingPlanEditViewModel
import com.example.mmarecomp.viewmodel.WeighInViewModel
import com.example.mmarecomp.viewmodel.WorkoutLogViewModel
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("dashboard", "Accueil", Icons.Filled.GridView),
    Tab("workout", "Séance", Icons.Filled.FitnessCenter),
    Tab("meals", "Repas", Icons.Filled.Restaurant),
    Tab("weighin", "Pesée", Icons.Filled.MonitorWeight),
    Tab("progress", "Progr.", Icons.Filled.BarChart),
    Tab("settings", "Réglages", Icons.Filled.Settings),
)

@Composable
fun MainNav(userId: String, userEmail: String, authRepository: AuthRepository, currentPhase: Phase, onPhaseChange: (Phase) -> Unit) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var fabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            if (backStackEntry?.destination?.route == "dashboard") {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatedVisibility(visible = fabExpanded) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExtendedFloatingActionButton(
                                onClick = { fabExpanded = false; navController.navigate("weighin") },
                                icon = { Icon(Icons.Filled.MonitorWeight, contentDescription = null) },
                                text = { Text("Pesée") },
                            )
                            ExtendedFloatingActionButton(
                                onClick = { fabExpanded = false; navController.navigate("meals") },
                                icon = { Icon(Icons.Filled.Restaurant, contentDescription = null) },
                                text = { Text("Repas") },
                            )
                            ExtendedFloatingActionButton(
                                onClick = { fabExpanded = false; navController.navigate("workout") },
                                icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
                                text = { Text("Séance") },
                            )
                        }
                    }
                    FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                        Icon(
                            if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                            contentDescription = if (fabExpanded) "Fermer" else "Accès rapide",
                        )
                    }
                }
            }
        },
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            fabExpanded = false
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = {
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
        ) {
            composable("dashboard") {
                val vm = remember(userId) { DashboardViewModel(userId = userId) }
                DashboardScreen(
                    vm,
                    currentPhase,
                    onEditPlanDay = { jourSemaine -> navController.navigate("plan_edit/$jourSemaine") },
                )
            }
            composable(
                "plan_edit/{jourSemaine}",
                arguments = listOf(navArgument("jourSemaine") { type = NavType.IntType }),
            ) { backStackEntry ->
                val jourSemaine = backStackEntry.arguments?.getInt("jourSemaine") ?: 1
                val vm = remember(jourSemaine) { TrainingPlanEditViewModel() }
                TrainingPlanEditScreen(
                    vm,
                    jourSemaine = jourSemaine,
                    phase = currentPhase,
                    onSaved = { navController.popBackStack() },
                )
            }
            composable("workout") {
                val vm = remember { WorkoutLogViewModel() }
                WorkoutLogScreen(vm, currentPhase, onOpenMmaSheet = { navController.navigate("workout/mma") })
            }
            composable("workout/mma") {
                val vm = remember { MmaSessionViewModel() }
                MmaSessionScreen(vm, onSaved = { navController.popBackStack() })
            }
            composable("meals") {
                val vm = remember(userId) { MealLogViewModel(userId = userId) }
                MealLogScreen(vm)
            }
            composable("weighin") {
                val vm = remember { WeighInViewModel() }
                WeighInScreen(vm)
            }
            composable("progress") {
                val vm = remember { ProgressViewModel() }
                ProgressScreen(vm)
            }
            composable("settings") {
                // Instancié directement (pas via viewModel()) pour garder le scaffold
                // simple : les champs ne survivent pas à une rotation d'écran.
                // À remplacer par un ViewModelProvider.Factory si besoin plus tard.
                val vm = remember(userId) { ProfileViewModel(userId) }
                SettingsScreen(
                    vm,
                    userEmail = userEmail,
                    onPhaseSaved = onPhaseChange,
                    onSignOut = { scope.launch { authRepository.signOut() } },
                    onImportPlan = { navController.navigate("plan_import") },
                    onOpenCalorieGoal = { navController.navigate("calorie_goal") },
                )
            }
            composable("plan_import") {
                val vm = remember { ImportTrainingPlanViewModel() }
                ImportTrainingPlanScreen(vm, currentPhase)
            }
            composable("calorie_goal") {
                val vm = remember(userId) { CalorieGoalViewModel(userId = userId) }
                CalorieGoalScreen(vm)
            }
        }
    }
}
