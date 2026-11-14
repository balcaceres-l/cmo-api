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
import com.cmo.hospital.core.domain.model.EspecialidadCatalogo
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.ui.DateUtils
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentRecepCitasBinding
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class CitasFragment : Fragment() {

    private var _binding: FragmentRecepCitasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CitasViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createCitasViewModel() }
    }

    private lateinit var citasAdapter: CitasRecepcionAdapter
    private lateinit var calendarioAdapter: CalendarioCitasAdapter
    private lateinit var selector: SelectorAgendaController

    private var formAbierto = false
    private var pacienteSeleccionado: Paciente? = null
    private var especialidadSeleccionadaId: String? = null
    private var especialidadSeleccionadaNombre: String? = null // null = todas
    private var actualizandoCalendarioWidget = false

    private data class EspecialidadFiltroUi(val id: String?, val nombre: String)

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

        setupTabs()
        setupRecyclerView()
        setupFiltros()
        setupCalendario()
        setupFormulario()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refrescar()
    }

    private fun setupTabs() {
        binding.tabLayoutCitasRecep.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val esCalendario = tab?.position == 1
                binding.scrollTabGestionCitas.visibility = if (esCalendario) View.GONE else View.VISIBLE
                binding.scrollTabCalendarioCitas.visibility = if (esCalendario) View.VISIBLE else View.GONE
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun setupRecyclerView() {
        citasAdapter = CitasRecepcionAdapter(
            onRegistrarLlegada = { cita -> viewModel.registrarLlegada(cita.id) },
            onAbrirTriaje = { cita -> abrirDialogoSignosVitales(cita) },
            onPasarDirectoAConsulta = { cita ->
                viewModel.guardarTriajeOPasarConsulta(cita.id, cita.signosVitales, pasarAConsulta = true)
            },
            onCancelarCita = { cita -> pedirMotivoCancelacion(cita) },
            onReprogramar = { cita -> abrirProgramacion(cita, ProgramarCitaDialogFragment.MODO_REPROGRAMAR) },
            onConfirmarSolicitud = { cita -> abrirProgramacion(cita, ProgramarCitaDialogFragment.MODO_CONFIRMAR) }
        )
        binding.rvCitasRecepcion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCitasRecepcion.adapter = citasAdapter

        calendarioAdapter = CalendarioCitasAdapter(
            onAbrirSignosVitales = { cita -> abrirDialogoSignosVitales(cita) }
        )
        binding.rvCalendarioCitas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCalendarioCitas.adapter = calendarioAdapter
    }

    private fun abrirDialogoSignosVitales(cita: Cita) {
        TriajeCitaDialogFragment(cita) { signos, pasar ->
            viewModel.guardarTriajeOPasarConsulta(cita.id, signos, pasar)
        }.show(childFragmentManager, "dialog_triaje_${cita.id}")
    }

    /** Filtros de la agenda por estado, por médico y por fecha. */
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

        binding.etFiltroFechaCita.setOnClickListener { abrirSelectorFiltroFecha() }
        binding.tilFiltroFechaCita.setOnClickListener { abrirSelectorFiltroFecha() }
        binding.btnLimpiarFiltroFechaCita.setOnClickListener {
            viewModel.limpiarFiltroFecha()
        }
    }

    /** Configuración del Módulo de Calendario con filtros por doctor y por fecha. */
    private fun setupCalendario() {
        binding.actvCalendarioDoctor.setOnItemClickListener { _, _, position, _ ->
            val elegido = binding.actvCalendarioDoctor.adapter?.getItem(position)?.toString().orEmpty()
            viewModel.setCalendarioDoctor(elegido)
        }
        binding.actvCalendarioDoctor.doAfterTextChanged {
            viewModel.setCalendarioDoctor(it?.toString().orEmpty().trim())
        }

        binding.calendarViewCitas.setOnDateChangeListener { _, year, month, dayOfMonth ->
            if (actualizandoCalendarioWidget) return@setOnDateChangeListener
            val iso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            viewModel.setCalendarioFecha(iso)
        }

        binding.etCalendarioFecha.setOnClickListener { abrirSelectorFechaCalendario() }
        binding.tilCalendarioFecha.setOnClickListener { abrirSelectorFechaCalendario() }

        binding.btnCalendarioHoy.setOnClickListener {
            val hoyIso = LocalDate.now().toString()
            viewModel.setCalendarioFecha(hoyIso)
            sincronizarCalendarViewConFecha(hoyIso)
        }

        binding.btnCalendarioTodasFechas.setOnClickListener {
            viewModel.setCalendarioFecha("")
        }
    }

    private fun abrirSelectorFiltroFecha() {
        if (childFragmentManager.findFragmentByTag("picker_filtro_fecha_cita") != null) return
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Filtrar citas por fecha")
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val iso = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
            viewModel.setFiltroFecha(iso)
        }
        picker.show(childFragmentManager, "picker_filtro_fecha_cita")
    }

    private fun abrirSelectorFechaCalendario() {
        if (childFragmentManager.findFragmentByTag("picker_calendario_fecha") != null) return
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha en el calendario")
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val iso = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
            viewModel.setCalendarioFecha(iso)
            sincronizarCalendarViewConFecha(iso)
        }
        picker.show(childFragmentManager, "picker_calendario_fecha")
    }

    private fun sincronizarCalendarViewConFecha(fechaIso: String) {
        if (fechaIso.isBlank()) return
        try {
            val ld = LocalDate.parse(DateUtils.toIsoDate(fechaIso))
            val millis = ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            actualizandoCalendarioWidget = true
            binding.calendarViewCitas.setDate(millis, true, true)
            actualizandoCalendarioWidget = false
        } catch (_: Exception) {
            actualizandoCalendarioWidget = false
        }
    }

    private fun actualizarDropdownFiltroDoctores(citas: List<Cita>) {
        val medicosApi = viewModel.agenda.value.medicos.map { it.nombreCompleto }
        val enCitas = citas.mapNotNull { c -> c.medicoNombre?.takeIf { it.isNotBlank() } }
        val opciones = listOf(CitasViewModel.TODOS_LOS_MEDICOS) + (medicosApi + enCitas).distinct().sorted()
        val array = opciones.toTypedArray()

        binding.actvFiltroDoctor.setSimpleItems(array)
        if (binding.actvFiltroDoctor.text.isNullOrBlank()) {
            binding.actvFiltroDoctor.setText(CitasViewModel.TODOS_LOS_MEDICOS, false)
        }

        binding.actvCalendarioDoctor.setSimpleItems(array)
        if (binding.actvCalendarioDoctor.text.isNullOrBlank()) {
            binding.actvCalendarioDoctor.setText(CitasViewModel.TODOS_LOS_MEDICOS, false)
        }
    }

    private fun setupFormulario() {
        binding.headerToggleAgendar.setOnClickListener { alternarFormulario(!formAbierto) }

        selector = SelectorAgendaController(
            binding = binding.selectorAgenda,
            fragmentManager = childFragmentManager
        ) { idMedico, fecha, idEspecialidad -> viewModel.cargarDisponibilidad(idMedico, fecha, idEspecialidad) }
        childFragmentManager.setFragmentResultListener(
            ProgramarCitaDialogFragment.RESULTADO_CERRADO, viewLifecycleOwner
        ) { _, _ -> viewModel.cargarDisponibilidad(selector.idMedico, selector.fecha, selector.idEspecialidad) }

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

    /** Especialidades del catálogo (con respaldo de especialidadVisible en médicos). */
    private fun opcionesEspecialidad(
        especialidades: List<EspecialidadCatalogo>,
        medicos: List<User>
    ): List<EspecialidadFiltroUi> {
        if (especialidades.isNotEmpty()) {
            return especialidades.map { EspecialidadFiltroUi(id = it.id, nombre = it.nombre) }
        }
        return medicos.mapNotNull { m ->
            m.especialidadVisible?.let { nombre -> EspecialidadFiltroUi(id = m.idEspecialidad, nombre = nombre) }
        }.distinctBy { it.nombre.lowercase() }.sortedBy { it.nombre }
    }

    private fun catalogosEspecialidades(
        especialidades: List<EspecialidadCatalogo>,
        medicos: List<User>
    ): List<EspecialidadCatalogo> {
        if (especialidades.isNotEmpty()) return especialidades
        return opcionesEspecialidad(especialidades, medicos)
            .mapNotNull { op -> op.id?.takeIf { it.isNotBlank() }?.let { EspecialidadCatalogo(id = it, nombre = op.nombre) } }
    }

    private fun pintarEspecialidades(especialidades: List<EspecialidadCatalogo>, medicos: List<User>) {
        val grupo = binding.chipGroupEspecialidades
        grupo.removeAllViews()
        val opciones = opcionesEspecialidad(especialidades, medicos)
        val sigueActiva = opciones.any {
            (especialidadSeleccionadaId != null && it.id == especialidadSeleccionadaId) ||
                (especialidadSeleccionadaNombre != null && it.nombre.equals(especialidadSeleccionadaNombre, ignoreCase = true))
        }
        if (!sigueActiva && opciones.isNotEmpty()) {
            val defecto = opciones.firstOrNull { it.nombre.equals("Medicina general", ignoreCase = true) }
                ?: opciones.first()
            especialidadSeleccionadaId = defecto.id
            especialidadSeleccionadaNombre = defecto.nombre
        }
        opciones.forEach { op ->
            val marcada = (especialidadSeleccionadaId != null && op.id == especialidadSeleccionadaId) ||
                (especialidadSeleccionadaNombre != null && op.nombre.equals(especialidadSeleccionadaNombre, ignoreCase = true))
            grupo.addView(chipAgenda(requireContext(), op.nombre, marcada).apply {
                setOnClickListener { cambiarEspecialidad(op, especialidades, medicos) }
            })
        }
    }

    private fun cambiarEspecialidad(
        opcion: EspecialidadFiltroUi?,
        especialidades: List<EspecialidadCatalogo>,
        medicos: List<User>
    ) {
        especialidadSeleccionadaId = opcion?.id
        especialidadSeleccionadaNombre = opcion?.nombre
        pintarEspecialidades(especialidades, medicos)
        selector.establecerEspecialidad(
            idEspecialidad = especialidadSeleccionadaId,
            nombreEspecialidad = especialidadSeleccionadaNombre
        )
    }

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
            .setMessage("${cita.paciente?.nombreCompleto ?: "Paciente"} • ${DateUtils.toUiDate(cita.fecha)} ${cita.horaInicio}".trim())
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
            "id_medico", "id_especialidad" -> selector.marcarErrorMedico(error.mensaje)
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
        selector.marcarErrorMedico(null)
        selector.marcarErrorFecha(null)
        viewModel.limpiarError(OrigenProgramacion.FORMULARIO)
    }

    private fun limpiarFormulario() {
        seleccionarPaciente(null)
        binding.etCitaPacienteBuscar.text?.clear()
        binding.etCitaMotivo.text?.clear()
        ocultarErroresFormulario()
        // El bloque recién usado ya no está libre.
        viewModel.cargarDisponibilidad(selector.idMedico, selector.fecha, selector.idEspecialidad)
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
                    viewModel.filtroFecha.collect { fechaFiltro ->
                        if (fechaFiltro.isBlank()) {
                            binding.etFiltroFechaCita.setText("")
                            binding.btnLimpiarFiltroFechaCita.visibility = View.GONE
                        } else {
                            binding.etFiltroFechaCita.setText(DateUtils.toUiDate(fechaFiltro))
                            binding.btnLimpiarFiltroFechaCita.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.citasCalendarioFiltradas.collect { citasCal ->
                        calendarioAdapter.submitList(citasCal)
                        binding.tvCalendarioTotalBadge.text = "${citasCal.size} citas"
                        binding.tvCalendarioEmpty.visibility = if (citasCal.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.calendarioFecha.collect { fechaCal ->
                        if (fechaCal.isBlank()) {
                            binding.etCalendarioFecha.setText("Todas las fechas")
                            binding.tvCalendarioSubtituloLista.text = "Todas las citas agendadas"
                        } else {
                            val ui = DateUtils.toUiDate(fechaCal)
                            binding.etCalendarioFecha.setText(ui)
                            binding.tvCalendarioSubtituloLista.text = "Citas agendadas para el $ui"
                        }
                    }
                }

                launch {
                    viewModel.citas.collect { lista ->
                        binding.tvCitasTotalBadge.text = "${lista.size} citas"
                        actualizarDropdownFiltroDoctores(lista)
                    }
                }

                launch {
                    var catalogosPintados: Pair<List<User>, List<EspecialidadCatalogo>>? = null
                    viewModel.agenda.collect { estado ->
                        val actuales = estado.medicos to estado.especialidades
                        if (actuales != catalogosPintados) {
                            catalogosPintados = actuales
                            pintarEspecialidades(estado.especialidades, estado.medicos)
                            selector.mostrarEspecialidades(catalogosEspecialidades(estado.especialidades, estado.medicos))
                            selector.establecerEspecialidad(
                                idEspecialidad = especialidadSeleccionadaId,
                                nombreEspecialidad = especialidadSeleccionadaNombre
                            )
                            actualizarDropdownFiltroDoctores(viewModel.citas.value)
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
