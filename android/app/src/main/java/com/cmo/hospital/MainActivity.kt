package com.cmo.hospital

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cmo.hospital.core.domain.model.CargoPerfil
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.databinding.ActivityMainBinding
import com.cmo.hospital.features.auth.presentation.AuthNavigationHost
import com.cmo.hospital.features.auth.presentation.LoginFragment
import com.cmo.hospital.features.auth.presentation.SplashFragment
import com.cmo.hospital.features.auth.presentation.SplashNavigationHost
import com.cmo.hospital.features.doctor.shell.presentation.DoctorShellFragment
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaShellFragment

class MainActivity : AppCompatActivity(), AuthNavigationHost, SplashNavigationHost {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            replaceRootFragment(SplashFragment())
        }
    }

    override fun onSplashSessionValid(user: User) {
        navigateForRole(user)
    }

    override fun onSplashRequireLogin() {
        replaceRootFragment(LoginFragment())
    }

    override fun onAuthenticated(user: User) {
        navigateForRole(user)
    }

    override fun onLogoutCompleted() {
        replaceRootFragment(LoginFragment())
    }

    private fun navigateForRole(user: User) {
        val target: Fragment = when (user.cargoPerfil) {
            CargoPerfil.MEDICO_CONSULTA,
            CargoPerfil.MEDICO_EMERGENCIA -> DoctorShellFragment()
            CargoPerfil.RECEPCIONISTA,
            CargoPerfil.ENFERMERA -> RecepcionistaShellFragment()
            CargoPerfil.NO_PERMITIDO -> LoginFragment()
        }
        replaceRootFragment(target)
    }

    private fun replaceRootFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.rootFragmentContainer, fragment)
            .commit()
    }
}
