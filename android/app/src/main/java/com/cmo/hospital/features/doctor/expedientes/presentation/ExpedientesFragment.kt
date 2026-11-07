package com.cmo.hospital.features.doctor.expedientes.presentation

import android.os.Bundle
import android.text.InputType
import android.widget.LinearLayout
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
import com.cmo.hospital.components.LabOrderCardBinder
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.CompLabOrderCardBinding
import com.cmo.hospital.databinding.FragmentDoctorExpedientesBinding
import com.cmo.hospital.features.doctor.laboratorio.presentation.IndicarExamenLabDialogFragment
import com.cmo.hospital.features.doctor.shell.presentation.DoctorShellHost
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ExpedientesFragment : Fragment() {

    private var _binding: FragmentDoctorExpedientesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpedientesViewModel by viewModels {
        simpleViewModelFactory {
            (requireActivity().application as CmoApplication).appContainer.createExpedientesViewModel()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorExpedientesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val histAdapter = AtencionesHistoricasAdapter()
        binding.rvAtencionesHistoricas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAtencionesHistoricas.adapter = histAdapter

        binding.etBuscarExpediente.doAfterTextChanged { editable ->
            val currentText = editable?.toString().orEmpty()
            if (currentText != viewModel.uiState.value.query) {
                viewModel.onQueryChanged(currentText)
            }
        }

        binding.btnExpIndicarExamen.setOnClickListener {
            val exp = viewModel.uiState.value.expedienteSeleccionado ?: return@setOnClickListener
            IndicarExamenLabDialogFragment().apply {
                pacienteFijo = exp.paciente
                examenesDisponibles = viewModel.catalogoExamenes()
                onConfirmarOrden = { examenes, prioridad, obs, _ ->
                    viewModel.emitirOrdenDesdeExpediente(
                        paciente = exp.paciente,
                        examenes = examenes,
                        prioridad = prioridad,
                        obs = obs
                    )
                }
            }.show(childFragmentManager, "dialog_indicar_lab_expediente")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        if (binding.etBuscarExpediente.text?.toString().orEmpty() != state.query) {
                            binding.etBuscarExpediente.setText(state.query)
                            binding.etBuscarExpediente.setSelection(state.query.length)
                        }
                        renderSelectorPacientes(state)
                        val exp = state.expedienteSeleccionado
                        if (exp == null) {
                            binding.tvExpEmptySearch.visibility = View.VISIBLE
                            binding.tvExpEmptySearch.text = when {
                                state.error != null -> "No se pudo cargar: ${state.error}"
                                state.buscando || state.cargandoDetalle -> "Cargando expedientes…"
                                state.query.isNotBlank() -> "No se encontraron expedientes con ese criterio de búsqueda."
                                else -> "Aún no tiene pacientes: aparecen al tener una cita agendada con usted."
                            }
                            binding.containerDetalleExpediente.visibility = View.GONE
                        } else {
                            binding.tvExpEmptySearch.visibility = View.GONE
                            binding.containerDetalleExpediente.visibility = View.VISIBLE
                            renderExpedienteDetalle(exp, histAdapter)
                        }
                    }
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

    private fun renderSelectorPacientes(state: ExpedientesUiState) {
        val ctx = requireContext()
        binding.chipGroupPacientesExpediente.removeAllViews()
        state.resultados.forEach { pac ->
            val chip = Chip(ctx).apply {
                id = View.generateViewId()
                text = "${pac.nombreCompleto} (${pac.numeroExpediente ?: "S/E"})"
                isCheckable = true
                isChecked = pac.id == state.idSeleccionado
                setOnClickListener {
                    viewModel.seleccionar(pac.id)
                }
            }
            binding.chipGroupPacientesExpediente.addView(chip)
        }
    }

    private fun renderExpedienteDetalle(
        exp: ExpedienteDetalle,
        histAdapter: AtencionesHistoricasAdapter
    ) {
        val pac = exp.paciente
        binding.tvExpNumeroBadge.text = "EXP: ${pac.numeroExpediente ?: "N/D"}"
        binding.tvExpVipBadge.visibility = if (pac.protegido) View.VISIBLE else View.GONE
        binding.tvExpPacienteNombre.text = pac.nombreCompleto

        val edadStr = if (pac.edad > 0) "${pac.edad} años" else "Edad N/D"
        val duiStr = pac.dui ?: "Sin DUI"
        val sangreStr = pac.tipoSangre ?: "N/D"
        binding.tvExpPacienteMeta.text = "$edadStr • DUI: $duiStr • Sangre: $sangreStr"
        binding.tvExpCamaBadge.text = "Ubicación: ${exp.cama ?: "Consulta externa"}"
        binding.btnExpEditarAntecedentes.setOnClickListener { editarAntecedentes(exp) }

        val todasAlergias = (exp.alergiasCriticas + listOfNotNull(pac.alergias))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (todasAlergias.isNotEmpty()) {
            binding.bannerAlertaAlergias.visibility = View.VISIBLE
            binding.tvExpAlergiasDetalle.text = todasAlergias.joinToString(" • ")
        } else {
            binding.bannerAlertaAlergias.visibility = View.GONE
        }

        // Comorbilidades
        binding.chipGroupComorbilidades.removeAllViews()
        val comorbilidades = exp.comorbilidades.ifEmpty { listOf("Sin antecedentes registrados") }
        comorbilidades.forEach { item ->
            val chip = Chip(requireContext()).apply {
                text = item
                isClickable = false
                isCheckable = false
            }
            binding.chipGroupComorbilidades.addView(chip)
        }

        // Órdenes de Laboratorio con cambio de estado inline
        binding.containerOrdenesExpediente.removeAllViews()
        if (exp.ordenesLaboratorio.isEmpty()) {
            binding.tvExpSinOrdenesLab.visibility = View.VISIBLE
        } else {
            binding.tvExpSinOrdenesLab.visibility = View.GONE
            val inflater = LayoutInflater.from(requireContext())
            exp.ordenesLaboratorio.forEach { orden ->
                val cardBinding = CompLabOrderCardBinding.inflate(
                    inflater,
                    binding.containerOrdenesExpediente,
                    false
                )
                LabOrderCardBinder.bind(
                    binding = cardBinding,
                    orden = orden,
                    showPatientLine = false
                ) { orden ->
                    viewModel.cancelarOrdenLab(orden)
                }
                binding.containerOrdenesExpediente.addView(cardBinding.root)
            }
        }

        histAdapter.submitList(exp.atencionesPrevias)
    }

    /** PUT /pacientes/{id}/historial con los antecedentes personales y familiares. */
    private fun editarAntecedentes(exp: ExpedienteDetalle) {
        val ctx = requireContext()
        fun campo(pista: String, valor: String?) = TextInputEditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 2
            setText(valor.orEmpty())
        }.let { et -> et to TextInputLayout(ctx).apply { hint = pista; addView(et) } }
        val (personales, tilPersonales) = campo("Antecedentes personales", exp.antecedentesPersonales)
        val (familiares, tilFamiliares) = campo("Antecedentes familiares", exp.antecedentesFamiliares)
        val contenedor = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(tilPersonales)
            addView(tilFamiliares)
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Antecedentes de ${exp.paciente.nombreCompleto}")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                viewModel.guardarAntecedentes(
                    personales.text?.toString().orEmpty(),
                    familiares.text?.toString().orEmpty()
                )
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
