package com.cmo.hospital.features.doctor.shell.presentation

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
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.databinding.FragmentDoctorShellBinding
import com.cmo.hospital.features.auth.presentation.AuthNavigationHost
import com.cmo.hospital.features.doctor.consulta.presentation.ConsultaFragment
import com.cmo.hospital.features.doctor.expedientes.presentation.ExpedientesFragment
import com.cmo.hospital.features.doctor.inicio.presentation.InicioDoctorFragment
import com.cmo.hospital.features.doctor.laboratorio.presentation.LaboratorioFragment
import kotlinx.coroutines.launch

class DoctorShellFragment : Fragment(), DoctorShellHost {

    private var _binding: FragmentDoctorShellBinding? = null
    private val binding get() = _binding!!
    private var toastManager: HucToastManager? = null

    companion object {
        private const val TAG_INICIO = "tab_doc_inicio"
        private const val TAG_CONSULTA = "tab_doc_consulta"
        private const val TAG_EXPEDIENTES = "tab_doc_expedientes"
        private const val TAG_LABORATORIO = "tab_doc_laboratorio"
        private const val KEY_SELECTED_ITEM = "selected_doc_item"
    }

    private var selectedItemId: Int = R.id.nav_doc_inicio

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorShellBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContainer = (requireActivity().application as CmoApplication).appContainer
        val currentUser = appContainer.getActiveUserUseCase()
        toastManager = HucToastManager(binding.toastOverlayDoctor)

        HucTopBarBinder.bind(
            binding = binding.topBarDoctor,
            moduleSubtitle = "CITAS & PACIENTES",
            currentUser = currentUser,
            guardiaLabel = "En Turno • Box 03",
            secondaryIconRes = R.drawable.ic_event,
            onSecondaryClick = {
                openAgendarSeguimientoDialog(
                    appContainer.clinicalDataSource.consultaActiva.value.paciente
                )
            },
            onLogoutClick = {
                viewLifecycleOwner.lifecycleScope.launch {
                    appContainer.logoutUseCase()
                    (activity as? AuthNavigationHost)?.onLogoutCompleted()
                }
            }
        )

        selectedItemId = savedInstanceState?.getInt(KEY_SELECTED_ITEM) ?: R.id.nav_doc_inicio
        switchTab(selectedItemId)
        binding.bottomNavDoctor.selectedItemId = selectedItemId

        binding.bottomNavDoctor.setOnItemSelectedListener { item ->
            selectedItemId = item.itemId
            switchTab(item.itemId)
            true
        }
    }

    private fun switchTab(itemId: Int) {
        val subtitle = when (itemId) {
            R.id.nav_doc_inicio -> "CITAS & PACIENTES"
            R.id.nav_doc_consulta -> "CONSULTA ACTIVA"
            R.id.nav_doc_expedientes -> "EXPEDIENTES HCE"
            R.id.nav_doc_laboratorio -> "LABORATORIO LIS"
            else -> "CITAS & PACIENTES"
        }
        HucTopBarBinder.updateSubtitle(binding.topBarDoctor, subtitle)

        val targetTag = when (itemId) {
            R.id.nav_doc_inicio -> TAG_INICIO
            R.id.nav_doc_consulta -> TAG_CONSULTA
            R.id.nav_doc_expedientes -> TAG_EXPEDIENTES
            R.id.nav_doc_laboratorio -> TAG_LABORATORIO
            else -> TAG_INICIO
        }

        val fm = childFragmentManager
        val tx = fm.beginTransaction()

        listOf(TAG_INICIO, TAG_CONSULTA, TAG_EXPEDIENTES, TAG_LABORATORIO).forEach { tag ->
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
                TAG_INICIO -> InicioDoctorFragment()
                TAG_CONSULTA -> ConsultaFragment()
                TAG_EXPEDIENTES -> ExpedientesFragment()
                else -> LaboratorioFragment()
            }
            tx.add(R.id.doctorTabContainer, newFrag, targetTag)
        }
        tx.commit()
    }

    override fun showToastSuccess(title: String, message: String) {
        toastManager?.showSuccess(title, message)
    }

    override fun showToastError(title: String, message: String) {
        toastManager?.showError(title, message)
    }

    override fun navigateToInicioTab() {
        binding.bottomNavDoctor.selectedItemId = R.id.nav_doc_inicio
    }

    override fun navigateToConsultaTab() {
        binding.bottomNavDoctor.selectedItemId = R.id.nav_doc_consulta
    }

    override fun navigateToExpedientesTab(numeroExpediente: String?) {
        if (!numeroExpediente.isNullOrBlank()) {
            val appContainer = (requireActivity().application as CmoApplication).appContainer
            appContainer.clinicalDataSource.solicitarAperturaExpediente(numeroExpediente)
        }
        binding.bottomNavDoctor.selectedItemId = R.id.nav_doc_expedientes
    }

    override fun navigateToLaboratorioTab() {
        binding.bottomNavDoctor.selectedItemId = R.id.nav_doc_laboratorio
    }

    override fun openAgendarSeguimientoDialog(pacientePreseleccionado: Paciente?) {
        val appContainer = (requireActivity().application as CmoApplication).appContainer
        val user = appContainer.getActiveUserUseCase()
        val consulta = appContainer.clinicalDataSource.consultaActiva.value
        AgendarSeguimientoDialogFragment().apply {
            this.pacientePreseleccionado = pacientePreseleccionado
            this.medicoNombre = user?.nombreCompleto ?: consulta.medicoNombre
            this.especialidad = user?.cargo ?: consulta.especialidad
            this.onCitaAgendada = { cita ->
                showToastSuccess(
                    "Control Médico Agendado",
                    "${cita.paciente?.nombreCompleto} • ${cita.fecha} (${cita.horaInicio} - ${cita.horaFin})"
                )
            }
        }.show(childFragmentManager, "dialog_agendar_seguimiento_doc")
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
