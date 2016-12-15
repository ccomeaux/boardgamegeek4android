package com.boardgamegeek.ui.widget;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;

import java.text.DecimalFormat;

public class IntegerYAxisValueFormatter implements YAxisValueFormatter {
	private final DecimalFormat format;

	public IntegerYAxisValueFormatter() {
		format = new DecimalFormat("#0");
	}

	@Override
	public String getFormattedValue(float value, YAxis yAxis) {
		return format.format(value);
	}
}
