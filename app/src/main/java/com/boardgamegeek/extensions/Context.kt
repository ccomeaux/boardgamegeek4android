@file:Suppress("NOTHING_TO_INLINE")

package com.boardgamegeek.extensions

import android.app.Activity
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.graphics.Bitmap
import android.os.Build
import android.text.Html
import android.text.SpannedString
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.core.text.htmlEncode
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.provider.BggContract

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
        encodedArgs.add(if (arg is String) arg.htmlEncode() else arg)
    }
    return encodedArgs
}

fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageInfoFlags.of(flags.toLong()))
    } else {
        getPackageInfo(packageName, flags)
    }

/**
 * Get the version name of the package, or "?.?" if not found.
 */
fun Context.versionName(): String {
    val unknownVersionName = "?.?"
    return try {
        packageManager.getPackageInfoCompat(packageName).versionName ?: unknownVersionName
    } catch (e: PackageManager.NameNotFoundException) {
        unknownVersionName
    }
}

fun Context.cancelSync() {
    this.cancelNotification(NotificationTags.SYNC_PROGRESS)
    Authenticator.getAccount(this)?.let { account ->
        ContentResolver.cancelSync(account, BggContract.CONTENT_AUTHORITY)
    }
}

fun Context.createWorkConstraints(preserveBattery: Boolean = false): Constraints {
    val syncPrefs: SharedPreferences = SyncPrefs.getPrefs(this)
    return Constraints.Builder()
        .setRequiredNetworkType(if (syncPrefs[KEY_SYNC_ONLY_WIFI, false] == true) NetworkType.METERED else NetworkType.CONNECTED)
        .setRequiresCharging(syncPrefs[KEY_SYNC_ONLY_CHARGING, false] ?: false)
        .setRequiresBatteryNotLow(preserveBattery)
        .build()
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

fun Context.showDialog(message: String, okButtonResId: Int = R.string.ok, okListener: DialogInterface.OnClickListener) {
    AlertDialog.Builder(this)
        .setMessage(message)
        .setCancelable(true)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(okButtonResId, okListener)
        .show()
}

fun Context.getBitmap(@DrawableRes resId: Int, tintColor: Int? = null): Bitmap {
    return AppCompatResources.getDrawable(this, resId)!!.apply {
        tintColor?.let { setTint(it) }
    }.toBitmap()
}

fun Context.createStatusMap() = resources.getStringArray(R.array.pref_sync_status_values)
    .zip(resources.getStringArray(R.array.pref_sync_status_entries))
    .toMap()
