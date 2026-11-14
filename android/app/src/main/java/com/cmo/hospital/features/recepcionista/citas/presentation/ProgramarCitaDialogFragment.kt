package com.cmo.hospital.features.recepcionista.citas.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.DialogProgramarCitaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Elige médico, fecha y horario libre para una cita existente:
 * - [MODO_CONFIRMAR]: solicitud del paciente (SOLICITADA) → AGENDADA.
 * - [MODO_REPROGRAMAR]: cita AGENDADA o EN_ESPERA → nuevo horario.
 * Comparte el ViewModel de [CitasFragment] (su fragmento padre).
 */
class ProgramarCitaDialogFragment : DialogFragment() {

    private val viewModel: CitasViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        factoryProducer = {
            val container = (requireActivity().application as CmoApplication).appContainer
            simpleViewModelFactory { container.createCitasViewModel() }
        }
    )

    private var _binding: DialogProgramarCitaBinding? = null
    private val binding get() = _binding!!

    private val citaId: String get() = requireArguments().getString(ARG_CITA).orEmpty()
    private val modo: String get() = requireArguments().getString(ARG_MODO) ?: MODO_REPROGRAMAR

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogProgramarCitaBinding.inflate(layoutInflater)
        val cita = viewModel.citas.value.firstOrNull { it.id == citaId }
        val confirmar = modo == MODO_CONFIRMAR

        binding.tvProgramarTitulo.text = if (confirmar) "Confirmar solicitud" else "Reprogramar cita"
        binding.tvProgramarPaciente.text = buildString {
            append(cita?.paciente?.nombreCompleto ?: "Paciente")
            cita?.especialidad?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
            cita?.motivo?.takeIf { it.isNotBlank() }?.let { append(" • Motivo: $it") }
            if (!confirmar && cita != null) {
                append("\nActual: ${cita.fecha} ${cita.horaInicio} con ${cita.medicoNombre ?: "Médico por asignar"}")
            }
        }
        binding.btnProgramarGuardar.text = if (confirmar) "Confirmar" else "Reprogramar"

        val selector = SelectorAgendaController(
            binding = binding.selectorAgenda,
            fragmentManager = childFragmentManager
        ) { idMedico, fecha, idEspecialidad -> viewModel.cargarDisponibilidad(idMedico, fecha, idEspecialidad) }
        if (cita != null) {
            selector.precargar(
                idMedico = if (confirmar) null else cita.idMedico,
                fecha = if (confirmar) null else cita.fecha,
                idEspecialidad = cita.idEspecialidad,
                nombreEspecialidad = cita.especialidad
            )
        }

        binding.btnProgramarCerrar.setOnClickListener { dismiss() }
        binding.btnProgramarGuardar.setOnClickListener {
            binding.tvProgramarError.visibility = View.GONE
            selector.marcarErrorFecha(null)
            val programacion = selector.programacion()
            if (confirmar) {
                viewModel.confirmarSolicitud(citaId, programacion) { dismiss() }
            } else {
                viewModel.reprogramarCita(citaId, programacion) { dismiss() }
            }
        }

        // El diálogo no tiene vista propia de ciclo de vida: se observa con el del fragmento.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.agenda.collect { estado ->
                        val b = _binding ?: return@collect
                        selector.mostrarMedicos(estado.medicos)
                        if (estado.catalogosError != null) {
                            b.selectorAgenda.tvAgendaAviso.text = "No se pudo cargar la agenda: ${estado.catalogosError}"
                        } else {
                            selector.mostrarDisponibilidad(estado.disponibilidad)
                        }
                        b.btnProgramarGuardar.isEnabled = !estado.enviando
                    }
                }
                launch {
                    viewModel.errorProgramacion.collect { error ->
                        val b = _binding ?: return@collect
                        if (error?.origen == OrigenProgramacion.DIALOGO) {
                            if (error.campo == "id_medico" || error.campo == "id_especialidad") {
                                selector.marcarErrorMedico(error.mensaje)
                            }
                            if (error.campo == "fecha") selector.marcarErrorFecha(error.mensaje)
                            b.tvProgramarError.text = error.mensaje
                            b.tvProgramarError.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
        if (viewModel.agenda.value.medicos.isEmpty()) viewModel.cargarCatalogos()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.limpiarError(OrigenProgramacion.DIALOGO)
        // Avisa al formulario para que vuelva a pedir sus propios horarios.
        parentFragmentManager.setFragmentResult(RESULTADO_CERRADO, Bundle())
        _binding = null
    }

    companion object {
        const val MODO_CONFIRMAR = "confirmar"
        const val MODO_REPROGRAMAR = "reprogramar"
        const val RESULTADO_CERRADO = "programar_cita_cerrado"
        private const val ARG_CITA = "cita_id"
        private const val ARG_MODO = "modo"

        fun nuevo(citaId: String, modo: String) = ProgramarCitaDialogFragment().apply {
            arguments = bundleOf(ARG_CITA to citaId, ARG_MODO to modo)
        }
    }
}
