package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogCollectionSortBinding
import com.boardgamegeek.extensions.getSyncPlays
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import org.jetbrains.anko.support.v4.act
import timber.log.Timber

class CollectionSortDialogFragment : DialogFragment() {
    private var _binding: DialogCollectionSortBinding? = null
    private val binding get() = _binding!!
    private lateinit var layout: View

    private val viewModel: CollectionViewViewModel by lazy {
        ViewModelProvider(requireActivity()).get(CollectionViewViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCollectionSortBinding.inflate(LayoutInflater.from(context))
        layout = binding.root
        return AlertDialog.Builder(requireContext()).setView(layout).setTitle(R.string.title_sort).create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val selectedType = arguments?.getInt(KEY_SORT_TYPE) ?: CollectionSorterFactory.TYPE_DEFAULT

        if (!requireContext().getSyncPlays()) {
            hideRadioButtonIfNotSelected(binding.playDateMaxRadioButton, selectedType)
            hideRadioButtonIfNotSelected(binding.playDateMinRadioButton, selectedType)
        }

        createNames()
        val radioButton = findRadioButtonByType(selectedType)
        radioButton?.isChecked = true
        binding.scrollContainer.requestChildFocus(radioButton, radioButton)
        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val sortType = getTypeFromView(group.findViewById(checkedId))
            Timber.d("Sort by $sortType")
            viewModel.setSort(sortType)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hideRadioButtonIfNotSelected(radioButton: RadioButton, selectedType: Int) {
        if (findRadioButtonByType(selectedType) !== radioButton) {
            radioButton.visibility = View.GONE
        }
    }

    private fun createNames() {
        val factory = CollectionSorterFactory(requireContext())

        binding.radioGroup.children.filterIsInstance<RadioButton>().forEach {
            val sortType = getTypeFromView(it)
            val sorter = factory.create(sortType)
            if (sorter != null) it.text = sorter.description
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
