package com.boardgamegeek.util.shortcut

import android.content.Context
import android.content.Intent
import com.boardgamegeek.R
import com.boardgamegeek.ui.CollectionActivity.Companion.createIntentAsShortcut

class CollectionShortcutTask(context: Context?, private val viewId: Long, viewName: String)
    : ShortcutTask(context) {
    override val shortcutName = viewName

    override fun createIntent(): Intent? {
        return context?.let { createIntentAsShortcut(it, viewId) }
    }

    override val shortcutIconResId = R.drawable.ic_shortcut_ic_collection

    override val id = "collection_view-$viewId"
}