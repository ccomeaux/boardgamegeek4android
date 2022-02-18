package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFiltererFactory
import kotlinx.android.synthetic.main.dialog_collection_filter.*
import timber.log.Timber
import kotlin.collections.ArrayList

class CollectionFilterDialogFragment : DialogFragment() {
    private lateinit var layout: View
    private var listener: Listener? = null

    interface Listener {
        fun onFilterSelected(filterType: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as Listener?
        if (listener == null) throw ClassCastException("$context must implement Listener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = layoutInflater.inflate(R.layout.dialog_collection_filter, null)

        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setView(layout)
                .setTitle(R.string.title_filter).create()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        for (filterType in arguments?.getIntegerArrayList(KEY_FILTER_TYPES) ?: arrayListOf<Int>()) {
            statusContainer.children
                    .filterIsInstance<CheckBox>()
                    .find { filterType == getTypeFromViewTag(it) }
                    ?.let {
                        it.isChecked = true
                    }
        }

        statusContainer.children.filterIsInstance<CheckBox>().forEach {
            it.setOnClickListener { view ->
                val type = getTypeFromViewTag(view)
                if (type != CollectionFiltererFactory.TYPE_UNKNOWN) {
                    Timber.d("Filter by %s", type)
                    listener?.onFilterSelected(type)
                } else {
                    Timber.w("Invalid filter type selected: %s", type)
                }
                dismiss()
            }
        }
    }

    private fun getTypeFromViewTag(it: View) =
            it.tag.toString().toIntOrNull() ?: CollectionFiltererFactory.TYPE_UNKNOWN

    companion object {
        private const val KEY_FILTER_TYPES = "filter_types"

        fun newInstance(filterTypes: List<Int>): CollectionFilterDialogFragment {
            return CollectionFilterDialogFragment().apply {
                arguments = Bundle().apply {
                    putIntegerArrayList(KEY_FILTER_TYPES, ArrayList(filterTypes))
                }
            }
        }
    }
}
