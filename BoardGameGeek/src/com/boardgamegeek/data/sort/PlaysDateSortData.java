package com.boardgamegeek.data.sort;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlaysDateSortData extends PlaysSortData {
	SimpleDateFormat mFormatter = new SimpleDateFormat("MMMM", Locale.getDefault());
	GregorianCalendar mCalendar = new GregorianCalendar();

	public PlaysDateSortData(Context context) {
		super(context);
		mOrderByClause = Plays.DEFAULT_SORT;
		mDescriptionId = R.string.menu_plays_sort_date;

		// account for leap years
		mCalendar.set(Calendar.YEAR, 2012);
		mCalendar.set(Calendar.DAY_OF_MONTH, 1);
	}

	@Override
	public int getType() {
		return PlaysSortDataFactory.TYPE_PLAY_DATE;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.DATE };
	}

	@Override
	public String getSectionText(Cursor cursor) {
		String date = getYearAndMonth(cursor);
		int month = Integer.parseInt(date.substring(5, 7));
		mCalendar.set(Calendar.MONTH, month - 1);
		return mFormatter.format(mCalendar.getTime()) + " " + date.substring(0, 4);
	}

	private String getYearAndMonth(Cursor cursor) {
		String date = getString(cursor, Plays.DATE);
		if (TextUtils.isEmpty(date)) {
			return "1969-01";
		}
		return date.substring(0, 7);
	}
}
