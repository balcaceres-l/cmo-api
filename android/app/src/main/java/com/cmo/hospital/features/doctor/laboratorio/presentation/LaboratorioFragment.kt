package com.cmo.hospital.features.doctor.laboratorio.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.R
import com.cmo.hospital.components.HucMicroBannerBinder
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentDoctorLaboratorioBinding
import com.cmo.hospital.features.doctor.shell.presentation.DoctorShellHost
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class LaboratorioFragment : Fragment() {

    private var _binding: FragmentDoctorLaboratorioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LaboratorioViewModel by viewModels {
        simpleViewModelFactory {
            (requireActivity().application as CmoApplication).appContainer.createLaboratorioViewModel()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorLaboratorioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        HucMicroBannerBinder.bind(
            binding = binding.microBannerLab,
            title = "Hospital Universitario Central - Laboratorio LIS",
            subtitle = "Trazabilidad STAT y Rutina • Sincronización Clínica",
            badgeText = "LIS Online",
            iconRes = R.drawable.ic_science
        )

        val adapter = OrdenesLabAdapter(showPatientLine = true) { ordenId, nuevoEstado ->
            viewModel.cambiarEstado(ordenId, nuevoEstado)
        }
        binding.rvOrdenesLaboratorio.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdenesLaboratorio.adapter = adapter

        // Construir chips de filtro: Todos + 5 estados válidos
        val filtros = listOf("Todos") + OrdenLaboratorioItem.ESTADOS_VALIDOS
        filtros.forEach { estado ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = estado
                isCheckable = true
                isChecked = estado == viewModel.filtroEstado.value
                setOnCheckedChangeListener { _, checked ->
                    if (checked) viewModel.setFiltroEstado(estado)
                }
            }
            binding.chipGroupFiltroLis.addView(chip)
        }

        binding.btnIndicarExamenLis.setOnClickListener {
            val pacientes = viewModel.expedientesFlow.value.map { it.paciente }
            IndicarExamenLabDialogFragment().apply {
                pacienteFijo = null
                pacientesDisponibles = pacientes
                onConfirmarOrden = { examenes, prioridad, obs, estadoInicial, paciente ->
                    viewModel.emitirOrdenes(examenes, prioridad, obs, estadoInicial, paciente)
                }
            }.show(childFragmentManager, "dialog_indicar_lab_lis")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.ordenesFiltradas.collect { lista ->
                        adapter.submitList(lista)
                        binding.tvLabEmptyState.visibility =
                            if (lista.isEmpty()) View.VISIBLE else View.GONE
                        val statCount = lista.count { it.prioridad.equals("STAT", true) }
                        binding.tvLabResumenContador.text =
                            "${lista.size} orden(es) visibles • $statCount prioridad STAT"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
