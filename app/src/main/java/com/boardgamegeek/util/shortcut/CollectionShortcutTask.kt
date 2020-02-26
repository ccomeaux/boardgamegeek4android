package com.boardgamegeek.util.shortcut

import android.content.Context
import android.content.Intent
import com.boardgamegeek.R
import com.boardgamegeek.ui.CollectionActivity.Companion.createIntentAsShortcut

class CollectionShortcutTask(context: Context?, private val viewId: Long, private val viewName: String)
    : ShortcutTask(context) {
    override fun getShortcutName() = viewName

    override fun createIntent(): Intent? {
        return context?.let { createIntentAsShortcut(it, viewId) }
    }

    override fun getShortcutIconResId() = R.drawable.ic_shortcut_ic_collection

    override fun getId() = "collection_view-$viewId"
}