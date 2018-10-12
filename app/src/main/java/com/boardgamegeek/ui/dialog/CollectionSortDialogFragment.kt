package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.boardgamegeek.R
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.dialog_collection_sort.*
import timber.log.Timber

class CollectionSortDialogFragment : DialogFragment() {
    private lateinit var layout: View
    private var listener: Listener? = null

    interface Listener {
        fun onSortSelected(sortType: Int)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as Listener?
        if (listener == null) throw ClassCastException("$context must implement Listener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_collection_sort, null)
        return AlertDialog.Builder(requireContext()).setView(layout).setTitle(R.string.title_sort).create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val selectedType = arguments?.getInt(KEY_SORT_TYPE) ?: CollectionSorterFactory.TYPE_DEFAULT

        if (!PreferencesUtils.getSyncPlays(context)) {
            hideRadioButtonIfNotSelected(playDateMaxRadioButton, selectedType)
            hideRadioButtonIfNotSelected(playDateMinRadioButton, selectedType)
        }

        createNames()
        val radioButton = findRadioButtonByType(selectedType)
        radioButton?.isChecked = true
        scrollContainer.requestChildFocus(radioButton, radioButton)
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val sortType = getTypeFromView(group.findViewById(checkedId))
            Timber.d("Sort by $sortType")
            listener?.onSortSelected(sortType)
            dismiss()
        }
    }

    private fun hideRadioButtonIfNotSelected(radioButton: RadioButton, selectedType: Int) {
        if (findRadioButtonByType(selectedType) !== radioButton) {
            radioButton.visibility = View.GONE
        }
    }

    private fun createNames() {
        val factory = CollectionSorterFactory(requireContext())

        radioGroup.children.filterIsInstance<RadioButton>().forEach {
            val sortType = getTypeFromView(it)
            val sorter = factory.create(sortType)
            if (sorter != null) it.text = sorter.description
        }
    }

    private fun findRadioButtonByType(type: Int): RadioButton? {
        return radioGroup.children.filterIsInstance<RadioButton>().find {
            getTypeFromView(it) == type
        }
    }

    private fun getTypeFromView(view: View): Int {
        return view.tag.toString().toIntOrNull() ?: CollectionSorterFactory.TYPE_UNKNOWN
    }

    companion object {
        private const val KEY_SORT_TYPE = "sort_type"

        @JvmStatic
        fun newInstance(sortType: Int): CollectionSortDialogFragment {
            return CollectionSortDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_SORT_TYPE, sortType)
                }
            }
        }
    }
}
