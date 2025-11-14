package com.boardgamegeek.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.boardgamegeek.BuildConfig
import com.boardgamegeek.util.PreferencesUtils
import timber.log.Timber

class CancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            SyncService.ACTION_CANCEL_SYNC ->
                cancelSync(context, "Sync cancelled at user request.")
            Intent.ACTION_BATTERY_LOW ->
                cancelSync(context, "Cancelling sync because battery is running low.")
            Intent.ACTION_POWER_DISCONNECTED ->
                if (PreferencesUtils.getSyncOnlyCharging(context)) {
                    cancelSync(context, "Cancelling sync because device was unplugged and user asked for this behavior.")
                }
            // CONNECTIVITY_ACTION handling removed - now handled by NetworkCallback in SyncAdapter
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
