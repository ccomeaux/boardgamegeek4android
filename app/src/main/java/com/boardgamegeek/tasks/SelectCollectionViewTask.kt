package com.boardgamegeek.tasks

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.AsyncTask
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.content.contentValuesOf
import androidx.core.content.getSystemService
import com.boardgamegeek.R
import com.boardgamegeek.extensions.load
import com.boardgamegeek.extensions.truncate
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.ui.CollectionActivity
import com.boardgamegeek.util.ShortcutUtils
import java.util.*

class SelectCollectionViewTask(context: Context?, private val viewId: Long) : AsyncTask<Void?, Void?, Void?>() {
    @SuppressLint("StaticFieldLeak")
    private val context: Context? = context?.applicationContext

    private val shortcutManager: ShortcutManager? by lazy {
        if (context != null && VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
            context.getSystemService<ShortcutManager>()
        } else {
            null
        }
    }

    override fun doInBackground(vararg params: Void?): Void? {
        if (context == null) return null
        if (viewId <= 0) return null
        updateSelection()
        if (VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
            shortcutManager?.reportShortcutUsed(createShortcutName(viewId))
            setShortcuts()
        }
        return null
    }

    private fun updateSelection() {
        if (context == null) return
        val uri = CollectionViews.buildViewUri(viewId)
        context.contentResolver.load(uri, arrayOf(CollectionViews.SELECTED_COUNT))?.use {
            if (it.moveToFirst()) {
                val currentCount = it.getInt(0)
                val contentValues = contentValuesOf(
                        CollectionViews.SELECTED_COUNT to currentCount + 1,
                        CollectionViews.SELECTED_TIMESTAMP to System.currentTimeMillis()
                )
                context.contentResolver.update(uri, contentValues, null, null)
            }
        }
    }

    @RequiresApi(VERSION_CODES.N_MR1)
    private fun setShortcuts() {
        if (context == null || shortcutManager == null) return
        val shortcuts: MutableList<ShortcutInfo> = ArrayList(SHORTCUT_COUNT)
        context.contentResolver.load(CollectionViews.CONTENT_URI,
                arrayOf(CollectionViews._ID, CollectionViews.NAME),
                sortOrder = "${CollectionViews.SELECTED_COUNT} DESC, ${CollectionViews.SELECTED_TIMESTAMP} DESC"
        )?.use {
            while (it.moveToNext()) {
                val name = it.getString(1)
                if (name.isNotBlank()) {
                    shortcuts.add(createShortcutInfo(context, it.getLong(0), name))
                    if (shortcuts.size >= SHORTCUT_COUNT) break
                }
            }
        }
        shortcutManager?.dynamicShortcuts = shortcuts
    }

    @RequiresApi(VERSION_CODES.N_MR1)
    private fun createShortcutInfo(context: Context, viewId: Long, viewName: String): ShortcutInfo {
        return ShortcutInfo.Builder(context, createShortcutName(viewId))
                .setShortLabel(viewName.truncate(ShortcutUtils.SHORT_LABEL_LENGTH))
                .setLongLabel(viewName.truncate(ShortcutUtils.LONG_LABEL_LENGTH))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_ic_collection))
                .setIntent(CollectionActivity.createIntentAsShortcut(context, viewId))
                .build()
    }

    companion object {
        private const val SHORTCUT_COUNT = 3
        private fun createShortcutName(viewId: Long): String {
            return "collection-view-$viewId"
        }
    }
}