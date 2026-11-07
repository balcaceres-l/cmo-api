package com.cmo.hospital.features.recepcionista.ambulancia.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.R
import com.cmo.hospital.components.HucMicroBannerBinder
import com.cmo.hospital.core.domain.model.AmbulanciaRecurso
import com.cmo.hospital.core.domain.model.ServicioAmbulanciaDetalle
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentRecepAmbulanciaBinding
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.CatalogoAmbulancia
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaToastHost
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AmbulanciaGuardiaFragment : Fragment() {

    private var _binding: FragmentRecepAmbulanciaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AmbulanciaGuardiaViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createAmbulanciaGuardiaViewModel() }
    }

    private lateinit var personalAdapter: PersonalGuardiaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecepAmbulanciaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        HucMicroBannerBinder.bind(
            binding = binding.microBannerAmbulancia,
            title = "Centro Médico de Occidente - Ambulancia",
            subtitle = "Despacho, reportes en ruta y médicos de turno",
            badgeText = "24/7",
            iconRes = R.drawable.ic_ambulance
        )

        personalAdapter = PersonalGuardiaAdapter()
        binding.rvPersonalGuardia.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPersonalGuardia.adapter = personalAdapter

        // El estado lo cambia la API (despachar/finalizar) o el administrador: aquí solo se muestra.
        listOf(binding.btnAmbDisponible, binding.btnAmbEnServicio, binding.btnAmbMantenimiento).forEach {
            it.isClickable = false
            it.isFocusable = false
        }

        binding.btnAbrirModalDespacho.setOnClickListener {
            DespachoAmbulanciaDialogFragment().show(childFragmentManager, "dialog_despacho")
        }

        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refrescar()
    }

    private fun styleStatusButton(btn: MaterialButton, selected: Boolean) {
        val ctx = requireContext()
        btn.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(ctx, if (selected) R.color.huc_primary_container else R.color.huc_surface_lowest)
        )
        btn.setTextColor(ContextCompat.getColor(ctx, if (selected) R.color.huc_on_primary else R.color.huc_primary_container))
    }

    private fun pintarAmbulancia(amb: AmbulanciaRecurso) {
        val ctx = requireContext()
        binding.tvAmbulanciaPlaca.text = "Placa: ${amb.placa.ifBlank { "—" }}"
        binding.tvAmbulanciaEstadoPill.text = CatalogoAmbulancia.etiqueta(amb.estado)
        val (fondo, texto) = when (amb.estado) {
            "DISPONIBLE" -> R.drawable.bg_pill_primary_fixed to R.color.huc_on_primary_fixed
            "EN_SERVICIO" -> R.drawable.bg_pill_error_container to R.color.huc_on_error_container
            else -> R.drawable.bg_pill_tertiary_fixed to R.color.huc_on_tertiary_fixed
        }
        binding.tvAmbulanciaEstadoPill.setBackgroundResource(fondo)
        binding.tvAmbulanciaEstadoPill.setTextColor(ContextCompat.getColor(ctx, texto))

        styleStatusButton(binding.btnAmbDisponible, amb.estado == "DISPONIBLE")
        styleStatusButton(binding.btnAmbEnServicio, amb.estado == "EN_SERVICIO")
        styleStatusButton(binding.btnAmbMantenimiento, amb.estado == "EN_MANTENIMIENTO")

        val srv = amb.servicioActivo
        binding.cardMisionActiva.visibility = if (srv != null) View.VISIBLE else View.GONE
        binding.btnAbrirModalDespacho.isEnabled = amb.estado == "DISPONIBLE"
        binding.btnAbrirModalDespacho.text = when (amb.estado) {
            "DISPONIBLE" -> "Despachar ambulancia"
            "EN_SERVICIO" -> "Ambulancia en servicio"
            else -> "Ambulancia en mantenimiento"
        }
        if (srv == null) return

        binding.tvMisionTitulo.text = "🚨 SERVICIO EN CURSO • ${CatalogoAmbulancia.etiqueta(srv.prioridad).uppercase()}"
        binding.tvMisionEta.text = "Salió ${hora(srv.horaSalida ?: srv.horaSolicitud)}"
        binding.tvMisionPacienteYDesc.text = "${srv.pacienteNombre ?: "Paciente"} • ${srv.descripcion.orEmpty()}"
        binding.tvMisionDireccionYDestino.text = "Dirección: ${srv.direccion ?: "N/D"}"
        binding.tvMisionUltimoReporte.text = srv.ultimoReporte?.let { r ->
            buildString {
                append("Último reporte ${hora(r.fecha)} • ${CatalogoAmbulancia.etiqueta(r.estadoPaciente)}")
                r.llegadaEstimada?.let { append(" • llega ${hora(it)}") }
                r.observaciones?.takeIf { it.isNotBlank() }?.let { append("\n$it") }
            }
        } ?: "Sin reportes del equipo en ruta."
        binding.btnMisionReportar.setOnClickListener {
            ReporteAmbulanciaDialogFragment.nuevo(srv.id).show(childFragmentManager, "dialog_reporte")
        }
        binding.btnMisionFinalizar.setOnClickListener { confirmarFinalizar(srv) }
    }

    private fun confirmarFinalizar(srv: ServicioAmbulanciaDetalle) {
        val ctx = requireContext()
        val campo = TextInputEditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val contenedor = FrameLayout(ctx).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(TextInputLayout(ctx).apply {
                hint = "Notas de cierre (opcional)"
                addView(campo)
            })
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Finalizar servicio")
            .setMessage("${srv.pacienteNombre ?: "Paciente"} • ${srv.direccion.orEmpty()}")
            .setView(contenedor)
            .setNegativeButton("Volver", null)
            .setPositiveButton("Finalizar") { _, _ -> viewModel.finalizar(srv.id, campo.text?.toString()) }
            .show()
    }

    private fun pintarBitacora(servicios: List<ServicioAmbulanciaDetalle>) {
        val ctx = requireContext()
        binding.llBitacora.removeAllViews()
        binding.tvBitacoraResumen.text =
            if (servicios.isEmpty()) "Sin servicios registrados." else "Últimos ${servicios.size} servicios."
        servicios.forEach { s ->
            binding.llBitacora.addView(TextView(ctx).apply {
                val pad = (6 * resources.displayMetrics.density).toInt()
                setPadding(0, pad, 0, pad)
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface))
                text = buildString {
                    append("${fechaHora(s.horaSolicitud)} • ${CatalogoAmbulancia.etiqueta(s.prioridad)} • ${CatalogoAmbulancia.etiqueta(s.estado)}\n")
                    append("${s.pacienteNombre ?: "Paciente"} — ${s.direccion.orEmpty()}")
                    s.notas?.takeIf { it.isNotBlank() }?.let { append("\nNotas: $it") }
                }
            })
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.estado.collect { st ->
                        val panel = st.panel
                        if (panel != null) {
                            pintarAmbulancia(panel.ambulancia)
                            pintarBitacora(panel.bitacora)
                            personalAdapter.submitList(panel.medicosDeTurno)
                            binding.tvGuardiaResumen.text = if (panel.medicosDeTurno.isEmpty()) {
                                "No hay médicos con turno en curso."
                            } else "Según los turnos registrados para hoy."
                        } else if (st.error != null) {
                            binding.tvAmbulanciaPlaca.text = "No se pudo cargar: ${st.error}"
                            binding.tvBitacoraResumen.text = "Sin datos."
                        }
                        binding.tvAmbTiempoReal.text = if (st.enVivo) "● En vivo" else "○ Sin conexión en vivo (se actualiza al volver)"
                        binding.btnMisionFinalizar.isEnabled = !st.enviando
                    }
                }
                launch {
                    viewModel.aviso.collect { av ->
                        if (av != null) {
                            val host = parentFragment as? RecepcionistaToastHost
                            if (av.esError) host?.showToastError(av.titulo, av.mensaje)
                            else host?.showToastSuccess(av.titulo, av.mensaje)
                            viewModel.consumirAviso()
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

    private companion object {
        val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val FECHA_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")

        fun local(iso: String?) = iso?.let {
            runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull()
        }

        fun hora(iso: String?) = local(iso)?.format(HORA) ?: "—"
        fun fechaHora(iso: String?) = local(iso)?.format(FECHA_HORA) ?: "—"
    }
}
