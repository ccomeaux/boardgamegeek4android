package com.boardgamegeek.ui.dialog

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.ExpansionStatusFilterer
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel

class ExpansionStatusFilterDialog : CollectionFilterDialog {
    override fun createDialog(activity: FragmentActivity, filter: CollectionFilterer?) {
        val viewModel by lazy { ViewModelProvider(activity)[CollectionViewViewModel::class.java] }
        var selectedSubtype = (filter as ExpansionStatusFilterer?)?.selectedSubtype ?: 0
        activity.createThemedBuilder()
            .setTitle(R.string.menu_expansion_status)
            .setSingleChoiceItems(R.array.expansion_status_filter, selectedSubtype) { _, which -> selectedSubtype = which }
            .setPositiveButton(R.string.ok) { _, _ ->
                if (selectedSubtype == 0) {
                    viewModel.removeFilter(getType(activity))
                } else {
                    viewModel.addFilter(ExpansionStatusFilterer(activity).apply {
                        this.selectedSubtype = selectedSubtype
                    })
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .show()
    }

    override fun getType(context: Context): Int {
        return ExpansionStatusFilterer(context).type
    }
}
