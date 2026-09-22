package com.manish.doomsql

import android.app.Application
import com.manish.doomsql.di.AppContainer
import com.manish.doomsql.di.DefaultAppContainer

class DoomSqlApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
