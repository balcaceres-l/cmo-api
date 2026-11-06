package com.cmo.hospital.features.recepcionista.citas.presentation

import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.ConsultorioInfo
import com.cmo.hospital.core.domain.model.ProgramacionCita
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.databinding.CompSelectorAgendaBinding
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Maneja el bloque médico → fecha → consultorio → horario libre. Cada vez que
 * cambian médico o fecha pide la disponibilidad a la API mediante
 * [onPedirDisponibilidad]; los bloques que muestra son solo los que devolvió.
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
    var idConsultorio: String? = null
        private set
    var horaInicio: String? = null
        private set

    private var medicos: List<User> = emptyList()
    private var consultorios: List<ConsultorioInfo> = emptyList()
    private var ultimaDisponibilidad: DisponibilidadUi? = null
    private var ultimaPedida: Pair<String?, String>? = null

    private val ctx: Context get() = binding.root.context

    init {
        binding.etAgendaFecha.setText(fecha)
        binding.etAgendaFecha.setOnClickListener { abrirCalendario() }
        binding.tilAgendaFecha.setOnClickListener { abrirCalendario() }
        binding.tvAgendaAviso.text = "Seleccione médico y fecha para ver los horarios."
    }

    /** Valores iniciales (p. ej. los de la cita que se reprograma). */
    fun precargar(idMedico: String?, fecha: String?, idConsultorio: String?) {
        this.idMedico = idMedico
        val previa = fecha?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        if (previa != null && !previa.isBefore(LocalDate.now())) this.fecha = previa.toString()
        this.idConsultorio = idConsultorio
        binding.etAgendaFecha.setText(this.fecha)
    }

    fun mostrarMedicos(lista: List<User>) {
        medicos = lista
        if (lista.none { it.id == idMedico }) {
            idMedico = lista.firstOrNull()?.id
            horaInicio = null
        }
        if (idMedico to fecha != ultimaPedida) pedirDisponibilidad()
        pintarMedicos()
    }

    fun mostrarConsultorios(lista: List<ConsultorioInfo>) {
        consultorios = lista
        if (lista.none { it.id == idConsultorio }) idConsultorio = null
        pintarConsultorios()
    }

    fun mostrarDisponibilidad(d: DisponibilidadUi?) {
        // Ignora respuestas de una selección anterior.
        if (d != null && (d.idMedico != idMedico || d.fecha != fecha)) return
        ultimaDisponibilidad = d
        val grupo = binding.chipGroupBloques
        grupo.removeAllViews()
        val aviso = when {
            idMedico == null -> "No hay médicos activos para esta especialidad."
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
        horaInicio = horaInicio.orEmpty(),
        idConsultorio = idConsultorio
    )

    /** Marca el campo de fecha cuando la API o la validación local lo rechazan. */
    fun marcarErrorFecha(mensaje: String?) {
        binding.tilAgendaFecha.error = mensaje
    }

    private fun pedirDisponibilidad() {
        ultimaPedida = idMedico to fecha
        binding.tilAgendaFecha.error = null
        onPedirDisponibilidad(idMedico, fecha)
    }

    private fun pintarMedicos() {
        val grupo = binding.chipGroupMedicos
        grupo.removeAllViews()
        medicos.forEach { m ->
            grupo.addView(chip(m.nombreCompleto, m.id == idMedico) {
                if (idMedico != m.id) {
                    idMedico = m.id
                    horaInicio = null
                    pintarMedicos()
                    pedirDisponibilidad()
                }
            })
        }
    }

    private fun pintarConsultorios() {
        val grupo = binding.chipGroupConsultorios
        grupo.removeAllViews()
        grupo.addView(chip("Sin asignar", idConsultorio == null) {
            idConsultorio = null
            pintarConsultorios()
        })
        consultorios.forEach { c ->
            val texto = listOfNotNull(c.codigo, c.nombre).joinToString(" · ")
            grupo.addView(chip(texto, c.id == idConsultorio) {
                idConsultorio = c.id
                pintarConsultorios()
            })
        }
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
                binding.etAgendaFecha.setText(elegida)
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
