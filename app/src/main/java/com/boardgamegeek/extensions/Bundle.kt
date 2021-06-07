package com.boardgamegeek.extensions

import android.os.Bundle

fun Bundle?.getBooleanOrElse(key: String, defaultValue: Boolean) = this?.getBoolean(key, defaultValue)
        ?: defaultValue

fun Bundle?.getIntOrElse(key: String, defaultValue: Int) = this?.getInt(key, defaultValue)
        ?: defaultValue