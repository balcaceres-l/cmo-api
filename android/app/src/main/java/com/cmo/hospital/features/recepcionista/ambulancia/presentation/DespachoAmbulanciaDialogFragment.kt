package com.cmo.hospital.features.recepcionista.ambulancia.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.DialogDespachoAmbulanciaBinding
import com.cmo.hospital.databinding.DialogReporteAmbulanciaBinding
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.CatalogoAmbulancia
import com.cmo.hospital.features.recepcionista.citas.presentation.chipAgenda
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** Base de los diálogos de ambulancia: comparten el ViewModel del fragmento padre. */
abstract class DialogoAmbulanciaBase : DialogFragment() {

    protected val viewModel: AmbulanciaGuardiaViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        factoryProducer = {
            val container = (requireActivity().application as CmoApplication).appContainer
            simpleViewModelFactory { container.createAmbulanciaGuardiaViewModel() }
        }
    )

    /** Chips de opción única; devuelve la opción elegida en cada momento. */
    protected fun opciones(grupo: ChipGroup, valores: List<String>, alElegir: () -> Unit = {}): () -> String? {
        var elegido: String? = null
        fun pintar() {
            grupo.removeAllViews()
            valores.forEach { v ->
                grupo.addView(chipAgenda(requireContext(), CatalogoAmbulancia.etiqueta(v), v == elegido).apply {
                    setOnClickListener {
                        elegido = v
                        pintar()
                        alElegir()
                    }
                })
            }
        }
        pintar()
        return { elegido }
    }

    protected fun observarErrores(dialogo: DialogoAmbulancia, mostrar: (ErrorDialogo) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorDialogo.collect { err -> if (err?.dialogo == dialogo) mostrar(err) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.limpiarErrorDialogo()
    }
}

class DespachoAmbulanciaDialogFragment : DialogoAmbulanciaBase() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = DialogDespachoAmbulanciaBinding.inflate(layoutInflater)
        val prioridad = opciones(b.chipGroupPrioridad, CatalogoAmbulancia.PRIORIDADES)

        b.btnDespachoCancelar.setOnClickListener { dismiss() }
        b.btnDespachoConfirmar.setOnClickListener {
            b.tvDespachoError.visibility = View.GONE
            listOf(b.tilDespachoDireccion, b.tilDespachoPaciente, b.tilDespachoDescripcion).forEach { it.error = null }
            viewModel.despachar(
                direccion = b.etDespachoDireccion.text?.toString().orEmpty(),
                paciente = b.etDespachoPaciente.text?.toString().orEmpty(),
                descripcion = b.etDespachoDescripcion.text?.toString().orEmpty(),
                prioridad = prioridad()
            ) { dismiss() }
        }
        observarErrores(DialogoAmbulancia.DESPACHO) { err ->
            when (err.campo) {
                "direccion" -> b.tilDespachoDireccion.error = err.mensaje
                "paciente_nombre" -> b.tilDespachoPaciente.error = err.mensaje
                "descripcion" -> b.tilDespachoDescripcion.error = err.mensaje
            }
            b.tvDespachoError.text = err.mensaje
            b.tvDespachoError.visibility = View.VISIBLE
        }
        return MaterialAlertDialogBuilder(requireContext()).setView(b.root).create()
    }
}

class ReporteAmbulanciaDialogFragment : DialogoAmbulanciaBase() {

    private val idServicio: String get() = requireArguments().getString(ARG_SERVICIO).orEmpty()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = DialogReporteAmbulanciaBinding.inflate(layoutInflater)
        val estado = opciones(b.chipGroupEstadoPaciente, CatalogoAmbulancia.ESTADOS_PACIENTE)

        b.btnReporteCancelar.setOnClickListener { dismiss() }
        b.btnReporteEnviar.setOnClickListener {
            b.tvReporteError.visibility = View.GONE
            b.tilReporteMinutos.error = null
            viewModel.enviarReporte(
                idServicio = idServicio,
                estadoPaciente = estado(),
                observaciones = b.etReporteObservaciones.text?.toString(),
                minutos = b.etReporteMinutos.text?.toString().orEmpty()
            ) { dismiss() }
        }
        observarErrores(DialogoAmbulancia.REPORTE) { err ->
            if (err.campo == "minutos_llegada") b.tilReporteMinutos.error = err.mensaje
            b.tvReporteError.text = err.mensaje
            b.tvReporteError.visibility = View.VISIBLE
        }
        return MaterialAlertDialogBuilder(requireContext()).setView(b.root).create()
    }

    companion object {
        private const val ARG_SERVICIO = "id_servicio"

        fun nuevo(idServicio: String) = ReporteAmbulanciaDialogFragment().apply {
            arguments = bundleOf(ARG_SERVICIO to idServicio)
        }
    }
}
