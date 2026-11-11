package com.cmo.hospital.features.recepcionista.admision.presentation

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import com.cmo.hospital.core.ui.DateUtils
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentRecepAdmisionBinding
import com.cmo.hospital.features.recepcionista.citas.presentation.chipAgenda
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteForm
import com.cmo.hospital.features.recepcionista.pacientes.domain.usecase.GuardarPacienteUseCase
import com.cmo.hospital.features.recepcionista.pacientes.presentation.DuiTextWatcher
import com.cmo.hospital.features.recepcionista.pacientes.presentation.PacienteFormDialogFragment
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import java.time.LocalDate
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
    private var listaPacientesActual: List<Paciente> = emptyList()

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
            subtitle = "Piso 3 VIP Individual • Piso 2 Semiprivadas",
            badgeText = "11 hab. / 17 camas"
        )

        binding.stepHeader1.ivStepIcon.setImageResource(R.drawable.ic_badge)
        binding.stepHeader1.tvStepTitle.text = "Paciente y médico tratante"
        binding.stepHeader1.tvStepBadge.text = "Paso 1 de 2"

        binding.stepHeader2.ivStepIcon.setImageResource(R.drawable.ic_single_bed)
        binding.stepHeader2.tvStepTitle.text = "Mapa de Habitaciones por Piso"
        binding.stepHeader2.tvStepBadge.text = "Paso 2 de 2"

        setupTabs()
        setupCamas()
        setupHabitaciones()
        setupFormulario()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refrescarCamas()
    }

    private fun setupCamas() {
        vipBedsAdapter = VipBedsAdapter(::alTocarCama)
        binding.rvCamasVip.layoutManager = GridLayoutManager(requireContext(), 2)
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
        habitacionesAdapter = HabitacionesDetalleAdapter(
            onAsignarCama = { cama ->
                if (cama.estaDisponible) {
                    if (viewModel.uiState.value.codigoCamaSeleccionada != cama.codigo) {
                        viewModel.alternarCama(cama)
                    }
                    binding.tabLayoutAdmision.getTabAt(0)?.select()
                    binding.scrollTabRegistro.post {
                        binding.scrollTabRegistro.smoothScrollTo(0, binding.cardPaso3Camas.top)
                    }
                } else {
                    abrirAccionesCama(cama)
                }
            },
            onGestionarCamaOcupada = { cama ->
                abrirAccionesCama(cama)
            }
        )
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

    private fun esModoNuevoPaciente(): Boolean =
        binding.toggleModoPacienteAdm.checkedButtonId == R.id.btnModoPacienteNuevo

    private fun tipoDocNuevoPaciente(): String =
        if (binding.toggleAdmNuevoTipoDoc.checkedButtonId == R.id.btnAdmDocPasaporte) "PASAPORTE" else "DUI"

    private fun tipoDocRespNuevo(): String =
        if (binding.toggleAdmRespTipoDoc.checkedButtonId == R.id.btnAdmRespDocPasaporte) "PASAPORTE" else "DUI"

    private fun actualizarEstadoMenorNuevoPaciente() {
        val isoNac = DateUtils.toIsoDate(binding.etAdmNuevoNacimiento.text?.toString().orEmpty())
        val edad = GuardarPacienteUseCase.calcularEdad(isoNac)
        val esMenor = edad != null && edad < 18

        if (edad != null) {
            binding.tvAdmNuevoEdadBadge.visibility = View.VISIBLE
            binding.tvAdmNuevoEdadBadge.text = if (esMenor) {
                "Edad calculada: $edad años • Menor de edad (se requieren datos del responsable)"
            } else {
                "Edad calculada: $edad años • Mayor de edad"
            }
        } else {
            binding.tvAdmNuevoEdadBadge.visibility = View.GONE
        }

        if (esMenor) {
            binding.tilAdmNuevoDui.error = null
            if (!binding.etAdmNuevoDui.text.isNullOrBlank()) {
                binding.etAdmNuevoDui.setText("")
            }
            binding.etAdmNuevoDui.isEnabled = false
            binding.tilAdmNuevoDui.isEnabled = false
            binding.btnAdmDocDui.isEnabled = false
            binding.btnAdmDocPasaporte.isEnabled = false
            binding.tilAdmNuevoDui.hint = "DUI o Pasaporte (deshabilitado por ser menor)"
            binding.tilAdmNuevoDui.helperText =
                "Menor de edad: ingrese el documento en la sección Datos del Responsable."
            binding.containerAdmNuevoResponsable.visibility = View.VISIBLE
        } else {
            binding.etAdmNuevoDui.isEnabled = true
            binding.tilAdmNuevoDui.isEnabled = true
            binding.btnAdmDocDui.isEnabled = true
            binding.btnAdmDocPasaporte.isEnabled = true
            val esPasaporte = tipoDocNuevoPaciente() == "PASAPORTE"
            binding.tilAdmNuevoDui.hint = if (esPasaporte) "Pasaporte del paciente *" else "DUI del paciente *"
            binding.tilAdmNuevoDui.helperText = if (esPasaporte) {
                "Alfanumérico de 6 a 12 caracteres"
            } else {
                "Formato DUI: 00000000-0"
            }
            binding.containerAdmNuevoResponsable.visibility = View.GONE
        }
    }

    private fun setupFormulario() {
        // Modo por defecto: Elegir paciente registrado
        binding.toggleModoPacienteAdm.check(R.id.btnModoPacienteRegistrado)
        binding.toggleAdmNuevoSexo.check(R.id.btnAdmSexoF)
        binding.toggleAdmNuevoTipoDoc.check(R.id.btnAdmDocDui)
        binding.toggleAdmRespTipoDoc.check(R.id.btnAdmRespDocDui)

        binding.toggleModoPacienteAdm.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val nuevo = checkedId == R.id.btnModoPacienteNuevo
                binding.containerAdmPacienteRegistrado.visibility = if (nuevo) View.GONE else View.VISIBLE
                binding.containerAdmPacienteNuevo.visibility = if (nuevo) View.VISIBLE else View.GONE
                viewModel.limpiarError()
            }
        }

        // Catálogos desplegables en modo Nuevo Paciente
        binding.actvAdmRespParentesco.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, PacienteFormDialogFragment.OPCIONES_PARENTESCO)
        )
        binding.actvAdmRespParentesco.setOnClickListener { binding.actvAdmRespParentesco.showDropDown() }

        binding.etAdmNuevoTipoSangre.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, PacienteFormDialogFragment.OPCIONES_TIPO_SANGRE)
        )
        binding.etAdmNuevoTipoSangre.setOnClickListener { binding.etAdmNuevoTipoSangre.showDropDown() }

        // Formateo automático de DUI
        var fmtDocPac = false
        binding.etAdmNuevoDui.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (fmtDocPac || s == null || !binding.etAdmNuevoDui.isEnabled) return
                if (tipoDocNuevoPaciente() == "DUI") {
                    val f = DuiTextWatcher.formatear(s.toString())
                    if (f != s.toString()) {
                        fmtDocPac = true
                        s.replace(0, s.length, f)
                        binding.etAdmNuevoDui.setSelection(f.length)
                        fmtDocPac = false
                    }
                }
            }
        })

        var fmtDocResp = false
        binding.etAdmRespDocumento.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (fmtDocResp || s == null) return
                if (tipoDocRespNuevo() == "DUI") {
                    val f = DuiTextWatcher.formatear(s.toString())
                    if (f != s.toString()) {
                        fmtDocResp = true
                        s.replace(0, s.length, f)
                        binding.etAdmRespDocumento.setSelection(f.length)
                        fmtDocResp = false
                    }
                }
            }
        })

        binding.toggleAdmNuevoTipoDoc.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) {
                binding.etAdmNuevoDui.setText("")
                binding.tilAdmNuevoDui.error = null
                actualizarEstadoMenorNuevoPaciente()
            }
        }

        binding.toggleAdmRespTipoDoc.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                binding.etAdmRespDocumento.setText("")
                binding.tilAdmRespDocumento.error = null
                val esPasaporte = checkedId == R.id.btnAdmRespDocPasaporte
                binding.tilAdmRespDocumento.hint =
                    if (esPasaporte) "Pasaporte del responsable *" else "DUI del responsable *"
                binding.tilAdmRespDocumento.helperText = if (esPasaporte) {
                    "Alfanumérico de 6 a 12 caracteres"
                } else {
                    "Formato DUI: 00000000-0"
                }
            }
        }

        // DatePickers para nuevo paciente y su responsable
        binding.etAdmNuevoNacimiento.setOnClickListener {
            val isoActual = DateUtils.toIsoDate(binding.etAdmNuevoNacimiento.text?.toString())
            val actual = runCatching { LocalDate.parse(isoActual) }.getOrDefault(LocalDate.now().minusYears(25))
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val sel = LocalDate.of(y, m + 1, d)
                binding.etAdmNuevoNacimiento.setText(DateUtils.toUiDate(sel.toString()))
                binding.tilAdmNuevoNacimiento.error = null
                actualizarEstadoMenorNuevoPaciente()
            }, actual.year, actual.monthValue - 1, actual.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis() - 86_400_000L
            }.show()
        }

        binding.etAdmRespNacimiento.setOnClickListener {
            val isoActual = DateUtils.toIsoDate(binding.etAdmRespNacimiento.text?.toString())
            val actual = runCatching { LocalDate.parse(isoActual) }.getOrDefault(LocalDate.now().minusYears(35))
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val sel = LocalDate.of(y, m + 1, d)
                binding.etAdmRespNacimiento.setText(DateUtils.toUiDate(sel.toString()))
                val edadResp = GuardarPacienteUseCase.calcularEdad(sel.toString())
                binding.tilAdmRespNacimiento.error = if (edadResp != null && edadResp < 18) {
                    "El responsable debe ser mayor de edad (18 años o más)."
                } else null
            }, actual.year, actual.monthValue - 1, actual.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis() - 86_400_000L
            }.show()
        }

        // Validaciones en tiempo real de campos de nuevo paciente
        binding.etAdmNuevoNombre.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilAdmNuevoNombre.error = if (v.isNotEmpty() && v.length < 3) "Ingrese nombre y apellido." else null
        }
        binding.etAdmNuevoDui.doAfterTextChanged { text ->
            if (!binding.etAdmNuevoDui.isEnabled) {
                binding.tilAdmNuevoDui.error = null
                return@doAfterTextChanged
            }
            val v = text?.toString()?.trim().orEmpty()
            binding.tilAdmNuevoDui.error = if (v.isEmpty()) null else GuardarPacienteUseCase.validarDocumento(v, tipoDocNuevoPaciente())
        }
        binding.etAdmNuevoTelefono.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilAdmNuevoTelefono.error = if (v.isEmpty() || GuardarPacienteUseCase.esTelefonoValido(v)) null else "Mínimo 8 dígitos."
        }
        binding.etAdmNuevoDireccion.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilAdmNuevoDireccion.error = if (v.isNotEmpty() && v.length < 4) "Ingrese una dirección válida." else null
        }
        binding.etAdmRespNombre.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilAdmRespNombre.error = if (v.isNotEmpty() && v.length < 3) "Ingrese el nombre completo del responsable." else null
        }
        binding.etAdmRespDocumento.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilAdmRespDocumento.error = if (v.isEmpty()) null else GuardarPacienteUseCase.validarDocumento(v, tipoDocRespNuevo())
        }
        binding.etAdmRespTelefono.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilAdmRespTelefono.error = if (v.isEmpty() || GuardarPacienteUseCase.esTelefonoValido(v)) null else "Mínimo 8 dígitos."
        }
        binding.actvAdmRespParentesco.doAfterTextChanged { binding.tilAdmRespParentesco.error = null }

        // Selector desplegable con todos los pacientes registrados
        binding.etAdmPacienteBuscar.setOnClickListener {
            binding.etAdmPacienteBuscar.showDropDown()
        }
        binding.etAdmPacienteBuscar.setOnItemClickListener { _, _, position, _ ->
            listaPacientesActual.getOrNull(position)?.let { seleccionarPaciente(it) }
        }
        binding.etAdmPacienteBuscar.doAfterTextChanged { texto ->
            val valor = texto?.toString().orEmpty()
            if (pacienteSeleccionado != null && valor != etiquetaPacienteDropdown(pacienteSeleccionado!!)) {
                pacienteSeleccionado = null
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
            val motivo = binding.etAdmisionMotivo.text?.toString().orEmpty()
            if (esModoNuevoPaciente()) {
                val isoNac = DateUtils.toIsoDate(binding.etAdmNuevoNacimiento.text?.toString().orEmpty())
                val esMenor = GuardarPacienteUseCase.esMenorDeEdad(isoNac)
                val form = PacienteForm(
                    nombreCompleto = binding.etAdmNuevoNombre.text?.toString().orEmpty(),
                    fechaNacimiento = isoNac,
                    sexo = if (binding.toggleAdmNuevoSexo.checkedButtonId == R.id.btnAdmSexoM) "M" else "F",
                    dui = if (esMenor) null else binding.etAdmNuevoDui.text?.toString(),
                    telefono = binding.etAdmNuevoTelefono.text?.toString(),
                    direccion = binding.etAdmNuevoDireccion.text?.toString(),
                    tipoSangre = binding.etAdmNuevoTipoSangre.text?.toString(),
                    alergias = binding.etAdmNuevoAlergias.text?.toString(),
                    tipoDocumento = tipoDocNuevoPaciente(),
                    responsableNombre = if (esMenor) binding.etAdmRespNombre.text?.toString() else null,
                    responsableFechaNacimiento = if (esMenor) {
                        DateUtils.toIsoDate(binding.etAdmRespNacimiento.text?.toString().orEmpty())
                    } else null,
                    responsableTipoDocumento = tipoDocRespNuevo(),
                    responsableDocumento = if (esMenor) binding.etAdmRespDocumento.text?.toString() else null,
                    responsableTelefono = if (esMenor) binding.etAdmRespTelefono.text?.toString() else null,
                    responsableParentesco = if (esMenor) binding.actvAdmRespParentesco.text?.toString() else null,
                    exigirDatosCompletos = true
                )
                viewModel.ingresarConNuevoPaciente(
                    form = form,
                    motivo = motivo,
                    onPacienteCreado = { creado -> seleccionarPaciente(creado) },
                    onSuccess = {
                        limpiarFormularioNuevoPaciente()
                        binding.toggleModoPacienteAdm.check(R.id.btnModoPacienteRegistrado)
                        seleccionarPaciente(null)
                        binding.etAdmPacienteBuscar.text?.clear()
                        binding.etAdmisionMotivo.text?.clear()
                    }
                )
            } else {
                viewModel.ingresar(
                    idPaciente = pacienteSeleccionado?.id,
                    motivo = motivo
                ) {
                    seleccionarPaciente(null)
                    binding.etAdmPacienteBuscar.text?.clear()
                    binding.etAdmisionMotivo.text?.clear()
                    binding.tilAdmPacienteBuscar.error = null
                    binding.tilAdmMotivo.error = null
                }
            }
        }
    }

    private fun limpiarFormularioNuevoPaciente() {
        binding.etAdmNuevoNombre.text?.clear()
        binding.etAdmNuevoNacimiento.text?.clear()
        binding.etAdmNuevoDui.text?.clear()
        binding.etAdmNuevoTelefono.text?.clear()
        binding.etAdmNuevoDireccion.text?.clear()
        binding.etAdmNuevoTipoSangre.text?.clear()
        binding.etAdmNuevoAlergias.text?.clear()
        binding.etAdmRespNombre.text?.clear()
        binding.etAdmRespNacimiento.text?.clear()
        binding.etAdmRespDocumento.text?.clear()
        binding.etAdmRespTelefono.text?.clear()
        binding.actvAdmRespParentesco.text?.clear()
        actualizarEstadoMenorNuevoPaciente()
    }

    private fun etiquetaPacienteDropdown(p: Paciente): String {
        val doc = p.dui ?: p.responsableEfectivo?.let { "Menor (Resp: ${it.documento})" } ?: p.numeroExpediente ?: "Sin DUI"
        return "${p.nombreCompleto} • $doc"
    }

    private fun seleccionarPaciente(paciente: Paciente?) {
        pacienteSeleccionado = paciente
        binding.tvAdmPacienteSeleccionado.text = paciente?.let {
            val docTexto = it.dui?.let { d -> " • Doc: $d" }
                ?: it.responsableEfectivo?.let { r -> " • Menor (Resp: ${r.nombreCompleto})" }
                ?: ""
            "✓ ${it.nombreCompleto} • Exp. ${it.numeroExpediente ?: "N/D"}$docTexto"
        } ?: "Ningún paciente seleccionado"
        if (paciente != null) {
            val etiqueta = etiquetaPacienteDropdown(paciente)
            binding.etAdmPacienteBuscar.setText(etiqueta, false)
            binding.etAdmPacienteBuscar.setSelection(binding.etAdmPacienteBuscar.length())
        }
    }

    private fun pintarPacientes(encontrados: List<Paciente>, buscando: Boolean) {
        listaPacientesActual = encontrados
        val opciones = encontrados.map(::etiquetaPacienteDropdown)
        binding.etAdmPacienteBuscar.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opciones)
        )

        val grupo = binding.chipGroupAdmPacientes
        grupo.removeAllViews()
        if (pacienteSeleccionado != null) return
        encontrados.take(8).forEach { p ->
            val texto = "${p.nombreCompleto} · ${p.dui ?: p.numeroExpediente ?: ""}".trimEnd(' ', '·')
            grupo.addView(chipAgenda(requireContext(), texto, false).apply {
                setOnClickListener { seleccionarPaciente(p) }
            })
        }
        val busqueda = binding.etAdmPacienteBuscar.text?.toString().orEmpty().trim()
        binding.tvAdmPacienteSeleccionado.text = when {
            buscando -> "Cargando pacientes registrados…"
            encontrados.isEmpty() && busqueda.isNotEmpty() ->
                "Sin coincidencias. Puede usar el botón 'Nuevo paciente' arriba para registrarlo."
            encontrados.isNotEmpty() ->
                "${encontrados.size} pacientes registrados disponibles en la lista"
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
            .setTitle("Habitación / Cama ${cama.codigo}")
            .setMessage(
                "Paciente: ${paciente.nombreCompleto}\n" +
                    "Expediente: ${paciente.numeroExpediente ?: "N/D"}\n" +
                    "Documento: ${paciente.dui ?: "Registrado en padrón"}"
            )
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
        val etiquetas = libres.map { "${it.codigo} • ${if (it.esVip) "VIP, Piso 3" else "Semiprivada, Piso 2"}" }
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
        binding.tvVipCountLabel.text = "${cap.vipDisponibles} disp. / ${cap.vipCamas.size} hab."
        binding.tvSemiCountLabel.text = "${cap.semiDisponibles} disp. / ${cap.salasSemiprivadas.size * 2} camas"
        vipBedsAdapter.submit(cap.vipCamas, seleccion)
        semiRoomsAdapter.submit(cap.salasSemiprivadas, seleccion)
        binding.tvSelectedBedPill.text = seleccion ?: "Ninguna seleccionada"
        binding.tvHabCapacidadOcupadasPill.text = "${cap.ocupadas} / ${cap.totalCamas} ocupadas"
        binding.progressHospitalCapacityTab2.progress = cap.porcentajeOcupacion
        binding.tvHabCapacidadDisponibles.text = "${cap.disponibles} camas disponibles para asignación"
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
                            st.cargandoCamas && viewModel.capacidad.value.totalCamas == 0 -> "Cargando mapa de habitaciones…"
                            else -> "Toque una habitación/cama libre para seleccionarla, o una ocupada para ver el paciente, trasladar o dar de alta."
                        }
                        binding.btnConfirmarIngreso.isEnabled = !st.enviando
                        binding.btnConfirmarIngreso.text = if (st.enviando) "Procesando…" else "Ingresar paciente"
                    }
                }
                launch {
                    viewModel.habitaciones.collect { todas ->
                        binding.chipFiltroHabTodas.text = "Todos los pisos (${todas.size})"
                        binding.chipFiltroHabVip.text = "Piso 3 • VIP (${todas.count { it.tipo.contains("VIP", true) }})"
                        binding.chipFiltroHabSemi.text = "Piso 2 • Semiprivadas (${todas.count { it.tipo.contains("Semiprivada", true) }})"
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
                            "nombre_completo" -> binding.tilAdmNuevoNombre.error = err.mensaje
                            "fecha_nacimiento" -> binding.tilAdmNuevoNacimiento.error = err.mensaje
                            "dui" -> binding.tilAdmNuevoDui.error = err.mensaje
                            "telefono" -> binding.tilAdmNuevoTelefono.error = err.mensaje
                            "direccion" -> binding.tilAdmNuevoDireccion.error = err.mensaje
                            "tipo_sangre" -> binding.tilAdmNuevoTipoSangre.error = err.mensaje
                            "responsable_nombre" -> binding.tilAdmRespNombre.error = err.mensaje
                            "responsable_fecha_nacimiento" -> binding.tilAdmRespNacimiento.error = err.mensaje
                            "responsable_documento" -> binding.tilAdmRespDocumento.error = err.mensaje
                            "responsable_telefono" -> binding.tilAdmRespTelefono.error = err.mensaje
                            "responsable_parentesco" -> binding.tilAdmRespParentesco.error = err.mensaje
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
