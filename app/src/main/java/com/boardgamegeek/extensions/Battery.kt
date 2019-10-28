@file:JvmName("BatteryUtils")

package com.boardgamegeek.extensions

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

fun Context.isCharging(): Boolean {
    val batteryStatus = getBatteryStatus()
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
    return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
}

fun Context.isBatteryLow(): Boolean {
    return getBatteryLevel() < 0.15 // 15% matches system low battery level
}

private fun Context.getBatteryLevel(): Float {
    val batteryStatus = getBatteryStatus()
    return if (batteryStatus != null) {
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 1)
        level / scale.toFloat()
    } else {
        0.5f
    }
}

private fun Context.getBatteryStatus(): Intent? {
    return registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
}
