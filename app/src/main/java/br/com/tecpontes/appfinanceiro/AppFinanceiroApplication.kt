package br.com.tecpontes.appfinanceiro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppFinanceiroApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
