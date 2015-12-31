package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.PresentationUtils;

public abstract class MoneySorter extends CollectionSorter {
	public static final String MISSING_DATA = "-";

	public MoneySorter(Context context) {
		super(context);
		orderByClause = getCurrencyColumnName() + " DESC, " + getAmountColumnName() + " DESC";
	}

	@NonNull
	protected abstract String getAmountColumnName();

	@NonNull
	protected abstract String getCurrencyColumnName();

	@Override
	public String[] getColumns() {
		return new String[] { getCurrencyColumnName(), getAmountColumnName() };
	}

	@NonNull
	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		double amount = CursorUtils.getDouble(cursor, getAmountColumnName());
		String currency = getString(cursor, getCurrencyColumnName());
		String info = PresentationUtils.describeMoney(currency, amount);
		return getInfoOrMissingInfo(info);
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		double amount = round(CursorUtils.getDouble(cursor, getAmountColumnName()));
		String currency = getString(cursor, getCurrencyColumnName());
		String info = PresentationUtils.describeMoneyWithoutDecimals(currency, amount);
		return getInfoOrMissingInfo(info);
	}

	@NonNull
	private String getInfoOrMissingInfo(String info) {
		if (TextUtils.isEmpty(info)) {
			return MISSING_DATA;
		}
		return info;
	}

	private double round(double value) {
		return ((int) (Math.ceil(value + 9) / 10)) * 10;
	}
}
