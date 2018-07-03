package com.boardgamegeek.filterer

import kotlin.reflect.KProperty

class IntervalDelegate(var value: Int, private val minValue: Int, private val maxValue: Int) {
    operator fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        value.coerceIn(minValue, maxValue)
    }
}