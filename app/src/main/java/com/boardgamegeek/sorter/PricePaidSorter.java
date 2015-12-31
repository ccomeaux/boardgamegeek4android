package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.PresentationUtils;

public class PricePaidSorter extends CollectionSorter {
	public static final String MISSING_DATA = "-";

	public PricePaidSorter(@NonNull Context context) {
		super(context);
		orderByClause = Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY + " DESC, " + Collection.PRIVATE_INFO_PRICE_PAID + " DESC";
		descriptionId = R.string.collection_sort_price_paid;
	}

	@Override
	protected int getTypeResource() {
		return R.string.collection_sort_type_price_paid;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY, Collection.PRIVATE_INFO_PRICE_PAID };
	}

	@NonNull
	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		double amount = CursorUtils.getDouble(cursor, Collection.PRIVATE_INFO_PRICE_PAID);
		String currency = getString(cursor, Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY);
		String info = PresentationUtils.describeMoney(currency, amount);
		return getInfoOrMissingInfo(info);
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		double amount = round(CursorUtils.getDouble(cursor, Collection.PRIVATE_INFO_PRICE_PAID));
		String currency = getString(cursor, Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY);
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
