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
import com.cmo.hospital.core.domain.model.EspecialidadCatalogo
import com.cmo.hospital.core.domain.model.ProgramacionCita
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
 * Maneja el bloque especialidad → fecha → horario libre.
 * Las citas de recepción se agendan siempre por especialidad (`id_especialidad`),
 * sin asignar un médico fijo (`id_medico = null`).
 */
class SelectorAgendaController(
    private val binding: CompSelectorAgendaBinding,
    private val fragmentManager: FragmentManager,
    private val onPedirDisponibilidad: (idMedico: String?, fecha: String, idEspecialidad: String?) -> Unit
) {
    val idMedico: String? = null
    var idEspecialidad: String? = null
        private set
    var fecha: String = LocalDate.now().toString()
        private set
    var horaInicio: String? = null
        private set

    private var nombreEspecialidad: String? = null
    private var especialidades: List<EspecialidadCatalogo> = emptyList()
    private var opcionesFiltradas: List<EspecialidadCatalogo> = emptyList()
    private var ultimaDisponibilidad: DisponibilidadUi? = null
    private var ultimaPedida: Pair<String, String?>? = null
    private var actualizandoTextoInterno = false

    private val ctx: Context get() = binding.root.context

    init {
        binding.etAgendaFecha.setText(DateUtils.toUiDate(fecha))
        binding.etAgendaFecha.setOnClickListener { abrirCalendario() }
        binding.tilAgendaFecha.setOnClickListener { abrirCalendario() }
        binding.tvAgendaAviso.text = "Seleccione especialidad y fecha para ver los horarios."

        binding.actvAgendaMedico.setOnClickListener {
            binding.actvAgendaMedico.showDropDown()
        }
        binding.actvAgendaMedico.setOnItemClickListener { _, _, position, _ ->
            val elegida = opcionesFiltradas.getOrNull(position) ?: return@setOnItemClickListener
            seleccionarEspecialidad(elegida)
        }
        binding.actvAgendaMedico.doAfterTextChanged { editable ->
            if (actualizandoTextoInterno) return@doAfterTextChanged
            val texto = editable?.toString()?.trim().orEmpty()
            val actual = especialidades.firstOrNull { it.id == idEspecialidad }
            if (actual != null && texto != actual.nombre) {
                binding.tilAgendaMedico.error = null
            }
        }
    }

    /**
     * Valores iniciales (p. ej. al confirmar o reprogramar una cita).
     */
    fun precargar(
        idMedico: String? = null,
        fecha: String?,
        idEspecialidad: String? = null,
        nombreEspecialidad: String? = null
    ) {
        this.idEspecialidad = idEspecialidad?.takeIf { it.isNotBlank() }
        this.nombreEspecialidad = nombreEspecialidad?.takeIf { it.isNotBlank() }
        val previa = fecha?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        if (previa != null && !previa.isBefore(LocalDate.now())) this.fecha = previa.toString()
        binding.etAgendaFecha.setText(DateUtils.toUiDate(this.fecha))
    }

    fun establecerEspecialidad(
        idEspecialidad: String?,
        nombreEspecialidad: String?
    ) {
        val nuevaEsp = idEspecialidad?.takeIf { it.isNotBlank() }
        val nuevoNombre = nombreEspecialidad?.takeIf { it.isNotBlank() }
        val cambio = nuevaEsp != this.idEspecialidad || nuevoNombre != this.nombreEspecialidad
        this.idEspecialidad = nuevaEsp
        this.nombreEspecialidad = nuevoNombre
        binding.tilAgendaMedico.error = null
        sincronizarEspecialidadSeleccionadaUi()
        if (cambio && nuevaEsp != null) {
            this.horaInicio = null
            pedirDisponibilidad()
        }
    }

    fun mostrarEspecialidades(lista: List<EspecialidadCatalogo>) {
        especialidades = lista
        if (lista.none { it.id == idEspecialidad }) {
            val porNombre = nombreEspecialidad?.let { nom ->
                lista.firstOrNull { it.nombre.equals(nom, ignoreCase = true) }
            }
            val medicinaGeneral = lista.firstOrNull {
                it.nombre.equals("Medicina general", ignoreCase = true)
            }
            val elegida = porNombre ?: medicinaGeneral ?: lista.firstOrNull()
            idEspecialidad = elegida?.id
            nombreEspecialidad = elegida?.nombre
            horaInicio = null
        } else if (nombreEspecialidad.isNullOrBlank()) {
            nombreEspecialidad = lista.firstOrNull { it.id == idEspecialidad }?.nombre
        }
        configurarAdaptadorEspecialidades()
        sincronizarEspecialidadSeleccionadaUi()
        if (idEspecialidad != null && (fecha to idEspecialidad) != ultimaPedida) {
            pedirDisponibilidad()
        }
    }

    fun mostrarDisponibilidad(d: DisponibilidadUi?) {
        // Ignora respuestas de una selección anterior.
        if (d != null && (d.idEspecialidad != idEspecialidad || d.fecha != fecha)) return
        ultimaDisponibilidad = d
        val grupo = binding.chipGroupBloques
        grupo.removeAllViews()
        val aviso = when {
            idEspecialidad == null -> "No hay especialidades activas disponibles."
            d == null -> "Seleccione especialidad y fecha para ver los horarios."
            d.cargando -> "Consultando horarios libres…"
            d.error != null -> d.error
            d.bloques.isEmpty() ->
                "Sin horarios libres ese día para esta especialidad (no hay médicos de turno o su agenda está llena)."
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
        idMedico = null,
        fecha = fecha,
        horaInicio = horaInicio.orEmpty(),
        idEspecialidad = idEspecialidad
    )

    fun marcarErrorMedico(mensaje: String?) {
        binding.tilAgendaMedico.error = mensaje
    }

    /** Marca el campo de fecha cuando la API o la validación local lo rechazan. */
    fun marcarErrorFecha(mensaje: String?) {
        binding.tilAgendaFecha.error = mensaje
    }

    private fun seleccionarEspecialidad(esp: EspecialidadCatalogo) {
        val cambio = idEspecialidad != esp.id
        idEspecialidad = esp.id
        nombreEspecialidad = esp.nombre
        binding.tilAgendaMedico.error = null
        sincronizarEspecialidadSeleccionadaUi()
        if (cambio) {
            horaInicio = null
            pedirDisponibilidad()
        }
    }

    private fun sincronizarEspecialidadSeleccionadaUi() {
        val actual = especialidades.firstOrNull { it.id == idEspecialidad }
        val nombre = actual?.nombre ?: nombreEspecialidad
        actualizandoTextoInterno = true
        if (!nombre.isNullOrBlank()) {
            if (binding.actvAgendaMedico.text?.toString() != nombre) {
                binding.actvAgendaMedico.setText(nombre, false)
            }
            binding.tvMedicoSeleccionadoInfo.text = "✓ Agenda por especialidad: $nombre"
            binding.tvMedicoSeleccionadoInfo.visibility = View.VISIBLE
        } else {
            binding.actvAgendaMedico.setText("", false)
            binding.tvMedicoSeleccionadoInfo.visibility = View.GONE
        }
        actualizandoTextoInterno = false
    }

    private fun configurarAdaptadorEspecialidades() {
        val todas = especialidades
        opcionesFiltradas = todas
        val adapter = object : ArrayAdapter<String>(
            ctx,
            android.R.layout.simple_dropdown_item_1line,
            todas.map { it.nombre }.toMutableList()
        ) {
            private val customFilter = object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.trim().orEmpty()
                    val actualNombre = especialidades.firstOrNull { it.id == idEspecialidad }?.nombre
                    val coincidencias = if (query.isEmpty() || (actualNombre != null && query.equals(actualNombre, ignoreCase = true))) {
                        todas
                    } else {
                        todas.filter { it.nombre.contains(query, ignoreCase = true) }
                    }
                    return FilterResults().apply {
                        values = coincidencias
                        count = coincidencias.size
                    }
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    opcionesFiltradas = (results?.values as? List<EspecialidadCatalogo>) ?: todas
                    clear()
                    addAll(opcionesFiltradas.map { it.nombre })
                    notifyDataSetChanged()
                }
            }

            override fun getFilter(): Filter = customFilter
        }
        binding.actvAgendaMedico.setAdapter(adapter)
    }

    private fun pedirDisponibilidad() {
        ultimaPedida = fecha to idEspecialidad
        binding.tilAgendaFecha.error = null
        onPedirDisponibilidad(null, fecha, idEspecialidad)
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
