package br.com.tecpontes.appfinanceiro

import android.app.Application
import br.com.tecpontes.appfinanceiro.notifications.NotificationChannels
import br.com.tecpontes.appfinanceiro.notifications.NotificationPollingScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppFinanceiroApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        NotificationPollingScheduler.schedule(this)
        NotificationPollingScheduler.runNow(this)
    }
}
