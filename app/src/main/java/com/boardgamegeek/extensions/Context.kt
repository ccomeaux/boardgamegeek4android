@file:Suppress("NOTHING_TO_INLINE", "unused")

package com.boardgamegeek.extensions

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.text.Html
import android.text.SpannedString
import android.text.TextUtils
import android.widget.Toast
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.preference.PreferenceManager
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.NotificationUtils

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

fun Context.cancelSync() {
    NotificationUtils.cancel(this, NotificationUtils.TAG_SYNC_PROGRESS)
    Authenticator.getAccount(this)?.let { account ->
        ContentResolver.cancelSync(account, BggContract.CONTENT_AUTHORITY)
    }
}

inline fun <reified T : Activity> Context.startActivity(vararg params: Pair<String, Any?>) = startActivity(T::class.java, params)

fun Context.startActivity(activity: Class<out Activity>, params: Array<out Pair<String, Any?>>) {
    startActivity(createIntent(activity, params))
}

inline fun <reified T : Any> Context.intentFor(vararg params: Pair<String, Any?>): Intent = createIntent(T::class.java, params)

fun <T> Context.createIntent(clazz: Class<out T>, params: Array<out Pair<String, Any?>>): Intent {
    val intent = Intent(this, clazz)
    if (params.isNotEmpty()) intent.putExtras(bundleOf(*params))
    return intent
}


inline fun Context.toast(message: Int): Toast = Toast
    .makeText(this, message, Toast.LENGTH_SHORT)
    .apply {
        show()
    }

inline fun Context.toast(message: CharSequence): Toast = Toast
    .makeText(this, message, Toast.LENGTH_SHORT)
    .apply {
        show()
    }

inline fun Context.longToast(message: Int): Toast = Toast
    .makeText(this, message, Toast.LENGTH_LONG)
    .apply {
        show()
    }
