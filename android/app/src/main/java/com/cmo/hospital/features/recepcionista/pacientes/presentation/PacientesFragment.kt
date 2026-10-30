package com.cmo.hospital.features.recepcionista.pacientes.presentation

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
import com.cmo.hospital.R
import com.cmo.hospital.components.HucMicroBannerBinder
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentRecepPacientesBinding
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PacientesFragment : Fragment() {

    private var _binding: FragmentRecepPacientesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PacientesViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createPacientesViewModel() }
    }

    private lateinit var pacientesAdapter: PacientesRecepcionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecepPacientesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        HucMicroBannerBinder.bind(
            binding = binding.microBannerPacientes,
            title = "Registro de Pacientes",
            subtitle = "Expedientes del hospital • Datos de filiación",
            badgeText = "API",
            iconRes = R.drawable.ic_person
        )

        pacientesAdapter = PacientesRecepcionAdapter(
            onEditar = { abrirFormulario(it) },
            onCodigoActivacion = { confirmarCodigo(it) }
        )
        binding.rvPacientesRecepcion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientesRecepcion.adapter = pacientesAdapter

        binding.etBuscarPaciente.doAfterTextChanged { viewModel.onQueryChanged(it?.toString().orEmpty()) }
        binding.btnNuevoPaciente.setOnClickListener { abrirFormulario(null) }
        binding.btnReintentarPacientes.setOnClickListener { viewModel.recargar() }

        observeState()
    }

    private fun abrirFormulario(paciente: Paciente?) {
        PacienteFormDialogFragment(paciente) { form, onTerminado ->
            viewModel.guardar(paciente, form, onTerminado)
        }.show(childFragmentManager, "dialog_paciente_${paciente?.id ?: "nuevo"}")
    }

    private fun confirmarCodigo(paciente: Paciente) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Código de activación")
            .setMessage("Genera un código de 6 dígitos para que ${paciente.nombreCompleto} vincule su cuenta de la app con este expediente. Vence en 24 horas.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Generar") { _, _ ->
                viewModel.generarCodigoActivacion(paciente) { codigo -> mostrarCodigo(paciente, codigo) }
            }
            .show()
    }

    private fun mostrarCodigo(paciente: Paciente, codigo: String) {
        if (_binding == null) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(codigo)
            .setMessage("Entregue este código a ${paciente.nombreCompleto}. Lo ingresará al registrarse en la app con su DUI.")
            .setPositiveButton("Listo", null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        pacientesAdapter.submitList(state.pacientes)
                        binding.progressPacientes.visibility = if (state.cargando) View.VISIBLE else View.GONE
                        binding.tvPacientesTotalBadge.text = "${state.pacientes.size} pacientes"

                        val sinResultados = !state.cargando && state.error == null && state.pacientes.isEmpty()
                        binding.tvPacientesVacio.visibility = if (sinResultados) View.VISIBLE else View.GONE
                        binding.containerErrorPacientes.visibility = if (state.error != null) View.VISIBLE else View.GONE
                        binding.tvErrorPacientes.text = state.error
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        if (event != null) {
                            val toastHost = parentFragment as? RecepcionistaToastHost
                            if (event.isError) {
                                toastHost?.showToastError(event.title, event.message)
                            } else {
                                toastHost?.showToastSuccess(event.title, event.message)
                            }
                            viewModel.consumeEvent()
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
