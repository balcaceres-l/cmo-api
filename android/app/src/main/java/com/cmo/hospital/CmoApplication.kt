package com.cmo.hospital

import android.app.Application
import com.cmo.hospital.core.di.AppContainer

class CmoApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
