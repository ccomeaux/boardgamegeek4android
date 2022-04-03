package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionNameFilter
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel

class CollectionNameFilterDialog : CollectionFilterDialog {
    lateinit var layout: View
    private val filterTextView: EditText by lazy { layout.findViewById(R.id.filterTextView) }
    private val startsWithCheckBox: CheckBox by lazy { layout.findViewById(R.id.startsWithCheckBox) }

    @SuppressLint("InflateParams")
    override fun createDialog(activity: FragmentActivity, filter: CollectionFilterer?) {
        val viewModel by lazy { ViewModelProvider(activity)[CollectionViewViewModel::class.java] }
        layout = LayoutInflater.from(activity).inflate(R.layout.dialog_collection_filter_name, null)
        (filter as? CollectionNameFilter)?.let {
            filterTextView.setAndSelectExistingText(it.filterText)
            startsWithCheckBox.isChecked = it.startsWith
        }
        activity.createThemedBuilder()
            .setTitle(R.string.menu_collection_name)
            .setPositiveButton(R.string.set) { _, _ ->
                viewModel.addFilter(CollectionNameFilter(activity).apply {
                    filterText = filterTextView.text.toString()
                    startsWith = startsWithCheckBox.isChecked
                })
            }
            .setNegativeButton(R.string.clear) { _, _ ->
                viewModel.removeFilter(getType(activity))
            }
            .setView(layout)
            .create()
            .apply {
                requestFocus(filterTextView)
                show()
            }
    }

    override fun getType(context: Context): Int {
        return CollectionNameFilter(context).type
    }
}
