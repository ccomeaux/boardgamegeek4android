package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import com.boardgamegeek.R
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionNameFilter

class CollectionNameFilterDialog : CollectionFilterDialog {
    lateinit var layout: View
    private val filterTextView: EditText by lazy { layout.findViewById(R.id.filterTextView) }
    private val startsWithCheckBox: CheckBox by lazy { layout.findViewById(R.id.startsWithCheckBox) }

    @SuppressLint("InflateParams")
    override fun createDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, filter: CollectionFilterer?) {
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_collection_filter_name, null)
        initializeUi(filter)
        createAlertDialog(context, listener, layout).apply {
            requestFocus(filterTextView)
            show()
        }
    }

    private fun createAlertDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, layout: View): AlertDialog {
        return Builder(context, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.menu_collection_name)
                .setPositiveButton(R.string.set) { _, _ ->
                    if (listener != null) {
                        CollectionNameFilter(context).apply {
                            filterText = filterTextView.text.toString()
                            startsWith = startsWithCheckBox.isChecked
                            listener.addFilter(this)
                        }
                    }
                }
                .setNegativeButton(R.string.clear) { _, _ -> listener?.removeFilter(CollectionNameFilter(context).type) }
                .setView(layout)
                .create()
    }

    private fun initializeUi(filter: CollectionFilterer?) {
        val collectionNameFilter = filter as CollectionNameFilter?
        if (collectionNameFilter != null) {
            filterTextView.setAndSelectExistingText(collectionNameFilter.filterText)
            startsWithCheckBox.isChecked = collectionNameFilter.startsWith
        }
    }

    override fun getType(context: Context): Int {
        return CollectionNameFilter(context).type
    }
}
