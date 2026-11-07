package com.cmo.hospital.features.recepcionista.admision.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.ContextCompat
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
import com.cmo.hospital.core.domain.model.*
import com.cmo.hospital.core.ui.DateUtils
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentRecepAdmisionBinding
import com.cmo.hospital.features.recepcionista.pacientes.presentation.DuiTextWatcher
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period

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

    private var signosAbiertos = false
    private var esMenorDeEdad = false

    private var duiWatcherPaciente: DuiTextWatcher? = null
    private var duiWatcherTutor: DuiTextWatcher? = null

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

        // Encabezados reutilizables de los 3 pasos
        binding.stepHeader1.ivStepIcon.setImageResource(R.drawable.ic_badge)
        binding.stepHeader1.tvStepTitle.text = "Datos de Filiación"
        binding.stepHeader1.tvStepBadge.text = "Paso 1 de 3"

        binding.stepHeader2.ivStepIcon.setImageResource(R.drawable.ic_medical_services)
        binding.stepHeader2.tvStepTitle.text = "Motivo Clínico & Triage"
        binding.stepHeader2.tvStepBadge.text = "Paso 2 de 3"

        binding.stepHeader3.ivStepIcon.setImageResource(R.drawable.ic_hotel)
        binding.stepHeader3.tvStepTitle.text = "Asignación de Cama"
        binding.stepHeader3.tvStepBadge.text = "Paso 3 de 3"

        setupTabs()
        setupCamasRecyclerViews()
        setupHabitacionesRecyclerView()
        setupInteractions()
        setupValidacionesInmediatas()
        observeState()
    }

    private fun setupTabs() {
        binding.tabLayoutAdmision.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.scrollTabRegistro.visibility = View.VISIBLE
                        binding.layoutTabHabitaciones.visibility = View.GONE
                    }
                    1 -> {
                        binding.scrollTabRegistro.visibility = View.GONE
                        binding.layoutTabHabitaciones.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun setupCamasRecyclerViews() {
        vipBedsAdapter = VipBedsAdapter { cama ->
            viewModel.seleccionarCama(cama.codigo)
        }
        binding.rvCamasVip.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvCamasVip.adapter = vipBedsAdapter

        semiRoomsAdapter = SemiPrivateRoomsAdapter { cama ->
            viewModel.seleccionarCama(cama.codigo)
        }
        binding.rvSalasSemiprivadas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSalasSemiprivadas.adapter = semiRoomsAdapter
    }

    private fun setupHabitacionesRecyclerView() {
        habitacionesAdapter = HabitacionesDetalleAdapter { camaSeleccionada ->
            viewModel.seleccionarCama(camaSeleccionada.codigo)
            // Cambiar automáticamente al Tab 0 para agendar en esta cama
            binding.tabLayoutAdmision.getTabAt(0)?.select()
            binding.scrollTabRegistro.post {
                binding.scrollTabRegistro.smoothScrollTo(0, binding.cardPaso3Camas.top)
            }
        }
        binding.rvHabitacionesDetalle.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHabitacionesDetalle.adapter = habitacionesAdapter

        // Filtros Tab 2: Tipo de Habitación
        binding.chipGroupFiltroHabTipo.setOnCheckedStateChangeListener { _, checkedIds ->
            val tipo = when (checkedIds.firstOrNull()) {
                R.id.chipFiltroHabVip -> "VIP"
                R.id.chipFiltroHabSemi -> "Semiprivadas"
                else -> "Todas"
            }
            viewModel.setFiltroTipoHabitacion(tipo)
        }

        // Filtros Tab 2: Disponibilidad de Camas
        binding.chipGroupFiltroHabOcup.setOnCheckedStateChangeListener { _, checkedIds ->
            val ocup = when (checkedIds.firstOrNull()) {
                R.id.chipFiltroOcupDisp -> "Disponibles"
                R.id.chipFiltroOcupLlenas -> "Ocupadas"
                else -> "Todas"
            }
            viewModel.setFiltroOcupacionHabitacion(ocup)
        }

        // Filtros Tab 2: Búsqueda rápida
        binding.etBuscarHabitacion.doAfterTextChanged { s ->
            viewModel.setQueryBusquedaHabitacion(s?.toString().orEmpty().trim())
        }

        // Botón Restablecer Filtros
        binding.btnResetFiltrosHabitaciones.setOnClickListener {
            binding.etBuscarHabitacion.text?.clear()
            binding.chipFiltroHabTodas.isChecked = true
            binding.chipFiltroOcupTodas.isChecked = true
            viewModel.resetFiltrosHabitaciones()
        }
    }

    private fun setupInteractions() {
        // Toggle Tipo de Documento Paciente (DUI vs Pasaporte)
        configurarToggleDocumento(
            isDui = binding.toggleTipoDocumento.checkedButtonId == R.id.btnDocDui,
            editText = binding.etAdmisionDocumento,
            layout = binding.tilAdmisionDocumento,
            onDuiWatcherUpdated = { watcher -> duiWatcherPaciente = watcher }
        )

        binding.toggleTipoDocumento.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                configurarToggleDocumento(
                    isDui = checkedId == R.id.btnDocDui,
                    editText = binding.etAdmisionDocumento,
                    layout = binding.tilAdmisionDocumento,
                    onDuiWatcherUpdated = { watcher -> duiWatcherPaciente = watcher }
                )
                validarFormularioCompleto()
            }
        }

        // Toggle Tipo de Documento Tutor
        configurarToggleDocumento(
            isDui = binding.toggleTutorTipoDocumento.checkedButtonId == R.id.btnTutorDocDui,
            editText = binding.etTutorDocumento,
            layout = binding.tilTutorDocumento,
            onDuiWatcherUpdated = { watcher -> duiWatcherTutor = watcher }
        )

        binding.toggleTutorTipoDocumento.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                configurarToggleDocumento(
                    isDui = checkedId == R.id.btnTutorDocDui,
                    editText = binding.etTutorDocumento,
                    layout = binding.tilTutorDocumento,
                    onDuiWatcherUpdated = { watcher -> duiWatcherTutor = watcher }
                )
                validarFormularioCompleto()
            }
        }

        // Fecha de Nacimiento: DatePicker en icono y clic
        binding.tilAdmisionFechaNac.setEndIconOnClickListener {
            abrirDatePickerNacimiento()
        }

        // Botón Escanear Documento
        binding.btnEscanearDni.setOnClickListener {
            viewModel.escanearDocumento()
        }

        // Selector Triage Manchester 2x2
        binding.btnTriageNivel1.setOnClickListener {
            viewModel.setNivelTriage(NivelTriageManchester.NIVEL_I_ROJO)
        }
        binding.btnTriageNivel2.setOnClickListener {
            viewModel.setNivelTriage(NivelTriageManchester.NIVEL_II_NARANJA)
        }
        binding.btnTriageNivel3.setOnClickListener {
            viewModel.setNivelTriage(NivelTriageManchester.NIVEL_III_AMARILLO)
        }
        binding.btnTriageNivel4.setOnClickListener {
            viewModel.setNivelTriage(NivelTriageManchester.NIVEL_IV_VERDE)
        }

        // Pase de Emergencia Directa
        binding.switchEmergenciaDirecta.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setEmergenciaDirecta(isChecked)
            validarFormularioCompleto()
        }

        // Desplegable de signos vitales con IMC automático
        binding.headerToggleSignosAdmision.setOnClickListener {
            signosAbiertos = !signosAbiertos
            binding.containerSignosAdmision.visibility =
                if (signosAbiertos) View.VISIBLE else View.GONE
            binding.tvToggleSignosAdmision.text =
                if (signosAbiertos) "Ocultar Signos" else "+ Registrar Signos"
        }

        fun recalcularImc() {
            val peso = binding.etAdmPeso.text?.toString()?.toDoubleOrNull()
            val talla = binding.etAdmTalla.text?.toString()?.toDoubleOrNull()
            val sv = SignosVitales(pesoKg = peso, tallaCm = talla)
            val imc = sv.imc
            binding.tvAdmImcBadge.text = if (imc != null) {
                "IMC Automático: $imc kg/m² (${sv.imcClasificacion})"
            } else {
                "IMC Automático: Ingrese peso y talla (opcional)"
            }
        }
        binding.etAdmPeso.doAfterTextChanged { recalcularImc() }
        binding.etAdmTalla.doAfterTextChanged { recalcularImc() }

        // Botón Primario: Confirmar Ingreso y Asignar Cama
        binding.btnConfirmarIngreso.setOnClickListener {
            val signos = SignosVitales(
                presionSistolica = binding.etAdmSist.text?.toString()?.toIntOrNull(),
                presionDiastolica = binding.etAdmDiast.text?.toString()?.toIntOrNull(),
                frecuenciaCardiaca = binding.etAdmFc.text?.toString()?.toIntOrNull(),
                frecuenciaRespiratoria = binding.etAdmFr.text?.toString()?.toIntOrNull(),
                temperaturaC = binding.etAdmTemp.text?.toString()?.toDoubleOrNull(),
                saturacionOxigeno = binding.etAdmSpo2.text?.toString()?.toIntOrNull(),
                pesoKg = binding.etAdmPeso.text?.toString()?.toDoubleOrNull(),
                tallaCm = binding.etAdmTalla.text?.toString()?.toDoubleOrNull()
            ).takeIf { it.hasAnyData }

            val responsable = if (esMenorDeEdad) {
                ResponsableMenor(
                    tipoDocumento = if (binding.toggleTutorTipoDocumento.checkedButtonId == R.id.btnTutorDocDui) "DUI" else "PASAPORTE",
                    documento = binding.etTutorDocumento.text?.toString().orEmpty().trim(),
                    nombreCompleto = binding.etTutorNombre.text?.toString().orEmpty().trim(),
                    telefono = binding.etTutorTelefono.text?.toString().orEmpty().trim(),
                    parentesco = binding.etTutorParentesco.text?.toString().orEmpty().trim()
                )
            } else null

            viewModel.confirmarIngreso(
                tipoDocumento = if (binding.toggleTipoDocumento.checkedButtonId == R.id.btnDocDui) "DUI" else "PASAPORTE",
                documento = binding.etAdmisionDocumento.text?.toString().orEmpty().trim(),
                nombres = binding.etAdmisionNombres.text?.toString().orEmpty().trim(),
                apellidos = binding.etAdmisionApellidos.text?.toString().orEmpty().trim(),
                fechaNacimiento = DateUtils.toIsoDate(binding.etAdmisionFechaNac.text?.toString().orEmpty().trim()),
                contactoNombre = binding.etContactoNombre.text?.toString().orEmpty().trim(),
                contactoTelefono = binding.etContactoTelefono.text?.toString().orEmpty().trim(),
                contactoParentesco = binding.etContactoParentesco.text?.toString().orEmpty().trim(),
                responsableMenor = responsable,
                motivoIngreso = binding.etAdmisionMotivo.text?.toString().orEmpty().trim(),
                signosVitales = signos,
                onFormReset = { resetFormFields() }
            )
        }

        // Botón Secundario: Generar Brazalete QR
        binding.btnGenerarBrazaleteQr.setOnClickListener {
            viewModel.generarBrazaleteQr(
                documento = binding.etAdmisionDocumento.text?.toString().orEmpty().trim(),
                nombres = binding.etAdmisionNombres.text?.toString().orEmpty().trim(),
                apellidos = binding.etAdmisionApellidos.text?.toString().orEmpty().trim()
            )
        }
    }

    private fun configurarToggleDocumento(
        isDui: Boolean,
        editText: EditText,
        layout: com.google.android.material.textfield.TextInputLayout,
        onDuiWatcherUpdated: (DuiTextWatcher?) -> Unit
    ) {
        if (isDui) {
            layout.hint = "Número de DUI (00000000-0)"
            editText.hint = null
            editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            val watcher = DuiTextWatcher(editText)
            editText.addTextChangedListener(watcher)
            onDuiWatcherUpdated(watcher)
        } else {
            layout.hint = "Número de Pasaporte"
            editText.hint = null
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            onDuiWatcherUpdated(null)
        }
    }

    private fun abrirDatePickerNacimiento() {
        DateUtils.showDatePicker(this, binding.etAdmisionFechaNac.text?.toString(), "Fecha de Nacimiento") { fechaSel ->
            binding.etAdmisionFechaNac.setText(fechaSel)
            evaluarFechaNacimientoYEdad(fechaSel)
            validarFormularioCompleto()
        }
    }

    private fun setupValidacionesInmediatas() {
        binding.etAdmisionDocumento.doAfterTextChanged { s ->
            val doc = s?.toString().orEmpty().trim()
            val isDui = binding.toggleTipoDocumento.checkedButtonId == R.id.btnDocDui
            if (doc.isEmpty()) {
                binding.tilAdmisionDocumento.error = "Ingrese el documento del paciente"
            } else if (isDui && !doc.matches(Regex("""^\d{8}-\d$"""))) {
                binding.tilAdmisionDocumento.error = "DUI incompleto (00000000-0)"
            } else if (!isDui && doc.length < 6) {
                binding.tilAdmisionDocumento.error = "Pasaporte inválido (mínimo 6 caracteres)"
            } else {
                binding.tilAdmisionDocumento.error = null
            }
            validarFormularioCompleto()
        }

        binding.etAdmisionNombres.doAfterTextChanged { s ->
            val txt = s?.toString().orEmpty().trim()
            if (txt.length < 2) {
                binding.tilAdmisionNombres.error = "Ingrese al menos un nombre"
            } else {
                binding.tilAdmisionNombres.error = null
            }
            validarFormularioCompleto()
        }

        binding.etAdmisionApellidos.doAfterTextChanged { s ->
            val txt = s?.toString().orEmpty().trim()
            if (txt.length < 2) {
                binding.tilAdmisionApellidos.error = "Ingrese los apellidos"
            } else {
                binding.tilAdmisionApellidos.error = null
            }
            validarFormularioCompleto()
        }

        binding.etAdmisionFechaNac.doAfterTextChanged { s ->
            val fecha = s?.toString().orEmpty().trim()
            evaluarFechaNacimientoYEdad(fecha)
            validarFormularioCompleto()
        }

        binding.etTutorDocumento.doAfterTextChanged { s ->
            if (esMenorDeEdad) {
                val doc = s?.toString().orEmpty().trim()
                val isDui = binding.toggleTutorTipoDocumento.checkedButtonId == R.id.btnTutorDocDui
                if (doc.isEmpty()) {
                    binding.tilTutorDocumento.error = "Ingrese el documento del responsable"
                } else if (isDui && !doc.matches(Regex("""^\d{8}-\d$"""))) {
                    binding.tilTutorDocumento.error = "DUI incompleto (00000000-0)"
                } else if (!isDui && doc.length < 6) {
                    binding.tilTutorDocumento.error = "Pasaporte inválido"
                } else {
                    binding.tilTutorDocumento.error = null
                }
            } else {
                binding.tilTutorDocumento.error = null
            }
            validarFormularioCompleto()
        }

        binding.etTutorNombre.doAfterTextChanged { s ->
            if (esMenorDeEdad && s?.toString().orEmpty().trim().length < 3) {
                binding.tilTutorNombre.error = "Ingrese el nombre del responsable"
            } else {
                binding.tilTutorNombre.error = null
            }
            validarFormularioCompleto()
        }

        binding.etTutorTelefono.doAfterTextChanged { s ->
            if (esMenorDeEdad && s?.toString().orEmpty().trim().length < 8) {
                binding.tilTutorTelefono.error = "Ingrese un teléfono válido"
            } else {
                binding.tilTutorTelefono.error = null
            }
            validarFormularioCompleto()
        }

        binding.etTutorParentesco.doAfterTextChanged { s ->
            if (esMenorDeEdad && s?.toString().orEmpty().trim().length < 2) {
                binding.tilTutorParentesco.error = "Ingrese el parentesco"
            } else {
                binding.tilTutorParentesco.error = null
            }
            validarFormularioCompleto()
        }

        binding.etContactoNombre.doAfterTextChanged { s ->
            if (s?.toString().orEmpty().trim().length < 2) {
                binding.tilContactoNombre.error = "Ingrese el nombre del contacto"
            } else {
                binding.tilContactoNombre.error = null
            }
            validarFormularioCompleto()
        }

        binding.etContactoTelefono.doAfterTextChanged { s ->
            if (s?.toString().orEmpty().trim().length < 8) {
                binding.tilContactoTelefono.error = "Teléfono de al menos 8 dígitos"
            } else {
                binding.tilContactoTelefono.error = null
            }
            validarFormularioCompleto()
        }

        binding.etContactoParentesco.doAfterTextChanged { s ->
            if (s?.toString().orEmpty().trim().length < 2) {
                binding.tilContactoParentesco.error = "Ingrese el parentesco"
            } else {
                binding.tilContactoParentesco.error = null
            }
            validarFormularioCompleto()
        }

        binding.etAdmisionMotivo.doAfterTextChanged { s ->
            if (s?.toString().orEmpty().trim().length < 3) {
                binding.tilAdmisionMotivo.error = "Describa el motivo clínico de ingreso"
            } else {
                binding.tilAdmisionMotivo.error = null
            }
            validarFormularioCompleto()
        }
    }

    private fun evaluarFechaNacimientoYEdad(fechaTexto: String) {
        val iso = DateUtils.toIsoDate(fechaTexto)
        val birthDate = runCatching { LocalDate.parse(iso) }.getOrNull()

        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            binding.tilAdmisionFechaNac.error = "Fecha inválida o futura (dd/mm/aaaa)"
            binding.tvEdadCalculadaBadge.text = "-- años"
            binding.containerResponsableMenor.visibility = View.GONE
            esMenorDeEdad = false
            return
        }

        binding.tilAdmisionFechaNac.error = null
        val edad = Period.between(birthDate, LocalDate.now()).years.coerceAtLeast(0)
        binding.tvEdadCalculadaBadge.text = "$edad años"

        if (edad < 18) {
            esMenorDeEdad = true
            binding.containerResponsableMenor.visibility = View.VISIBLE
        } else {
            esMenorDeEdad = false
            binding.containerResponsableMenor.visibility = View.GONE
            binding.tilTutorDocumento.error = null
            binding.tilTutorNombre.error = null
            binding.tilTutorTelefono.error = null
            binding.tilTutorParentesco.error = null
        }
    }

    private fun validarFormularioCompleto() {
        val doc = binding.etAdmisionDocumento.text?.toString().orEmpty().trim()
        val isDui = binding.toggleTipoDocumento.checkedButtonId == R.id.btnDocDui
        val docValido = if (isDui) doc.matches(Regex("""^\d{8}-\d$""")) else doc.length >= 6

        val nombresValidos = binding.etAdmisionNombres.text?.toString().orEmpty().trim().length >= 2
        val apellidosValidos = binding.etAdmisionApellidos.text?.toString().orEmpty().trim().length >= 2

        val fechaNac = binding.etAdmisionFechaNac.text?.toString().orEmpty().trim()
        val iso = DateUtils.toIsoDate(fechaNac)
        val birthDate = runCatching { LocalDate.parse(iso) }.getOrNull()
        val fechaValida = birthDate != null && !birthDate.isAfter(LocalDate.now())

        val tutorValido = if (esMenorDeEdad) {
            val tDoc = binding.etTutorDocumento.text?.toString().orEmpty().trim()
            val tIsDui = binding.toggleTutorTipoDocumento.checkedButtonId == R.id.btnTutorDocDui
            val tDocOk = if (tIsDui) tDoc.matches(Regex("""^\d{8}-\d$""")) else tDoc.length >= 6
            tDocOk &&
                binding.etTutorNombre.text?.toString().orEmpty().trim().length >= 3 &&
                binding.etTutorTelefono.text?.toString().orEmpty().trim().length >= 8 &&
                binding.etTutorParentesco.text?.toString().orEmpty().trim().length >= 2
        } else {
            true
        }

        val contactoValido =
            binding.etContactoNombre.text?.toString().orEmpty().trim().length >= 2 &&
            binding.etContactoTelefono.text?.toString().orEmpty().trim().length >= 8 &&
            binding.etContactoParentesco.text?.toString().orEmpty().trim().length >= 2

        val motivoValido = binding.etAdmisionMotivo.text?.toString().orEmpty().trim().length >= 3

        val st = viewModel.uiState.value
        val camaValida = st.esEmergenciaDirecta || !st.codigoCamaSeleccionada.isNullOrBlank()

        val todoValido = docValido && nombresValidos && apellidosValidos && fechaValida && tutorValido && contactoValido && motivoValido && camaValida

        binding.btnConfirmarIngreso.isEnabled = todoValido

        val ctx = requireContext()
        if (todoValido) {
            binding.tvFormValidacionFeedback.text = "✓ Todos los datos de filiación, triage y cama están listos."
            binding.tvFormValidacionFeedback.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary))
        } else {
            val falta = when {
                !docValido -> "Número de documento inválido o incompleto."
                !nombresValidos || !apellidosValidos -> "Complete nombres y apellidos del paciente."
                !fechaValida -> "Fecha de nacimiento requerida (dd/mm/aaaa)."
                !tutorValido -> "Complete los datos del tutor legal para el menor de edad."
                !contactoValido -> "Complete los 3 campos de contacto de emergencia."
                !motivoValido -> "Describa el motivo de ingreso clínico."
                !camaValida -> "Seleccione una cama disponible o active Pase de Emergencia Directa."
                else -> "Complete todos los campos requeridos."
            }
            binding.tvFormValidacionFeedback.text = falta
            binding.tvFormValidacionFeedback.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface_variant))
        }
    }

    private fun resetFormFields() {
        binding.etAdmisionDocumento.text?.clear()
        binding.etAdmisionNombres.text?.clear()
        binding.etAdmisionApellidos.text?.clear()
        binding.etAdmisionFechaNac.text?.clear()
        binding.tvEdadCalculadaBadge.text = "-- años"
        binding.containerResponsableMenor.visibility = View.GONE
        esMenorDeEdad = false

        binding.etTutorDocumento.text?.clear()
        binding.etTutorNombre.text?.clear()
        binding.etTutorTelefono.text?.clear()
        binding.etTutorParentesco.text?.clear()

        binding.etContactoNombre.text?.clear()
        binding.etContactoTelefono.text?.clear()
        binding.etContactoParentesco.text?.clear()

        binding.etAdmisionMotivo.text?.clear()
        binding.etAdmSist.text?.clear()
        binding.etAdmDiast.text?.clear()
        binding.etAdmFc.text?.clear()
        binding.etAdmFr.text?.clear()
        binding.etAdmTemp.text?.clear()
        binding.etAdmSpo2.text?.clear()
        binding.etAdmPeso.text?.clear()
        binding.etAdmTalla.text?.clear()

        binding.switchEmergenciaDirecta.isChecked = false
        validarFormularioCompleto()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.capacidad.collect { cap ->
                        if (cap.totalCamas > 0) {
                            binding.tvCapacidadOcupadasPill.text =
                                "${cap.ocupadas} / ${cap.totalCamas} Ocupadas"
                            binding.progressHospitalCapacity.progress = cap.porcentajeOcupacion
                            binding.tvCapacidadDisponibles.text =
                                "${cap.disponibles} Camas Disponibles"
                            binding.tvVipCountLabel.text =
                                "${cap.vipDisponibles} Disp. / ${cap.vipCamas.size} Totales"
                            binding.tvSemiCountLabel.text =
                                "${cap.semiDisponibles} Disp. / ${cap.salasSemiprivadas.size * 2} Totales"

                            // Tab 2 métricas
                            binding.tvHabCapacidadOcupadasPill.text = "${cap.ocupadas} Ocupadas"
                            binding.progressHospitalCapacityTab2.progress = cap.porcentajeOcupacion
                            binding.tvHabCapacidadDisponibles.text =
                                "${cap.disponibles} Camas Disponibles para Asignación Inmediata"

                            val sel = viewModel.uiState.value.codigoCamaSeleccionada
                            vipBedsAdapter.submit(cap.vipCamas, sel)
                            semiRoomsAdapter.submit(cap.salasSemiprivadas, sel)
                        }
                    }
                }

                launch {
                    viewModel.habitaciones.collect { todas ->
                        if (todas.isNotEmpty()) {
                            val vipCount = todas.count { it.tipo.contains("VIP", true) }
                            val semiCount = todas.count { it.tipo.contains("Semiprivada", true) }
                            val dispCount = todas.count { it.estaDisponible }
                            val llenasCount = todas.count { it.estaCompletamenteOcupada }

                            binding.chipFiltroHabTodas.text = "Todas (${todas.size})"
                            binding.chipFiltroHabVip.text = "VIP Individual ($vipCount)"
                            binding.chipFiltroHabSemi.text = "Semiprivadas ($semiCount)"

                            binding.chipFiltroOcupTodas.text = "Cualquier Estado (${todas.size})"
                            binding.chipFiltroOcupDisp.text = "Con Disponibilidad ($dispCount)"
                            binding.chipFiltroOcupLlenas.text = "100% Ocupadas ($llenasCount)"
                        }
                    }
                }

                launch {
                    viewModel.habitacionesFiltradas.collect { lista ->
                        habitacionesAdapter.submitList(lista)
                        binding.tvHabitacionesEmpty.visibility =
                            if (lista.isEmpty()) View.VISIBLE else View.GONE
                        val total = viewModel.habitaciones.value.size.takeIf { it > 0 } ?: 11
                        binding.tvFiltrosActivosBadge.text = "${lista.size} de $total Habitaciones"

                        val hayFiltro = viewModel.filtroTipoHabitacion.value != "Todas" ||
                            viewModel.filtroOcupacionHabitacion.value != "Todas" ||
                            viewModel.queryBusquedaHabitacion.value.isNotBlank()
                        binding.btnResetFiltrosHabitaciones.visibility =
                            if (hayFiltro) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.uiState.collect { st ->
                        val ctx = requireContext()

                        // 1. Estado escáner DNI
                        binding.btnEscanearDni.isEnabled = !st.isScanningDni
                        binding.btnEscanearDni.text =
                            if (st.isScanningDni) "Escaneando..." else "Escanear"
                        binding.bannerScanFeedback.visibility =
                            if (st.scanFeedbackVisible) View.VISIBLE else View.GONE

                        st.scannedData?.let { scan ->
                            binding.etAdmisionDocumento.setText(scan.dni)
                            binding.etAdmisionNombres.setText(scan.nombres)
                            binding.etAdmisionApellidos.setText(scan.apellidos)
                            binding.etAdmisionFechaNac.setText(scan.fechaNacimiento)
                            evaluarFechaNacimientoYEdad(scan.fechaNacimiento)
                            binding.etContactoNombre.setText("Carlos Navarro Silva")
                            binding.etContactoTelefono.setText("7654-3210")
                            binding.etContactoParentesco.setText("Hermano")
                            viewModel.consumeScannedData()
                            validarFormularioCompleto()
                        }

                        // 2. Estado visual Triage Manchester 2x2
                        renderTriageSelection(st.nivelTriage)

                        // 3. Emergencia directa vs Asignación de Cama
                        binding.bannerEmergenciaDirecta.visibility =
                            if (st.esEmergenciaDirecta) View.VISIBLE else View.GONE
                        if (st.esEmergenciaDirecta) {
                            binding.btnConfirmarIngreso.text = "Confirmar Pase Directo a Box"
                            binding.btnConfirmarIngreso.backgroundTintList =
                                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_error))
                        } else {
                            binding.btnConfirmarIngreso.text = "Confirmar Ingreso y Asignar Cama"
                            binding.btnConfirmarIngreso.backgroundTintList =
                                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary_container))
                        }

                        // 4. Cama seleccionada
                        val cap = viewModel.capacidad.value
                        vipBedsAdapter.submit(cap.vipCamas, st.codigoCamaSeleccionada)
                        semiRoomsAdapter.submit(cap.salasSemiprivadas, st.codigoCamaSeleccionada)

                        if (!st.codigoCamaSeleccionada.isNullOrBlank()) {
                            binding.tvSelectedBedPill.text = "${st.codigoCamaSeleccionada} (Asignada)"
                            binding.tvSelectedBedPill.setBackgroundResource(R.drawable.bg_pill_primary_container)
                            binding.tvSelectedBedPill.setTextColor(
                                ContextCompat.getColor(ctx, R.color.huc_on_primary)
                            )
                        } else {
                            binding.tvSelectedBedPill.text = "Ninguna seleccionada"
                            binding.tvSelectedBedPill.setBackgroundResource(R.drawable.bg_card_rounded_16)
                            binding.tvSelectedBedPill.setTextColor(
                                ContextCompat.getColor(ctx, R.color.huc_primary_container)
                            )
                        }

                        validarFormularioCompleto()

                        // 5. Modal Brazalete QR
                        st.brazaleteEmitido?.let { payload ->
                            viewModel.consumeBrazaleteDialog()
                            BrazaleteQrDialogFragment(payload)
                                .show(childFragmentManager, "dialog_brazalete_qr")
                        }

                        // 6. Toast institucional CMO
                        if (!st.toastTitle.isNullOrBlank() && !st.toastMessage.isNullOrBlank()) {
                            val toastHost = parentFragment as? RecepcionistaToastHost
                            if (st.toastIsError) {
                                toastHost?.showToastError(st.toastTitle, st.toastMessage)
                            } else {
                                toastHost?.showToastSuccess(st.toastTitle, st.toastMessage)
                            }
                            viewModel.consumeToast()
                        }
                    }
                }
            }
        }
    }

    private fun renderTriageSelection(selected: NivelTriageManchester) {
        val ctx = requireContext()
        val normalText = ContextCompat.getColor(ctx, R.color.huc_on_surface)

        binding.btnTriageNivel1.setBackgroundResource(
            if (selected == NivelTriageManchester.NIVEL_I_ROJO) {
                R.drawable.bg_error_container_16
            } else R.drawable.bg_surface_container_16
        )
        binding.tvTriageTitle1.setTextColor(
            if (selected == NivelTriageManchester.NIVEL_I_ROJO) {
                ContextCompat.getColor(ctx, R.color.huc_on_error_container)
            } else normalText
        )

        binding.btnTriageNivel2.setBackgroundResource(
            if (selected == NivelTriageManchester.NIVEL_II_NARANJA) {
                R.drawable.bg_tertiary_fixed_16
            } else R.drawable.bg_surface_container_16
        )
        binding.tvTriageTitle2.setTextColor(
            if (selected == NivelTriageManchester.NIVEL_II_NARANJA) {
                ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed)
            } else normalText
        )

        binding.btnTriageNivel3.setBackgroundResource(
            if (selected == NivelTriageManchester.NIVEL_III_AMARILLO) {
                R.drawable.bg_tertiary_fixed_16
            } else R.drawable.bg_surface_container_16
        )
        binding.tvTriageTitle3.setTextColor(
            if (selected == NivelTriageManchester.NIVEL_III_AMARILLO) {
                ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed)
            } else normalText
        )

        binding.btnTriageNivel4.setBackgroundResource(
            if (selected == NivelTriageManchester.NIVEL_IV_VERDE) {
                R.drawable.bg_primary_fixed_16
            } else R.drawable.bg_surface_container_16
        )
        binding.tvTriageTitle4.setTextColor(
            if (selected == NivelTriageManchester.NIVEL_IV_VERDE) {
                ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed)
            } else normalText
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
