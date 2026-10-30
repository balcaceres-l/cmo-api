package com.cmo.hospital.features.recepcionista.citas.presentation

import android.content.res.ColorStateList
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
import com.cmo.hospital.components.HucMicroBannerBinder
import com.cmo.hospital.core.domain.model.CatalogoClinico
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentRecepCitasBinding
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class CitasFragment : Fragment() {

    private var _binding: FragmentRecepCitasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CitasViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createCitasViewModel() }
    }

    private lateinit var citasAdapter: CitasRecepcionAdapter

    private var formAbierto = false
    private var especialidadSeleccionada = CatalogoClinico.CATALOGO_ESPECIALIDADES.keys.first()
    private var medicoSeleccionado =
        CatalogoClinico.CATALOGO_ESPECIALIDADES[especialidadSeleccionada]?.first().orEmpty()
    private var horaSeleccionada = "09:30"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecepCitasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        HucMicroBannerBinder.bind(
            binding = binding.microBannerCitas,
            title = "Hospital Universitario Central - Consultorios",
            subtitle = "Admisión Externa • Puesto 04 • Bloques 30 min",
            badgeText = "Puesto 04"
        )

        setupRecyclerView()
        setupLlamadorSala()
        setupFormularioCascada()
        observeState()
    }

    private fun setupRecyclerView() {
        citasAdapter = CitasRecepcionAdapter(
            onRegistrarLlegada = { cita -> viewModel.registrarLlegada(cita.id) },
            onAbrirTriaje = { cita ->
                TriajeCitaDialogFragment(cita) { signos, pasar ->
                    viewModel.guardarTriajeOPasarConsulta(cita.id, signos, pasar)
                }.show(childFragmentManager, "dialog_triaje_${cita.id}")
            },
            onPasarDirectoAConsulta = { cita ->
                viewModel.guardarTriajeOPasarConsulta(cita.id, cita.signosVitales, pasarAConsulta = true)
            },
            onCancelarCita = { cita -> viewModel.cancelarCita(cita.id) }
        )
        binding.rvCitasRecepcion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCitasRecepcion.adapter = citasAdapter
    }

    private fun setupLlamadorSala() {
        binding.btnCallerReLlamar.setOnClickListener { viewModel.reLlamarEnSala() }
        binding.btnCallerSiguiente.setOnClickListener { viewModel.llamarSiguienteEnSala() }
    }

    private fun setupFormularioCascada() {
        binding.headerToggleAgendar.setOnClickListener {
            formAbierto = !formAbierto
            binding.containerFormAgendar.visibility = if (formAbierto) View.VISIBLE else View.GONE
            binding.tvToggleAgendarLabel.text = if (formAbierto) "Cerrar" else "+ Abrir"
        }

        // 1. Poblar chips de especialidades
        binding.chipGroupEspecialidades.removeAllViews()
        CatalogoClinico.CATALOGO_ESPECIALIDADES.keys.forEach { esp ->
            val chip = createSelectableChip(esp, esp == especialidadSeleccionada)
            chip.setOnClickListener {
                especialidadSeleccionada = esp
                poblarMedicosDeEspecialidad(esp)
                actualizarChipsBloques30m()
            }
            binding.chipGroupEspecialidades.addView(chip)
        }

        poblarMedicosDeEspecialidad(especialidadSeleccionada)

        binding.etCitaFecha.doAfterTextChanged { actualizarChipsBloques30m() }
        actualizarChipsBloques30m()

        binding.btnConfirmarAgendarCita.setOnClickListener {
            viewModel.agendarCita(
                especialidad = especialidadSeleccionada,
                medicoNombre = medicoSeleccionado,
                pacienteNombre = binding.etCitaPacienteNombre.text?.toString().orEmpty(),
                dui = binding.etCitaDui.text?.toString().orEmpty(),
                fechaNacimiento = binding.etCitaFechaNac.text?.toString(),
                fecha = binding.etCitaFecha.text?.toString().orEmpty(),
                horaInicio = horaSeleccionada,
                motivo = binding.etCitaMotivo.text?.toString().orEmpty(),
                box = binding.etCitaBox.text?.toString().orEmpty(),
                onSuccess = {
                    binding.etCitaPacienteNombre.text?.clear()
                    binding.etCitaDui.text?.clear()
                    binding.etCitaFechaNac.text?.clear()
                    binding.etCitaMotivo.text?.clear()
                    formAbierto = false
                    binding.containerFormAgendar.visibility = View.GONE
                    binding.tvToggleAgendarLabel.text = "+ Abrir"
                }
            )
        }
    }

    private fun poblarMedicosDeEspecialidad(especialidad: String) {
        val doctores = CatalogoClinico.CATALOGO_ESPECIALIDADES[especialidad].orEmpty()
        medicoSeleccionado = doctores.firstOrNull().orEmpty()
        binding.chipGroupMedicos.removeAllViews()
        doctores.forEachIndexed { index, doc ->
            val chip = createSelectableChip(doc, index == 0)
            chip.setOnClickListener {
                medicoSeleccionado = doc
                actualizarChipsBloques30m()
            }
            binding.chipGroupMedicos.addView(chip)
        }
    }

    private fun actualizarChipsBloques30m() {
        val citasActuales = viewModel.citas.value
        val fecha = binding.etCitaFecha.text?.toString()?.trim().orEmpty()
        binding.chipGroupBloques30m.removeAllViews()

        // Si la hora seleccionada actual quedó ocupada, preseleccionar el primer bloque libre
        if (Cita.hayConflictoHorario(citasActuales, medicoSeleccionado, fecha, horaSeleccionada)) {
            CatalogoClinico.BLOQUES_30_MINUTOS.firstOrNull { h ->
                !Cita.hayConflictoHorario(citasActuales, medicoSeleccionado, fecha, h)
            }?.let { libre ->
                horaSeleccionada = libre
            }
        }

        val ctx = requireContext()
        CatalogoClinico.BLOQUES_30_MINUTOS.forEach { hora ->
            val ocupado = Cita.hayConflictoHorario(citasActuales, medicoSeleccionado, fecha, hora)
            val seleccionado = hora == horaSeleccionada && !ocupado
            val chip = Chip(ctx).apply {
                text = if (ocupado) "$hora ⛔" else hora
                isCheckable = !ocupado
                isChecked = seleccionado
                isEnabled = !ocupado
                textSize = 11f
                if (ocupado) {
                    chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.huc_error_container)
                    )
                    setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_error_container))
                } else if (seleccionado) {
                    chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.huc_primary_container)
                    )
                    setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary))
                } else {
                    chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.huc_surface_low)
                    )
                    setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface))
                }
                setOnClickListener {
                    if (!ocupado) {
                        horaSeleccionada = hora
                        actualizarChipsBloques30m()
                    }
                }
            }
            binding.chipGroupBloques30m.addView(chip)
        }

        val fin = Cita.calcularFin30Minutos(horaSeleccionada)
        binding.tvCitaBloqueResumen.text = "$horaSeleccionada - $fin (30 min)"

        val conflictoActual =
            Cita.hayConflictoHorario(citasActuales, medicoSeleccionado, fecha, horaSeleccionada)
        binding.tvCitaConflictoWarning.visibility =
            if (conflictoActual) View.VISIBLE else View.GONE
        binding.btnConfirmarAgendarCita.isEnabled = !conflictoActual
    }

    private fun createSelectableChip(label: String, checked: Boolean): Chip {
        val ctx = requireContext()
        return Chip(ctx).apply {
            text = label
            isCheckable = true
            isChecked = checked
            textSize = 11f
            setOnCheckedChangeListener { buttonView, isChecked ->
                buttonView.setTextColor(
                    ContextCompat.getColor(
                        ctx,
                        if (isChecked) R.color.huc_on_primary else R.color.huc_on_surface
                    )
                )
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        ctx,
                        if (isChecked) R.color.huc_primary_container else R.color.huc_surface_low
                    )
                )
            }
            // Estado inicial
            setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (checked) R.color.huc_on_primary else R.color.huc_on_surface
                )
            )
            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(
                    ctx,
                    if (checked) R.color.huc_primary_container else R.color.huc_surface_low
                )
            )
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.citas.collect { lista ->
                        citasAdapter.submitList(lista)
                        binding.tvCitasTotalBadge.text = "${lista.size} citas"
                        actualizarChipsBloques30m()
                    }
                }

                launch {
                    viewModel.turnoEnBox.collect { turno ->
                        if (turno != null) {
                            binding.tvCallerBoxBadge.text = turno.consultorio?.codigo ?: "Box 03"
                            val rawName = turno.paciente?.nombreCompleto ?: "Paciente en Box"
                            binding.tvCallerPatientAnon.text = Paciente.anonimizarNombre(rawName)
                            binding.tvCallerDetail.text =
                                "${turno.horaInicio} • ${turno.especialidad ?: "Medicina General"} • ${turno.medicoNombre ?: "Médico"}"
                        } else {
                            binding.tvCallerBoxBadge.text = "Sala Libre"
                            binding.tvCallerPatientAnon.text = "Sin turno activo"
                            binding.tvCallerDetail.text = "Presione 'Siguiente a Box' para llamar paciente"
                        }
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
