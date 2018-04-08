@file:JvmName("RemoteConfig")

package com.boardgamegeek.util

import com.boardgamegeek.BuildConfig
import com.boardgamegeek.R
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import timber.log.Timber


class RemoteConfig {
    companion object {
        const val KEY_SYNC_ENABLED = "sync_enabled"
        const val KEY_SYNC_BUDDIES_DAYS = "sync_buddies_days"
        const val KEY_SYNC_BUDDIES_MAX = "sync_buddies_max"
        const val KEY_SYNC_BUDDIES_FETCH_INTERVAL_DAYS = "sync_buddies_fetch_interval_days"
        const val KEY_SYNC_BUDDIES_FETCH_PAUSE_MILLIS = "sync_buddies_fetch_pause_millis"
        const val KEY_SYNC_COLLECTION_FETCH_INTERVAL_DAYS = "sync_collection_fetch_interval_days"
        const val KEY_SYNC_COLLECTION_FETCH_PAUSE_MILLIS = "sync_collection_fetch_pause_millis"
        const val KEY_SYNC_COLLECTION_GAMES_PER_FETCH = "sync_collection_games_per_fetch"
        const val KEY_SYNC_COLLECTION_FETCH_MAX = "sync_collection_fetch_max"
        const val KEY_SYNC_GAMES_PER_FETCH = "sync_games_per_fetch"
        const val KEY_SYNC_GAMES_FETCH_MAX = "sync_games_fetch_max"
        const val KEY_SYNC_GAMES_FETCH_MAX_UNUPDATED = "sync_games_fetch_max_unupdated"
        const val KEY_SYNC_GAMES_FETCH_PAUSE_MILLIS = "sync_games_fetch_pause_millis"
        const val KEY_SYNC_GAMES_DELETE_VIEW_HOURS = "sync_games_delete_view_hours"
        const val KEY_SYNC_PLAYS_FETCH_PAUSE_MILLIS = "sync_plays_fetch_pause_millis"

        @JvmStatic
        fun init() {
            val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                    .setDeveloperModeEnabled(BuildConfig.DEBUG)
                    .build()
            firebaseRemoteConfig.setConfigSettings(configSettings)
            firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults)
            fetch()
        }

        @JvmStatic
        fun fetch() {
            val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
            val cacheExpiration = if (firebaseRemoteConfig.info.configSettings.isDeveloperModeEnabled) 0L else 43200L
            firebaseRemoteConfig.fetch(cacheExpiration).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.i("Successfully fetched Firebase remote config.")
                    firebaseRemoteConfig.activateFetched()
                } else {
                    Timber.i(task.exception, "Failed to fetch Firebase remote config.")
                }
            }
        }

        @JvmStatic
        fun getBoolean(key: String) = FirebaseRemoteConfig.getInstance().getBoolean(key)

        @JvmStatic
        fun getInt(key: String) = FirebaseRemoteConfig.getInstance().getLong(key).toInt()

        @JvmStatic
        fun getLong(key: String) = FirebaseRemoteConfig.getInstance().getLong(key)
    }
}
