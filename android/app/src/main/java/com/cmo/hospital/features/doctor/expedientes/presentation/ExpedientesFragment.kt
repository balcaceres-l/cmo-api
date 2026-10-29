package com.cmo.hospital.features.doctor.expedientes.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.components.LabOrderCardBinder
import com.cmo.hospital.components.VitalsGridBinder
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.CompLabOrderCardBinding
import com.cmo.hospital.databinding.FragmentDoctorExpedientesBinding
import com.cmo.hospital.features.doctor.laboratorio.presentation.IndicarExamenLabDialogFragment
import com.cmo.hospital.features.doctor.shell.presentation.DoctorShellHost
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class ExpedientesFragment : Fragment() {

    private var _binding: FragmentDoctorExpedientesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpedientesViewModel by viewModels {
        simpleViewModelFactory {
            (requireActivity().application as CmoApplication).appContainer.createExpedientesViewModel()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorExpedientesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val histAdapter = AtencionesHistoricasAdapter()
        binding.rvAtencionesHistoricas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAtencionesHistoricas.adapter = histAdapter

        binding.etBuscarExpediente.doAfterTextChanged { editable ->
            val currentText = editable?.toString().orEmpty()
            if (currentText != viewModel.uiState.value.query) {
                viewModel.onQueryChanged(currentText)
            }
        }

        binding.btnExpIndicarExamen.setOnClickListener {
            val exp = viewModel.uiState.value.expedienteSeleccionado ?: return@setOnClickListener
            IndicarExamenLabDialogFragment().apply {
                pacienteFijo = exp.paciente
                onConfirmarOrden = { examenes, prioridad, obs, estadoInicial, _ ->
                    viewModel.emitirOrdenDesdeExpediente(
                        paciente = exp.paciente,
                        examenes = examenes,
                        prioridad = prioridad,
                        obs = obs,
                        estadoInicial = estadoInicial
                    )
                }
            }.show(childFragmentManager, "dialog_indicar_lab_expediente")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        if (binding.etBuscarExpediente.text?.toString().orEmpty() != state.query) {
                            binding.etBuscarExpediente.setText(state.query)
                            binding.etBuscarExpediente.setSelection(state.query.length)
                        }
                        renderSelectorPacientes(state)
                        val exp = state.expedienteSeleccionado
                        if (exp == null) {
                            binding.tvExpEmptySearch.visibility = View.VISIBLE
                            binding.containerDetalleExpediente.visibility = View.GONE
                        } else {
                            binding.tvExpEmptySearch.visibility = View.GONE
                            binding.containerDetalleExpediente.visibility = View.VISIBLE
                            renderExpedienteDetalle(exp, histAdapter)
                        }
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

    private fun renderSelectorPacientes(state: ExpedientesUiState) {
        val ctx = requireContext()
        binding.chipGroupPacientesExpediente.removeAllViews()
        val selNum = state.expedienteSeleccionado?.paciente?.numeroExpediente
        state.resultados.forEach { exp ->
            val numExp = exp.paciente.numeroExpediente
            val chip = Chip(ctx).apply {
                id = View.generateViewId()
                text = "${exp.paciente.nombreCompleto} (${numExp ?: "S/E"})"
                isCheckable = true
                isChecked = numExp == selNum
                setOnClickListener {
                    viewModel.selectExpediente(numExp)
                }
            }
            binding.chipGroupPacientesExpediente.addView(chip)
        }
    }

    private fun renderExpedienteDetalle(
        exp: ExpedienteDetalle,
        histAdapter: AtencionesHistoricasAdapter
    ) {
        val pac = exp.paciente
        binding.tvExpNumeroBadge.text = "EXP: ${pac.numeroExpediente ?: "N/D"}"
        binding.tvExpVipBadge.visibility = if (pac.protegido) View.VISIBLE else View.GONE
        binding.tvExpPacienteNombre.text = pac.nombreCompleto

        val edadStr = if (pac.edad > 0) "${pac.edad} años" else "Edad N/D"
        val dniStr = pac.dui ?: "S/D"
        val prevStr = pac.prevision ?: "Fonasa Tramo D"
        val sangreStr = pac.tipoSangre ?: "O+"
        binding.tvExpPacienteMeta.text =
            "$edadStr • DNI: $dniStr • Sangre: $sangreStr • $prevStr"
        binding.tvExpCamaBadge.text = "Ubicación: ${exp.cama ?: "Consulta Ambulatoria"}"

        val todasAlergias = (exp.alergiasCriticas + listOfNotNull(pac.alergias))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (todasAlergias.isNotEmpty()) {
            binding.bannerAlertaAlergias.visibility = View.VISIBLE
            binding.tvExpAlergiasDetalle.text = todasAlergias.joinToString(" • ")
        } else {
            binding.bannerAlertaAlergias.visibility = View.GONE
        }

        // Comorbilidades
        binding.chipGroupComorbilidades.removeAllViews()
        val comorbilidades = exp.comorbilidades.ifEmpty { listOf("Sin comorbilidades crónicas registradas") }
        comorbilidades.forEach { item ->
            val chip = Chip(requireContext()).apply {
                text = item
                isClickable = false
                isCheckable = false
            }
            binding.chipGroupComorbilidades.addView(chip)
        }

        // 7 Chips de Signos Vitales
        VitalsGridBinder.bindAllSeven(
            paChip = binding.vitalExpPa,
            pulsoChip = binding.vitalExpPulso,
            tempChip = binding.vitalExpTemp,
            spo2Chip = binding.vitalExpSpo2,
            pesoChip = binding.vitalExpPeso,
            tallaChip = binding.vitalExpTalla,
            imcChip = binding.vitalExpImc,
            signos = exp.signosVitales
        )

        // Órdenes de Laboratorio con cambio de estado inline
        binding.containerOrdenesExpediente.removeAllViews()
        if (exp.ordenesLaboratorio.isEmpty()) {
            binding.tvExpSinOrdenesLab.visibility = View.VISIBLE
        } else {
            binding.tvExpSinOrdenesLab.visibility = View.GONE
            val inflater = LayoutInflater.from(requireContext())
            exp.ordenesLaboratorio.forEach { orden ->
                val cardBinding = CompLabOrderCardBinding.inflate(
                    inflater,
                    binding.containerOrdenesExpediente,
                    false
                )
                LabOrderCardBinder.bind(
                    binding = cardBinding,
                    orden = orden,
                    showPatientLine = false
                ) { ordenId, nuevoEstado ->
                    viewModel.cambiarEstadoOrdenLab(ordenId, nuevoEstado)
                }
                binding.containerOrdenesExpediente.addView(cardBinding.root)
            }
        }

        histAdapter.submitList(exp.atencionesPrevias)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
