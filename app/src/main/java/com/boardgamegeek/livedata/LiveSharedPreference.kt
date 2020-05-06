package com.boardgamegeek.livedata

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.extensions.preferences

@Suppress("UNCHECKED_CAST")
class LiveSharedPreference<T>(context: Context, preferenceKey: String, sharedPreferencesName: String? = null) : MutableLiveData<T>() {
    private val listener: SharedPreferences.OnSharedPreferenceChangeListener
    private val sharedPreferences: SharedPreferences = context.preferences(sharedPreferencesName)

    init {
        listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == preferenceKey) {
                value = sharedPreferences.all[key] as T
            }
        }
        value = sharedPreferences.all[preferenceKey] as T
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