package com.cmo.hospital.features.recepcionista.admision.presentation

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.R
import com.cmo.hospital.components.HucMicroBannerBinder
import com.cmo.hospital.core.domain.model.NivelTriageManchester
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentRecepAdmisionBinding
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import kotlinx.coroutines.launch

class AdmisionFragment : Fragment() {

    private var _binding: FragmentRecepAdmisionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdmisionViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createAdmisionViewModel() }
    }

    private lateinit var vipBedsAdapter: VipBedsAdapter
    private lateinit var semiRoomsAdapter: SemiPrivateRoomsAdapter
    private var signosAbiertos = false

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

        // Micro-banner idéntico al prototipo HTML
        HucMicroBannerBinder.bind(
            binding = binding.microBannerAdmision,
            title = "Hospital Universitario Central - Admisión",
            subtitle = "Puesto de Admisión Activo • Turno Matutino",
            badgeText = "Online"
        )

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

        setupCamasRecyclerViews()
        setupInteractions()
        observeState()
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

    private fun setupInteractions() {
        // Toggle Ley 19.628
        binding.switchVipLey19628.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setProtegidoLey19628(isChecked)
        }

        // Botón Escanear DNI
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

            viewModel.confirmarIngreso(
                dni = binding.etAdmisionDni.text?.toString().orEmpty(),
                nombres = binding.etAdmisionNombres.text?.toString().orEmpty(),
                apellidos = binding.etAdmisionApellidos.text?.toString().orEmpty(),
                edadTexto = binding.etAdmisionEdad.text?.toString().orEmpty(),
                prevision = binding.spinnerAdmisionPrevision.selectedItem?.toString()
                    ?: "Fonasa Tramo D",
                contactoEmergencia = binding.etAdmisionContacto.text?.toString().orEmpty(),
                motivoIngreso = binding.etAdmisionMotivo.text?.toString().orEmpty(),
                signosVitales = signos,
                onFormReset = { resetFormFields() }
            )
        }

        // Botón Secundario: Generar Brazalete QR
        binding.btnGenerarBrazaleteQr.setOnClickListener {
            viewModel.generarBrazaleteQr(
                dni = binding.etAdmisionDni.text?.toString().orEmpty(),
                nombres = binding.etAdmisionNombres.text?.toString().orEmpty(),
                apellidos = binding.etAdmisionApellidos.text?.toString().orEmpty()
            )
        }
    }

    private fun resetFormFields() {
        binding.etAdmisionDni.text?.clear()
        binding.etAdmisionNombres.text?.clear()
        binding.etAdmisionApellidos.text?.clear()
        binding.etAdmisionEdad.text?.clear()
        binding.etAdmisionContacto.text?.clear()
        binding.etAdmisionMotivo.text?.clear()
        binding.etAdmSist.text?.clear()
        binding.etAdmDiast.text?.clear()
        binding.etAdmFc.text?.clear()
        binding.etAdmFr.text?.clear()
        binding.etAdmTemp.text?.clear()
        binding.etAdmSpo2.text?.clear()
        binding.etAdmPeso.text?.clear()
        binding.etAdmTalla.text?.clear()
        binding.switchVipLey19628.isChecked = false
        binding.switchEmergenciaDirecta.isChecked = false
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

                            val sel = viewModel.uiState.value.codigoCamaSeleccionada
                            vipBedsAdapter.submit(cap.vipCamas, sel)
                            semiRoomsAdapter.submit(cap.salasSemiprivadas, sel)
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { st ->
                        val ctx = requireContext()

                        // 1. Estado visual Toggle Ley 19.628 (idéntico al script del prototipo HTML)
                        if (st.protegidoLey19628) {
                            binding.vipWrapper.setBackgroundResource(R.drawable.bg_tertiary_fixed_16)
                            binding.vipIconBox.setBackgroundResource(R.drawable.bg_circle_tertiary_container)
                            binding.ivVipIcon.setImageResource(R.drawable.ic_visibility_off)
                            binding.ivVipIcon.imageTintList =
                                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_on_tertiary))
                            binding.tvVipDesc.text =
                                "Paciente Protegido/VIP: Nombre anonimizado en monitores públicos"
                            binding.tvVipDesc.setTextColor(
                                ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed_variant)
                            )
                        } else {
                            binding.vipWrapper.setBackgroundResource(R.drawable.bg_surface_low_16)
                            binding.vipIconBox.setBackgroundResource(R.drawable.bg_circle_surface_highest)
                            binding.ivVipIcon.setImageResource(R.drawable.ic_visibility)
                            binding.ivVipIcon.imageTintList =
                                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_on_surface_variant))
                            binding.tvVipDesc.text =
                                "Nombre visible en pantallas y monitores de sala"
                            binding.tvVipDesc.setTextColor(
                                ContextCompat.getColor(ctx, R.color.huc_on_surface_variant)
                            )
                        }

                        // 2. Estado escáner DNI
                        binding.btnEscanearDni.isEnabled = !st.isScanningDni
                        binding.btnEscanearDni.text =
                            if (st.isScanningDni) "Escaneando..." else "Escanear"
                        binding.bannerScanFeedback.visibility =
                            if (st.scanFeedbackVisible) View.VISIBLE else View.GONE

                        st.scannedData?.let { scan ->
                            binding.etAdmisionDni.setText(scan.dni)
                            binding.etAdmisionNombres.setText(scan.nombres)
                            binding.etAdmisionApellidos.setText(scan.apellidos)
                            binding.etAdmisionEdad.setText(scan.edad.toString())
                            binding.etAdmisionContacto.setText(scan.contactoEmergencia)
                            viewModel.consumeScannedData()
                        }

                        // 3. Estado visual Triage Manchester 2x2
                        renderTriageSelection(st.nivelTriage)

                        // 4. Emergencia directa vs Asignación de Cama
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

                        // 5. Cama seleccionada
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

                        // 6. Modal Brazalete QR
                        st.brazaleteEmitido?.let { payload ->
                            viewModel.consumeBrazaleteDialog()
                            BrazaleteQrDialogFragment(payload)
                                .show(childFragmentManager, "dialog_brazalete_qr")
                        }

                        // 7. Toast HUC Móvil
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
