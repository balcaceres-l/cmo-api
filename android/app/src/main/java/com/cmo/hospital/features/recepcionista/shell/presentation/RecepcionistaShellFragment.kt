package com.cmo.hospital.features.recepcionista.shell.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.R
import com.cmo.hospital.components.HucToastManager
import com.cmo.hospital.components.HucTopBarBinder
import com.cmo.hospital.databinding.FragmentRecepcionistaShellBinding
import com.cmo.hospital.features.auth.presentation.AuthNavigationHost
import com.cmo.hospital.features.recepcionista.admision.presentation.AdmisionFragment
import com.cmo.hospital.features.recepcionista.ambulancia.presentation.AmbulanciaGuardiaFragment
import com.cmo.hospital.features.recepcionista.citas.presentation.CitasFragment
import kotlinx.coroutines.launch

interface RecepcionistaToastHost {
    fun showToastSuccess(title: String, message: String)
    fun showToastError(title: String, message: String)
}

class RecepcionistaShellFragment : Fragment(), RecepcionistaToastHost {

    private var _binding: FragmentRecepcionistaShellBinding? = null
    private val binding get() = _binding!!
    private var toastManager: HucToastManager? = null

    companion object {
        private const val TAG_CITAS = "tab_recep_citas"
        private const val TAG_ADMISION = "tab_recep_admision"
        private const val TAG_AMBULANCIA = "tab_recep_ambulancia"
        private const val KEY_SELECTED_ITEM = "selected_recep_item"
    }

    private var selectedItemId: Int = R.id.nav_recep_admision

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecepcionistaShellBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContainer = (requireActivity().application as CmoApplication).appContainer
        val currentUser = appContainer.getActiveUserUseCase()
        toastManager = HucToastManager(binding.toastOverlay)

        HucTopBarBinder.bind(
            binding = binding.topBar,
            moduleSubtitle = "PACIENTES",
            currentUser = currentUser,
            guardiaLabel = "Guardia Activa",
            onGuardiaClick = {
                binding.bottomNavRecepcionista.selectedItemId = R.id.nav_recep_ambulancia
            },
            onLogoutClick = {
                viewLifecycleOwner.lifecycleScope.launch {
                    appContainer.logoutUseCase()
                    (activity as? AuthNavigationHost)?.onLogoutCompleted()
                }
            }
        )

        selectedItemId = savedInstanceState?.getInt(KEY_SELECTED_ITEM) ?: R.id.nav_recep_admision
        switchTab(selectedItemId)
        binding.bottomNavRecepcionista.selectedItemId = selectedItemId

        binding.bottomNavRecepcionista.setOnItemSelectedListener { item ->
            selectedItemId = item.itemId
            switchTab(item.itemId)
            true
        }
    }

    private fun switchTab(itemId: Int) {
        val subtitle = when (itemId) {
            R.id.nav_recep_citas -> "CITAS & SALA"
            R.id.nav_recep_admision -> "PACIENTES"
            R.id.nav_recep_ambulancia -> "GUARDIA & FLOTA"
            else -> "PACIENTES"
        }
        HucTopBarBinder.updateSubtitle(binding.topBar, subtitle)

        val targetTag = when (itemId) {
            R.id.nav_recep_citas -> TAG_CITAS
            R.id.nav_recep_admision -> TAG_ADMISION
            R.id.nav_recep_ambulancia -> TAG_AMBULANCIA
            else -> TAG_ADMISION
        }

        val fm = childFragmentManager
        val tx = fm.beginTransaction()

        listOf(TAG_CITAS, TAG_ADMISION, TAG_AMBULANCIA).forEach { tag ->
            fm.findFragmentByTag(tag)?.let { frag ->
                if (tag != targetTag && !frag.isHidden) {
                    tx.hide(frag)
                }
            }
        }

        val existing = fm.findFragmentByTag(targetTag)
        if (existing != null) {
            tx.show(existing)
        } else {
            val newFrag: Fragment = when (targetTag) {
                TAG_CITAS -> CitasFragment()
                TAG_ADMISION -> AdmisionFragment()
                else -> AmbulanciaGuardiaFragment()
            }
            tx.add(R.id.recepcionistaTabContainer, newFrag, targetTag)
        }
        tx.commit()
    }

    override fun showToastSuccess(title: String, message: String) {
        toastManager?.showSuccess(title, message)
    }

    override fun showToastError(title: String, message: String) {
        toastManager?.showError(title, message)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_ITEM, selectedItemId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toastManager?.clear()
        toastManager = null
        _binding = null
    }
}
