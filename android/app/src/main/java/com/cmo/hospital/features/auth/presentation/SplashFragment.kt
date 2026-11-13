package com.cmo.hospital.features.auth.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cmo.hospital.CmoApplication
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface SplashNavigationHost {
    fun onSplashSessionValid(user: User)
    fun onSplashRequireLogin()
}

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardSplashLogo.alpha = 0f
        binding.cardSplashLogo.scaleX = 0.9f
        binding.cardSplashLogo.scaleY = 0.9f
        binding.cardSplashLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(420L)
            .start()

        viewLifecycleOwner.lifecycleScope.launch {
            val container = (requireActivity().application as CmoApplication).appContainer
            val localUser = container.getActiveUserUseCase()
            delay(650L)

            if (localUser == null) {
                (activity as? SplashNavigationHost)?.onSplashRequireLogin()
                return@launch
            }

            binding.tvSplashStatus.text = "Verificando sesión activa..."
            val meOk = try {
                val res = container.apiService.getMe()
                res.isSuccessful && res.body()?.success == true
            } catch (_: Exception) {
                // Si el backend no responde en frío, permitimos continuar con la sesión local
                true
            }

            if (meOk) {
                (activity as? SplashNavigationHost)?.onSplashSessionValid(localUser)
            } else {
                container.logoutUseCase()
                (activity as? SplashNavigationHost)?.onSplashRequireLogin()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

