package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogPrivateInfoCollectionDetailsBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat

@AndroidEntryPoint
class CollectionDetailPrivateInfoDialogFragment : DialogFragment() {
    private var _binding: DialogPrivateInfoCollectionDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<CollectionDetailsViewModel>()
    private var internalId: Long = INVALID_ID.toLong()
    private var acquisitionDate = 0L
    private val acquiredFromAdapter by lazy { AutoCompleteAdapter(requireContext()) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = ViewModelProvider(requireActivity())[CollectionDetailsViewModel::class.java]
        _binding = DialogPrivateInfoCollectionDetailsBinding.inflate(layoutInflater)
        return requireContext().createThemedBuilder()
            .setTitle(R.string.title_buy)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.markedAsAcquired(
                    internalId,
                    binding.priceCurrencyView.selectedItem.toString(),
                    binding.priceView.getDoubleOrNull(),
                    binding.quantityView.getIntOrNull(),
                    acquisitionDate,
                    binding.acquiredFromView.text.trim().toString(),
                )
            }
            .create().apply {
                requestFocus()
            }
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.acquiredFrom.observe(viewLifecycleOwner) {
            acquiredFromAdapter.addData(it)
        }

        internalId = savedInstanceState?.getLong(KEY_INTERNAL_ID, INVALID_ID.toLong()) ?: arguments?.getLong(KEY_INTERNAL_ID) ?: INVALID_ID.toLong()
        binding.gameNameView.text = savedInstanceState?.getString(KEY_GAME_NAME) ?: arguments?.getString(KEY_GAME_NAME)
        setUpCurrencyView(binding.priceCurrencyView, savedInstanceState?.getString(KEY_PRICE_CURRENCY) ?: arguments?.getString(KEY_PRICE_CURRENCY))
        setUpValue(binding.priceView, savedInstanceState?.getDouble(KEY_PRICE) ?: arguments?.getDouble(KEY_PRICE))
        binding.quantityView.setAndSelectExistingText((savedInstanceState?.getInt(KEY_QUANTITY) ?: arguments?.getInt(KEY_QUANTITY)).toString())
        setAndDisplayAcquisitionDate(savedInstanceState?.getLong(KEY_ACQUISITION_DATE) ?: arguments?.getLong(KEY_ACQUISITION_DATE) ?: 0L)
        binding.acquiredFromView.setAndSelectExistingText(savedInstanceState?.getString(KEY_ACQUIRED_FROM) ?: arguments?.getString(KEY_ACQUIRED_FROM))

        binding.acquisitionDateView.setOnClickListener {
            val date = if (acquisitionDate == 0L) MaterialDatePicker.todayInUtcMilliseconds() else acquisitionDate.fromLocalToUtc()
            val datePicker = MaterialDatePicker.Builder.datePicker().setSelection(date).build()
            datePicker.addOnPositiveButtonClickListener {
                setAndDisplayAcquisitionDate(it.fromLocalToUtc())
            }
            datePicker.show(parentFragmentManager, DATE_PICKER_DIALOG_TAG)
        }

        binding.clearDateView.setOnClickListener {
            setAndDisplayAcquisitionDate(0L)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_INTERNAL_ID, internalId)
        outState.putString(KEY_PRICE_CURRENCY, binding.priceCurrencyView.selectedItem as? String)
        binding.priceView.getDoubleOrNull()?.let { outState.putDouble(KEY_PRICE, it) }
        binding.quantityView.getIntOrNull()?.let { outState.putInt(KEY_QUANTITY, it) }
        outState.putLong(KEY_ACQUISITION_DATE, acquisitionDate)
        outState.putString(KEY_ACQUIRED_FROM, binding.acquiredFromView.text.trim().toString())
    }

    override fun onResume() {
        super.onResume()
        binding.acquiredFromView.setAdapter(acquiredFromAdapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpCurrencyView(spinner: Spinner, item: String?) {
        val priceCurrencyAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.currency, android.R.layout.simple_spinner_item)
        priceCurrencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = priceCurrencyAdapter
        spinner.setSelection(priceCurrencyAdapter.getPosition(item))
    }

    private fun setUpValue(editText: EditText, value: Double?) {
        editText.setAndSelectExistingText(if (value == null || value == 0.0) "" else CURRENCY_FORMAT.format(value))
    }

    private fun setAndDisplayAcquisitionDate(new: Long) {
        acquisitionDate = new
        binding.acquisitionDateView.text = acquisitionDate.formatDateTime(context, 0, DateUtils.FORMAT_SHOW_DATE)
        binding.acquisitionDateLabelView.isInvisible = binding.acquisitionDateView.text.isEmpty()
    }

    companion object {
        private val CURRENCY_FORMAT = DecimalFormat("0.00")
        private const val DATE_PICKER_DIALOG_TAG = "DATE_PICKER_DIALOG"
        private const val KEY_INTERNAL_ID = "INTERNAL_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_PRICE_CURRENCY = "PRICE_CURRENCY"
        private const val KEY_PRICE = "PRICE"
        private const val KEY_QUANTITY = "QUANTITY"
        private const val KEY_ACQUISITION_DATE = "ACQUISITION_DATE"
        private const val KEY_ACQUIRED_FROM = "ACQUIRED_FROM"

        fun newInstance(
            internalId: Long,
            gameName: String,
            priceCurrency: String?,
            price: Double?,
            quantity: Int?,
            acquisitionDate: Long?,
            acquiredFrom: String?,
        ) = CollectionDetailPrivateInfoDialogFragment().apply {
            arguments = bundleOf(
                KEY_INTERNAL_ID to internalId,
                KEY_GAME_NAME to gameName,
                KEY_PRICE_CURRENCY to priceCurrency,
                KEY_PRICE to price,
                KEY_QUANTITY to quantity,
                KEY_ACQUISITION_DATE to acquisitionDate,
                KEY_ACQUIRED_FROM to acquiredFrom,
            )
        }
    }
}
