package com.boardgamegeek.util.shortcut

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.AsyncTask
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.boardgamegeek.R
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.boardgamegeek.extensions.truncate
import com.boardgamegeek.util.ShortcutUtils
import com.squareup.picasso.Picasso
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

abstract class ShortcutTask @JvmOverloads constructor(context: Context?, thumbnailUrl: String? = null) : AsyncTask<Void?, Void?, Void?>() {
    @SuppressLint("StaticFieldLeak")
    protected val context: Context? = context?.applicationContext
    protected val thumbnailUrl: String = thumbnailUrl?.ensureHttpsScheme() ?: ""
    protected open val shortcutIconResId: Int = R.mipmap.ic_launcher_foreground
    protected abstract val shortcutName: String
    protected abstract fun createIntent(): Intent?

    protected abstract val id: String?

    override fun doInBackground(vararg params: Void?): Void? {
        if (context == null) return null
        if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            createShortcutForOreo()
        } else {
            val shortcutIntent = ShortcutUtils.createShortcutIntent(context, shortcutName, createIntent(), shortcutIconResId)
            if (thumbnailUrl.isNotBlank()) {
                fetchThumbnail()?.let {
                    @Suppress("DEPRECATION")
                    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, it)
                }
            }
            context.sendBroadcast(shortcutIntent)
        }
        return null
    }

    @RequiresApi(api = VERSION_CODES.O)
    private fun createShortcutForOreo() {
        val shortcutManager = context?.getSystemService<ShortcutManager>()
        if (shortcutManager?.isRequestPinShortcutSupported == true) {
            createIntent()?.let {
                val builder = ShortcutInfo.Builder(context, id)
                        .setShortLabel(shortcutName.truncate(ShortcutUtils.SHORT_LABEL_LENGTH))
                        .setLongLabel(shortcutName.truncate(ShortcutUtils.LONG_LABEL_LENGTH))
                        .setIntent(it)
                if (thumbnailUrl.isNotBlank()) {
                    val bitmap = fetchThumbnail()
                    if (bitmap != null) {
                        builder.setIcon(Icon.createWithAdaptiveBitmap(bitmap))
                    } else {
                        builder.setIcon(Icon.createWithResource(context, shortcutIconResId))
                    }
                } else {
                    builder.setIcon(Icon.createWithResource(context, shortcutIconResId))
                }
                shortcutManager.requestPinShortcut(builder.build(), null)
            }
        }
    }

    override fun onPostExecute(nothing: Void?) {}

    private fun fetchThumbnail(): Bitmap? {
        var bitmap: Bitmap? = null
        val file = ShortcutUtils.getThumbnailFile(context, thumbnailUrl)
        if (file?.exists() == true) {
            bitmap = BitmapFactory.decodeFile(file.absolutePath)
        } else {
            try {
                bitmap = Picasso.with(context)
                        .load(thumbnailUrl)
                        .resizeDimen(R.dimen.shortcut_icon_size, R.dimen.shortcut_icon_size)
                        .centerCrop()
                        .get()
            } catch (e: IOException) {
                Timber.e(e, "Error downloading the thumbnail.")
            }
        }
        if (bitmap != null && file != null) {
            try {
                val out: OutputStream = BufferedOutputStream(FileOutputStream(file))
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()
            } catch (e: IOException) {
                Timber.e(e, "Error saving the thumbnail file.")
            }
        }
        return bitmap
    }

}