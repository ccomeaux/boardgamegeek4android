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
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogPrivateInfoBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter
import com.boardgamegeek.ui.model.PrivateInfo
import com.boardgamegeek.ui.widget.DatePickerDialogFragment
import java.text.DecimalFormat
import java.util.*

class PrivateInfoDialogFragment : DialogFragment() {
    var privateInfo = PrivateInfo()

    interface PrivateInfoDialogListener {
        fun onPrivateInfoChanged(privateInfo: PrivateInfo)
    }

    private var _binding: DialogPrivateInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var layout: View
    private var listener: PrivateInfoDialogListener? = null
    private var acquisitionDate = ""

    private val acquiredFromAdapter: AutoCompleteAdapter by lazy {
        AutoCompleteAdapter(requireContext(), Collection.PRIVATE_INFO_ACQUIRED_FROM, Collection.buildAcquiredFromUri())
    }

    private val inventoryLocationAdapter: AutoCompleteAdapter by lazy {
        AutoCompleteAdapter(requireContext(), Collection.PRIVATE_INFO_INVENTORY_LOCATION, Collection.buildInventoryLocationUri())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? PrivateInfoDialogListener
        if (listener == null) throw ClassCastException("$context must implement PrivateInfoDialogListener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPrivateInfoBinding.inflate(LayoutInflater.from(context))
        layout = binding.root
        return AlertDialog.Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_private_info)
                .setView(layout)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    if (listener != null) {
                        val privateInfo = captureForm()
                        listener?.onPrivateInfoChanged(privateInfo)
                    }
                }
                .create().apply {
                    requestFocus()
                }
    }

    private fun captureForm(): PrivateInfo {
        return PrivateInfo(
                binding.priceCurrencyView.selectedItem.toString(),
                binding.priceView.getDouble(),
                binding.currentValueCurrencyView.selectedItem.toString(),
                binding.currentValueView.getDouble(),
                binding.quantityView.getInt(),
                acquisitionDate,
                binding.acquiredFromView.text.trim().toString(),
                binding.inventoryLocationView.text.trim().toString())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

        setUpCurrencyView(binding.priceCurrencyView, privateInfo.priceCurrency)
        setUpValue(binding.priceView, privateInfo.price)
        setUpCurrencyView(binding.currentValueCurrencyView, privateInfo.currentValueCurrency)
        setUpValue(binding.currentValueView, privateInfo.currentValue)
        binding.quantityView.setAndSelectExistingText(privateInfo.quantity.toString())
        acquisitionDate = privateInfo.acquisitionDate ?: ""
        binding.acquisitionDateView.text = formatDateFromApi(privateInfo.acquisitionDate)
        showOrHideAcquisitionDateLabel()
        binding.acquiredFromView.setAndSelectExistingText(privateInfo.acquiredFrom)
        binding.inventoryLocationView.setAndSelectExistingText(privateInfo.inventoryLocation)

        binding.acquisitionDateView.setOnClickListener {
            val datePickerDialogFragment = createDatePickerDialogFragment()
            fragmentManager?.executePendingTransactions()
            datePickerDialogFragment.setOnDateSetListener(OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, monthOfYear, dayOfMonth)
                binding.acquisitionDateView.text = DateUtils.formatDateTime(context, calendar.timeInMillis, DateUtils.FORMAT_SHOW_DATE)
                acquisitionDate = calendar.timeInMillis.asDateForApi()
                showOrHideAcquisitionDateLabel()
            })
            datePickerDialogFragment.setCurrentDateInMillis(privateInfo.acquisitionDate.toMillisFromApiDate(System.currentTimeMillis()))
            datePickerDialogFragment.show(requireFragmentManager(), DATE_PICKER_DIALOG_TAG)
        }


        binding.clearDateView.setOnClickListener {
            acquisitionDate = ""
            binding.acquisitionDateView.text = ""
            showOrHideAcquisitionDateLabel()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        return fragmentManager?.findFragmentByTag(DATE_PICKER_DIALOG_TAG) as DatePickerDialogFragment?
                ?: DatePickerDialogFragment()
    }

    override fun onResume() {
        super.onResume()
        binding.acquiredFromView.setAdapter<AutoCompleteAdapter>(acquiredFromAdapter)
        binding.inventoryLocationView.setAdapter<AutoCompleteAdapter>(inventoryLocationAdapter)
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
        binding.acquisitionDateLabelView.visibility = if (binding.acquisitionDateView.text.isEmpty()) View.INVISIBLE else View.VISIBLE
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
