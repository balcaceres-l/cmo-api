package com.cmo.hospital.features.doctor.shell.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.ui.DateUtils
import com.cmo.hospital.databinding.DialogAgendarSeguimientoBinding
import com.cmo.hospital.features.recepcionista.citas.presentation.chipAgenda
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Control de seguimiento agendado por el médico (POST /citas). Los pacientes son
 * los que ya atiende (GET /pacientes) y los horarios, sus bloques libres dentro
 * de su turno (GET /agenda/disponibilidad).
 */
class AgendarSeguimientoDialogFragment : DialogFragment() {

    var pacientePreseleccionado: Paciente? = null
    var idMedico: String? = null
    var medicoNombre: String = ""
    var especialidad: String = ""
    var onCitaAgendada: ((Cita) -> Unit)? = null

    private var pacienteSeleccionado: Paciente? = null
    private var horaSeleccionada: String? = null
    private var bloquesJob: Job? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAgendarSeguimientoBinding.inflate(LayoutInflater.from(requireContext()))
        val ctx = requireContext()
        val repo = (requireActivity().application as CmoApplication).appContainer.consultaRepository
        val appContainer = (requireActivity().application as CmoApplication).appContainer

        fun mostrarError(mensaje: String?) {
            binding.tvSegErrorInline.text = mensaje
            binding.tvSegErrorInline.visibility = if (mensaje == null) View.GONE else View.VISIBLE
        }

        binding.tvSegMedicoInfo.text = listOf(medicoNombre, especialidad)
            .filter { it.isNotBlank() }.joinToString(" • ") + " • horarios libres de su turno"
        binding.etSegFecha.setText(DateUtils.toUiDate(LocalDate.now().plusDays(7).toString()))
        binding.etSegFecha.setOnClickListener {
            DateUtils.showDatePicker(this, binding.etSegFecha.text?.toString()) { seleccionada ->
                binding.etSegFecha.setText(seleccionada)
            }
        }

        fun pintarPacientes(pacientes: List<Paciente>) {
            binding.chipGroupSegPacientes.removeAllViews()
            if (pacientes.none { it.id == pacienteSeleccionado?.id }) {
                pacienteSeleccionado = pacientes.firstOrNull { it.id == pacientePreseleccionado?.id }
                    ?: pacientes.firstOrNull()
            }
            pacientes.forEach { pac ->
                val texto = "${pac.nombreCompleto} (${pac.numeroExpediente ?: "S/E"})"
                binding.chipGroupSegPacientes.addView(chipAgenda(ctx, texto, pac.id == pacienteSeleccionado?.id).apply {
                    setOnClickListener {
                        pacienteSeleccionado = pac
                        pintarPacientes(pacientes)
                    }
                })
            }
            if (pacientes.isEmpty()) mostrarError("Aún no tiene pacientes: aparecen al atenderlos en una cita.")
        }

        fun cargarBloques() {
            val fecha = DateUtils.toIsoDate(binding.etSegFecha.text?.toString())
            val medico = idMedico
            horaSeleccionada = null
            binding.chipGroupSegBloques.removeAllViews()
            bloquesJob?.cancel()
            if (medico.isNullOrBlank()) {
                mostrarError("No se identificó al médico de la sesión. Vuelva a iniciar sesión.")
                return
            }
            bloquesJob = lifecycleScope.launch {
                repo.disponibilidad(medico, fecha).fold(
                    onSuccess = { bloques ->
                        fun pintar() {
                            binding.chipGroupSegBloques.removeAllViews()
                            bloques.forEach { b ->
                                binding.chipGroupSegBloques.addView(
                                    chipAgenda(ctx, "${b.horaInicio} - ${b.horaFin}", b.horaInicio == horaSeleccionada).apply {
                                        setOnClickListener {
                                            horaSeleccionada = b.horaInicio
                                            pintar()
                                        }
                                    }
                                )
                            }
                        }
                        horaSeleccionada = bloques.firstOrNull()?.horaInicio
                        pintar()
                        mostrarError(if (bloques.isEmpty()) "Sin horarios libres ese día: no tiene turno o su agenda está llena." else null)
                    },
                    onFailure = { mostrarError(it.message) }
                )
            }
        }

        lifecycleScope.launch {
            repo.pacientesTratados().fold(
                onSuccess = { pintarPacientes(it) },
                onFailure = { mostrarError(it.message) }
            )
        }
        cargarBloques()
        binding.etSegFecha.doAfterTextChanged { cargarBloques() }

        binding.btnSegCancelar.setOnClickListener { dismiss() }
        binding.btnSegConfirmar.setOnClickListener {
            mostrarError(null)
            binding.btnSegConfirmar.isEnabled = false
            lifecycleScope.launch {
                appContainer.agendarCitaSeguimientoDoctorUseCase(
                    idPaciente = pacienteSeleccionado?.id,
                    idMedico = idMedico,
                    fecha = DateUtils.toIsoDate(binding.etSegFecha.text?.toString()),
                    horaInicio = horaSeleccionada,
                    motivo = binding.etSegMotivo.text?.toString().orEmpty()
                ).fold(
                    onSuccess = { cita ->
                        onCitaAgendada?.invoke(cita)
                        dismiss()
                    },
                    onFailure = { err ->
                        binding.btnSegConfirmar.isEnabled = true
                        mostrarError(err.message ?: "No se pudo agendar el seguimiento.")
                        cargarBloques()
                    }
                )
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
}
