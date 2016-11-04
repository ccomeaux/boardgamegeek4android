package com.boardgamegeek.ui.widget;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.Calendar;

public class DatePickerDialogFragment extends DialogFragment {
	private OnDateSetListener listener;
	private long dateInMillis = 0;
	private final Calendar calendar;

	public DatePickerDialogFragment() {
		calendar = Calendar.getInstance();
	}

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		calendar.setTimeInMillis(dateInMillis);
		return new DatePickerDialog(getActivity(),
			listener,
			calendar.get(Calendar.YEAR),
			calendar.get(Calendar.MONTH),
			calendar.get(Calendar.DAY_OF_MONTH));
	}

	public void setOnDateSetListener(DatePickerDialog.OnDateSetListener listener) {
		this.listener = listener;
	}

	public void setCurrentDateInMillis(long date) {
		dateInMillis = date;
	}
}
