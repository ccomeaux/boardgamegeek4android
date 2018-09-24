package com.boardgamegeek.ui.dialog

import android.content.Context
import android.support.v7.app.AlertDialog
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionStatusFilterer

class CollectionStatusFilterDialog : CollectionFilterDialog {
    override fun createDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, filter: CollectionFilterer?) {
        val statusEntries = context.resources.getStringArray(R.array.collection_status_filter_entries)
        val selectedStatuses = (filter as CollectionStatusFilterer?)?.selectedStatuses
                ?: BooleanArray(statusEntries.size)

        AlertDialog.Builder(context, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.menu_collection_status)
                .setMultiChoiceItems(statusEntries, selectedStatuses) { _, which, isChecked -> selectedStatuses[which] = isChecked }
                .setNegativeButton(R.string.or) { _, _ ->
                    if (listener != null) {
                        CollectionStatusFilterer(context).apply {
                            this.selectedStatuses = selectedStatuses
                            shouldJoinWithOr = true
                            listener.addFilter(this)
                        }
                    }
                }
                .setPositiveButton(R.string.and) { _, _ ->
                    if (listener != null) {
                        CollectionStatusFilterer(context).apply {
                            this.selectedStatuses = selectedStatuses
                            shouldJoinWithOr = false
                            listener.addFilter(this)
                        }
                    }
                }
                .setNeutralButton(R.string.clear) { _, _ -> listener?.removeFilter(getType(context)) }
                .create()
                .show()
    }

    override fun getType(context: Context): Int {
        return CollectionStatusFilterer(context).type
    }
}
