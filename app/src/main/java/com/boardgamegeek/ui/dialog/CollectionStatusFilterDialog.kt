package com.boardgamegeek.ui.dialog

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionStatusFilterer
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel

class CollectionStatusFilterDialog : CollectionFilterDialog {

    override fun createDialog(activity: FragmentActivity, filter: CollectionFilterer?) {
        val viewModel by lazy { ViewModelProvider(activity)[CollectionViewViewModel::class.java] }
        val statusEntries = activity.resources.getStringArray(R.array.collection_status_filter_entries)
        val selectedStatuses = (filter as CollectionStatusFilterer?)?.selectedStatuses ?: BooleanArray(statusEntries.size)

        activity.createThemedBuilder()
            .setTitle(R.string.menu_collection_status)
            .setMultiChoiceItems(statusEntries, selectedStatuses) { _, which, isChecked -> selectedStatuses[which] = isChecked }
            .setNegativeButton(R.string.or) { _, _ ->
                viewModel.addFilter(CollectionStatusFilterer(activity).apply {
                    this.selectedStatuses = selectedStatuses
                    shouldJoinWithOr = true
                })
            }
            .setPositiveButton(R.string.and) { _, _ ->
                viewModel.addFilter(CollectionStatusFilterer(activity).apply {
                    this.selectedStatuses = selectedStatuses
                    shouldJoinWithOr = false
                })
            }
            .setNeutralButton(R.string.clear) { _, _ ->
                viewModel.removeFilter(getType(activity))
            }
            .create()
            .show()
    }

    override fun getType(context: Context): Int {
        return CollectionStatusFilterer(context).type
    }
}
