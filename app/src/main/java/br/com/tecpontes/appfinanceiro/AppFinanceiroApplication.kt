package br.com.tecpontes.appfinanceiro

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import br.com.tecpontes.appfinanceiro.notifications.NotificationChannels
import br.com.tecpontes.appfinanceiro.notifications.PushRegistrationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppFinanceiroApplication : Application() {

    @Inject lateinit var pushRegistrationManager: PushRegistrationManager

    private val timezoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (Intent.ACTION_TIMEZONE_CHANGED == intent?.action) {
                pushRegistrationManager.requestSync()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        pushRegistrationManager.requestSync()
        ContextCompat.registerReceiver(
            this,
            timezoneReceiver,
            IntentFilter(Intent.ACTION_TIMEZONE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
