package com.example.fitnessrecord

import android.app.Application
import com.example.fitnessrecord.util.AppLogger

class FitnessRecordApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        appContainer = AppContainer(this)
    }
}
