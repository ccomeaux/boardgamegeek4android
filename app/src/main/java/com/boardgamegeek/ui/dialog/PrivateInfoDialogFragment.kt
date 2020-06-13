package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter
import com.boardgamegeek.ui.model.PrivateInfo
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel
import com.boardgamegeek.ui.widget.DatePickerDialogFragment
import kotlinx.android.synthetic.main.dialog_private_info.*
import java.text.DecimalFormat
import java.util.*

class PrivateInfoDialogFragment : DialogFragment() {
    var privateInfo = PrivateInfo()

    private lateinit var layout: View
    private var acquisitionDate = ""

    private val acquiredFromAdapter: AutoCompleteAdapter by lazy {
        AcquiredFromAdapter(requireContext())
    }

    private val inventoryLocationAdapter: AutoCompleteAdapter by lazy {
        InventoryLocationAdapter(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = ViewModelProvider(requireActivity()).get(GameCollectionItemViewModel::class.java)
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_private_info, null)
        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_private_info)
                .setView(layout)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val privateInfo = captureForm()
                    viewModel.update(privateInfo)
                }
                .create().apply {
                    requestFocus()
                }
    }

    private fun captureForm(): PrivateInfo {
        return PrivateInfo(
                priceCurrencyView.selectedItem.toString(),
                priceView.getDouble(),
                currentValueCurrencyView.selectedItem.toString(),
                currentValueView.getDouble(),
                quantityView.getInt(),
                acquisitionDate,
                acquiredFromView.text.trim().toString(),
                inventoryLocationView.text.trim().toString())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            privateInfo = PrivateInfo(
                    savedInstanceState.getString(KEY_PRICE_CURRENCY),
                    savedInstanceState.getDouble(KEY_PRICE),
                    savedInstanceState.getString(KEY_CURRENT_VALUE_CURRENCY),
                    savedInstanceState.getDouble(KEY_CURRENT_VALUE),
                    savedInstanceState.getInt(KEY_QUANTITY),
                    savedInstanceState.getString(KEY_ACQUISITION_DATE),
                    savedInstanceState.getString(KEY_ACQUIRED_FROM),
                    savedInstanceState.getString(KEY_INVENTORY_LOCATION)
            )
        }

        setUpCurrencyView(priceCurrencyView, privateInfo.priceCurrency)
        setUpValue(priceView, privateInfo.price)
        setUpCurrencyView(currentValueCurrencyView, privateInfo.currentValueCurrency)
        setUpValue(currentValueView, privateInfo.currentValue)
        quantityView.setAndSelectExistingText(privateInfo.quantity.toString())
        acquisitionDate = privateInfo.acquisitionDate ?: ""
        acquisitionDateView.text = formatDateFromApi(privateInfo.acquisitionDate)
        showOrHideAcquisitionDateLabel()
        acquiredFromView.setAndSelectExistingText(privateInfo.acquiredFrom)
        inventoryLocationView.setAndSelectExistingText(privateInfo.inventoryLocation)

        acquisitionDateView.setOnClickListener {
            val datePickerDialogFragment = createDatePickerDialogFragment()
            parentFragmentManager.executePendingTransactions()
            datePickerDialogFragment.setOnDateSetListener(OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, monthOfYear, dayOfMonth)
                acquisitionDateView.text = DateUtils.formatDateTime(context, calendar.timeInMillis, DateUtils.FORMAT_SHOW_DATE)
                acquisitionDate = calendar.timeInMillis.asDateForApi()
                showOrHideAcquisitionDateLabel()
            })
            datePickerDialogFragment.setCurrentDateInMillis(acquisitionDate.toMillisFromApiDate(System.currentTimeMillis()))
            datePickerDialogFragment.show(parentFragmentManager, DATE_PICKER_DIALOG_TAG)
        }


        clearDateView.setOnClickListener {
            acquisitionDate = ""
            acquisitionDateView.text = ""
            showOrHideAcquisitionDateLabel()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        captureForm().apply {
            outState.putString(KEY_PRICE_CURRENCY, priceCurrency)
            outState.putDouble(KEY_PRICE, price)
            outState.putString(KEY_CURRENT_VALUE_CURRENCY, currentValueCurrency)
            outState.putDouble(KEY_CURRENT_VALUE, currentValue)
            outState.putInt(KEY_QUANTITY, quantity)
            outState.putString(KEY_ACQUISITION_DATE, acquisitionDate)
            outState.putString(KEY_ACQUIRED_FROM, acquiredFrom)
            outState.putString(KEY_INVENTORY_LOCATION, inventoryLocation)
        }
    }

    private fun formatDateFromApi(date: String?): String {
        val millis = date.toMillisFromApiDate()
        return if (millis == 0L) "" else DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE)
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

    override fun onPause() {
        super.onPause()
        acquiredFromAdapter.changeCursor(null)
        inventoryLocationAdapter.changeCursor(null)
    }

    private fun setUpCurrencyView(spinner: Spinner, item: String?) {
        val priceCurrencyAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.currency, android.R.layout.simple_spinner_item)
        priceCurrencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = priceCurrencyAdapter
        spinner.setSelection(priceCurrencyAdapter.getPosition(item))
    }

    private fun setUpValue(editText: EditText, value: Double) {
        editText.setAndSelectExistingText(if (value == 0.0) "" else CURRENCY_FORMAT.format(value))
    }

    private fun showOrHideAcquisitionDateLabel() {
        acquisitionDateLabelView.visibility = if (acquisitionDateView.text.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    inner class AcquiredFromAdapter(context: Context) : AutoCompleteAdapter(context, Collection.PRIVATE_INFO_ACQUIRED_FROM, Collection.buildAcquiredFromUri()) {
        override val defaultSelection = "${Collection.PRIVATE_INFO_ACQUIRED_FROM}<>''"
    }

    class InventoryLocationAdapter(context: Context) : AutoCompleteAdapter(context, Collection.PRIVATE_INFO_INVENTORY_LOCATION, Collection.buildInventoryLocationUri()) {
        override val defaultSelection = "${Collection.PRIVATE_INFO_INVENTORY_LOCATION}<>''"
    }

    companion object {
        private val CURRENCY_FORMAT = DecimalFormat("0.00")
        private const val DATE_PICKER_DIALOG_TAG = "DATE_PICKER_DIALOG"
        private const val KEY_PRICE_CURRENCY = "PRICE_CURRENCY"
        private const val KEY_PRICE = "PRICE"
        private const val KEY_CURRENT_VALUE_CURRENCY = "CURRENT_VALUE_CURRENCY"
        private const val KEY_CURRENT_VALUE = "ACQUISITION_DATE"
        private const val KEY_QUANTITY = "QUANTITY"
        private const val KEY_ACQUISITION_DATE = "ACQUISITION_DATE"
        private const val KEY_ACQUIRED_FROM = "ACQUIRED_FROM"
        private const val KEY_INVENTORY_LOCATION = "INVENTORY_LOCATION"

        @JvmStatic
        fun newInstance(): PrivateInfoDialogFragment {
            return PrivateInfoDialogFragment()
        }
    }
}
