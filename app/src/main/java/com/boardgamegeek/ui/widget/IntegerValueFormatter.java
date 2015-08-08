package com.boardgamegeek.ui.widget;

import com.github.mikephil.charting.utils.ValueFormatter;

import java.text.DecimalFormat;

public class IntegerValueFormatter implements ValueFormatter {
	private DecimalFormat mFormat;

	public IntegerValueFormatter() {
		mFormat = new DecimalFormat("#0");
	}

	@Override
	public String getFormattedValue(float value) {
		return mFormat.format(value);
	}
}
