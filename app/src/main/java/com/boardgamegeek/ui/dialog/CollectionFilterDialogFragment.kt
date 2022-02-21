package com.boardgamegeek.ui.dialog

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
import com.boardgamegeek.databinding.DialogCollectionFilterBinding
import com.boardgamegeek.filterer.CollectionFiltererFactory
import timber.log.Timber

class CollectionFilterDialogFragment : DialogFragment() {
    private var _binding: DialogCollectionFilterBinding? = null
    private val binding get() = _binding!!
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
        _binding = DialogCollectionFilterBinding.inflate(layoutInflater)

        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setView(binding.root)
            .setTitle(R.string.title_filter).create()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        for (filterType in arguments?.getIntegerArrayList(KEY_FILTER_TYPES) ?: arrayListOf<Int>()) {
            binding.statusContainer.children
                .filterIsInstance<CheckBox>()
                .find { filterType == getTypeFromViewTag(it) }
                ?.let {
                    it.isChecked = true
                }
        }

        binding.statusContainer.children.filterIsInstance<CheckBox>().forEach {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getTypeFromViewTag(it: View) =
        it.tag.toString().toIntOrNull() ?: CollectionFiltererFactory.TYPE_UNKNOWN

    companion object {
        private const val KEY_FILTER_TYPES = "filter_types"

        fun newInstance(filterTypes: List<Int>) = CollectionFilterDialogFragment().apply {
            arguments = Bundle().apply {
                putIntegerArrayList(KEY_FILTER_TYPES, ArrayList(filterTypes))
            }
        }
    }
}
