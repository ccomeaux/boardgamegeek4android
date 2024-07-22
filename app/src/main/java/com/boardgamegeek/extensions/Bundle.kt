package com.boardgamegeek.extensions

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

fun Bundle?.getBooleanOrElse(key: String, defaultValue: Boolean) = this?.getBoolean(key, defaultValue) ?: defaultValue

fun Bundle?.getIntOrElse(key: String, defaultValue: Int) = this?.getInt(key, defaultValue) ?: defaultValue

inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

@Suppress("SameParameterValue")
inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String): ArrayList<T>? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayList(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}
