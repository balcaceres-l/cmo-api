package com.cmo.hospital.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object DoctorDashboard : Screen("doctor_dashboard")
    object ReceptionDashboard : Screen("reception_dashboard")
    object DoctorMain : Screen("doctor_main")
    object RecepcionistaMain : Screen("recepcionista_main")
}
