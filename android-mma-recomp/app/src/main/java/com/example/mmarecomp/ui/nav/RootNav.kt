package com.example.mmarecomp.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mmarecomp.data.AuthRepository
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.ui.dashboard.DashboardScreen
import com.example.mmarecomp.ui.nutrition.MealLogScreen
import com.example.mmarecomp.ui.progress.ProgressScreen
import com.example.mmarecomp.ui.settings.SettingsScreen
import com.example.mmarecomp.ui.trainingplan.TrainingPlanScreen
import com.example.mmarecomp.ui.weighin.WeighInScreen
import com.example.mmarecomp.ui.workout.MmaSessionScreen
import com.example.mmarecomp.ui.workout.WorkoutLogScreen
import com.example.mmarecomp.viewmodel.DashboardViewModel
import com.example.mmarecomp.viewmodel.MealLogViewModel
import com.example.mmarecomp.viewmodel.MmaSessionViewModel
import com.example.mmarecomp.viewmodel.ProfileViewModel
import com.example.mmarecomp.viewmodel.ProgressViewModel
import com.example.mmarecomp.viewmodel.TrainingPlanViewModel
import com.example.mmarecomp.viewmodel.WeighInViewModel
import com.example.mmarecomp.viewmodel.WorkoutLogViewModel
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("dashboard", "Dashboard", Icons.Filled.GridView),
    Tab("workout", "Séance", Icons.Filled.FitnessCenter),
    Tab("meals", "Repas", Icons.Filled.Restaurant),
    Tab("weighin", "Pesée", Icons.Filled.MonitorWeight),
    Tab("progress", "Progr.", Icons.Filled.BarChart),
    Tab("settings", "Réglages", Icons.Filled.Settings),
)

@Composable
fun MainNav(userId: String, authRepository: AuthRepository, currentPhase: Phase, onPhaseChange: (Phase) -> Unit) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // contentDescription = null : le label texte est toujours affiché
                        // à côté (alwaysShowLabel par défaut), l'icône est donc décorative —
                        // sinon TalkBack annoncerait le libellé deux fois de suite.
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding),
        ) {
            composable("dashboard") {
                val vm = remember { DashboardViewModel() }
                DashboardScreen(vm, currentPhase)
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
                val vm = remember { MealLogViewModel() }
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
                    onPhaseSaved = onPhaseChange,
                    onSignOut = { scope.launch { authRepository.signOut() } },
                    onOpenTrainingPlan = { navController.navigate("training-plan") },
                )
            }
            composable("training-plan") {
                val vm = remember { TrainingPlanViewModel() }
                TrainingPlanScreen(vm, currentPhase)
            }
        }
    }
}
