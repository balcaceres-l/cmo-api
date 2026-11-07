package com.cmo.hospital.features.recepcionista.citas.presentation

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.EstadoCita
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentRecepCitasBinding
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class CitasFragment : Fragment() {

    private var _binding: FragmentRecepCitasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CitasViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createCitasViewModel() }
    }

    private lateinit var citasAdapter: CitasRecepcionAdapter
    private lateinit var selector: SelectorAgendaController

    private var formAbierto = false
    private var pacienteSeleccionado: Paciente? = null
    private var especialidadSeleccionada: String? = null // null = todas

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

        setupRecyclerView()
        setupFiltros()
        setupFormulario()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        // Trae citas nuevas (p. ej. solicitudes hechas desde la app del paciente).
        viewModel.refrescar()
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
            onCancelarCita = { cita -> pedirMotivoCancelacion(cita) },
            onConfirmarSolicitud = { cita -> abrirProgramacion(cita, ProgramarCitaDialogFragment.MODO_CONFIRMAR) },
            onReprogramar = { cita -> abrirProgramacion(cita, ProgramarCitaDialogFragment.MODO_REPROGRAMAR) }
        )
        binding.rvCitasRecepcion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCitasRecepcion.adapter = citasAdapter
    }

    /** Filtros de la agenda por estado y por médico. */
    private fun setupFiltros() {
        val grupo = binding.chipGroupFiltroEstadoCitas
        grupo.isSingleSelection = true
        grupo.removeAllViews()
        CitasViewModel.FILTROS_ESTADO.forEach { estado ->
            grupo.addView(Chip(requireContext()).apply {
                id = View.generateViewId()
                text = estado
                isCheckable = true
                isChecked = estado == viewModel.filtroEstado.value
                textSize = 12f
                setOnClickListener { viewModel.setFiltroEstado(estado) }
            })
        }
        actualizarDropdownFiltroDoctores(emptyList())
        binding.actvFiltroDoctor.setOnItemClickListener { _, _, position, _ ->
            viewModel.setFiltroDoctor(binding.actvFiltroDoctor.adapter?.getItem(position)?.toString().orEmpty())
        }
        binding.actvFiltroDoctor.doAfterTextChanged { viewModel.setFiltroDoctor(it?.toString().orEmpty().trim()) }
    }

    private fun actualizarDropdownFiltroDoctores(citas: List<Cita>) {
        val medicosApi = viewModel.agenda.value.medicos.map { it.nombreCompleto }
        val enCitas = citas.mapNotNull { c -> c.medicoNombre?.takeIf { it.isNotBlank() && c.idMedico != null } }
        val opciones = listOf(CitasViewModel.TODOS_LOS_MEDICOS) + (medicosApi + enCitas).distinct().sorted()
        binding.actvFiltroDoctor.setSimpleItems(opciones.toTypedArray())
        if (binding.actvFiltroDoctor.text.isNullOrBlank()) {
            binding.actvFiltroDoctor.setText(CitasViewModel.TODOS_LOS_MEDICOS, false)
        }
    }

    private fun setupFormulario() {
        binding.headerToggleAgendar.setOnClickListener { alternarFormulario(!formAbierto) }

        selector = SelectorAgendaController(
            binding = binding.selectorAgenda,
            fragmentManager = childFragmentManager
        ) { idMedico, fecha -> viewModel.cargarDisponibilidad(idMedico, fecha) }
        childFragmentManager.setFragmentResultListener(
            ProgramarCitaDialogFragment.RESULTADO_CERRADO, viewLifecycleOwner
        ) { _, _ -> viewModel.cargarDisponibilidad(selector.idMedico, selector.fecha) }

        binding.etCitaPacienteBuscar.doAfterTextChanged { texto ->
            val valor = texto?.toString().orEmpty()
            // Escribir de nuevo descarta al paciente elegido.
            if (pacienteSeleccionado != null && valor != etiquetaPaciente(pacienteSeleccionado!!)) {
                seleccionarPaciente(null)
            }
            if (pacienteSeleccionado == null) viewModel.buscarPacientes(valor)
            binding.tilCitaPacienteBuscar.error = null
        }
        binding.etCitaMotivo.doAfterTextChanged { binding.tilCitaMotivo.error = null }

        binding.btnConfirmarAgendarCita.setOnClickListener {
            ocultarErroresFormulario()
            viewModel.agendarCita(
                idPaciente = pacienteSeleccionado?.id,
                programacion = selector.programacion(),
                motivo = binding.etCitaMotivo.text?.toString().orEmpty()
            ) {
                limpiarFormulario()
                alternarFormulario(false)
            }
        }
    }

    private fun alternarFormulario(abrir: Boolean) {
        formAbierto = abrir
        binding.containerFormAgendar.visibility = if (abrir) View.VISIBLE else View.GONE
        binding.tvToggleAgendarLabel.text = if (abrir) "Cerrar" else "+ Abrir"
        if (abrir && viewModel.agenda.value.medicos.isEmpty()) viewModel.cargarCatalogos()
    }

    private fun seleccionarPaciente(paciente: Paciente?) {
        pacienteSeleccionado = paciente
        binding.tvCitaPacienteSeleccionado.text = paciente?.let {
            "✓ ${it.nombreCompleto} • Exp. ${it.numeroExpediente ?: "N/D"}" +
                (it.dui?.let { dui -> " • DUI $dui" } ?: "")
        } ?: "Ningún paciente seleccionado"
        if (paciente != null) {
            binding.etCitaPacienteBuscar.setText(etiquetaPaciente(paciente))
            binding.etCitaPacienteBuscar.setSelection(binding.etCitaPacienteBuscar.length())
            viewModel.limpiarBusquedaPacientes()
        }
    }

    private fun etiquetaPaciente(p: Paciente) = p.nombreCompleto

    private fun pintarPacientes(encontrados: List<Paciente>, buscando: Boolean) {
        val grupo = binding.chipGroupPacientes
        grupo.removeAllViews()
        if (pacienteSeleccionado != null) return
        encontrados.forEach { p ->
            val texto = "${p.nombreCompleto} · ${p.dui ?: p.numeroExpediente ?: ""}".trimEnd(' ', '·')
            grupo.addView(chipAgenda(requireContext(), texto, false).apply {
                setOnClickListener { seleccionarPaciente(p) }
            })
        }
        val busqueda = binding.etCitaPacienteBuscar.text?.toString().orEmpty().trim()
        if (!buscando && encontrados.isEmpty() && busqueda.length >= 2) {
            binding.tvCitaPacienteSeleccionado.text =
                "Sin coincidencias. Registre al paciente en la pestaña Pacientes."
        } else if (encontrados.isEmpty()) {
            binding.tvCitaPacienteSeleccionado.text = if (buscando) "Buscando…" else "Ningún paciente seleccionado"
        }
    }

    /** Especialidad = cargo del médico en la API ("Medicina General", "Alergología"…). */
    private fun pintarEspecialidades(medicos: List<User>) {
        val grupo = binding.chipGroupEspecialidades
        grupo.removeAllViews()
        val especialidades = medicos.mapNotNull { it.cargo?.takeIf(String::isNotBlank) }.distinct().sorted()
        if (especialidadSeleccionada !in especialidades) especialidadSeleccionada = null
        grupo.addView(chipAgenda(requireContext(), "Todas", especialidadSeleccionada == null).apply {
            setOnClickListener { cambiarEspecialidad(null, medicos) }
        })
        especialidades.forEach { esp ->
            grupo.addView(chipAgenda(requireContext(), esp, esp == especialidadSeleccionada).apply {
                setOnClickListener { cambiarEspecialidad(esp, medicos) }
            })
        }
    }

    private fun cambiarEspecialidad(especialidad: String?, medicos: List<User>) {
        especialidadSeleccionada = especialidad
        pintarEspecialidades(medicos)
        selector.mostrarMedicos(medicosFiltrados(medicos))
    }

    private fun medicosFiltrados(medicos: List<User>) =
        especialidadSeleccionada?.let { esp -> medicos.filter { it.cargo == esp } } ?: medicos

    private fun pedirMotivoCancelacion(cita: Cita) {
        val ctx = requireContext()
        val campo = TextInputEditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText("Cancelada en mostrador")
        }
        val contenedor = FrameLayout(ctx).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(TextInputLayout(ctx).apply {
                hint = "Motivo de la cancelación"
                addView(campo)
            })
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Cancelar cita")
            .setMessage("${cita.paciente?.nombreCompleto ?: "Paciente"} • ${cita.fecha} ${cita.horaInicio}".trim())
            .setView(contenedor)
            .setNegativeButton("Volver", null)
            .setPositiveButton("Cancelar cita") { _, _ ->
                viewModel.cancelarCita(cita.id, campo.text?.toString().orEmpty())
            }
            .show()
    }

    private fun abrirProgramacion(cita: Cita, modo: String) {
        ProgramarCitaDialogFragment.nuevo(cita.id, modo)
            .show(childFragmentManager, "programar_${cita.id}")
    }

    private fun mostrarErrorFormulario(error: ErrorProgramacion) {
        when (error.campo) {
            "id_paciente" -> binding.tilCitaPacienteBuscar.error = error.mensaje
            "motivo" -> binding.tilCitaMotivo.error = error.mensaje
            "fecha" -> selector.marcarErrorFecha(error.mensaje)
        }
        binding.tvCitaFormError.text = error.mensaje
        binding.tvCitaFormError.visibility = View.VISIBLE
    }

    private fun ocultarErroresFormulario() {
        binding.tvCitaFormError.visibility = View.GONE
        binding.tilCitaPacienteBuscar.error = null
        binding.tilCitaMotivo.error = null
        selector.marcarErrorFecha(null)
        viewModel.limpiarError(OrigenProgramacion.FORMULARIO)
    }

    private fun limpiarFormulario() {
        seleccionarPaciente(null)
        binding.etCitaPacienteBuscar.text?.clear()
        binding.etCitaMotivo.text?.clear()
        ocultarErroresFormulario()
        // El bloque recién usado ya no está libre.
        viewModel.cargarDisponibilidad(selector.idMedico, selector.fecha)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.citasFiltradas.collect { filtradas ->
                        citasAdapter.submitList(filtradas)
                        binding.tvCitasEmpty.visibility = if (filtradas.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.citas.collect { lista ->
                        val solicitudes = lista.count { it.estado == EstadoCita.SOLICITADA }
                        binding.tvCitasTotalBadge.text = buildString {
                            append("${lista.size} citas")
                            if (solicitudes > 0) append(" • $solicitudes por confirmar")
                        }
                        actualizarDropdownFiltroDoctores(lista)
                    }
                }

                launch {
                    var medicosPintados: List<User>? = null
                    viewModel.agenda.collect { estado ->
                        if (estado.medicos != medicosPintados) {
                            medicosPintados = estado.medicos
                            pintarEspecialidades(estado.medicos)
                            selector.mostrarMedicos(medicosFiltrados(estado.medicos))
                        }
                        if (estado.catalogosError != null) {
                            binding.selectorAgenda.tvAgendaAviso.text =
                                "No se pudo cargar la agenda: ${estado.catalogosError}"
                        } else {
                            selector.mostrarDisponibilidad(estado.disponibilidad)
                        }
                        pintarPacientes(estado.pacientesEncontrados, estado.buscandoPacientes)
                        binding.btnConfirmarAgendarCita.isEnabled = !estado.enviando
                        binding.btnConfirmarAgendarCita.text =
                            if (estado.enviando) "Agendando…" else "Agendar cita"
                    }
                }

                launch {
                    viewModel.errorProgramacion.collect { error ->
                        if (error?.origen == OrigenProgramacion.FORMULARIO) mostrarErrorFormulario(error)
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
