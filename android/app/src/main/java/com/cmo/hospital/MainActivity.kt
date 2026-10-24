package com.cmo.hospital

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cmo.hospital.data.local.SessionManager
import com.cmo.hospital.ui.dashboard.DoctorDashboardScreen
import com.cmo.hospital.ui.dashboard.DoctorDashboardViewModel
import com.cmo.hospital.ui.login.LoginScreen
import com.cmo.hospital.ui.login.LoginViewModel
import com.cmo.hospital.ui.navigation.Screen
import com.cmo.hospital.ui.theme.CMOHospitalTheme

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val doctorDashboardViewModel: DoctorDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)
        val currentUser = sessionManager.getUser()
        val startDestination = if (sessionManager.isLoggedIn() && currentUser?.rol == "MEDICO") {
            Screen.DoctorDashboard.route
        } else {
            Screen.Login.route
        }

        setContent {
            CMOHospitalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        startDestination = startDestination,
                        loginViewModel = loginViewModel,
                        doctorDashboardViewModel = doctorDashboardViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    startDestination: String,
    loginViewModel: LoginViewModel,
    doctorDashboardViewModel: DoctorDashboardViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { user ->
                    if (user.rol == "MEDICO") {
                        doctorDashboardViewModel.loadDashboard()
                        navController.navigate(Screen.DoctorDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        // Recepción o personal
                        navController.navigate(Screen.DoctorDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.DoctorDashboard.route) {
            DoctorDashboardScreen(
                viewModel = doctorDashboardViewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.DoctorDashboard.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
