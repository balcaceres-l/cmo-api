package com.cmo.hospital.features.doctor.shell.presentation

import com.cmo.hospital.core.domain.model.Paciente

interface DoctorShellHost {
    fun showToastSuccess(title: String, message: String)
    fun showToastError(title: String, message: String)
    fun navigateToInicioTab()
    fun navigateToConsultaTab()
    fun navigateToExpedientesTab(numeroExpediente: String? = null)
    fun navigateToLaboratorioTab()
    fun openAgendarSeguimientoDialog(pacientePreseleccionado: Paciente? = null)
}
