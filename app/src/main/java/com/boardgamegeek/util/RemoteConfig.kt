@file:JvmName("RemoteConfig")

package com.boardgamegeek.util

import com.boardgamegeek.BuildConfig
import com.boardgamegeek.R
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
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

        const val KEY_REFRESH_GAME_MINUTES = "refresh_game_minutes"
        const val KEY_REFRESH_GAME_COLLECTION_MINUTES = "refresh_game_collection_minutes"
        const val KEY_REFRESH_GAME_PLAYS_PARTIAL_MINUTES = "refresh_game_plays_partial_minutes"
        const val KEY_REFRESH_GAME_PLAYS_FULL_HOURS = "refresh_game_plays_full_hours"

        const val KEY_RETRY_202_INITIAL_INTERVAL_MILLIS = "retry_202_initial_interval_millis"
        const val KEY_RETRY_202_RANDOMIZATION_FACTOR = "retry_202_randomization_factor"
        const val KEY_RETRY_202_MULTIPLIER = "retry_202_multiplier"
        const val KEY_RETRY_202_MAX_INTERVAL_MILLIS = "retry_202_max_interval_millis"
        const val KEY_RETRY_202_MAX_ELAPSED_MILLIS = "retry_202_max_elapsed_millis"
        const val KEY_RETRY_429_MAX_BACKOFF_COUNT = "retry_429_max_backoff_count"

        const val KEY_FETCH_IMAGE_WITH_API = "fetch_image_with_api"

        const val KEY_PRIVACY_CHECK_WEEKS = "privacy_check_weeks"

        @JvmStatic
        fun init() {
            val firebaseRemoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0L else 3600L
            }
            firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
            firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        }

        @JvmStatic
        fun fetch() {
            val firebaseRemoteConfig = Firebase.remoteConfig
            firebaseRemoteConfig.fetch().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.i("Successfully fetched Firebase remote config.")
                    firebaseRemoteConfig.activate()
                } else {
                    Timber.i(task.exception, "Failed to fetch Firebase remote config.")
                }
            }
        }

        @JvmStatic
        fun getBoolean(key: String) = Firebase.remoteConfig.getBoolean(key)

        @JvmStatic
        fun getInt(key: String) = Firebase.remoteConfig.getLong(key).toInt()

        fun getLong(key: String) = Firebase.remoteConfig.getLong(key)

        fun getDouble(key: String) = Firebase.remoteConfig.getDouble(key)
    }
}
