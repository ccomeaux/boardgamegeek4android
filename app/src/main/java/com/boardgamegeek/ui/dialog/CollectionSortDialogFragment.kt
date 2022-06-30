package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogCollectionSortBinding
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import timber.log.Timber

class CollectionSortDialogFragment : DialogFragment() {
    private var _binding: DialogCollectionSortBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<CollectionViewViewModel>()
    private val factory by lazy { CollectionSorterFactory(requireContext()) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCollectionSortBinding.inflate(layoutInflater)
        return AlertDialog.Builder(requireContext()).setView(binding.root).setTitle(R.string.title_sort_by).create()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val selectedType = arguments?.getInt(KEY_SORT_TYPE) ?: CollectionSorterFactory.TYPE_DEFAULT

        if (requireContext().preferences()[PREFERENCES_KEY_SYNC_PLAYS, false] != true) {
            binding.playDateMax.hideIfNotSelected(selectedType)
            binding.playDateMin.hideIfNotSelected(selectedType)
        }

        createNames(binding.container)

        findChipByType(selectedType, binding.container)?.let {
            it.isChecked = true
            it.chipIcon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_baseline_arrow_upward_24)
            it.isChipIconVisible = true
            binding.scrollContainer.scrollToDescendant(it)
        }

        wireUp(binding.container)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Chip.hideIfNotSelected(selectedType: Int) {
        if (findChipByType(selectedType, binding.container) !== this) {
            isVisible = false
        }
    }

    private fun createNames(view: View) {
        if (view is ChipGroup)
            view.children.filterIsInstance<Chip>().forEach {
                val sortType = getSortTypeFromView(it)
                val sorter = factory.create(sortType)
                it.text = sorter?.description.orEmpty()
            }
        else if (view is ViewGroup)
            view.children.forEach { createNames(it) }
    }

    private fun wireUp(view: View) {
        if (view is ChipGroup)
            view.setOnCheckedChangeListener { group, checkedId ->
                if (checkedId != View.NO_ID) {
                    Timber.w("$group $checkedId")
                    val sortType = getSortTypeFromView(group.findViewById(checkedId))
                    Timber.d("Sort by $sortType")
                    viewModel.setSort(sortType)
                    FirebaseAnalytics.getInstance(requireContext()).logEvent("Sort") {
                        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Collection")
                        param("SortBy", sortType.toString())
                    }
                }
                dismiss()
            }
        else if (view is ViewGroup)
            view.children.forEach { wireUp(it) }
    }

    private fun findChipByType(type: Int, view: View): Chip? {
        return when (view) {
            is ChipGroup -> {
                view.children.filterIsInstance<Chip>().find {
                    getSortTypeFromView(it) == type
                }
            }
            is ViewGroup -> {
                view.children.forEach {
                    val chip = findChipByType(type, it)
                    if (chip != null) return chip
                }
                null
            }
            else -> null
        }
    }

    private fun getSortTypeFromView(view: View) = view.tag.toString().toIntOrNull() ?: CollectionSorterFactory.TYPE_UNKNOWN

    companion object {
        private const val KEY_SORT_TYPE = "sort_type"

        fun newInstance(sortType: Int) = CollectionSortDialogFragment().apply {
            arguments = bundleOf(KEY_SORT_TYPE to sortType)
        }
    }
}
