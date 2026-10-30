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
            title = "Laboratorio Clínico",
            subtitle = "Pendiente → En Proceso → Completado • Solo estados",
            badgeText = "API",
            iconRes = R.drawable.ic_science
        )

        val adapter = OrdenesLabAdapter(showPatientLine = true) { orden ->
            viewModel.cancelarOrden(orden)
        }
        binding.rvOrdenesLaboratorio.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdenesLaboratorio.adapter = adapter

        // Chips de filtro: Todos + los estados del laboratorio
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
            val consulta = viewModel.consultaAbierta()
            if (consulta == null) {
                (parentFragment as? DoctorShellHost)?.showToastError(
                    "Sin consulta abierta",
                    "Las órdenes de laboratorio se emiten desde la consulta del paciente. Inicie una consulta en Mis Citas."
                )
                return@setOnClickListener
            }
            IndicarExamenLabDialogFragment().apply {
                pacienteFijo = consulta.paciente
                examenesDisponibles = viewModel.catalogoExamenes()
                onConfirmarOrden = { examenes, prioridad, obs, _ ->
                    viewModel.emitirOrdenes(examenes, prioridad, obs)
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
                        // Tocar el contador vuelve a consultar el estado en el laboratorio.
                        binding.tvLabResumenContador.setOnClickListener { viewModel.refrescar() }
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
