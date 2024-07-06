package com.boardgamegeek.ui.dialog

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogCollectionSortBinding
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class CollectionSortDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogCollectionSortBinding? = null
    private val binding get() = _binding!!
    private var selectedType: Int = CollectionSorterFactory.TYPE_DEFAULT
    private val viewModel by activityViewModels<CollectionViewViewModel>()
    private val factory by lazy { CollectionSorterFactory(requireContext()) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogCollectionSortBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selectedType = arguments?.getInt(KEY_SORT_TYPE) ?: CollectionSorterFactory.TYPE_DEFAULT

        if (requireContext().preferences()[PREFERENCES_KEY_SYNC_PLAYS, false] != true) {
            if (findChipByType(selectedType, binding.container) !== binding.playDate) {
                binding.playDate.isVisible = false
            }
        }

        createNames(binding.container)

        findChipByType(selectedType, binding.container)?.let {
            selectChip(it, factory.create(selectedType)?.second ?: false)
        }

        wireUp(binding.container)
    }

    private fun selectChip(chip: Chip, direction: Boolean) {
        chip.isChecked = true
        chip.chipIcon = AppCompatResources.getDrawable(
            requireContext(), if (direction)
                R.drawable.ic_baseline_arrow_downward_24
            else
                R.drawable.ic_baseline_arrow_upward_24
        )
        chip.chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_dark))
        chip.isChipIconVisible = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.scrollContainer.scrollToDescendant(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createNames(view: View) {
        if (view is ChipGroup)
            view.children.filterIsInstance<Chip>().forEach {
                val sortType = getSortTypeFromView(it)
                val sorter = factory.create(sortType)?.first
                it.text = sorter?.description.orEmpty()
            }
        else if (view is ViewGroup)
            view.children.forEach { createNames(it) }
    }

    private fun wireUp(view: View) {
        if (view is ChipGroup)
            view.setOnCheckedStateChangeListener { group, checkedIds ->
                val checkedId = checkedIds.firstOrNull() ?: View.NO_ID
                val sortType = if (checkedId != View.NO_ID) {
                    getSortTypeFromView(group.findViewById(checkedId))
                } else {
                    factory.reverse(selectedType)
                } ?: CollectionSorterFactory.TYPE_DEFAULT
                Timber.d("Sort by $sortType")
                viewModel.setSort(sortType)
                FirebaseAnalytics.getInstance(requireContext()).logEvent("Sort") {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "Collection")
                    param("SortBy", sortType.toString())
                }
                dismiss()
            }
        else if (view is ViewGroup)
            view.children.forEach { wireUp(it) }
    }

    private fun findChipByType(type: Int, view: View): Chip? {
        return when (view) {
            is ChipGroup -> {
                val chip = view.children.filterIsInstance<Chip>().find {
                    getSortTypeFromView(it) == type
                }
                if (chip == null) {
                    val reversedType = factory.reverse(type)
                    view.children.filterIsInstance<Chip>().find {
                        getSortTypeFromView(it) == reversedType
                    }
                } else chip
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

    private fun getSortTypeFromView(view: View) = view.tag?.toString()?.toIntOrNull() ?: CollectionSorterFactory.TYPE_UNKNOWN

    companion object {
        private const val KEY_SORT_TYPE = "sort_type"

        fun newInstance(sortType: Int) = CollectionSortDialogFragment().apply {
            arguments = bundleOf(KEY_SORT_TYPE to sortType)
        }
    }
}
