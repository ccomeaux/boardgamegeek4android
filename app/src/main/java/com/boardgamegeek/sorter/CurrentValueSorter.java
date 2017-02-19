package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class CurrentValueSorter extends MoneySorter {
	public CurrentValueSorter(Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_current_value;
	}

	@StringRes
	@Override
	protected int getTypeResource() {
		return R.string.collection_sort_type_current_value;
	}

	@NonNull
	@Override
	protected String getAmountColumnName() {
		return Collection.PRIVATE_INFO_CURRENT_VALUE;
	}

	@NonNull
	@Override
	protected String getCurrencyColumnName() {
		return Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY;
	}
}
