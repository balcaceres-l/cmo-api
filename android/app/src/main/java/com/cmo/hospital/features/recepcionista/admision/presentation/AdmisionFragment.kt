package com.cmo.hospital.features.recepcionista.admision.presentation

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.R
import com.cmo.hospital.components.HucMicroBannerBinder
import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.CapacidadHospitalaria
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentRecepAdmisionBinding
import com.cmo.hospital.features.recepcionista.citas.presentation.chipAgenda
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/** Camas: ingreso hospitalario, alta y traslado contra la API. */
class AdmisionFragment : Fragment() {

    private var _binding: FragmentRecepAdmisionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdmisionViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createAdmisionViewModel() }
    }

    private lateinit var vipBedsAdapter: VipBedsAdapter
    private lateinit var semiRoomsAdapter: SemiPrivateRoomsAdapter
    private lateinit var habitacionesAdapter: HabitacionesDetalleAdapter

    private var pacienteSeleccionado: Paciente? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecepAdmisionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        HucMicroBannerBinder.bind(
            binding = binding.microBannerAdmision,
            title = "Centro Médico de Occidente - Hospitalización",
            subtitle = "Planta 2 semiprivadas • Planta 3 VIP",
            badgeText = "17 camas"
        )

        binding.stepHeader1.ivStepIcon.setImageResource(R.drawable.ic_badge)
        binding.stepHeader1.tvStepTitle.text = "Paciente y médico tratante"
        binding.stepHeader1.tvStepBadge.text = "Paso 1 de 2"

        binding.stepHeader2.ivStepIcon.setImageResource(R.drawable.ic_hotel)
        binding.stepHeader2.tvStepTitle.text = "Cama"
        binding.stepHeader2.tvStepBadge.text = "Paso 2 de 2"

        setupTabs()
        setupCamas()
        setupHabitaciones()
        setupFormulario()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        // Otro puesto o el panel web pudo cambiar la ocupación.
        viewModel.refrescarCamas()
    }

    private fun setupCamas() {
        vipBedsAdapter = VipBedsAdapter(::alTocarCama)
        binding.rvCamasVip.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvCamasVip.adapter = vipBedsAdapter

        semiRoomsAdapter = SemiPrivateRoomsAdapter(::alTocarCama)
        binding.rvSalasSemiprivadas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSalasSemiprivadas.adapter = semiRoomsAdapter
    }

    /** Pestañas: 0 = ingreso a cama, 1 = vista de habitaciones. */
    private fun setupTabs() {
        binding.tabLayoutAdmision.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val enIngreso = tab?.position != 1
                binding.scrollTabRegistro.visibility = if (enIngreso) View.VISIBLE else View.GONE
                binding.layoutTabHabitaciones.visibility = if (enIngreso) View.GONE else View.VISIBLE
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun setupHabitaciones() {
        habitacionesAdapter = HabitacionesDetalleAdapter { cama ->
            if (cama.estaDisponible) {
                // Una cama libre se elige para el ingreso y se vuelve a la pestaña 1.
                if (viewModel.uiState.value.codigoCamaSeleccionada != cama.codigo) viewModel.alternarCama(cama)
                binding.tabLayoutAdmision.getTabAt(0)?.select()
                binding.scrollTabRegistro.post {
                    binding.scrollTabRegistro.smoothScrollTo(0, binding.cardPaso3Camas.top)
                }
            } else {
                abrirAccionesCama(cama)
            }
        }
        binding.rvHabitacionesDetalle.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHabitacionesDetalle.adapter = habitacionesAdapter

        binding.chipGroupFiltroHabTipo.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.setFiltroTipoHabitacion(
                when (checkedIds.firstOrNull()) {
                    R.id.chipFiltroHabVip -> "VIP"
                    R.id.chipFiltroHabSemi -> "Semiprivadas"
                    else -> "Todas"
                }
            )
        }
        binding.chipGroupFiltroHabOcup.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.setFiltroOcupacionHabitacion(
                when (checkedIds.firstOrNull()) {
                    R.id.chipFiltroOcupDisp -> "Disponibles"
                    R.id.chipFiltroOcupLlenas -> "Ocupadas"
                    else -> "Todas"
                }
            )
        }
        binding.etBuscarHabitacion.doAfterTextChanged { s ->
            viewModel.setQueryBusquedaHabitacion(s?.toString().orEmpty().trim())
        }
        binding.btnResetFiltrosHabitaciones.setOnClickListener {
            binding.etBuscarHabitacion.text?.clear()
            binding.chipFiltroHabTodas.isChecked = true
            binding.chipFiltroOcupTodas.isChecked = true
            viewModel.resetFiltrosHabitaciones()
        }
    }

    private fun alTocarCama(cama: Cama) {
        if (cama.estaDisponible) viewModel.alternarCama(cama) else abrirAccionesCama(cama)
    }

    private fun setupFormulario() {
        binding.etAdmPacienteBuscar.doAfterTextChanged { texto ->
            val valor = texto?.toString().orEmpty()
            if (pacienteSeleccionado != null && valor != pacienteSeleccionado!!.nombreCompleto) {
                seleccionarPaciente(null)
            }
            if (pacienteSeleccionado == null) viewModel.buscarPacientes(valor)
            binding.tilAdmPacienteBuscar.error = null
            viewModel.limpiarErrorCampo("id_paciente")
        }
        binding.etAdmisionMotivo.doAfterTextChanged {
            binding.tilAdmMotivo.error = null
            viewModel.limpiarErrorCampo("motivo")
        }
        binding.btnConfirmarIngreso.setOnClickListener {
            viewModel.ingresar(
                idPaciente = pacienteSeleccionado?.id,
                motivo = binding.etAdmisionMotivo.text?.toString().orEmpty()
            ) {
                seleccionarPaciente(null)
                binding.etAdmPacienteBuscar.text?.clear()
                binding.etAdmisionMotivo.text?.clear()
                binding.tilAdmPacienteBuscar.error = null
                binding.tilAdmMotivo.error = null
            }
        }
    }

    private fun seleccionarPaciente(paciente: Paciente?) {
        pacienteSeleccionado = paciente
        binding.tvAdmPacienteSeleccionado.text = paciente?.let {
            "✓ ${it.nombreCompleto} • Exp. ${it.numeroExpediente ?: "N/D"}" + (it.dui?.let { d -> " • DUI $d" } ?: "")
        } ?: "Ningún paciente seleccionado"
        if (paciente != null) {
            binding.etAdmPacienteBuscar.setText(paciente.nombreCompleto)
            binding.etAdmPacienteBuscar.setSelection(binding.etAdmPacienteBuscar.length())
            viewModel.limpiarBusqueda()
        }
    }

    private fun pintarPacientes(encontrados: List<Paciente>, buscando: Boolean) {
        val grupo = binding.chipGroupAdmPacientes
        grupo.removeAllViews()
        if (pacienteSeleccionado != null) return
        encontrados.forEach { p ->
            val texto = "${p.nombreCompleto} · ${p.dui ?: p.numeroExpediente ?: ""}".trimEnd(' ', '·')
            grupo.addView(chipAgenda(requireContext(), texto, false).apply {
                setOnClickListener { seleccionarPaciente(p) }
            })
        }
        val busqueda = binding.etAdmPacienteBuscar.text?.toString().orEmpty().trim()
        binding.tvAdmPacienteSeleccionado.text = when {
            buscando -> "Buscando…"
            encontrados.isEmpty() && busqueda.length >= 2 -> "Sin coincidencias. Registre al paciente en la pestaña Pacientes."
            else -> "Ningún paciente seleccionado"
        }
    }

    private fun pintarMedicos(medicos: List<User>, seleccionado: String?) {
        val grupo = binding.chipGroupAdmMedicos
        grupo.removeAllViews()
        medicos.forEach { m ->
            val texto = listOfNotNull(m.nombreCompleto, m.cargo).joinToString(" · ")
            grupo.addView(chipAgenda(requireContext(), texto, m.id == seleccionado).apply {
                setOnClickListener { m.id?.let(viewModel::seleccionarMedico) }
            })
        }
    }

    private fun abrirAccionesCama(cama: Cama) {
        val paciente = cama.paciente
        if (cama.idIngreso == null || paciente == null) {
            avisar("Cama ${cama.codigo} ocupada", "No tiene permiso para ver este ingreso.", esError = true)
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cama ${cama.codigo}")
            .setMessage("${paciente.nombreCompleto}\nExp. ${paciente.numeroExpediente ?: "N/D"}")
            .setNeutralButton("Cerrar", null)
            .setNegativeButton("Trasladar") { _, _ -> elegirDestino(cama) }
            .setPositiveButton("Dar de alta") { _, _ -> confirmarAlta(cama) }
            .show()
    }

    private fun confirmarAlta(cama: Cama) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Dar de alta")
            .setMessage("¿Registrar el alta de ${cama.paciente?.nombreCompleto} y liberar ${cama.codigo}?")
            .setNegativeButton("Volver", null)
            .setPositiveButton("Dar de alta") { _, _ -> viewModel.darAlta(cama) }
            .show()
    }

    private fun elegirDestino(origen: Cama) {
        val libres = todasLasCamas(viewModel.capacidad.value).filter { it.estaDisponible }
        if (libres.isEmpty()) {
            avisar("Sin camas libres", "No hay camas disponibles para el traslado.", esError = true)
            return
        }
        val etiquetas = libres.map { "${it.codigo} • ${if (it.esVip) "VIP, planta 3" else "Semiprivada, planta 2"}" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Trasladar desde ${origen.codigo}")
            .setItems(etiquetas.toTypedArray()) { _, i -> viewModel.trasladar(origen, libres[i]) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun todasLasCamas(cap: CapacidadHospitalaria): List<Cama> =
        cap.vipCamas + cap.salasSemiprivadas.flatMap { listOf(it.camaA, it.camaB) }

    private fun pintarCapacidad(cap: CapacidadHospitalaria, seleccion: String?) {
        binding.tvCapacidadOcupadasPill.text = "${cap.ocupadas} / ${cap.totalCamas} ocupadas"
        binding.progressHospitalCapacity.progress = cap.porcentajeOcupacion
        binding.tvCapacidadDisponibles.text = "${cap.disponibles} camas disponibles"
        binding.tvVipCountLabel.text = "${cap.vipDisponibles} disp. / ${cap.vipCamas.size}"
        binding.tvSemiCountLabel.text = "${cap.semiDisponibles} disp. / ${cap.salasSemiprivadas.size * 2}"
        vipBedsAdapter.submit(cap.vipCamas, seleccion)
        semiRoomsAdapter.submit(cap.salasSemiprivadas, seleccion)
        binding.tvSelectedBedPill.text = seleccion ?: "Ninguna"
        binding.tvHabCapacidadOcupadasPill.text = "${cap.ocupadas} ocupadas"
        binding.progressHospitalCapacityTab2.progress = cap.porcentajeOcupacion
        binding.tvHabCapacidadDisponibles.text = "${cap.disponibles} camas disponibles"
    }

    private fun avisar(titulo: String, mensaje: String, esError: Boolean) {
        val host = parentFragment as? RecepcionistaToastHost
        if (esError) host?.showToastError(titulo, mensaje) else host?.showToastSuccess(titulo, mensaje)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.capacidad.collect { cap ->
                        pintarCapacidad(cap, viewModel.uiState.value.codigoCamaSeleccionada)
                    }
                }
                launch {
                    viewModel.uiState.collect { st ->
                        pintarCapacidad(viewModel.capacidad.value, st.codigoCamaSeleccionada)
                        pintarMedicos(st.medicos, st.idMedicoSeleccionado)
                        pintarPacientes(st.pacientesEncontrados, st.buscandoPacientes)
                        binding.tvAdmCamasAviso.text = when {
                            st.cargandoCamas && viewModel.capacidad.value.totalCamas == 0 -> "Cargando camas…"
                            st.errorCamas != null -> "No se pudo cargar el mapa de camas: ${st.errorCamas}"
                            else -> "Toque una cama libre para el ingreso, o una ocupada para dar el alta o trasladar."
                        }
                        binding.btnConfirmarIngreso.isEnabled = !st.enviando
                        binding.btnConfirmarIngreso.text = if (st.enviando) "Ingresando…" else "Ingresar paciente"
                    }
                }
                launch {
                    viewModel.habitaciones.collect { todas ->
                        binding.chipFiltroHabTodas.text = "Todas (${todas.size})"
                        binding.chipFiltroHabVip.text = "VIP Individual (${todas.count { it.tipo.contains("VIP", true) }})"
                        binding.chipFiltroHabSemi.text = "Semiprivadas (${todas.count { it.tipo.contains("Semiprivada", true) }})"
                        binding.chipFiltroOcupTodas.text = "Cualquier estado (${todas.size})"
                        binding.chipFiltroOcupDisp.text = "Con disponibilidad (${todas.count { it.estaDisponible }})"
                        binding.chipFiltroOcupLlenas.text = "100% ocupadas (${todas.count { it.estaCompletamenteOcupada }})"
                    }
                }
                launch {
                    viewModel.habitacionesFiltradas.collect { lista ->
                        habitacionesAdapter.submitList(lista)
                        binding.tvHabitacionesEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvFiltrosActivosBadge.text = "${lista.size} de ${viewModel.habitaciones.value.size} habitaciones"
                        val hayFiltro = viewModel.filtroTipoHabitacion.value != "Todas" ||
                            viewModel.filtroOcupacionHabitacion.value != "Todas" ||
                            viewModel.queryBusquedaHabitacion.value.isNotBlank()
                        binding.btnResetFiltrosHabitaciones.visibility = if (hayFiltro) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.error.collect { err ->
                        binding.tvAdmFormError.visibility = if (err != null) View.VISIBLE else View.GONE
                        binding.tvAdmFormError.text = err?.mensaje
                        when (err?.campo) {
                            "id_paciente" -> binding.tilAdmPacienteBuscar.error = err.mensaje
                            "motivo" -> binding.tilAdmMotivo.error = err.mensaje
                        }
                    }
                }
                launch {
                    viewModel.evento.collect { ev ->
                        if (ev != null) {
                            avisar(ev.titulo, ev.mensaje, ev.esError)
                            viewModel.consumirEvento()
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
