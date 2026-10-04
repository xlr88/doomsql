package com.chaduvukondi.firstu

import android.app.Application
import com.chaduvukondi.firstu.di.AppContainer
import com.chaduvukondi.firstu.di.DefaultAppContainer

class DoomSqlApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
