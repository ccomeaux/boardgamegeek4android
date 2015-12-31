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
		return getInfo(cursor, amount);
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		double amount = CursorUtils.getDouble(cursor, Collection.PRIVATE_INFO_PRICE_PAID);
		return getInfo(cursor, amount);
	}

	private String getInfo(@NonNull Cursor cursor, double amount) {
		String info = PresentationUtils.describeMoney(
			getString(cursor, Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY),
			amount);
		if (TextUtils.isEmpty(info)) {
			return "-";
		}
		return info;
	}
}
