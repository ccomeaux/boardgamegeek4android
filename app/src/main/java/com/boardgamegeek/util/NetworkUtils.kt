package com.boardgamegeek.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.getSystemService

object NetworkUtils {
    @JvmStatic
    fun isOnWiFi(context: Context?): Boolean {
        val connectivityManager = context?.getSystemService<ConnectivityManager>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nc = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
            nc?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        } else {
            @Suppress("Deprecation")
            connectivityManager?.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }
}
