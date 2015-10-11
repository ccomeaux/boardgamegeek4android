package com.boardgamegeek.ui.widget;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;

public class IntegerValueFormatter implements ValueFormatter {
	private final DecimalFormat mFormat;
	private final boolean mSuppressZero;

	public IntegerValueFormatter(boolean suppressZero) {
		mSuppressZero = suppressZero;
		mFormat = new DecimalFormat("#0");
	}

	@Override
	public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
		if (mSuppressZero && value == 0.0f) {
			return "";
		}
		return mFormat.format(value);
	}
}
