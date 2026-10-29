package com.cmo.hospital.features.recepcionista.ambulancia.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import com.cmo.hospital.databinding.FragmentRecepAmbulanciaBinding
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class AmbulanciaGuardiaFragment : Fragment() {

    private var _binding: FragmentRecepAmbulanciaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AmbulanciaGuardiaViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createAmbulanciaGuardiaViewModel() }
    }

    private lateinit var personalAdapter: PersonalGuardiaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecepAmbulanciaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        HucMicroBannerBinder.bind(
            binding = binding.microBannerAmbulancia,
            title = "Hospital Universitario Central - Guardia & Flota",
            subtitle = "Coordinación Prehospitalaria Alfa-1 y Dotación en Turno",
            badgeText = "24/7 Activo",
            iconRes = R.drawable.ic_ambulance
        )

        personalAdapter = PersonalGuardiaAdapter()
        binding.rvPersonalGuardia.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPersonalGuardia.adapter = personalAdapter

        binding.btnAmbDisponible.setOnClickListener {
            viewModel.cambiarEstadoAmbulancia("DISPONIBLE")
        }
        binding.btnAmbEnServicio.setOnClickListener {
            viewModel.cambiarEstadoAmbulancia("EN_SERVICIO")
        }
        binding.btnAmbMantenimiento.setOnClickListener {
            viewModel.cambiarEstadoAmbulancia("EN_MANTENIMIENTO")
        }

        binding.btnAbrirModalDespacho.setOnClickListener {
            DespachoAmbulanciaDialogFragment { dir, pac, desc ->
                viewModel.despacharEmergencia(dir, pac, desc)
            }.show(childFragmentManager, "dialog_despacho_alfa1")
        }

        binding.chipGuardiaTodos.setOnClickListener { viewModel.setFiltroRol("TODOS") }
        binding.chipGuardiaMedicos.setOnClickListener { viewModel.setFiltroRol("MEDICO") }
        binding.chipGuardiaRecepcion.setOnClickListener { viewModel.setFiltroRol("RECEPCIONISTA") }

        observeState()
    }

    private fun styleStatusButton(btn: MaterialButton, selected: Boolean) {
        val ctx = requireContext()
        if (selected) {
            btn.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary_container))
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary))
        } else {
            btn.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_surface_lowest))
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary_container))
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.ambulancia.collect { amb ->
                        val ctx = requireContext()
                        binding.tvAmbulanciaPlaca.text = "Placa Oficial: ${amb.placa} • UCI Móvil"
                        binding.tvAmbulanciaEstadoPill.text = amb.estado
                        when (amb.estado) {
                            "DISPONIBLE" -> {
                                binding.tvAmbulanciaEstadoPill.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                                binding.tvAmbulanciaEstadoPill.setTextColor(
                                    ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed)
                                )
                            }
                            "EN_SERVICIO" -> {
                                binding.tvAmbulanciaEstadoPill.setBackgroundResource(R.drawable.bg_pill_error_container)
                                binding.tvAmbulanciaEstadoPill.setTextColor(
                                    ContextCompat.getColor(ctx, R.color.huc_on_error_container)
                                )
                            }
                            else -> {
                                binding.tvAmbulanciaEstadoPill.setBackgroundResource(R.drawable.bg_pill_tertiary_fixed)
                                binding.tvAmbulanciaEstadoPill.setTextColor(
                                    ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed)
                                )
                            }
                        }

                        styleStatusButton(binding.btnAmbDisponible, amb.estado == "DISPONIBLE")
                        styleStatusButton(binding.btnAmbEnServicio, amb.estado == "EN_SERVICIO")
                        styleStatusButton(binding.btnAmbMantenimiento, amb.estado == "EN_MANTENIMIENTO")

                        val srv = amb.servicioActivo
                        if (amb.estado == "EN_SERVICIO" && srv != null) {
                            binding.cardMisionActiva.visibility = View.VISIBLE
                            binding.tvMisionEta.text = "ETA: ${srv.minutosLlegada ?: 15} min"
                            binding.tvMisionPacienteYDesc.text =
                                "${srv.pacienteNombre ?: "Paciente"} • ${srv.descripcion ?: "Traslado urgente"}"
                            binding.tvMisionDireccionYDestino.text =
                                "Origen: ${srv.direccion ?: "N/D"} → ${srv.destino ?: "Urgencias HUC"}"
                            binding.tvMisionTripulacion.text =
                                "Tripulación: ${srv.tripulacionParamedico ?: "M. Peralta"} • ${srv.tripulacionChofer ?: "G. Soto"}"
                        } else {
                            binding.cardMisionActiva.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.personalFiltrado.collect { lista ->
                        personalAdapter.submitList(lista)
                    }
                }

                launch {
                    viewModel.toastEvent.collect { ev ->
                        if (ev != null) {
                            val host = parentFragment as? RecepcionistaToastHost
                            if (ev.third) host?.showToastError(ev.first, ev.second)
                            else host?.showToastSuccess(ev.first, ev.second)
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
