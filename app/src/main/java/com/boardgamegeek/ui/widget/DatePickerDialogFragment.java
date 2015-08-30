package com.boardgamegeek.ui.widget;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.Calendar;

public class DatePickerDialogFragment extends DialogFragment {
	private OnDateSetListener mListener;
	private long mDateInMillis = 0;
	private final Calendar mCalendar;

	public DatePickerDialogFragment() {
		mCalendar = Calendar.getInstance();
	}

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		mCalendar.setTimeInMillis(mDateInMillis);
		return new DatePickerDialog(getActivity(),
			mListener,
			mCalendar.get(Calendar.YEAR),
			mCalendar.get(Calendar.MONTH),
			mCalendar.get(Calendar.DAY_OF_MONTH));
	}

	public void setOnDateSetListener(DatePickerDialog.OnDateSetListener listener) {
		mListener = listener;
	}

	public void setCurrentDateInMillis(long date) {
		mDateInMillis = date;
	}
}
