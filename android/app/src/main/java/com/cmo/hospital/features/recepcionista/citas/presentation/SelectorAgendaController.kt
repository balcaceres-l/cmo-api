package com.cmo.hospital.features.recepcionista.citas.presentation

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.ProgramacionCita
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.ui.DateUtils
import com.cmo.hospital.databinding.CompSelectorAgendaBinding
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Maneja el bloque médico → fecha → horario libre.
 * El selector de médico utiliza un menú desplegable con búsqueda integrada por nombre
 * o especialidad para escalar cuando existen múltiples médicos en el sistema.
 */
class SelectorAgendaController(
    private val binding: CompSelectorAgendaBinding,
    private val fragmentManager: FragmentManager,
    private val onPedirDisponibilidad: (idMedico: String?, fecha: String) -> Unit
) {
    var idMedico: String? = null
        private set
    var fecha: String = LocalDate.now().toString()
        private set
    var horaInicio: String? = null
        private set

    private var medicos: List<User> = emptyList()
    private var medicosFiltrados: List<User> = emptyList()
    private var ultimaDisponibilidad: DisponibilidadUi? = null
    private var ultimaPedida: Pair<String?, String>? = null
    private var actualizandoTextoInterno = false

    private val ctx: Context get() = binding.root.context

    init {
        binding.etAgendaFecha.setText(DateUtils.toUiDate(fecha))
        binding.etAgendaFecha.setOnClickListener { abrirCalendario() }
        binding.tilAgendaFecha.setOnClickListener { abrirCalendario() }
        binding.tvAgendaAviso.text = "Seleccione médico y fecha para ver los horarios."

        binding.actvAgendaMedico.setOnClickListener {
            binding.actvAgendaMedico.showDropDown()
        }
        binding.actvAgendaMedico.setOnItemClickListener { _, _, position, _ ->
            val elegido = medicosFiltrados.getOrNull(position) ?: return@setOnItemClickListener
            seleccionarMedico(elegido)
        }
        binding.actvAgendaMedico.doAfterTextChanged { editable ->
            if (actualizandoTextoInterno) return@doAfterTextChanged
            val texto = editable?.toString()?.trim().orEmpty()
            val actual = medicos.firstOrNull { it.id == idMedico }
            if (actual != null && texto != etiquetaMedico(actual)) {
                // El usuario está escribiendo para buscar otro médico
                binding.tilAgendaMedico.error = null
            }
        }
    }

    /**
     * Valores iniciales (p. ej. los de la cita que se reprograma). El consultorio
     * no se elige en recepción: al reprogramar, la API conserva el que tenía.
     */
    fun precargar(idMedico: String?, fecha: String?) {
        this.idMedico = idMedico
        val previa = fecha?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        if (previa != null && !previa.isBefore(LocalDate.now())) this.fecha = previa.toString()
        binding.etAgendaFecha.setText(DateUtils.toUiDate(this.fecha))
    }

    fun mostrarMedicos(lista: List<User>) {
        medicos = lista
        if (lista.none { it.id == idMedico }) {
            idMedico = lista.firstOrNull()?.id
            horaInicio = null
        }
        configurarAdaptadorMedicos()
        sincronizarMedicoSeleccionadoUi()
        if (idMedico to fecha != ultimaPedida) pedirDisponibilidad()
    }

    fun mostrarDisponibilidad(d: DisponibilidadUi?) {
        // Ignora respuestas de una selección anterior.
        if (d != null && (d.idMedico != idMedico || d.fecha != fecha)) return
        ultimaDisponibilidad = d
        val grupo = binding.chipGroupBloques
        grupo.removeAllViews()
        val aviso = when {
            idMedico == null -> "No hay médicos activos disponibles."
            d == null -> "Seleccione médico y fecha para ver los horarios."
            d.cargando -> "Consultando horarios libres…"
            d.error != null -> d.error
            d.bloques.isEmpty() -> "Sin horarios libres ese día: el médico no tiene turno o su agenda está llena."
            else -> null
        }
        val bloques = d?.takeIf { !it.cargando && it.error == null }?.bloques.orEmpty()
        if (bloques.none { it.horaInicio == horaInicio }) horaInicio = null
        bloques.forEach { b ->
            grupo.addView(chip(b.horaInicio, b.horaInicio == horaInicio) {
                horaInicio = b.horaInicio
                mostrarDisponibilidad(ultimaDisponibilidad)
            })
        }
        binding.tvAgendaAviso.text = aviso ?: horaInicio?.let { h ->
            val fin = bloques.firstOrNull { it.horaInicio == h }?.horaFin
            "Horario elegido: $h – $fin"
        } ?: "Seleccione uno de los ${bloques.size} horarios libres."
    }

    fun programacion(): ProgramacionCita = ProgramacionCita(
        idMedico = idMedico.orEmpty(),
        fecha = fecha,
        horaInicio = horaInicio.orEmpty()
    )

    fun marcarErrorMedico(mensaje: String?) {
        binding.tilAgendaMedico.error = mensaje
    }

    /** Marca el campo de fecha cuando la API o la validación local lo rechazan. */
    fun marcarErrorFecha(mensaje: String?) {
        binding.tilAgendaFecha.error = mensaje
    }

    private fun seleccionarMedico(medico: User) {
        val cambio = idMedico != medico.id
        idMedico = medico.id
        binding.tilAgendaMedico.error = null
        sincronizarMedicoSeleccionadoUi()
        if (cambio) {
            horaInicio = null
            pedirDisponibilidad()
        }
    }

    private fun sincronizarMedicoSeleccionadoUi() {
        val actual = medicos.firstOrNull { it.id == idMedico }
        actualizandoTextoInterno = true
        if (actual != null) {
            val etiqueta = etiquetaMedico(actual)
            if (binding.actvAgendaMedico.text?.toString() != etiqueta) {
                binding.actvAgendaMedico.setText(etiqueta, false)
            }
            val esp = actual.cargo?.takeIf { it.isNotBlank() } ?: "Consulta Externa"
            binding.tvMedicoSeleccionadoInfo.text = "✓ Médico seleccionado: ${actual.nombreCompleto} • $esp"
            binding.tvMedicoSeleccionadoInfo.visibility = View.VISIBLE
        } else {
            binding.actvAgendaMedico.setText("", false)
            binding.tvMedicoSeleccionadoInfo.visibility = View.GONE
        }
        actualizandoTextoInterno = false
    }

    private fun configurarAdaptadorMedicos() {
        medicosFiltrados = medicos
        val adapter = object : ArrayAdapter<String>(
            ctx,
            android.R.layout.simple_dropdown_item_1line,
            medicos.map { etiquetaMedico(it) }.toMutableList()
        ) {
            private val customFilter = object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.trim().orEmpty()
                    val actual = medicos.firstOrNull { it.id == idMedico }
                    // Si el texto coincide con el médico ya elegido o está vacío, mostrar todos los médicos
                    val coincidencias = if (query.isEmpty() || (actual != null && query.equals(etiquetaMedico(actual), ignoreCase = true))) {
                        medicos
                    } else {
                        medicos.filter { m ->
                            m.nombreCompleto.contains(query, ignoreCase = true) ||
                                (m.cargo?.contains(query, ignoreCase = true) == true)
                        }
                    }
                    return FilterResults().apply {
                        values = coincidencias
                        count = coincidencias.size
                    }
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    medicosFiltrados = (results?.values as? List<User>) ?: medicos
                    clear()
                    addAll(medicosFiltrados.map { etiquetaMedico(it) })
                    notifyDataSetChanged()
                }
            }

            override fun getFilter(): Filter = customFilter
        }
        binding.actvAgendaMedico.setAdapter(adapter)
    }

    private fun etiquetaMedico(medico: User): String {
        val especialidad = medico.cargo?.takeIf { it.isNotBlank() } ?: "Medicina General"
        return "${medico.nombreCompleto} — $especialidad"
    }

    private fun pedirDisponibilidad() {
        ultimaPedida = idMedico to fecha
        binding.tilAgendaFecha.error = null
        onPedirDisponibilidad(idMedico, fecha)
    }

    private fun abrirCalendario() {
        if (fragmentManager.findFragmentByTag(TAG_CALENDARIO) != null) return
        val seleccion = LocalDate.parse(fecha).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Fecha de la cita")
            .setSelection(seleccion)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.from(hoyUtcMillis()))
                    .build()
            )
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val elegida = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
            if (elegida != fecha) {
                fecha = elegida
                binding.etAgendaFecha.setText(DateUtils.toUiDate(elegida))
                horaInicio = null
                pedirDisponibilidad()
            }
        }
        picker.show(fragmentManager, TAG_CALENDARIO)
    }

    private fun hoyUtcMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun chip(texto: String, marcado: Boolean, onClick: () -> Unit): Chip =
        chipAgenda(ctx, texto, marcado).apply { setOnClickListener { onClick() } }

    companion object {
        private const val TAG_CALENDARIO = "calendario_cita"
    }
}

/** Chip con los colores de selección de la app (mismo estilo en toda la agenda). */
fun chipAgenda(ctx: Context, texto: String, marcado: Boolean): Chip = Chip(ctx).apply {
    text = texto
    isCheckable = true
    isChecked = marcado
    isCheckedIconVisible = false
    textSize = 12f
    setTextColor(ContextCompat.getColor(ctx, if (marcado) R.color.huc_on_primary else R.color.huc_on_surface))
    chipBackgroundColor = ColorStateList.valueOf(
        ContextCompat.getColor(ctx, if (marcado) R.color.huc_primary_container else R.color.huc_surface_low)
    )
}
