package com.cmo.hospital.features.doctor.consulta.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.R
import com.cmo.hospital.components.LabOrderCardBinder
import com.cmo.hospital.components.VitalsGridBinder
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.EstadoSemaforo
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.CompLabOrderCardBinding
import com.cmo.hospital.databinding.FragmentDoctorConsultaBinding
import com.cmo.hospital.features.doctor.laboratorio.presentation.IndicarExamenLabDialogFragment
import com.cmo.hospital.features.doctor.shell.presentation.DoctorShellHost
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class ConsultaFragment : Fragment() {

    private var _binding: FragmentDoctorConsultaBinding? = null
    private val binding get() = _binding!!

    private var updatingAnamnesisFromState = false
    private var lastRenderedConsultaId: String? = null

    private val viewModel: ConsultaViewModel by viewModels {
        simpleViewModelFactory {
            (requireActivity().application as CmoApplication).appContainer.createConsultaViewModel()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorConsultaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recetasAdapter = RecetasConsultaAdapter { recetaId ->
            viewModel.eliminarReceta(recetaId)
        }
        binding.rvRecetasConsulta.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecetasConsulta.adapter = recetasAdapter

        binding.btnVolverAInicioMedico.setOnClickListener {
            (parentFragment as? DoctorShellHost)?.navigateToInicioTab()
        }

        binding.btnIrASeleccionarCita.setOnClickListener {
            (parentFragment as? DoctorShellHost)?.navigateToInicioTab()
        }

        binding.btnPausarReanudarReloj.setOnClickListener {
            viewModel.toggleCronometro()
        }

        binding.btnTomarAjustarSignos.setOnClickListener {
            EditarSignosDialogFragment().apply {
                signosIniciales = viewModel.consultaState.value.signosVitales
                onGuardarSignos = { nuevos -> viewModel.guardarSignos(nuevos) }
            }.show(childFragmentManager, "dialog_editar_signos_doc")
        }

        binding.btnAgregarCie10.setOnClickListener {
            AgregarCie10DialogFragment().apply {
                onSeleccionarCie10 = { codigo, desc ->
                    viewModel.agregarCie10(codigo, desc)
                }
            }.show(childFragmentManager, "dialog_agregar_cie10")
        }

        binding.etConsultaAnamnesis.doAfterTextChanged { editable ->
            if (!updatingAnamnesisFromState) {
                viewModel.actualizarAnamnesis(editable?.toString().orEmpty())
            }
        }

        binding.btnPrescribirReceta.setOnClickListener {
            AgregarRecetaDialogFragment().apply {
                onConfirmarReceta = { med, dosis, frec, dur, ind, tipo ->
                    viewModel.agregarReceta(med, dosis, frec, dur, ind, tipo)
                }
            }.show(childFragmentManager, "dialog_agregar_receta")
        }

        binding.btnConsultaIndicarLab.setOnClickListener {
            val pacienteActivo = viewModel.consultaState.value.paciente
            IndicarExamenLabDialogFragment().apply {
                pacienteFijo = pacienteActivo
                examenesDisponibles = viewModel.catalogoExamenes()
                onConfirmarOrden = { examenes, prioridad, obs, _ ->
                    viewModel.emitirOrdenLabEnConsulta(examenes, prioridad, obs)
                }
            }.show(childFragmentManager, "dialog_indicar_lab_consulta")
        }

        binding.btnConsultaAgendarControl.setOnClickListener {
            val pac = viewModel.consultaState.value.paciente
            (parentFragment as? DoctorShellHost)?.openAgendarSeguimientoDialog(pac)
        }

        binding.btnConsultaIrALis.setOnClickListener {
            (parentFragment as? DoctorShellHost)?.navigateToLaboratorioTab()
        }

        binding.btnFinalizarConsulta.setOnClickListener {
            viewModel.finalizarConsulta {
                (parentFragment as? DoctorShellHost)?.navigateToInicioTab()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.consultaState.collect { consulta ->
                        renderConsulta(consulta, recetasAdapter)
                    }
                }
                launch {
                    viewModel.isCronometroCorriendo.collect { corriendo ->
                        binding.btnPausarReanudarReloj.text =
                            if (corriendo) "Pausar" else "Reanudar"
                        binding.btnPausarReanudarReloj.setIconResource(
                            if (corriendo) R.drawable.ic_pause else R.drawable.ic_play
                        )
                    }
                }
                launch {
                    viewModel.toastEvent.collect { evt ->
                        if (evt != null) {
                            val host = parentFragment as? DoctorShellHost
                            if (evt.third) {
                                host?.showToastError(evt.first, evt.second)
                            } else {
                                host?.showToastSuccess(evt.first, evt.second)
                            }
                            viewModel.consumeToast()
                        }
                    }
                }
            }
        }
    }

    private fun renderConsulta(
        consulta: ConsultaActiva,
        recetasAdapter: RecetasConsultaAdapter
    ) {
        val ctx = requireContext()

        // 1. Semáforo de 30 minutos
        when (consulta.estadoSemaforo) {
            EstadoSemaforo.VERDE -> {
                binding.cardSemaforoConsulta.setBackgroundResource(R.drawable.bg_semaforo_verde)
                binding.tvSemaforoEstadoLabel.text = "🟢 EN TIEMPO ÓPTIMO (< 20 MIN)"
                binding.tvSemaforoEstadoLabel.setTextColor(
                    ContextCompat.getColor(ctx, R.color.huc_semaforo_verde)
                )
            }
            EstadoSemaforo.AMBAR -> {
                binding.cardSemaforoConsulta.setBackgroundResource(R.drawable.bg_semaforo_ambar)
                binding.tvSemaforoEstadoLabel.text = "🟡 PRÓXIMO A CONCLUIR (20 - 25 MIN)"
                binding.tvSemaforoEstadoLabel.setTextColor(
                    ContextCompat.getColor(ctx, R.color.huc_semaforo_ambar)
                )
            }
            EstadoSemaforo.ROJO -> {
                binding.cardSemaforoConsulta.setBackgroundResource(R.drawable.bg_semaforo_rojo)
                binding.tvSemaforoEstadoLabel.text = "🔴 TIEMPO EXCEDIDO (> 25 MIN)"
                binding.tvSemaforoEstadoLabel.setTextColor(
                    ContextCompat.getColor(ctx, R.color.huc_semaforo_rojo)
                )
            }
        }
        binding.tvConsultaBoxPill.text = "${consulta.box} • ${consulta.especialidad}"
        binding.tvCronometroReloj.text = consulta.relojFormateado
        binding.tvCronometroRestante.text =
            "Restan ${consulta.minutosRestantes} min del bloque reglamentario (30m)"

        // 2. Ficha del Paciente en Box
        val pac = consulta.paciente
        binding.tvConsExpedientePill.text = "EXP: ${pac.numeroExpediente ?: "N/D"}"
        binding.tvConsProtegidoBadge.visibility = if (pac.protegido) View.VISIBLE else View.GONE
        binding.tvConsFinalizadaBadge.visibility =
            if (consulta.finalizada) View.VISIBLE else View.GONE
        binding.tvConsPacienteNombre.text = pac.nombreCompleto

        val edadStr = if (pac.edad > 0) "${pac.edad} años" else "Edad N/D"
        val nacStr = pac.fechaNacimiento ?: "N/D"
        val duiStr = pac.dui ?: "S/D"
        binding.tvConsPacienteDetalles.text =
            "$edadStr • Nac: $nacStr • DUI: $duiStr"

        if (!pac.alergias.isNullOrBlank()) {
            binding.bannerConsAlergias.visibility = View.VISIBLE
            binding.tvConsAlergiasTexto.text = "ALERTA FARMACOLÓGICA: ${pac.alergias}"
        } else {
            binding.bannerConsAlergias.visibility = View.GONE
        }

        // 3. Signos Vitales
        val sv = consulta.signosVitales
        binding.badgeSinTriajePrevio.visibility =
            if (!sv.hasAnyData) View.VISIBLE else View.GONE
        VitalsGridBinder.bindAllSeven(
            paChip = binding.vitalConsPa,
            pulsoChip = binding.vitalConsPulso,
            tempChip = binding.vitalConsTemp,
            spo2Chip = binding.vitalConsSpo2,
            pesoChip = binding.vitalConsPeso,
            tallaChip = binding.vitalConsTalla,
            imcChip = binding.vitalConsImc,
            signos = sv
        )
        binding.tvConsSignosObs.text =
            "Observaciones: ${sv.observaciones ?: "Sin notas adicionales de triaje."}"

        // 4. CIE-10
        binding.chipGroupDiagnosticosCie10.removeAllViews()
        if (consulta.diagnosticosCie10.isEmpty()) {
            binding.tvCie10Empty.visibility = View.VISIBLE
        } else {
            binding.tvCie10Empty.visibility = View.GONE
            consulta.diagnosticosCie10.forEach { cie ->
                val chip = Chip(ctx).apply {
                    text = "${cie.codigo} • ${cie.descripcion}"
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        viewModel.eliminarCie10(cie.codigo)
                    }
                }
                binding.chipGroupDiagnosticosCie10.addView(chip)
            }
        }

        // 5. Anamnesis (evitar perder cursor al escribir; sincronizar si cambia de paciente/consulta)
        val currentText = binding.etConsultaAnamnesis.text?.toString().orEmpty()
        if (lastRenderedConsultaId != consulta.id ||
            (!binding.etConsultaAnamnesis.hasFocus() && currentText != consulta.anamnesis)
        ) {
            updatingAnamnesisFromState = true
            binding.etConsultaAnamnesis.setText(consulta.anamnesis)
            binding.etConsultaAnamnesis.setSelection(consulta.anamnesis.length)
            updatingAnamnesisFromState = false
        }
        lastRenderedConsultaId = consulta.id

        // 6. Recetas
        binding.tvRecetasEmpty.visibility =
            if (consulta.recetas.isEmpty()) View.VISIBLE else View.GONE
        recetasAdapter.submitList(consulta.recetas)

        // 7. Órdenes LIS en Consulta
        binding.containerOrdenesLabConsulta.removeAllViews()
        if (consulta.ordenesLaboratorio.isEmpty()) {
            binding.tvConsSinOrdenesLab.visibility = View.VISIBLE
        } else {
            binding.tvConsSinOrdenesLab.visibility = View.GONE
            val inflater = LayoutInflater.from(ctx)
            consulta.ordenesLaboratorio.forEach { orden ->
                val cardBinding = CompLabOrderCardBinding.inflate(
                    inflater,
                    binding.containerOrdenesLabConsulta,
                    false
                )
                LabOrderCardBinder.bind(
                    binding = cardBinding,
                    orden = orden,
                    showPatientLine = false
                ) { orden ->
                    viewModel.cancelarOrdenLab(orden)
                }
                binding.containerOrdenesLabConsulta.addView(cardBinding.root)
            }
        }

        // 8. Estado botón finalizar y aviso de consulta cerrada
        binding.cardSinConsultaAbierta.visibility =
            if (consulta.finalizada) View.VISIBLE else View.GONE
        binding.btnFinalizarConsulta.isEnabled = !consulta.finalizada
        binding.btnFinalizarConsulta.text = if (consulta.finalizada) {
            "✓ CONSULTA FINALIZADA"
        } else {
            "FINALIZAR Y GUARDAR CONSULTA"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
