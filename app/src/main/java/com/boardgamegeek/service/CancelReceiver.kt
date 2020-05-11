package com.boardgamegeek.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.widget.Toast
import com.boardgamegeek.BuildConfig
import com.boardgamegeek.extensions.*
import timber.log.Timber

class CancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.preferences()
        when (intent.action) {
            SyncService.ACTION_CANCEL_SYNC ->
                cancelSync(context, "Sync cancelled at user request.")
            Intent.ACTION_BATTERY_LOW ->
                cancelSync(context, "Cancelling sync because battery is running low.")
            Intent.ACTION_POWER_DISCONNECTED ->
                if (prefs[KEY_SYNC_ONLY_CHARGING, false] == true) {
                    cancelSync(context, "Cancelling sync because device was unplugged and user asked for this behavior.")
                }
            @Suppress("deprecation")
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                if (prefs[KEY_SYNC_ONLY_WIFI, false] == true && !context.isOnWiFi()) {
                    cancelSync(context, "Cancelling sync because device lost Wifi and user asked for this behavior.")
                }
            }
            else -> notifyCause(context, "Not cancelling sync due to an unexpected action: " + intent.action)
        }
    }

    private fun notifyCause(context: Context, message: String) {
        Timber.i(message)
        if (BuildConfig.DEBUG) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun cancelSync(context: Context, message: String) {
        notifyCause(context, message)
        SyncService.cancelSync(context)
    }
}
