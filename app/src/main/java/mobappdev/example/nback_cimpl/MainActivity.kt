package mobappdev.example.nback_cimpl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mobappdev.example.nback_cimpl.ui.screens.GameScreen
import mobappdev.example.nback_cimpl.ui.screens.HistoryScreen
import mobappdev.example.nback_cimpl.ui.screens.HomeScreen
import mobappdev.example.nback_cimpl.ui.screens.SettingsScreen
import mobappdev.example.nback_cimpl.ui.theme.NBack_CImplTheme
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import mobappdev.example.nback_cimpl.ui.viewmodels.HistoryVM

/**
 * App entry point.
 *
 * Owns one [GameVM] instance scoped to the activity's ViewModelStore so
 * the Home and Game routes share a single source of truth across
 * navigation and configuration changes.
 *
 * Per STYLE.md §1, the ViewModel does not drive navigation itself; each
 * screen exposes callbacks that the NavHost consumes.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NBack_CImplTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    val gameViewModel: GameVM = viewModel(factory = GameVM.Factory)
                    // Activity-scoped on purpose: navigation-compose 2.4.0 does
                    // NOT include APPLICATION_KEY in the per-backstack-entry
                    // extras, so constructing HistoryVM inside its route
                    // composable would crash on `this[APPLICATION_KEY] as
                    // GameApplication`. Hoisting here reuses the Activity's
                    // extras which do include it.
                    val historyViewModel: HistoryVM = viewModel(factory = HistoryVM.Factory)

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                vm = gameViewModel,
                                onStartClicked = { navController.navigate(ROUTE_GAME) },
                                onSettingsClicked = { navController.navigate(ROUTE_SETTINGS) },
                                onHistoryClicked = { navController.navigate(ROUTE_HISTORY) },
                            )
                        }
                        composable(ROUTE_GAME) {
                            GameScreen(
                                vm = gameViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                vm = gameViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(ROUTE_HISTORY) {
                            HistoryScreen(
                                vm = historyViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val ROUTE_HOME = "home"
        const val ROUTE_GAME = "game"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_HISTORY = "history"
    }
}
