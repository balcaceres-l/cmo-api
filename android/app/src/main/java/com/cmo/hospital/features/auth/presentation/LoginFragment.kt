package com.cmo.hospital.features.auth.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.core.ui.simpleViewModelFactory
import com.cmo.hospital.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        val container = (requireActivity().application as CmoApplication).appContainer
        simpleViewModelFactory { container.createLoginViewModel() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etUsuario.doAfterTextChanged { viewModel.clearError() }
        binding.etPassword.doAfterTextChanged { viewModel.clearError() }

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitLogin()
                true
            } else false
        }

        binding.btnLoginSubmit.setOnClickListener { submitLogin() }

        // Accesos rápidos de demostración
        binding.btnDemoMedicoGeneral.setOnClickListener { fillDemo("medico.general") }
        binding.btnDemoMedicoUrgencias.setOnClickListener { fillDemo("medico.emergencia") }
        binding.btnDemoRecepcion.setOnClickListener { fillDemo("recepcion") }
        binding.btnDemoEnfermeria.setOnClickListener { fillDemo("enfermeria") }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnLoginSubmit.isEnabled = !state.isLoading
                    binding.btnLoginSubmit.text = if (state.isLoading) "" else "Iniciar Sesión"
                    binding.progressLogin.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    if (!state.errorMessage.isNullOrBlank()) {
                        binding.bannerLoginError.visibility = View.VISIBLE
                        binding.tvLoginError.text = state.errorMessage
                    } else {
                        binding.bannerLoginError.visibility = View.GONE
                    }

                    state.loggedInUser?.let { user ->
                        viewModel.consumeLoginEvent()
                        (activity as? AuthNavigationHost)?.onAuthenticated(user)
                    }
                }
            }
        }
    }

    private fun fillDemo(username: String) {
        binding.etUsuario.setText(username)
        binding.etPassword.setText("Hospital2026!")
        viewModel.clearError()
    }

    private fun submitLogin() {
        val user = binding.etUsuario.text?.toString().orEmpty()
        val pass = binding.etPassword.text?.toString().orEmpty()
        viewModel.login(user, pass)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
