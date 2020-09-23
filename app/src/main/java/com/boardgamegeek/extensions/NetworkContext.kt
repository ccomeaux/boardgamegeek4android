@file:JvmName("NetworkUtils")

package com.boardgamegeek.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.getSystemService

fun Context?.isOnWiFi(): Boolean {
    val connectivityManager = this?.getSystemService<ConnectivityManager>()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nc = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
        nc?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    } else {
        @Suppress("Deprecation")
        connectivityManager?.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
    }
}

fun Context?.isOffline(): Boolean {
    val connectivityManager = this?.getSystemService<ConnectivityManager>()
    val isOnline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val capabilities = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
                ?: return false
        when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    } else {
        @Suppress("DEPRECATION")
        connectivityManager?.activeNetworkInfo?.let {
            when (it.type) {
                ConnectivityManager.TYPE_WIFI -> true
                ConnectivityManager.TYPE_MOBILE -> true
                ConnectivityManager.TYPE_ETHERNET -> true
                else -> false
            }
        } ?: false
    }

    return !isOnline
}
