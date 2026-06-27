package com.example.fitnessrecord

import android.app.Application

class FitnessRecordApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
