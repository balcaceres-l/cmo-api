package com.cmo.hospital

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.databinding.ActivityMainBinding
import com.cmo.hospital.features.auth.presentation.AuthNavigationHost
import com.cmo.hospital.features.auth.presentation.LoginFragment
import com.cmo.hospital.features.doctor.shell.presentation.DoctorShellFragment
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaShellFragment

class MainActivity : AppCompatActivity(), AuthNavigationHost {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            val appContainer = (application as CmoApplication).appContainer
            val activeUser = appContainer.getActiveUserUseCase()
            if (activeUser != null) {
                navigateForRole(activeUser)
            } else {
                replaceRootFragment(LoginFragment())
            }
        }
    }

    override fun onAuthenticated(user: User) {
        navigateForRole(user)
    }

    override fun onLogoutCompleted() {
        replaceRootFragment(LoginFragment())
    }

    private fun navigateForRole(user: User) {
        val target: Fragment = when (user.rol.trim().uppercase()) {
            "MEDICO" -> DoctorShellFragment()
            "RECEPCIONISTA" -> RecepcionistaShellFragment()
            else -> LoginFragment()
        }
        replaceRootFragment(target)
    }

    private fun replaceRootFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.rootFragmentContainer, fragment)
            .commit()
    }
}
