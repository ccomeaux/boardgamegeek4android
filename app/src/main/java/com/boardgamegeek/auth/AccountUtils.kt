package com.boardgamegeek.auth

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set

object AccountUtils {
    private const val KEY_PREFIX = "account_"
    private const val KEY_USERNAME = KEY_PREFIX + "username"
    private const val KEY_FULL_NAME = KEY_PREFIX + "full_name"
    private const val KEY_AVATAR_URL = KEY_PREFIX + "avatar_url"

    fun clearFields(context: Context?) {
        setUsername(context, null)
        setFullName(context, null)
        setAvatarUrl(context, null)
    }

    fun setUsername(context: Context?, username: String?) {
        setString(context, username, KEY_USERNAME)
        if (!username.isNullOrEmpty()) FirebaseCrashlytics.getInstance().setUserId(username.hashCode().toString())
    }

    fun setFullName(context: Context?, fullName: String?) {
        setString(context, fullName, KEY_FULL_NAME)
    }

    fun setAvatarUrl(context: Context?, avatarUrl: String?) {
        setString(context, avatarUrl, KEY_AVATAR_URL)
    }

    @JvmStatic
    fun getUsername(context: Context?): String? {
        return getString(context, KEY_USERNAME)
    }

    fun getFullName(context: Context): String? {
        return getString(context, KEY_FULL_NAME)
    }

    fun getAvatarUrl(context: Context): String? {
        return getString(context, KEY_AVATAR_URL)
    }

    private fun setString(context: Context, value: String?, key: String) {
        context.preferences()[key] = value
    }

    private fun getString(context: Context, key: String): String? {
        return context.preferences()[key, ""]
    }
}
