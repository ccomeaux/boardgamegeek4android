package com.boardgamegeek.ui.dialog

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.boardgamegeek.R
import com.boardgamegeek.filterer.InventoryLocationFilter
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel

class InventoryLocationFilterDialog : CollectionTextFilterDialog() {
    override val titleResId: Int
        get() = R.string.inventory_location

    override fun createFilter(context: Context) = InventoryLocationFilter(context)

    override fun createAdapter(viewModel: CollectionViewViewModel, activity: FragmentActivity) =
        AutoCompleteAdapter(activity).also { adapter ->
            viewModel.inventoryLocation.observe(activity) {
                adapter.addData(it)
            }
        }
}
