package com.boardgamegeek.ui.dialog;

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.model.PrivateInfo;
import com.boardgamegeek.ui.widget.DatePickerDialogFragment;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.StringUtils;

import java.text.DecimalFormat;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class PrivateInfoDialogFragment extends DialogFragment {
	private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("0.00");
	private static final String DATE_PICKER_DIALOG_TAG = "DATE_PICKER_DIALOG";

	public interface PrivateInfoDialogListener {
		void onFinishEditDialog(PrivateInfo privateInfo);
	}

	private ViewGroup root;
	private DatePickerDialogFragment datePickerDialogFragment;
	private PrivateInfoDialogListener listener;
	@BindView(R.id.price_currency) Spinner priceCurrencyView;
	@BindView(R.id.price) EditText priceView;
	@BindView(R.id.current_value_currency) Spinner currentValueCurrencyView;
	@BindView(R.id.current_value) EditText currentValueView;
	@BindView(R.id.quantity) EditText quantityView;
	@BindView(R.id.acquisition_date_label) TextView acquisitionDateLabelView;
	@BindView(R.id.acquisition_date) TextView acquisitionDateView;
	@BindView(R.id.acquired_from) EditText acquiredFromView;
	@BindView(R.id.comment) EditText commentView;
	@State String priceCurrency;
	@State double price;
	@State String currentValueCurrency;
	@State double currentValue;
	@State String quantity;
	@State String acquisitionDate;
	@State String acquiredFrom;
	@State String comment;

	@NonNull
	public static PrivateInfoDialogFragment newInstance(@Nullable ViewGroup root, PrivateInfoDialogListener listener) {
		PrivateInfoDialogFragment fragment = new PrivateInfoDialogFragment();
		fragment.initialize(root, listener);
		return fragment;
	}

	private void initialize(@Nullable ViewGroup root, PrivateInfoDialogListener listener) {
		this.root = root;
		this.listener = listener;
	}

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		View rootView = layoutInflater.inflate(R.layout.dialog_private_info, root, false);
		ButterKnife.bind(this, rootView);

		Icepick.restoreInstanceState(this, savedInstanceState);
		populateUi();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.title_private_info);
		builder.setView(rootView)
			.setNegativeButton(R.string.cancel, null)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null) {
						PrivateInfo privateInfo = new PrivateInfo();
						privateInfo.setPriceCurrency(priceCurrencyView.getSelectedItem().toString());
						privateInfo.setPrice(StringUtils.parseDouble(priceView.getText().toString().trim()));
						privateInfo.setCurrentValueCurrency(currentValueCurrencyView.getSelectedItem().toString());
						privateInfo.setCurrentValue(StringUtils.parseDouble(currentValueView.getText().toString().trim()));
						privateInfo.setQuantity(StringUtils.parseInt(quantityView.getText().toString().trim(), 1));
						privateInfo.setAcquisitionDate(acquisitionDate);
						privateInfo.setAcquiredFrom(acquiredFromView.getText().toString().trim());
						privateInfo.setPrivateComment(commentView.getText().toString().trim());
						listener.onFinishEditDialog(privateInfo);
					}
				}
			});

		final AlertDialog dialog = builder.create();
		DialogUtils.requestFocus(dialog);
		return dialog;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	private void populateUi() {
		setUpCurrencyView(priceCurrencyView, priceCurrency);
		setUpValue(priceView, price);
		setUpCurrencyView(currentValueCurrencyView, currentValueCurrency);
		setUpValue(currentValueView, currentValue);
		PresentationUtils.setAndSelectExistingText(quantityView, quantity);
		showOrHideAcquisitionDateLabel();
		acquisitionDateView.setText(DateTimeUtils.formatDateFromApi(getContext(), acquisitionDate));
		PresentationUtils.setAndSelectExistingText(acquiredFromView, acquiredFrom);
		PresentationUtils.setAndSelectExistingText(commentView, comment);
	}

	private void setUpCurrencyView(Spinner spinner, String item) {
		ArrayAdapter<CharSequence> priceCurrencyAdapter = ArrayAdapter.createFromResource(getContext(), R.array.currency, android.R.layout.simple_spinner_item);
		priceCurrencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(priceCurrencyAdapter);
		spinner.setSelection(priceCurrencyAdapter.getPosition(item));
	}

	private void setUpValue(EditText editText, double value) {
		if (value == 0.0) {
			editText.setText("");
		} else {
			PresentationUtils.setAndSelectExistingText(editText, CURRENCY_FORMAT.format(value));
		}
	}

	private void showOrHideAcquisitionDateLabel() {
		acquisitionDateLabelView.setVisibility(TextUtils.isEmpty(acquisitionDate) ? View.INVISIBLE : View.VISIBLE);
	}

	@DebugLog
	@OnClick(R.id.acquisition_date)
	public void onDateClick() {
		final FragmentManager fragmentManager = getFragmentManager();
		datePickerDialogFragment = (DatePickerDialogFragment) fragmentManager.findFragmentByTag(DATE_PICKER_DIALOG_TAG);

		datePickerDialogFragment = new DatePickerDialogFragment();
		datePickerDialogFragment.setOnDateSetListener(new OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				acquisitionDate = DateTimeUtils.formatDateForApi(year, monthOfYear, dayOfMonth);
				Calendar calendar = Calendar.getInstance();
				calendar.set(year, monthOfYear, dayOfMonth);
				acquisitionDateView.setText(DateUtils.formatDateTime(getContext(), calendar.getTimeInMillis(), DateUtils.FORMAT_SHOW_DATE));
				showOrHideAcquisitionDateLabel();
			}
		});

		fragmentManager.executePendingTransactions();
		datePickerDialogFragment.setCurrentDateInMillis(DateTimeUtils.getMillisFromApiDate(acquisitionDate, System.currentTimeMillis()));
		datePickerDialogFragment.show(fragmentManager, DATE_PICKER_DIALOG_TAG);
	}

	@DebugLog
	@OnClick(R.id.clear_date)
	public void onClearDateClick() {
		acquisitionDate = "";
		acquisitionDateView.setText("");
		showOrHideAcquisitionDateLabel();
	}

	public void setPriceCurrency(String priceCurrency) {
		this.priceCurrency = priceCurrency;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public void setCurrentValueCurrency(String currentValueCurrency) {
		this.currentValueCurrency = currentValueCurrency;
	}

	public void setCurrentValue(double currentValue) {
		this.currentValue = currentValue;
	}

	public void setQuantity(int quantity) {
		this.quantity = String.valueOf(quantity);
	}

	public void setAcquisitionDate(String acquisitionDate) {
		this.acquisitionDate = acquisitionDate;
	}

	public void setAcquiredFrom(String acquiredFrom) {
		this.acquiredFrom = acquiredFrom;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}
