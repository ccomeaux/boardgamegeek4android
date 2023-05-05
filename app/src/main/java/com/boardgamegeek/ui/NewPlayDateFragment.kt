package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNewPlayDateBinding
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.fromLocalToUtc
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class NewPlayDateFragment : Fragment() {
    private var _binding: FragmentNewPlayDateBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private var lastPlayDate: Long? = null

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
            viewModel.setDate(Calendar.getInstance().apply { add(Calendar.DATE, -1) }.timeInMillis)
        }

        binding.earlierButton.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(earlierDate())
                .build()
            datePicker.addOnPositiveButtonClickListener {
                viewModel.setDate(it.fromLocalToUtc())
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER_DIALOG")
        }

        binding.lastPlayDateButton.setOnClickListener {
            lastPlayDate?.let {
                val datePicker = MaterialDatePicker.Builder.datePicker().setSelection(it).build()
                datePicker.addOnPositiveButtonClickListener { newDate ->
                    viewModel.setDate(newDate.fromLocalToUtc())
                }
                datePicker.show(parentFragmentManager, "DATE_PICKER_DIALOG")
            }
        }

        viewModel.lastPlayDate.observe(viewLifecycleOwner) {
            it?.let {
                lastPlayDate = it
                Timber.i(it.formatDateTime(requireContext()).toString() + " / " + earlierDate().formatDateTime(requireContext()))
                if (it < earlierDate()) {
                    binding.lastPlayDateButton.text = it.formatDateTime(requireContext())
                    binding.lastPlayDateButton.isVisible = true
                } else binding.lastPlayDateButton.isVisible = false
            }
        }
    }

    private fun earlierDate() = Calendar.getInstance().apply { add(Calendar.DATE, -2) }.timeInMillis

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
