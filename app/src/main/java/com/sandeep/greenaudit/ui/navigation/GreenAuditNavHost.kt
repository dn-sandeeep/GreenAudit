package com.sandeep.greenaudit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sandeep.greenaudit.ui.login.LoginScreen
import com.sandeep.greenaudit.ui.mappark.MapNewParkScreen
import com.sandeep.greenaudit.ui.parks.ParksScreen

@Composable
fun GreenAuditNavHost(navController: NavHostController = rememberNavController()) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            LaunchedEffect(isLoggedIn) {
                when (isLoggedIn) {
                    true -> navController.navigate(Routes.PARKS) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                    false -> navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                    null -> Unit
                }
            }
            SplashScreen()
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.PARKS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.PARKS) {
            ParksScreen(
                onAddPark = { navController.navigate(Routes.MAP_NEW_PARK) },
                onStartSurvey = { parkId ->
                    navController.navigate(Routes.survey(parkId, surveyId = "new"))
                },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.PARKS) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAP_NEW_PARK) {
            MapNewParkScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
