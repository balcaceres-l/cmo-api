package com.cmo.hospital.features.recepcionista.laboratorio.presentation

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
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentRecepLaboratorioBinding
import com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase.FiltroLaboratorio
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

/** Recepción / laboratorio: avanza las órdenes que piden los médicos (solo estados). */
class LaboratorioRecepcionFragment : Fragment() {

    private var _binding: FragmentRecepLaboratorioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LaboratorioRecepcionViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createLaboratorioRecepcionViewModel() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecepLaboratorioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        HucMicroBannerBinder.bind(
            binding = binding.microBannerLabRecep,
            title = "Laboratorio Clínico",
            subtitle = "Pendiente → En Proceso → Completado • Solo estados",
            badgeText = "API",
            iconRes = R.drawable.ic_science
        )

        val adapter = OrdenesRecepcionAdapter { viewModel.avanzar(it) }
        binding.rvOrdenesLabRecep.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrdenesLabRecep.adapter = adapter

        FiltroLaboratorio.values().forEach { filtro ->
            binding.chipGroupFiltroLabRecep.addView(Chip(requireContext()).apply {
                id = View.generateViewId()
                text = filtro.etiqueta
                isCheckable = true
                isChecked = filtro == viewModel.state.value.filtro
                setOnCheckedChangeListener { _, checked -> if (checked) viewModel.seleccionarFiltro(filtro) }
            })
        }
        binding.btnRecargarLabRecep.setOnClickListener { viewModel.recargar() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        adapter.submitList(state.ordenes)
                        adapter.procesando = state.procesando
                        binding.progressLabRecep.visibility = if (state.cargando) View.VISIBLE else View.GONE
                        binding.tvLabRecepContador.text = "${state.ordenes.size} orden(es) • ${state.filtro.etiqueta}"
                        binding.tvLabRecepError.text = state.error
                        binding.tvLabRecepError.visibility = if (state.error != null) View.VISIBLE else View.GONE
                        binding.tvLabRecepVacio.visibility =
                            if (!state.cargando && state.error == null && state.ordenes.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.toastEvent.collect { evt ->
                        if (evt != null) {
                            val host = parentFragment as? RecepcionistaToastHost
                            if (evt.third) host?.showToastError(evt.first, evt.second)
                            else host?.showToastSuccess(evt.first, evt.second)
                            viewModel.consumeToast()
                        }
                    }
                }
            }
        }
    }

    /** Al volver a la pestaña se vuelve a leer el estado: los médicos pudieron pedir órdenes nuevas. */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) viewModel.recargar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
