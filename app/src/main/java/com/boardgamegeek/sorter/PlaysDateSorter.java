package com.boardgamegeek.sorter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlaysDateSorter extends PlaysSorter {
	private final SimpleDateFormat formatter = new SimpleDateFormat("MMMM", Locale.getDefault());
	private final GregorianCalendar calendar = new GregorianCalendar();

	public PlaysDateSorter(Context context) {
		super(context);
		orderByClause = Plays.DEFAULT_SORT;
		descriptionId = R.string.menu_plays_sort_date;

		// account for leap years
		calendar.set(Calendar.YEAR, 2012);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
	}

	@Override
	public int getType() {
		return PlaysSorterFactory.TYPE_PLAY_DATE;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.DATE };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		String date = getYearAndMonth(cursor);
		int month = Integer.parseInt(date.substring(5, 7));
		calendar.set(Calendar.MONTH, month - 1);
		return formatter.format(calendar.getTime()) + " " + date.substring(0, 4);
	}

	private String getYearAndMonth(Cursor cursor) {
		String date = getString(cursor, Plays.DATE);
		if (TextUtils.isEmpty(date)) {
			return "1969-01";
		}
		return date.substring(0, 7);
	}
}
