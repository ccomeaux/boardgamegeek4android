package com.boardgamegeek.ui.dialog

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.boardgamegeek.R
import com.boardgamegeek.filterer.AcquiredFromFilter
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel

class AcquiredFromFilterDialog : CollectionTextFilterDialog() {
    override val titleResId: Int
        get() = R.string.acquired_from

    override fun createFilter(context: Context) = AcquiredFromFilter(context)

    override fun createAdapter(viewModel: CollectionViewViewModel, activity: FragmentActivity) =
        AutoCompleteAdapter(activity).also { adapter ->
            viewModel.acquiredFrom.observe(activity) {
                adapter.addData(it)
            }
        }
}
