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
import com.cmo.hospital.databinding.DialogAgendarSeguimientoBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate

class AgendarSeguimientoDialogFragment : DialogFragment() {

    var pacientePreseleccionado: Paciente? = null
    var medicoNombre: String = "Dr. Alejandro Restrepo"
    var especialidad: String = "Medicina Interna"
    var onCitaAgendada: ((Cita) -> Unit)? = null

    private var pacienteSeleccionado: Paciente? = null
    private var horaSeleccionada: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAgendarSeguimientoBinding.inflate(LayoutInflater.from(requireContext()))
        val ctx = requireContext()
        val appContainer = (requireActivity().application as CmoApplication).appContainer

        val pacientes = appContainer.clinicalDataSource.expedientes.value
            .map { it.paciente }
            .distinctBy { it.numeroExpediente ?: it.nombreCompleto }

        val fechaDefault = LocalDate.now().plusDays(7).toString()
        binding.etSegFecha.setText(fechaDefault)
        binding.tvSegMedicoInfo.text = "$medicoNombre • $especialidad (Bloques 30m)"

        pacienteSeleccionado = pacientePreseleccionado ?: pacientes.firstOrNull()
        pacientes.forEach { pac ->
            val chip = Chip(ctx).apply {
                id = View.generateViewId()
                text = "${pac.nombreCompleto} (${pac.numeroExpediente ?: "S/E"})"
                isCheckable = true
                isChecked = pac.numeroExpediente == pacienteSeleccionado?.numeroExpediente
                setOnCheckedChangeListener { _, checked ->
                    if (checked) pacienteSeleccionado = pac
                }
            }
            binding.chipGroupSegPacientes.addView(chip)
        }

        fun renderizarBloques(fecha: String) {
            binding.chipGroupSegBloques.removeAllViews()
            val citasExistentes = appContainer.clinicalDataSource.citasState.value
            horaSeleccionada = null

            Cita.BLOQUES_30_MINUTOS.forEach { bloque ->
                val ocupado = Cita.hayConflictoHorario(
                    citasExistentes = citasExistentes,
                    medicoNombre = medicoNombre,
                    fecha = fecha.trim(),
                    horaInicio = bloque
                )
                val chip = Chip(ctx).apply {
                    id = View.generateViewId()
                    text = if (ocupado) "$bloque (Ocupado)" else "$bloque - ${Cita.calcularFin30Minutos(bloque)}"
                    isCheckable = !ocupado
                    isEnabled = !ocupado
                    if (!ocupado && horaSeleccionada == null) {
                        horaSeleccionada = bloque
                        isChecked = true
                    }
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) horaSeleccionada = bloque
                    }
                }
                binding.chipGroupSegBloques.addView(chip)
            }
        }

        renderizarBloques(fechaDefault)
        binding.etSegFecha.doAfterTextChanged { editable ->
            renderizarBloques(editable?.toString().orEmpty())
        }

        binding.btnSegCancelar.setOnClickListener { dismiss() }
        binding.btnSegConfirmar.setOnClickListener {
            val pac = pacienteSeleccionado
            if (pac == null) {
                binding.tvSegErrorInline.text = "Seleccione un paciente."
                binding.tvSegErrorInline.visibility = View.VISIBLE
                return@setOnClickListener
            }
            val fecha = binding.etSegFecha.text?.toString()?.trim().orEmpty()
            val hora = horaSeleccionada.orEmpty()
            val motivo = binding.etSegMotivo.text?.toString()?.trim().orEmpty()

            lifecycleScope.launch {
                appContainer.agendarCitaSeguimientoDoctorUseCase(
                    paciente = pac,
                    medicoNombre = medicoNombre,
                    especialidad = especialidad,
                    fecha = fecha,
                    horaInicio = hora,
                    motivo = motivo
                ).fold(
                    onSuccess = { cita ->
                        onCitaAgendada?.invoke(cita)
                        dismiss()
                    },
                    onFailure = { err ->
                        binding.tvSegErrorInline.text =
                            err.message ?: "No se pudo agendar el seguimiento."
                        binding.tvSegErrorInline.visibility = View.VISIBLE
                    }
                )
            }
        }

        return MaterialAlertDialogBuilder(ctx)
            .setView(binding.root)
            .create()
    }
}
