package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogCollectionFilterBinding
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import timber.log.Timber

class CollectionFilterDialogFragment : DialogFragment() {
    private var _binding: DialogCollectionFilterBinding? = null
    private val binding get() = _binding!!
    private val filters = mutableListOf<CollectionFilterer>()
    private val viewModel by activityViewModels<CollectionViewViewModel>()
    private val factory by lazy { CollectionFiltererFactory(requireContext()) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCollectionFilterBinding.inflate(layoutInflater)
        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setView(binding.root)
            .setTitle(R.string.title_filter)
            .create()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.effectiveFilters.observe(viewLifecycleOwner) {
            filters.clear()
            it?.let { filters.addAll(it) }
            bindUi()
        }
    }

    private fun bindUi() {
        binding.container.children.filterIsInstance<ChipGroup>().forEach {
            it.children.filterIsInstance<Chip>().forEach { chip ->
                val type = getTypeFromViewTag(chip)
                if (filters.map { filter -> filter.type }.contains(type)) {
                    chip.isChecked = true
                    chip.isCloseIconVisible = true
                }

                factory.create(type)?.let { filter ->
                    chip.checkedIcon = ContextCompat.getDrawable(requireContext(), filter.iconResourceId)
                }

                chip.setOnClickListener {
                    if (type != CollectionFiltererFactory.TYPE_UNKNOWN) {
                        Timber.d("Filter by %s", type)
                        CollectionFilterDialogFactory().create(requireContext(), type)?.let { dialog ->
                            dialog.createDialog(requireActivity(), filters.find { filter -> filter.type == type })
                        } ?: Timber.w("Couldn't find a filter dialog of type %s", type)
                    } else {
                        Timber.w("Invalid filter type selected: %s", type)
                    }
                    dismiss()
                }

                chip.setOnCloseIconClickListener {
                    viewModel.removeFilter(type)
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getTypeFromViewTag(it: View) = it.tag?.toString()?.toIntOrNull() ?: CollectionFiltererFactory.TYPE_UNKNOWN
}
