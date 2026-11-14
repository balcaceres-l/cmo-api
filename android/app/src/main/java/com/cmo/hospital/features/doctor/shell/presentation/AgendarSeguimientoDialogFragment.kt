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
import com.cmo.hospital.core.domain.model.EspecialidadCatalogo
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
    private var especialidadSeleccionada: EspecialidadCatalogo? = null
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
            .filter { it.isNotBlank() }.joinToString(" • ") + " • agenda por especialidad"
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
            val espId = especialidadSeleccionada?.id
            horaSeleccionada = null
            binding.chipGroupSegBloques.removeAllViews()
            bloquesJob?.cancel()
            if (espId.isNullOrBlank()) {
                return
            }
            bloquesJob = lifecycleScope.launch {
                repo.disponibilidad(idMedico = null, fecha = fecha, idEspecialidad = espId).fold(
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
                        mostrarError(if (bloques.isEmpty()) "Sin horarios libres ese día para la especialidad seleccionada." else null)
                    },
                    onFailure = { mostrarError(it.message) }
                )
            }
        }

        fun pintarEspecialidades(lista: List<EspecialidadCatalogo>) {
            binding.chipGroupSegEspecialidades.removeAllViews()
            if (lista.none { it.id == especialidadSeleccionada?.id }) {
                especialidadSeleccionada = lista.firstOrNull {
                    !especialidad.isNullOrBlank() && it.nombre.equals(especialidad, ignoreCase = true)
                } ?: lista.firstOrNull {
                    it.nombre.contains("general", ignoreCase = true)
                } ?: lista.firstOrNull()
            }
            lista.forEach { esp ->
                binding.chipGroupSegEspecialidades.addView(
                    chipAgenda(ctx, esp.nombre, esp.id == especialidadSeleccionada?.id).apply {
                        setOnClickListener {
                            especialidadSeleccionada = esp
                            pintarEspecialidades(lista)
                            cargarBloques()
                        }
                    }
                )
            }
            if (lista.isEmpty()) mostrarError("No hay especialidades activas disponibles.")
        }

        lifecycleScope.launch {
            repo.pacientesTratados().fold(
                onSuccess = { pintarPacientes(it) },
                onFailure = { mostrarError(it.message) }
            )
        }
        lifecycleScope.launch {
            repo.especialidades().fold(
                onSuccess = {
                    pintarEspecialidades(it)
                    cargarBloques()
                },
                onFailure = { mostrarError(it.message) }
            )
        }
        binding.etSegFecha.doAfterTextChanged { cargarBloques() }

        binding.btnSegCancelar.setOnClickListener { dismiss() }
        binding.btnSegConfirmar.setOnClickListener {
            mostrarError(null)
            binding.btnSegConfirmar.isEnabled = false
            lifecycleScope.launch {
                appContainer.agendarCitaSeguimientoDoctorUseCase(
                    idPaciente = pacienteSeleccionado?.id,
                    idMedico = null,
                    idEspecialidad = especialidadSeleccionada?.id,
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
