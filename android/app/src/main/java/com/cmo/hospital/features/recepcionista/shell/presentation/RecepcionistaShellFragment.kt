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
import com.cmo.hospital.core.domain.model.CargoAccessPolicy
import com.cmo.hospital.databinding.FragmentRecepcionistaShellBinding
import com.cmo.hospital.features.auth.presentation.AuthNavigationHost
import com.cmo.hospital.features.recepcionista.admision.presentation.AdmisionFragment
import com.cmo.hospital.features.recepcionista.ambulancia.presentation.AmbulanciaGuardiaFragment
import com.cmo.hospital.features.recepcionista.citas.presentation.CitasFragment
import com.cmo.hospital.features.recepcionista.insumos.presentation.InsumosEnfermeriaFragment
import com.cmo.hospital.features.recepcionista.laboratorio.presentation.LaboratorioRecepcionFragment
import com.cmo.hospital.features.recepcionista.pacientes.presentation.PacientesFragment
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
        private const val TAG_PACIENTES = "tab_recep_pacientes"
        private const val TAG_LABORATORIO = "tab_recep_laboratorio"
        private const val TAG_ADMISION = "tab_recep_admision"
        private const val TAG_INSUMOS = "tab_enf_insumos"
        private const val TAG_AMBULANCIA = "tab_recep_ambulancia"
        private const val KEY_SELECTED_ITEM = "selected_recep_item"
    }

    private var esEnfermeria: Boolean = false
    private var selectedItemId: Int = R.id.nav_recep_citas

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
        esEnfermeria = CargoAccessPolicy.esEnfermeria(currentUser)
        toastManager = HucToastManager(binding.toastOverlay)

        // Separación por cargo: Enfermería vs Recepción de Mostrador
        binding.bottomNavRecepcionista.menu.clear()
        if (esEnfermeria) {
            binding.bottomNavRecepcionista.inflateMenu(R.menu.menu_enfermeria_bottom)
        } else {
            binding.bottomNavRecepcionista.inflateMenu(R.menu.menu_recepcionista_bottom)
        }

        val defaultTab = if (esEnfermeria) R.id.nav_recep_admision else R.id.nav_recep_citas
        selectedItemId = savedInstanceState?.getInt(KEY_SELECTED_ITEM) ?: defaultTab

        HucTopBarBinder.bind(
            binding = binding.topBar,
            moduleSubtitle = subtitleFor(selectedItemId),
            currentUser = currentUser,
            guardiaLabel = if (esEnfermeria) "En Piso" else "Mostrador",
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

        switchTab(selectedItemId)
        binding.bottomNavRecepcionista.selectedItemId = selectedItemId

        binding.bottomNavRecepcionista.setOnItemSelectedListener { item ->
            selectedItemId = item.itemId
            switchTab(item.itemId)
            true
        }
    }

    private fun subtitleFor(itemId: Int): String = when (itemId) {
        R.id.nav_recep_citas -> "CITAS Y SALA"
        R.id.nav_recep_pacientes -> "REGISTRO DE PACIENTES"
        R.id.nav_recep_admision -> if (esEnfermeria) "CENSO DE PISO (17 CAMAS)" else "HOSPITALIZACIÓN (17 CAMAS)"
        R.id.nav_enf_insumos -> "INSUMOS Y BOTIQUÍN"
        R.id.nav_recep_laboratorio -> "LABORATORIO LIS"
        R.id.nav_recep_ambulancia -> if (esEnfermeria) "TELEMETRÍA AMBULANCIA" else "GUARDIA Y AMBULANCIA"
        else -> "HOSPITAL CMO"
    }

    private fun switchTab(itemId: Int) {
        HucTopBarBinder.updateSubtitle(binding.topBar, subtitleFor(itemId))

        val targetTag = when (itemId) {
            R.id.nav_recep_citas -> TAG_CITAS
            R.id.nav_recep_pacientes -> TAG_PACIENTES
            R.id.nav_recep_admision -> TAG_ADMISION
            R.id.nav_enf_insumos -> TAG_INSUMOS
            R.id.nav_recep_laboratorio -> TAG_LABORATORIO
            R.id.nav_recep_ambulancia -> TAG_AMBULANCIA
            else -> if (esEnfermeria) TAG_ADMISION else TAG_CITAS
        }

        val fm = childFragmentManager
        val tx = fm.beginTransaction()

        listOf(TAG_CITAS, TAG_PACIENTES, TAG_ADMISION, TAG_INSUMOS, TAG_LABORATORIO, TAG_AMBULANCIA).forEach { tag ->
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
                TAG_PACIENTES -> PacientesFragment()
                TAG_INSUMOS -> InsumosEnfermeriaFragment()
                TAG_LABORATORIO -> LaboratorioRecepcionFragment()
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
