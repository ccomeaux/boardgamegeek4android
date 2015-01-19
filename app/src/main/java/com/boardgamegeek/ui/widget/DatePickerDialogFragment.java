package com.boardgamegeek.ui.widget;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class DatePickerDialogFragment extends DialogFragment {
	public static final String KEY_DATE_IN_MILLIS = "DATE";
	private OnDateSetListener mListener;
	private long mDateInMillis = 0;
	private Calendar mCalendar;

	public DatePickerDialogFragment() {
		mCalendar = Calendar.getInstance();
	}

	@Override
	public Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
		mCalendar.setTimeInMillis(mDateInMillis);
		return new DatePickerDialog(getActivity(), mListener, mCalendar.get(Calendar.YEAR),
			mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
	}

	public void setOnDateSetListener(DatePickerDialog.OnDateSetListener listener) {
		mListener = listener;
	}

	public void setCurrentDateInMillis(long date) {
		mDateInMillis = date;
	}
}
