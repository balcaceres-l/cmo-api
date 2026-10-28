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
import com.cmo.hospital.ui.doctor.DoctorMainScreen
import com.cmo.hospital.ui.doctor.DoctorViewModel
import com.cmo.hospital.ui.login.LoginScreen
import com.cmo.hospital.ui.login.LoginViewModel
import com.cmo.hospital.ui.navigation.Screen
import com.cmo.hospital.ui.recepcionista.RecepcionistaMainScreen
import com.cmo.hospital.ui.recepcionista.RecepcionistaViewModel
import com.cmo.hospital.ui.theme.CMOHospitalTheme

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val doctorViewModel: DoctorViewModel by viewModels()
    private val recepcionistaViewModel: RecepcionistaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)
        val currentUser = sessionManager.getUser()
        val startDestination = if (sessionManager.isLoggedIn()) {
            when (currentUser?.rol) {
                "MEDICO" -> Screen.DoctorMain.route
                "RECEPCIONISTA" -> Screen.RecepcionistaMain.route
                else -> Screen.Login.route
            }
        } else {
            Screen.Login.route
        }

        setContent {
            CMOHospitalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        startDestination = startDestination,
                        sessionManager = sessionManager,
                        loginViewModel = loginViewModel,
                        doctorViewModel = doctorViewModel,
                        recepcionistaViewModel = recepcionistaViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    startDestination: String,
    sessionManager: SessionManager,
    loginViewModel: LoginViewModel,
    doctorViewModel: DoctorViewModel,
    recepcionistaViewModel: RecepcionistaViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { user ->
                    when (user.rol) {
                        "MEDICO" -> {
                            doctorViewModel.cargarDatos()
                            navController.navigate(Screen.DoctorMain.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                        "RECEPCIONISTA" -> {
                            recepcionistaViewModel.cargarDatos()
                            navController.navigate(Screen.RecepcionistaMain.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(Screen.DoctorMain.route) {
            DoctorMainScreen(
                viewModel = doctorViewModel,
                onLogout = {
                    sessionManager.clear()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.DoctorMain.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RecepcionistaMain.route) {
            RecepcionistaMainScreen(
                viewModel = recepcionistaViewModel,
                onLogout = {
                    sessionManager.clear()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RecepcionistaMain.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
