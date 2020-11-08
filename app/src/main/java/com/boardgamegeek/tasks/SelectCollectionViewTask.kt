package com.boardgamegeek.tasks

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
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.ui.CollectionActivity
import com.boardgamegeek.util.ShortcutUtils
import kotlin.io.use

class SelectCollectionViewTask(private val context: Context?, private val viewId: Long) : AsyncTask<Void?, Void?, Void?>() {
    private val shortcutManager: ShortcutManager? by lazy {
        if (VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
            context?.applicationContext?.getSystemService()
        } else {
            null
        }
    }

    override fun doInBackground(vararg params: Void?): Void? {
        if (viewId <= 0) return null
        updateSelection()
        if (VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
            shortcutManager?.reportShortcutUsed(createShortcutName(viewId))
            setShortcuts()
        }
        return null
    }

    private fun updateSelection() {
        context?.applicationContext?.contentResolver?.let { resolver ->
            val uri = CollectionViews.buildViewUri(viewId)
            resolver.load(uri, arrayOf(CollectionViews.SELECTED_COUNT))?.use {
                if (it.moveToFirst()) {
                    val currentCount = it.getIntOrNull(0) ?: 0
                    val values = contentValuesOf(
                            CollectionViews.SELECTED_COUNT to currentCount + 1,
                            CollectionViews.SELECTED_TIMESTAMP to System.currentTimeMillis()
                    )
                    resolver.update(uri, values, null, null)
                }
            }
        }
    }

    @RequiresApi(VERSION_CODES.N_MR1)
    private fun setShortcuts() {
        context?.applicationContext?.let { ctx ->
            val shortcuts = mutableListOf<ShortcutInfo>()
            ctx.contentResolver.load(CollectionViews.CONTENT_URI,
                    arrayOf(CollectionViews._ID, CollectionViews.NAME),
                    sortOrder = "${CollectionViews.SELECTED_COUNT} DESC, ${CollectionViews.SELECTED_TIMESTAMP} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val viewId = cursor.getLongOrNull(0) ?: BggContract.INVALID_ID.toLong()
                    val name = cursor.getStringOrNull(1)
                    if (name?.isNotBlank() == true) {
                        shortcuts.add(ShortcutInfo.Builder(ctx, createShortcutName(viewId))
                                .setShortLabel(name.truncate(ShortcutUtils.SHORT_LABEL_LENGTH))
                                .setLongLabel(name.truncate(ShortcutUtils.LONG_LABEL_LENGTH))
                                .setIcon(Icon.createWithResource(ctx, R.drawable.ic_shortcut_ic_collection))
                                .setIntent(CollectionActivity.createIntentAsShortcut(ctx, viewId))
                                .build())
                        if (shortcuts.size >= SHORTCUT_COUNT) break
                    }
                }
            }
            shortcutManager?.dynamicShortcuts = shortcuts
        }
    }

    private fun createShortcutName(viewId: Long) = "collection-view-$viewId"

    companion object {
        private const val SHORTCUT_COUNT = 3
    }
}