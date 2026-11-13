package com.cmo.hospital.features.recepcionista.insumos.presentation

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
import com.cmo.hospital.core.network.dto.RecursoDto
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.DialogDescargoInsumoBinding
import com.cmo.hospital.databinding.FragmentEnfermeriaInsumosBinding
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class InsumosEnfermeriaFragment : Fragment() {

    private var _binding: FragmentEnfermeriaInsumosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InsumosEnfermeriaViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createInsumosEnfermeriaViewModel() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnfermeriaInsumosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = InsumosAdapter { recurso -> abrirDialogDescargo(recurso) }
        binding.rvInsumos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInsumos.adapter = adapter

        listOf("Todos", "Medicamento", "Insumo", "Equipo").forEachIndexed { idx, cat ->
            binding.chipGroupInsumosFiltro.addView(Chip(requireContext()).apply {
                id = View.generateViewId()
                text = cat
                isCheckable = true
                isChecked = idx == 0
                setOnCheckedChangeListener { _, checked ->
                    if (checked) viewModel.onCategoriaSelected(cat)
                }
            })
        }

        binding.etBuscarInsumo.doAfterTextChanged {
            viewModel.onQueryChanged(it?.toString().orEmpty())
        }
        binding.btnRecargarInsumos.setOnClickListener { viewModel.recargar() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        adapter.submitList(state.filtrados)
                        binding.progressInsumos.visibility = if (state.cargando) View.VISIBLE else View.GONE
                        binding.tvInsumosError.visibility = if (state.error != null) View.VISIBLE else View.GONE
                        binding.tvInsumosError.text = state.error
                        binding.tvInsumosVacio.visibility =
                            if (!state.cargando && state.error == null && state.filtrados.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvInsumosResumen.text =
                            "${state.filtrados.size} recurso(s) disponibles • Descargo en piso"
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

    private fun abrirDialogDescargo(recurso: RecursoDto) {
        val dlgBinding = DialogDescargoInsumoBinding.inflate(layoutInflater)
        dlgBinding.tvDescargoTitulo.text = recurso.nombre
        dlgBinding.tvDescargoDisponible.text =
            "Disponible: ${recurso.cantidad} ${recurso.unidad} • ${recurso.ubicacion ?: "Botiquín"}"

        MaterialAlertDialogBuilder(requireContext())
            .setView(dlgBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Registrar salida") { _, _ ->
                val cantidad = dlgBinding.etDescargoCantidad.text?.toString()?.toIntOrNull() ?: 0
                val motivo = dlgBinding.etDescargoMotivo.text?.toString().orEmpty()
                viewModel.registrarSalida(recurso, cantidad, motivo)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

