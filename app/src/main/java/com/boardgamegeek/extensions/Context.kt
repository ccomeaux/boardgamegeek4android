@file:JvmName("ContextUtils")

package com.boardgamegeek.extensions

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.text.Html
import android.text.SpannedString
import android.text.TextUtils
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.service.SyncService

fun Context.preferences(name: String? = null): SharedPreferences = if (name.isNullOrEmpty())
    PreferenceManager.getDefaultSharedPreferences(this)
else
    this.getSharedPreferences(name, Context.MODE_PRIVATE)

@Suppress("DEPRECATION")
fun Context.getText(@StringRes id: Int, vararg args: Any): CharSequence {
    val encodedArgs = encodeArgs(args)
    val htmlString = String.format(Html.toHtml(SpannedString(getText(id))), *encodedArgs.toTypedArray())
    return Html.fromHtml(htmlString).trimTrailingWhitespace()
}

@Suppress("DEPRECATION")
fun Context.getQuantityText(@PluralsRes id: Int, quantity: Int, vararg args: Any?): CharSequence {
    val encodedArgs = encodeArgs(args)
    val htmlString = String.format(Html.toHtml(SpannedString(resources.getQuantityText(id, quantity))), *encodedArgs.toTypedArray())
    return Html.fromHtml(htmlString).trimTrailingWhitespace()
}

private fun encodeArgs(args: Array<out Any?>): List<Any?> {
    val encodedArgs = mutableListOf<Any?>()
    for (i in args.indices) {
        val arg = args[i]
        encodedArgs.add(if (arg is String) TextUtils.htmlEncode(arg) else arg)
    }
    return encodedArgs
}

/**
 * Get the version name of the package, or "?.?" if not found.
 */
fun Context.versionName(): String {
    return try {
        packageManager.getPackageInfo(packageName, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "?.?"
    }
}

fun Context.logQuickPlay(gameId: Int, gameName: String) {
    val play = Play(gameId, gameName)
    play.setCurrentDate()
    play.updateTimestamp = System.currentTimeMillis()
    PlayPersister(this).save(play, BggContract.INVALID_ID.toLong(), false)
    SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD)
}

fun Context.isIntentAvailable(intent: Intent): Boolean {
    val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return list.size > 0
}
