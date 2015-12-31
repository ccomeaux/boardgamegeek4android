package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PricePaidSorter extends MoneySorter {
	public PricePaidSorter(@NonNull Context context) {
		super(context);
		descriptionId = R.string.collection_sort_price_paid;
	}

	@Override
	protected int getTypeResource() {
		return R.string.collection_sort_type_price_paid;
	}

	@Override
	@NonNull
	protected String getAmountColumnName() {
		return Collection.PRIVATE_INFO_PRICE_PAID;
	}

	@Override
	@NonNull
	protected String getCurrencyColumnName() {
		return Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY;
	}
}
