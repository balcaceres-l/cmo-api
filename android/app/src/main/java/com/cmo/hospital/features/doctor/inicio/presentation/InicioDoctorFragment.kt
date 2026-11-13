package com.cmo.hospital.features.doctor.inicio.presentation

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
import com.cmo.hospital.core.domain.model.EstadoSemaforo
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.ResumenTurnos
import com.cmo.hospital.core.domain.model.TurnoPersonal
import java.time.format.DateTimeFormatter
import com.cmo.hospital.databinding.DialogReporteAmbulanciaBinding
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.CatalogoAmbulancia
import com.cmo.hospital.features.recepcionista.citas.presentation.chipAgenda
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentDoctorInicioBinding
import com.cmo.hospital.features.doctor.shell.presentation.DoctorShellHost
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class InicioDoctorFragment : Fragment() {

    private var _binding: FragmentDoctorInicioBinding? = null
    private val binding get() = _binding!!

    private val ambulanciaViewModel: AmbulanciaMedicoViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createAmbulanciaMedicoViewModel() }
    }

    private val viewModel: InicioDoctorViewModel by viewModels {
        simpleViewModelFactory {
            (requireActivity().application as CmoApplication).appContainer.createInicioDoctorViewModel()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContainer = (requireActivity().application as CmoApplication).appContainer
        val activeUser = appContainer.getActiveUserUseCase()
        val nombreDoc = activeUser?.nombreCompleto ?: "Dr. Alejandro Restrepo"
        val cargoDoc = activeUser?.cargo ?: "Medicina Interna"

        HucMicroBannerBinder.bind(
            binding = binding.microBannerDoctorInicio,
            title = "Panel Médico Principal • $nombreDoc",
            subtitle = "$cargoDoc • Agenda de Citas y Pacientes",
            badgeText = "En Turno",
            iconRes = R.drawable.ic_local_hospital
        )

        val citasAdapter = CitasMedicoAdapter(
            onIniciarORetomarClick = { cita ->
                viewModel.iniciarORetomarConsulta(cita) {
                    (parentFragment as? DoctorShellHost)?.navigateToConsultaTab()
                }
            },
            onVerExpedienteClick = { cita ->
                val exp = cita.paciente?.numeroExpediente
                    ?: cita.paciente?.nombreCompleto?.let { Paciente.generarNumeroExpediente(it) }
                viewModel.prepararAperturaExpediente(exp)
                (parentFragment as? DoctorShellHost)?.navigateToExpedientesTab(exp)
            },
            onAgendarControlClick = { cita ->
                val pac = cita.paciente?.let {
                    Paciente(
                        id = it.id,
                        nombreCompleto = it.nombreCompleto,
                        numeroExpediente = it.numeroExpediente,
                        dui = it.dui,
                        sexo = it.sexo,
                        fechaNacimiento = it.fechaNacimiento,
                        alergias = it.alergias,
                        tipoSangre = it.tipoSangre,
                        protegido = it.protegido
                    )
                }
                (parentFragment as? DoctorShellHost)?.openAgendarSeguimientoDialog(pac)
            }
        )
        binding.rvCitasMedico.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCitasMedico.adapter = citasAdapter

        val pacientesAdapter = PacientesMedicoAdapter(
            onAbrirExpedienteClick = { paciente ->
                viewModel.prepararAperturaExpediente(paciente.numeroExpediente)
                (parentFragment as? DoctorShellHost)?.navigateToExpedientesTab(paciente.numeroExpediente)
            },
            onAgendarControlClick = { paciente ->
                (parentFragment as? DoctorShellHost)?.openAgendarSeguimientoDialog(paciente)
            }
        )
        binding.rvPacientesMedico.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientesMedico.adapter = pacientesAdapter

        // Chips de filtro de citas
        listOf("Todas", "Pendientes", "En Atención", "Atendidas").forEachIndexed { idx, filtro ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = filtro
                isCheckable = true
                isChecked = idx == 0
                setOnCheckedChangeListener { _, checked ->
                    if (checked) viewModel.seleccionarFiltroCitas(filtro)
                }
            }
            binding.chipGroupFiltroCitasDoc.addView(chip)
        }

        binding.btnSegmentoMisCitas.setOnClickListener {
            viewModel.seleccionarSegmento(SegmentoInicioMedico.MIS_CITAS)
        }
        binding.btnSegmentoMisPacientes.setOnClickListener {
            viewModel.seleccionarSegmento(SegmentoInicioMedico.MIS_PACIENTES)
        }

        binding.etBuscarMiPaciente.doAfterTextChanged { editable ->
            viewModel.actualizarBusquedaPacientes(editable?.toString().orEmpty())
        }

        binding.btnVolverAConsultaActiva.setOnClickListener {
            (parentFragment as? DoctorShellHost)?.navigateToConsultaTab()
        }

        binding.btnInicioAgendarControl.setOnClickListener {
            (parentFragment as? DoctorShellHost)?.openAgendarSeguimientoDialog(null)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderUiState(state, citasAdapter, pacientesAdapter)
                    }
                }
                launch {
                    viewModel.turnos.collect { res -> renderTurnos(res) }
                }
                launch {
                    ambulanciaViewModel.estado.collect { renderAmbulancia(it) }
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

    private fun renderAmbulancia(st: AmbulanciaMedicoUiState) {
        val amb = st.ambulancia
        val srv = amb?.servicioActivo
        binding.tvAmbMedEstado.text = amb?.let { CatalogoAmbulancia.etiqueta(it.estado) } ?: "—"
        binding.tvAmbMedDetalle.text = when {
            amb == null -> st.error?.let { "No se pudo cargar: $it" } ?: "Cargando…"
            srv == null -> "Sin traslados en curso."
            else -> buildString {
                append("Prioridad ${CatalogoAmbulancia.etiqueta(srv.prioridad).lowercase()} • salió ${horaLocal(srv.horaSalida)}")
                val r = srv.ultimoReporte
                if (r == null) {
                    append("\nSin reportes del equipo en ruta.")
                } else {
                    append("\nÚltimo reporte ${horaLocal(r.fecha)} • ${CatalogoAmbulancia.etiqueta(r.estadoPaciente)}")
                    r.llegadaEstimada?.let { append(" • llega ${horaLocal(it)}") }
                    r.observaciones?.takeIf { it.isNotBlank() }?.let { append("\n$it") }
                }
            }
        }
        binding.tvAmbMedEnVivo.text = when {
            st.enVivo && st.reportesEnVivo -> "● En vivo (estado y reportes)"
            st.enVivo -> "● En vivo (estado) • reportes al actualizar"
            else -> "○ Sin conexión en vivo: se actualiza al volver a la pantalla"
        }
        binding.btnAmbMedReportar.visibility = if (srv != null) View.VISIBLE else View.GONE
        binding.btnAmbMedReportar.setOnClickListener { srv?.let { abrirReporteAmbulancia(it.id) } }
        st.resultadoReporte?.let { res ->
            val host = parentFragment as? DoctorShellHost
            res.fold(
                onSuccess = { host?.showToastSuccess("Reporte enviado", it) },
                onFailure = { host?.showToastError("No se envió el reporte", it.message ?: "Intente de nuevo.") }
            )
            ambulanciaViewModel.consumirResultado()
        }
    }

    /** Mismo formulario que usa recepción (estado del paciente, minutos y observaciones). */
    private fun abrirReporteAmbulancia(idServicio: String) {
        val ctx = requireContext()
        val b = DialogReporteAmbulanciaBinding.inflate(layoutInflater)
        var estadoElegido: String? = null
        fun pintar() {
            b.chipGroupEstadoPaciente.removeAllViews()
            CatalogoAmbulancia.ESTADOS_PACIENTE.forEach { e ->
                b.chipGroupEstadoPaciente.addView(chipAgenda(ctx, CatalogoAmbulancia.etiqueta(e), e == estadoElegido).apply {
                    setOnClickListener { estadoElegido = e; pintar() }
                })
            }
        }
        pintar()
        val dialogo = MaterialAlertDialogBuilder(ctx).setView(b.root).create()
        b.btnReporteCancelar.setOnClickListener { dialogo.dismiss() }
        b.btnReporteEnviar.setOnClickListener {
            if (estadoElegido == null) {
                b.tvReporteError.text = "Seleccione el estado del paciente."
                b.tvReporteError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            ambulanciaViewModel.enviarReporte(
                idServicio, estadoElegido, b.etReporteObservaciones.text?.toString(), b.etReporteMinutos.text?.toString().orEmpty()
            )
            dialogo.dismiss()
        }
        dialogo.show()
    }

    private fun horaLocal(iso: String?): String = iso?.let {
        runCatching {
            java.time.OffsetDateTime.parse(it).atZoneSameInstant(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull()
    } ?: "—"

    private fun renderTurnos(res: Result<ResumenTurnos>?) {
        val formato = DateTimeFormatter.ofPattern("EEE d MMM", Locale("es"))
        fun texto(t: TurnoPersonal) =
            "${t.fecha.format(formato)} • ${t.horaInicio}–${t.horaFin}${t.area?.let { " • $it" }.orEmpty()}"
        val resumen = res?.getOrNull()
        binding.tvTurnoActual.text = when {
            res == null -> "Cargando turnos…"
            resumen == null -> "No se pudieron cargar los turnos: ${res.exceptionOrNull()?.message}"
            resumen.actual != null -> "● En turno ahora: ${texto(resumen.actual)}"
            else -> "Sin turno en este momento: solo puede abrir consultas durante su turno."
        }
        binding.tvTurnosProximos.text = when {
            resumen == null -> ""
            resumen.proximos.isEmpty() -> "No tiene turnos próximos registrados."
            else -> "Próximos:\n" + resumen.proximos.joinToString("\n") { texto(it) }
        }
    }

    private fun renderUiState(
        state: InicioDoctorUiState,
        citasAdapter: CitasMedicoAdapter,
        pacientesAdapter: PacientesMedicoAdapter
    ) {
        val ctx = requireContext()
        binding.tvKpiDocPendientes.text = state.kpiPendientes.toString()
        binding.tvKpiDocEnBox.text = state.kpiEnBox.toString()
        binding.tvKpiDocAtendidas.text = state.kpiAtendidas.toString()

        // Banner Consulta Activa en Curso
        val activa = state.consultaEnCurso
        if (activa != null) {
            binding.cardConsultaEnCursoInicio.visibility = View.VISIBLE
            when (activa.estadoSemaforo) {
                EstadoSemaforo.VERDE -> {
                    binding.cardConsultaEnCursoInicio.setBackgroundResource(R.drawable.bg_semaforo_verde)
                    binding.tvBannerActivaEstado.text =
                        "🟢 CONSULTA EN CURSO • ${activa.relojFormateado}"
                    binding.tvBannerActivaEstado.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_semaforo_verde)
                    )
                }
                EstadoSemaforo.AMBAR -> {
                    binding.cardConsultaEnCursoInicio.setBackgroundResource(R.drawable.bg_semaforo_ambar)
                    binding.tvBannerActivaEstado.text =
                        "🟡 CONSULTA EN CURSO • ${activa.relojFormateado}"
                    binding.tvBannerActivaEstado.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_semaforo_ambar)
                    )
                }
                EstadoSemaforo.ROJO -> {
                    binding.cardConsultaEnCursoInicio.setBackgroundResource(R.drawable.bg_semaforo_rojo)
                    binding.tvBannerActivaEstado.text =
                        "🔴 TIEMPO EXCEDIDO • ${activa.relojFormateado}"
                    binding.tvBannerActivaEstado.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_semaforo_rojo)
                    )
                }
            }
            binding.tvBannerActivaBox.text = activa.box
            binding.tvBannerActivaPaciente.text = activa.paciente.nombreCompleto
            binding.tvBannerActivaMeta.text =
                "EXP: ${activa.paciente.numeroExpediente ?: "N/D"} • Restan ${activa.minutosRestantes} min"
        } else {
            binding.cardConsultaEnCursoInicio.visibility = View.GONE
        }

        // Botones Segmentados con contadores
        binding.btnSegmentoMisCitas.text = "Mis Citas (${state.totalCitas})"
        binding.btnSegmentoMisPacientes.text = "Mis Pacientes (${state.totalPacientes})"

        val viendoCitas = state.segmentoActivo == SegmentoInicioMedico.MIS_CITAS
        binding.tvInicioSeccionTitulo.text =
            if (viendoCitas) "Mis Citas Asignadas" else "Todos Mis Pacientes"

        binding.btnSegmentoMisCitas.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                ctx,
                if (viendoCitas) R.color.huc_primary_container else R.color.huc_transparent
            )
        )
        binding.btnSegmentoMisCitas.setTextColor(
            ContextCompat.getColor(
                ctx,
                if (viendoCitas) R.color.huc_on_primary else R.color.huc_on_surface_variant
            )
        )

        binding.btnSegmentoMisPacientes.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                ctx,
                if (!viendoCitas) R.color.huc_primary_container else R.color.huc_transparent
            )
        )
        binding.btnSegmentoMisPacientes.setTextColor(
            ContextCompat.getColor(
                ctx,
                if (!viendoCitas) R.color.huc_on_primary else R.color.huc_on_surface_variant
            )
        )

        binding.scrollFiltrosCitasDoc.visibility = if (viendoCitas) View.VISIBLE else View.GONE
        binding.sectionListaMisCitas.visibility = if (viendoCitas) View.VISIBLE else View.GONE
        binding.tilBuscarMiPaciente.visibility = if (!viendoCitas) View.VISIBLE else View.GONE
        binding.sectionListaMisPacientes.visibility = if (!viendoCitas) View.VISIBLE else View.GONE

        citasAdapter.submitList(state.citasFiltradas)
        binding.tvCitasMedicoEmpty.visibility =
            if (viendoCitas && state.citasFiltradas.isEmpty()) View.VISIBLE else View.GONE

        pacientesAdapter.submitList(state.pacientesFiltrados)
        binding.tvPacientesMedicoEmpty.visibility =
            if (!viendoCitas && state.pacientesFiltrados.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
