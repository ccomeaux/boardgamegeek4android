package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNewPlayDateBinding
import com.boardgamegeek.extensions.fromLocalToUtc
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*

class NewPlayDateFragment : Fragment() {
    private var _binding: FragmentNewPlayDateBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<NewPlayViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNewPlayDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.todayButton.setOnClickListener {
            viewModel.setDate(System.currentTimeMillis())
        }

        binding.yesterdayButton.setOnClickListener {
            val date = Calendar.getInstance()
            date.add(Calendar.DATE, -1)
            viewModel.setDate(date.timeInMillis)
        }

        binding.earlierButton.setOnClickListener {
            val date = Calendar.getInstance()
            date.add(Calendar.DATE, -2)
            val datePicker = MaterialDatePicker.Builder.datePicker().setSelection(date.timeInMillis).build()
            datePicker.addOnPositiveButtonClickListener {
                viewModel.setDate(it.fromLocalToUtc())
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER_DIALOG")

        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
