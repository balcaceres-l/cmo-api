package com.cmo.hospital.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Fábrica ligera y tipada para instanciar ViewModels inyectados con casos de uso de Clean Architecture.
 */
inline fun <reified VM : ViewModel> simpleViewModelFactory(
    crossinline creator: () -> VM
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}
