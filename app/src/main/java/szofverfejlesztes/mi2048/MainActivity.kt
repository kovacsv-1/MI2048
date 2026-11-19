package szofverfejlesztes.mi2048

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import szofverfejlesztes.mi2048.ui.AuthScreen
import szofverfejlesztes.mi2048.ui.GameScreen
import szofverfejlesztes.mi2048.ui.LeaderboardScreen
import szofverfejlesztes.mi2048.ui.MainViewModel
import szofverfejlesztes.mi2048.ui.theme._2048Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _2048Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainAppNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainAppNavHost(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "auth",
        modifier = modifier
    ) {
        composable("auth") {
            AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate("game") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("game") {
            GameScreen(
                viewModel = viewModel,
                onNavigateToLeaderboard = {
                    navController.navigate("leaderboard")
                },
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo("game") { inclusive = true }
                    }
                }
            )
        }
        composable("leaderboard") {
            LeaderboardScreen(
                viewModel = viewModel,
                onBackToGame = {
                    navController.navigateUp()
                }
            )
        }
    }
}
