package com.cmo.hospital

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.session.EventosSesion
import com.cmo.hospital.databinding.ActivityMainBinding
import com.cmo.hospital.features.auth.presentation.AuthNavigationHost
import com.cmo.hospital.features.auth.presentation.LoginFragment
import com.cmo.hospital.features.doctor.shell.presentation.DoctorShellFragment
import com.cmo.hospital.features.recepcionista.shell.presentation.RecepcionistaShellFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), AuthNavigationHost {

    private lateinit var binding: ActivityMainBinding

    private val appContainer get() = (application as CmoApplication).appContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            val activeUser = appContainer.getActiveUserUseCase()
            if (activeUser != null) {
                navigateForRole(activeUser)
            } else {
                replaceRootFragment(LoginFragment())
            }
        }

        // La API rechazó el token (401): de vuelta al login sin cerrar la app.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                EventosSesion.sesionExpirada.collect {
                    if (supportFragmentManager.findFragmentById(R.id.rootFragmentContainer) !is LoginFragment) {
                        Toast.makeText(
                            this@MainActivity,
                            "Tu sesión expiró. Inicia sesión de nuevo.",
                            Toast.LENGTH_LONG
                        ).show()
                        irAlLogin()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Si el 401 llegó con la app en segundo plano, la sesión ya se borró.
        val actual = supportFragmentManager.findFragmentById(R.id.rootFragmentContainer)
        if (actual != null && actual !is LoginFragment && appContainer.getActiveUserUseCase() == null) {
            irAlLogin()
        }
    }

    override fun onAuthenticated(user: User) {
        navigateForRole(user)
    }

    override fun onLogoutCompleted() {
        irAlLogin()
    }

    private fun irAlLogin() {
        appContainer.clinicalDataSource.limpiar()
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
        if (isFinishing || isDestroyed) return
        // Tolera que la navegación llegue justo después de onSaveInstanceState
        // (p. ej. la respuesta del logout con la app pasando a segundo plano).
        supportFragmentManager.beginTransaction()
            .replace(R.id.rootFragmentContainer, fragment)
            .commitAllowingStateLoss()
    }
}
