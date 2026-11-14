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
    private val onPedirDisponibilidad: (idMedico: String?, fecha: String, idEspecialidad: String?) -> Unit
) {
    var idMedico: String? = null
        private set
    var idEspecialidad: String? = null
        private set
    var fecha: String = LocalDate.now().toString()
        private set
    var horaInicio: String? = null
        private set

    private data class OpcionMedicoUi(val medico: User?, val etiqueta: String)

    private var nombreEspecialidad: String? = null
    private var conservarSinMedico = false
    private var medicos: List<User> = emptyList()
    private var opcionesFiltradas: List<OpcionMedicoUi> = emptyList()
    private var ultimaDisponibilidad: DisponibilidadUi? = null
    private var ultimaPedida: Triple<String?, String, String?>? = null
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
            val elegido = opcionesFiltradas.getOrNull(position) ?: return@setOnItemClickListener
            if (elegido.medico == null) {
                seleccionarPorEspecialidad()
            } else {
                seleccionarMedico(elegido.medico)
            }
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
     * Valores iniciales (p. ej. los de la cita que se confirma o reprograma). El consultorio
     * no se elige en recepción: al reprogramar, la API conserva el que tenía.
     */
    fun precargar(
        idMedico: String?,
        fecha: String?,
        idEspecialidad: String? = null,
        nombreEspecialidad: String? = null
    ) {
        this.idMedico = idMedico?.takeIf { it.isNotBlank() }
        this.idEspecialidad = idEspecialidad?.takeIf { it.isNotBlank() }
        this.nombreEspecialidad = nombreEspecialidad?.takeIf { it.isNotBlank() }
        this.conservarSinMedico = this.idMedico == null && this.idEspecialidad != null
        val previa = fecha?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        if (previa != null && !previa.isBefore(LocalDate.now())) this.fecha = previa.toString()
        binding.etAgendaFecha.setText(DateUtils.toUiDate(this.fecha))
    }

    fun establecerEspecialidad(
        idEspecialidad: String?,
        nombreEspecialidad: String?,
        listaFiltrada: List<User>
    ) {
        val nuevaEsp = idEspecialidad?.takeIf { it.isNotBlank() }
        val nuevoNombre = nombreEspecialidad?.takeIf { it.isNotBlank() }
        val cambio = nuevaEsp != this.idEspecialidad || nuevoNombre != this.nombreEspecialidad
        this.idEspecialidad = nuevaEsp
        this.nombreEspecialidad = nuevoNombre
        if (cambio && nuevaEsp != null) {
            this.idMedico = null
            this.horaInicio = null
        }
        this.conservarSinMedico = nuevaEsp != null && this.idMedico == null
        mostrarMedicos(listaFiltrada)
    }

    fun mostrarMedicos(lista: List<User>) {
        medicos = lista
        if (!(conservarSinMedico && idEspecialidad != null) && lista.none { it.id == idMedico }) {
            idMedico = if (idEspecialidad != null) null else lista.firstOrNull()?.id
            horaInicio = null
        }
        configurarAdaptadorMedicos()
        sincronizarMedicoSeleccionadoUi()
        if (Triple(idMedico, fecha, idEspecialidad) != ultimaPedida) pedirDisponibilidad()
    }

    fun mostrarDisponibilidad(d: DisponibilidadUi?) {
        // Ignora respuestas de una selección anterior.
        if (d != null && (d.idMedico != idMedico || d.idEspecialidad != idEspecialidad || d.fecha != fecha)) return
        ultimaDisponibilidad = d
        val grupo = binding.chipGroupBloques
        grupo.removeAllViews()
        val aviso = when {
            idMedico == null && idEspecialidad == null -> "No hay médicos activos disponibles."
            d == null -> "Seleccione médico o especialidad y fecha para ver los horarios."
            d.cargando -> "Consultando horarios libres…"
            d.error != null -> d.error
            d.bloques.isEmpty() -> if (idMedico == null) {
                "Sin horarios libres ese día para la especialidad seleccionada."
            } else {
                "Sin horarios libres ese día: el médico no tiene turno o su agenda está llena."
            }
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
        idMedico = idMedico?.takeIf { it.isNotBlank() },
        fecha = fecha,
        horaInicio = horaInicio.orEmpty(),
        idEspecialidad = if (idMedico.isNullOrBlank()) idEspecialidad else null
    )

    fun marcarErrorMedico(mensaje: String?) {
        binding.tilAgendaMedico.error = mensaje
    }

    /** Marca el campo de fecha cuando la API o la validación local lo rechazan. */
    fun marcarErrorFecha(mensaje: String?) {
        binding.tilAgendaFecha.error = mensaje
    }

    private fun seleccionarPorEspecialidad() {
        val cambio = idMedico != null || !conservarSinMedico
        idMedico = null
        conservarSinMedico = true
        binding.tilAgendaMedico.error = null
        sincronizarMedicoSeleccionadoUi()
        if (cambio) {
            horaInicio = null
            pedirDisponibilidad()
        }
    }

    private fun seleccionarMedico(medico: User) {
        val cambio = idMedico != medico.id
        idMedico = medico.id
        conservarSinMedico = false
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
            val esp = actual.especialidadVisible ?: "Consulta Externa"
            binding.tvMedicoSeleccionadoInfo.text = "✓ Médico seleccionado: ${actual.nombreCompleto} • $esp"
            binding.tvMedicoSeleccionadoInfo.visibility = View.VISIBLE
        } else if (idEspecialidad != null) {
            val etiqueta = etiquetaEspecialidadAbierta()
            if (binding.actvAgendaMedico.text?.toString() != etiqueta) {
                binding.actvAgendaMedico.setText(etiqueta, false)
            }
            binding.tvMedicoSeleccionadoInfo.text = "✓ Agenda abierta por especialidad: ${nombreEspecialidad ?: "Seleccionada"}"
            binding.tvMedicoSeleccionadoInfo.visibility = View.VISIBLE
        } else {
            binding.actvAgendaMedico.setText("", false)
            binding.tvMedicoSeleccionadoInfo.visibility = View.GONE
        }
        actualizandoTextoInterno = false
    }

    private fun construirOpciones(): List<OpcionMedicoUi> = buildList {
        if (idEspecialidad != null) {
            add(OpcionMedicoUi(medico = null, etiqueta = etiquetaEspecialidadAbierta()))
        }
        medicos.forEach { add(OpcionMedicoUi(medico = it, etiqueta = etiquetaMedico(it))) }
    }

    private fun configurarAdaptadorMedicos() {
        val todas = construirOpciones()
        opcionesFiltradas = todas
        val adapter = object : ArrayAdapter<String>(
            ctx,
            android.R.layout.simple_dropdown_item_1line,
            todas.map { it.etiqueta }.toMutableList()
        ) {
            private val customFilter = object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.trim().orEmpty()
                    val actual = medicos.firstOrNull { it.id == idMedico }
                    val etiquetaActual = actual?.let { etiquetaMedico(it) }
                        ?: if (idEspecialidad != null) etiquetaEspecialidadAbierta() else null
                    // Si el texto coincide con lo ya elegido o está vacío, mostrar todas las opciones
                    val coincidencias = if (query.isEmpty() || (etiquetaActual != null && query.equals(etiquetaActual, ignoreCase = true))) {
                        todas
                    } else {
                        todas.filter { op ->
                            op.etiqueta.contains(query, ignoreCase = true) ||
                                (op.medico?.especialidadVisible?.contains(query, ignoreCase = true) == true)
                        }
                    }
                    return FilterResults().apply {
                        values = coincidencias
                        count = coincidencias.size
                    }
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    opcionesFiltradas = (results?.values as? List<OpcionMedicoUi>) ?: todas
                    clear()
                    addAll(opcionesFiltradas.map { it.etiqueta })
                    notifyDataSetChanged()
                }
            }

            override fun getFilter(): Filter = customFilter
        }
        binding.actvAgendaMedico.setAdapter(adapter)
    }

    private fun etiquetaEspecialidadAbierta(): String =
        "Cualquier médico disponible — ${nombreEspecialidad ?: "Por especialidad"}"

    private fun etiquetaMedico(medico: User): String {
        val especialidad = medico.especialidadVisible ?: "Medicina General"
        return "${medico.nombreCompleto} — $especialidad"
    }

    private fun pedirDisponibilidad() {
        ultimaPedida = Triple(idMedico, fecha, idEspecialidad)
        binding.tilAgendaFecha.error = null
        onPedirDisponibilidad(idMedico, fecha, idEspecialidad)
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
