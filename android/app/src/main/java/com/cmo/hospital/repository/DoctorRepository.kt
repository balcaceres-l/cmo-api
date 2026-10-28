package com.cmo.hospital.repository

import android.content.Context
import com.cmo.hospital.data.model.Cita
import com.cmo.hospital.data.model.ConsultorioInfo
import com.cmo.hospital.data.model.DoctorSummary
import com.cmo.hospital.data.model.PacienteInfo
import com.cmo.hospital.data.remote.RetrofitClient

class DoctorRepository(private val context: Context) {
    private val apiService = RetrofitClient.getApiService(context)

    suspend fun getAgendaDelDia(): List<Cita> {
        return try {
            val response = apiService.getCitas()
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.data ?: emptyList()
            } else {
                getMockAgenda()
            }
        } catch (e: Exception) {
            getMockAgenda()
        }
    }

    suspend fun getDoctorSummary(): DoctorSummary {
        val ambulancia = try {
            val res = apiService.getAmbulancia()
            if (res.isSuccessful && res.body()?.success == true) {
                (res.body()?.data?.get("estado") as? String) ?: "DISPONIBLE"
            } else "DISPONIBLE"
        } catch (e: Exception) {
            "DISPONIBLE"
        }

        return DoctorSummary(
            citasHoy = 5,
            pacientesAtendidos = 2,
            ordenesLaboratorio = 3,
            ambulanciaEstado = ambulancia
        )
    }

    private fun getMockAgenda(): List<Cita> {
        return listOf(
            Cita(
                id = "c-101",
                fecha = "2026-10-24",
                horaInicio = "08:30",
                horaFin = "09:00",
                estado = "EN_ESPERA",
                motivo = "Control de hipertensión y revisión de exámenes",
                paciente = PacienteInfo("p-1", "María Elena Vásquez", "EXP-2026-000104", "04829103-5", "F"),
                consultorio = ConsultorioInfo("cs-1", "Consultorio 101", "C-101")
            ),
            Cita(
                id = "c-102",
                fecha = "2026-10-24",
                horaInicio = "09:15",
                horaFin = "09:45",
                estado = "AGENDADA",
                motivo = "Evaluación preoperatoria vesicular",
                paciente = PacienteInfo("p-2", "Carlos Alberto Henríquez", "EXP-2026-000108", "02391048-2", "M"),
                consultorio = ConsultorioInfo("cs-1", "Consultorio 101", "C-101")
            ),
            Cita(
                id = "c-103",
                fecha = "2026-10-24",
                horaInicio = "10:00",
                horaFin = "10:30",
                estado = "AGENDADA",
                motivo = "Consulta de medicina interna y cefalea crónica",
                paciente = PacienteInfo("p-3", "Ana Julia Flores", "EXP-2026-000115", "05192834-9", "F"),
                consultorio = ConsultorioInfo("cs-1", "Consultorio 101", "C-101")
            ),
            Cita(
                id = "c-104",
                fecha = "2026-10-24",
                horaInicio = "07:30",
                horaFin = "08:00",
                estado = "ATENDIDA",
                motivo = "Seguimiento post-alta neumonía",
                paciente = PacienteInfo("p-4", "José Ricardo Cornejo", "EXP-2026-000092", "01928374-1", "M"),
                consultorio = ConsultorioInfo("cs-1", "Consultorio 101", "C-101")
            )
        )
    }
}
