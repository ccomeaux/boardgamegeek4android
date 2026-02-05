package com.boardgamegeek.livedata

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.extensions.preferences

@Suppress("UNCHECKED_CAST")
class LiveSharedPreferencePrefix<T>(context: Context, keyPrefix: String, sharedPreferencesName: String? = null) : MutableLiveData<Map<String, T>>() {
    private val listener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key?.startsWith(keyPrefix) == true) {
                (sharedPreferences.all[key] as? T)?.let {
                    val copy = value?.toMutableMap() ?: mutableMapOf()
                    copy[key] = it
                    value = copy
                }
            }
        }
    private val sharedPreferences: SharedPreferences = context.preferences(sharedPreferencesName)

    init {
        val initialMap = mutableMapOf<String, T>()
        sharedPreferences.all
            .filter { it.key.startsWith(keyPrefix) }
            .forEach { entry: Map.Entry<String, Any?> ->
                (entry.value as? T)?.let {
                    initialMap[entry.key] = it
                }
            }
        value = initialMap
    }

    override fun onActive() {
        super.onActive()
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onInactive() {
        super.onInactive()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
