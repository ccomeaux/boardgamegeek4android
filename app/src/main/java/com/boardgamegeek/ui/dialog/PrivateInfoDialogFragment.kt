package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel
import com.boardgamegeek.ui.widget.DatePickerDialogFragment
import kotlinx.android.synthetic.main.dialog_private_info.*
import java.text.DecimalFormat
import java.util.*

class PrivateInfoDialogFragment : DialogFragment() {
    private val viewModel by activityViewModels<GameCollectionItemViewModel>()
    private var acquisitionDate = 0L
    private lateinit var layout: View
    private val acquiredFromAdapter by lazy { AutoCompleteAdapter(requireContext()) }
    private val inventoryLocationAdapter by lazy { AutoCompleteAdapter(requireContext()) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = ViewModelProvider(requireActivity())[GameCollectionItemViewModel::class.java]
        @SuppressLint("InflateParams")
        layout = layoutInflater.inflate(R.layout.dialog_private_info, null)
        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setTitle(R.string.title_private_info)
            .setView(layout)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.updatePrivateInfo(
                    priceCurrencyView.selectedItem.toString(),
                    priceView.getDoubleOrNull(),
                    currentValueCurrencyView.selectedItem.toString(),
                    currentValueView.getDoubleOrNull(),
                    quantityView.getIntOrNull(),
                    acquisitionDate,
                    acquiredFromView.text.trim().toString(),
                    inventoryLocationView.text.trim().toString()
                )
            }
            .create().apply {
                requestFocus()
            }
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.acquiredFrom.observe(viewLifecycleOwner) {
            acquiredFromAdapter.addData(it)
        }
        viewModel.inventoryLocation.observe(viewLifecycleOwner) {
            inventoryLocationAdapter.addData(it)
        }

        setUpCurrencyView(priceCurrencyView, savedInstanceState?.getString(KEY_PRICE_CURRENCY) ?: arguments?.getString(KEY_PRICE_CURRENCY))
        setUpValue(priceView, savedInstanceState?.getDouble(KEY_PRICE) ?: arguments?.getDouble(KEY_PRICE))
        setUpCurrencyView(
            currentValueCurrencyView,
            savedInstanceState?.getString(KEY_CURRENT_VALUE_CURRENCY) ?: arguments?.getString(KEY_CURRENT_VALUE_CURRENCY)
        )
        setUpValue(currentValueView, savedInstanceState?.getDouble(KEY_CURRENT_VALUE) ?: arguments?.getDouble(KEY_CURRENT_VALUE))
        quantityView.setAndSelectExistingText((savedInstanceState?.getInt(KEY_QUANTITY) ?: arguments?.getInt(KEY_QUANTITY)).toString())
        acquisitionDate = savedInstanceState?.getLong(KEY_ACQUISITION_DATE) ?: arguments?.getLong(KEY_ACQUISITION_DATE) ?: 0L
        acquisitionDateView.text = if (acquisitionDate == 0L) "" else DateUtils.formatDateTime(context, acquisitionDate, DateUtils.FORMAT_SHOW_DATE)
        showOrHideAcquisitionDateLabel()
        acquiredFromView.setAndSelectExistingText(savedInstanceState?.getString(KEY_ACQUIRED_FROM) ?: arguments?.getString(KEY_ACQUIRED_FROM))
        inventoryLocationView.setAndSelectExistingText(
            savedInstanceState?.getString(KEY_INVENTORY_LOCATION) ?: arguments?.getString(
                KEY_INVENTORY_LOCATION
            )
        )

        acquisitionDateView.setOnClickListener {
            val datePickerDialogFragment = createDatePickerDialogFragment()
            parentFragmentManager.executePendingTransactions()
            datePickerDialogFragment.setOnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, monthOfYear, dayOfMonth)
                acquisitionDateView.text = DateUtils.formatDateTime(context, calendar.timeInMillis, DateUtils.FORMAT_SHOW_DATE)
                acquisitionDate = calendar.timeInMillis
                showOrHideAcquisitionDateLabel()
            }
            datePickerDialogFragment.setCurrentDateInMillis(if (acquisitionDate == 0L) System.currentTimeMillis() else acquisitionDate)
            showAndSurvive(datePickerDialogFragment, DATE_PICKER_DIALOG_TAG)
        }

        clearDateView.setOnClickListener {
            acquisitionDate = 0L
            acquisitionDateView.text = ""
            showOrHideAcquisitionDateLabel()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PRICE_CURRENCY, priceCurrencyView.selectedItem as? String)
        priceView.getDoubleOrNull()?.let { outState.putDouble(KEY_PRICE, it) }
        outState.putString(KEY_CURRENT_VALUE_CURRENCY, currentValueCurrencyView.selectedItem as? String)
        currentValueView.getDoubleOrNull()?.let { outState.putDouble(KEY_CURRENT_VALUE, it) }
        quantityView.getIntOrNull()?.let { outState.putInt(KEY_QUANTITY, it) }
        outState.putLong(KEY_ACQUISITION_DATE, acquisitionDate)
        outState.putString(KEY_ACQUIRED_FROM, acquiredFromView.text.trim().toString())
        outState.putString(KEY_INVENTORY_LOCATION, inventoryLocationView.text.trim().toString())
    }

    private fun createDatePickerDialogFragment(): DatePickerDialogFragment {
        return parentFragmentManager.findFragmentByTag(DATE_PICKER_DIALOG_TAG) as DatePickerDialogFragment?
            ?: DatePickerDialogFragment()
    }

    override fun onResume() {
        super.onResume()
        acquiredFromView.setAdapter(acquiredFromAdapter)
        inventoryLocationView.setAdapter(inventoryLocationAdapter)
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

    private fun showOrHideAcquisitionDateLabel() {
        acquisitionDateLabelView.visibility = if (acquisitionDateView.text.isEmpty()) View.INVISIBLE else View.VISIBLE // TODO
    }

    companion object {
        private val CURRENCY_FORMAT = DecimalFormat("0.00")
        private const val DATE_PICKER_DIALOG_TAG = "DATE_PICKER_DIALOG"
        private const val KEY_PRICE_CURRENCY = "PRICE_CURRENCY"
        private const val KEY_PRICE = "PRICE"
        private const val KEY_CURRENT_VALUE_CURRENCY = "CURRENT_VALUE_CURRENCY"
        private const val KEY_CURRENT_VALUE = "CURRENT_VALUE"
        private const val KEY_QUANTITY = "QUANTITY"
        private const val KEY_ACQUISITION_DATE = "ACQUISITION_DATE"
        private const val KEY_ACQUIRED_FROM = "ACQUIRED_FROM"
        private const val KEY_INVENTORY_LOCATION = "INVENTORY_LOCATION"

        fun newInstance(
            priceCurrency: String?,
            price: Double?,
            currentValueCurrency: String?,
            currentValue: Double?,
            quantity: Int?,
            acquisitionDate: Long?,
            acquiredFrom: String?,
            inventoryLocation: String?
        ): PrivateInfoDialogFragment {
            return PrivateInfoDialogFragment().apply {
                arguments = bundleOf(
                    KEY_PRICE_CURRENCY to priceCurrency,
                    KEY_PRICE to price,
                    KEY_CURRENT_VALUE_CURRENCY to currentValueCurrency,
                    KEY_CURRENT_VALUE to currentValue,
                    KEY_QUANTITY to quantity,
                    KEY_ACQUISITION_DATE to acquisitionDate,
                    KEY_ACQUIRED_FROM to acquiredFrom,
                    KEY_INVENTORY_LOCATION to inventoryLocation,
                )
            }
        }
    }
}
