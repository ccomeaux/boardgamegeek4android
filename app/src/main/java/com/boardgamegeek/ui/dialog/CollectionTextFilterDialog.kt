package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionTextFilter
import com.boardgamegeek.filterer.TextOperator
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel

abstract class CollectionTextFilterDialog: CollectionFilterDialog {
    lateinit var layout: View
    private val textOperatorView: Spinner by lazy { layout.findViewById(R.id.textOperatorView) }
    private val filterTextView: EditText by lazy { layout.findViewById(R.id.filterTextView) }
    private val matchCaseView: CheckBox by lazy { layout.findViewById(R.id.matchCaseView) }

    abstract val titleResId: Int
    abstract fun createFilter(context: Context): CollectionTextFilter

    @SuppressLint("InflateParams")
    override fun createDialog(activity: FragmentActivity, filter: CollectionFilterer?) {
        val viewModel by lazy { ViewModelProvider(activity)[CollectionViewViewModel::class.java] }
        layout = LayoutInflater.from(activity).inflate(R.layout.dialog_collection_filter_text, null)
        textOperatorView.adapter = ArrayAdapter.createFromResource(activity, R.array.text_operator, android.R.layout.simple_spinner_item).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        (filter as? CollectionTextFilter)?.let {
            textOperatorView.setSelection(it.textOperator.key - 1)
            filterTextView.setAndSelectExistingText(it.filterText)
            matchCaseView.isChecked = !it.ignoreCase
        }
        textOperatorView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val key = position + 1
                val isEnabled = !(key == TextOperator.IsEmpty.key || key == TextOperator.IsNotEmpty.key)
                filterTextView.isEnabled = isEnabled
                matchCaseView.isEnabled = isEnabled
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        activity.createThemedBuilder()
            .setTitle(titleResId)
            .setPositiveButton(R.string.set) { _, _ ->
                viewModel.addFilter(createFilter(activity).apply {
                    textOperator = TextOperator.getByKey(textOperatorView.selectedItemPosition + 1) ?: TextOperator.default
                    filterText = filterTextView.text.toString()
                    ignoreCase = !matchCaseView.isChecked
                })
            }
            .setNegativeButton(R.string.clear) { _, _ ->
                viewModel.removeFilter(getType(activity))
            }
            .setView(layout)
            .create()
            .show()
    }

    override fun getType(context: Context) = createFilter(context).type
}
