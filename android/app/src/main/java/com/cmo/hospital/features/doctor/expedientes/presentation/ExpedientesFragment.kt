package com.cmo.hospital.features.doctor.expedientes.presentation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.R
import com.cmo.hospital.components.LabOrderCardBinder
import com.cmo.hospital.components.VitalsGridBinder
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.CompLabOrderCardBinding
import com.cmo.hospital.databinding.FragmentDoctorExpedientesBinding
import com.cmo.hospital.features.doctor.laboratorio.presentation.IndicarExamenLabDialogFragment
import com.cmo.hospital.features.doctor.shell.presentation.DoctorShellHost
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class ExpedientesFragment : Fragment() {

    private var _binding: FragmentDoctorExpedientesBinding? = null
    private val binding get() = _binding!!
    private var dropdownAbierto = false
    private lateinit var dropdownAdapter: PacientesDropdownAdapter

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

        dropdownAdapter = PacientesDropdownAdapter { exp ->
            viewModel.selectExpediente(exp.paciente.numeroExpediente)
            cerrarDropdownYTeclado()
        }
        binding.rvPacientesDropdown.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientesDropdown.adapter = dropdownAdapter

        binding.etBuscarExpediente.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) abrirDropdown()
        }
        binding.etBuscarExpediente.setOnClickListener {
            abrirDropdown()
        }
        binding.btnCerrarDropdownPacientes.setOnClickListener {
            cerrarDropdownYTeclado()
        }

        binding.etBuscarExpediente.doAfterTextChanged { editable ->
            val currentText = editable?.toString().orEmpty()
            if (binding.etBuscarExpediente.hasFocus() && !dropdownAbierto) {
                abrirDropdown()
            }
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
                        if (!binding.etBuscarExpediente.hasFocus() &&
                            binding.etBuscarExpediente.text?.toString().orEmpty() != state.query
                        ) {
                            binding.etBuscarExpediente.setText(state.query)
                            binding.etBuscarExpediente.setSelection(state.query.length)
                        }
                        renderSelectorPacientes(state)
                        val exp = state.expedienteSeleccionado
                        if (exp == null) {
                            binding.tvExpEmptySearch.visibility = View.VISIBLE
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

    private fun abrirDropdown() {
        dropdownAbierto = true
        binding.containerDropdownPacientes.visibility = View.VISIBLE
    }

    private fun cerrarDropdownYTeclado() {
        dropdownAbierto = false
        binding.containerDropdownPacientes.visibility = View.GONE
        binding.etBuscarExpediente.clearFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etBuscarExpediente.windowToken, 0)
    }

    private fun renderSelectorPacientes(state: ExpedientesUiState) {
        val q = state.query.trim().lowercase()
        val ordenados = state.resultados.sortedWith(
            compareBy<ExpedienteDetalle> { exp ->
                if (q.isNotEmpty() && exp.paciente.nombreCompleto.lowercase().startsWith(q)) 0 else 1
            }.thenBy { it.paciente.nombreCompleto.lowercase() }
        )
        val selNum = state.expedienteSeleccionado?.paciente?.numeroExpediente
        binding.tvDropdownPacientesCount.text = if (ordenados.isEmpty()) {
            "Sin coincidencias para \"$q\""
        } else {
            "Pacientes (${ordenados.size} • A–Z)"
        }
        dropdownAdapter.submit(ordenados, selNum)
        binding.rvPacientesDropdown.visibility = if (ordenados.isEmpty()) View.GONE else View.VISIBLE
        binding.containerDropdownPacientes.visibility = if (dropdownAbierto) View.VISIBLE else View.GONE
    }

    private class PacientesDropdownAdapter(
        private val onSelect: (ExpedienteDetalle) -> Unit
    ) : RecyclerView.Adapter<PacientesDropdownAdapter.VH>() {

        private var items: List<ExpedienteDetalle> = emptyList()
        private var selectedExp: String? = null

        fun submit(list: List<ExpedienteDetalle>, selected: String?) {
            items = list
            selectedExp = selected
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(24, 22, 24, 22)
                textSize = 13.5f
            }
            return VH(tv)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val exp = items[position]
            val p = exp.paciente
            val isSel = p.numeroExpediente != null && p.numeroExpediente == selectedExp
            val tv = holder.itemView as TextView
            val duiStr = p.dui?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
            tv.text = "${p.nombreCompleto} (${p.numeroExpediente ?: "S/E"})$duiStr"
            tv.setTextColor(
                ContextCompat.getColor(
                    tv.context,
                    if (isSel) R.color.cmo_primary else R.color.cmo_text
                )
            )
            tv.setBackgroundColor(
                ContextCompat.getColor(
                    tv.context,
                    if (isSel) R.color.cmo_primary_soft else R.color.huc_transparent
                )
            )
            tv.setOnClickListener { onSelect(exp) }
        }

        class VH(view: View) : RecyclerView.ViewHolder(view)
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
        val dniStr = pac.dui ?: "S/D"
        val prevStr = pac.prevision ?: "Particular / Privado"
        val sangreStr = pac.tipoSangre ?: "O+"
        binding.tvExpPacienteMeta.text =
            "$edadStr • DNI: $dniStr • Sangre: $sangreStr • $prevStr"
        binding.tvExpCamaBadge.text = "Ubicación: ${exp.cama ?: "Consulta Ambulatoria"}"

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
        val comorbilidades = exp.comorbilidades.ifEmpty { listOf("Sin comorbilidades crónicas registradas") }
        comorbilidades.forEach { item ->
            val chip = Chip(requireContext()).apply {
                text = item
                isClickable = false
                isCheckable = false
            }
            binding.chipGroupComorbilidades.addView(chip)
        }

        // 7 Chips de Signos Vitales
        VitalsGridBinder.bindAllSeven(
            paChip = binding.vitalExpPa,
            pulsoChip = binding.vitalExpPulso,
            tempChip = binding.vitalExpTemp,
            spo2Chip = binding.vitalExpSpo2,
            pesoChip = binding.vitalExpPeso,
            tallaChip = binding.vitalExpTalla,
            imcChip = binding.vitalExpImc,
            signos = exp.signosVitales
        )

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
