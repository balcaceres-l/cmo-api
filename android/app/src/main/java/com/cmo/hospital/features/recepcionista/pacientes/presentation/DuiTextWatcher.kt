package com.cmo.hospital.features.recepcionista.pacientes.presentation

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * Formatea el DUI mientras se escribe: el usuario solo teclea los 9 dígitos
 * y el guion se coloca antes del dígito verificador (00000000-0).
 */
class DuiTextWatcher(private val editText: EditText) : TextWatcher {

    private var formateando = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(s: Editable?) {
        if (formateando || s == null) return
        val formateado = formatear(s.toString())
        if (formateado == s.toString()) return
        formateando = true
        editText.setText(formateado)
        editText.setSelection(formateado.length)
        formateando = false
    }

    companion object {
        fun formatear(texto: String): String {
            val digitos = texto.filter { it.isDigit() }.take(9)
            return if (digitos.length > 8) "${digitos.take(8)}-${digitos.drop(8)}" else digitos
        }
    }
}
