package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;

import java.text.DecimalFormat;

public abstract class RatingSorter extends CollectionSorter {
	private final String defaultValue;

	public RatingSorter(@NonNull Context context) {
		super(context);
		defaultValue = context.getString(R.string.text_unknown);
	}

	@Override
	protected boolean isSortDescending() {
		return true;
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getInfo(cursor, null);
	}

	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		return getInfo(cursor, getDisplayFormat());
	}

	protected String getInfo(@NonNull Cursor cursor, DecimalFormat format) {
		return getDoubleAsString(cursor, getSortColumn(), defaultValue, true, format);
	}

	protected abstract DecimalFormat getDisplayFormat();
}
