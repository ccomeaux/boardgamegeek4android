package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import timber.log.Timber

class CollectionSortDialogFragment : DialogFragment() {
    private var _binding: DialogCollectionSortBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<CollectionViewViewModel>()

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
            hideRadioButtonIfNotSelected(binding.playDateMaxRadioButton, selectedType)
            hideRadioButtonIfNotSelected(binding.playDateMinRadioButton, selectedType)
        }

        createNames()
        findRadioButtonByType(selectedType)?.let {
            it.isChecked = true
            binding.scrollContainer.requestChildFocus(it, it)
        }

        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val sortType = getTypeFromView(group.findViewById(checkedId))
            Timber.d("Sort by $sortType")
            viewModel.setSort(sortType)
            FirebaseAnalytics.getInstance(requireContext()).logEvent("Sort") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Collection")
                param("SortBy", sortType.toString())
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hideRadioButtonIfNotSelected(radioButton: RadioButton, selectedType: Int) {
        if (findRadioButtonByType(selectedType) !== radioButton) {
            radioButton.isVisible = false
        }
    }

    private fun createNames() {
        val factory = CollectionSorterFactory(requireContext())

        binding.radioGroup.children.filterIsInstance<RadioButton>().forEach {
            val sortType = getTypeFromView(it)
            val sorter = factory.create(sortType)
            it.text = sorter?.description.orEmpty()
        }
    }

    private fun findRadioButtonByType(type: Int): RadioButton? {
        return binding.radioGroup.children.filterIsInstance<RadioButton>().find {
            getTypeFromView(it) == type
        }
    }

    private fun getTypeFromView(view: View): Int {
        return view.tag.toString().toIntOrNull() ?: CollectionSorterFactory.TYPE_UNKNOWN
    }

    companion object {
        private const val KEY_SORT_TYPE = "sort_type"

        fun newInstance(sortType: Int) = CollectionSortDialogFragment().apply {
            arguments = bundleOf(KEY_SORT_TYPE to sortType)
        }
    }
}
