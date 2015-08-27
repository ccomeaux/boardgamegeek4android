package com.boardgamegeek.ui.widget;

import com.github.mikephil.charting.utils.ValueFormatter;

import java.text.DecimalFormat;

public class IntegerValueFormatter implements ValueFormatter {
	private final DecimalFormat mFormat;
	private final boolean mSuppressZero;

	public IntegerValueFormatter() {
		mSuppressZero = false;
		mFormat = new DecimalFormat("#0");
	}

	public IntegerValueFormatter(boolean suppressZero) {
		mSuppressZero = suppressZero;
		mFormat = new DecimalFormat("#0");
	}

	@Override
	public String getFormattedValue(float value) {
		if (mSuppressZero && value == 0.0f) {
			return "";
		}
		return mFormat.format(value);
	}
}
