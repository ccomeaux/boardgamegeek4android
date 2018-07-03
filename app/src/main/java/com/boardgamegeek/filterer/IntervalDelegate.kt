package com.boardgamegeek.filterer

import kotlin.reflect.KProperty

class IntervalDelegate(var value: Int, private val minValue: Int, private val maxValue: Int) {
    operator fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        this.value = value.coerceIn(minValue, maxValue)
    }
}

class DoubleIntervalDelegate(var value: Double, private val minValue: Double, private val maxValue: Double) {
    operator fun getValue(thisRef: Any, property: KProperty<*>): Double {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
        this.value = value.coerceIn(minValue, maxValue)
    }
}