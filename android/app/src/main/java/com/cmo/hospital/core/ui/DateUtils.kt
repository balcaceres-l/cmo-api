package com.cmo.hospital.core.ui

import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utilidades para formateo y captura uniforme de fechas en formato DD/MM/AAAA (dd/MM/yyyy)
 * para la interfaz de usuario, manteniendo compatibilidad transparente con el formato ISO (AAAA-MM-DD)
 * requerido para persistencia en base de datos e interoperabilidad con la API.
 */
object DateUtils {

    private val UI_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val ISO_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    fun todayUi(): String = LocalDate.now().format(UI_FORMATTER)

    fun todayIso(): String = LocalDate.now().format(ISO_FORMATTER)

    fun toUiDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        val trimmed = dateStr.trim()
        if (trimmed.matches(Regex("""\d{2}/\d{2}/\d{4}"""))) {
            return trimmed
        }
        return try {
            val isoPart = if (trimmed.length >= 10) trimmed.substring(0, 10) else trimmed
            val parsed = LocalDate.parse(isoPart, ISO_FORMATTER)
            parsed.format(UI_FORMATTER)
        } catch (_: Exception) {
            trimmed
        }
    }

    fun toIsoDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        val trimmed = dateStr.trim()
        if (trimmed.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            return trimmed
        }
        return try {
            val parts = trimmed.split("/")
            if (parts.size == 3) {
                val d = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                val y = parts[2]
                val parsed = LocalDate.parse("$d/$m/$y", UI_FORMATTER)
                parsed.format(ISO_FORMATTER)
            } else {
                trimmed
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    fun showDatePicker(
        fragment: Fragment,
        initialDateUi: String? = null,
        title: String = "Seleccionar Fecha",
        onDateSelected: (String) -> Unit
    ) {
        val initialEpochMillis = try {
            val iso = toIsoDate(initialDateUi)
            val date = if (iso.isNotBlank()) LocalDate.parse(iso, ISO_FORMATTER) else LocalDate.now()
            date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(initialEpochMillis)
            .build()

        picker.addOnPositiveButtonClickListener { selectionMillis ->
            val date = Instant.ofEpochMilli(selectionMillis)
                .atZone(ZoneId.of("UTC"))
                .toLocalDate()
            onDateSelected(date.format(UI_FORMATTER))
        }

        picker.show(fragment.parentFragmentManager, "date_picker_${System.currentTimeMillis()}")
    }
}
