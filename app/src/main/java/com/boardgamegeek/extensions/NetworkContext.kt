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
    var result = false
    val connectivityManager = this?.getSystemService<ConnectivityManager>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager?.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        result = when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    } else {
        connectivityManager.run {
            @Suppress("DEPRECATION")
            connectivityManager?.activeNetworkInfo?.run {
                result = when (type) {
                    ConnectivityManager.TYPE_WIFI -> true
                    ConnectivityManager.TYPE_MOBILE -> true
                    ConnectivityManager.TYPE_ETHERNET -> true
                    else -> false
                }
            }
        }
    }

    return result
}
