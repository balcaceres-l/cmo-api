package com.cmo.hospital.components

import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import com.cmo.hospital.R
import com.cmo.hospital.databinding.CompHucToastBinding

/**
 * Controlador del banner/toast flotante inferior inspirado en el prototipo HTML HUC Móvil.
 */
class HucToastManager(
    private val binding: CompHucToastBinding
) {
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        binding.toastRoot.visibility = View.GONE
    }

    init {
        binding.btnToastClose.setOnClickListener {
            handler.removeCallbacks(hideRunnable)
            binding.toastRoot.visibility = View.GONE
        }
    }

    fun showSuccess(title: String, message: String, durationMs: Long = 4500L) {
        val ctx = binding.root.context
        handler.removeCallbacks(hideRunnable)
        binding.toastRoot.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary))
        binding.toastIconBox.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary_container))
        binding.ivToastIcon.setImageResource(R.drawable.ic_check)
        binding.ivToastIcon.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
        binding.tvToastTitle.text = title
        binding.tvToastMessage.text = message
        binding.tvToastMessage.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary_fixed_dim))
        binding.toastRoot.visibility = View.VISIBLE
        handler.postDelayed(hideRunnable, durationMs)
    }

    fun showError(title: String, message: String, durationMs: Long = 5000L) {
        val ctx = binding.root.context
        handler.removeCallbacks(hideRunnable)
        binding.toastRoot.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_error))
        binding.toastIconBox.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_on_error_container))
        binding.ivToastIcon.setImageResource(R.drawable.ic_warning)
        binding.ivToastIcon.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_error_container))
        binding.tvToastTitle.text = title
        binding.tvToastMessage.text = message
        binding.tvToastMessage.setTextColor(ContextCompat.getColor(ctx, R.color.huc_error_container))
        binding.toastRoot.visibility = View.VISIBLE
        handler.postDelayed(hideRunnable, durationMs)
    }

    fun clear() {
        handler.removeCallbacks(hideRunnable)
        binding.toastRoot.visibility = View.GONE
    }
}
