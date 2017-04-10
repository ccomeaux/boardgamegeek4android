package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class AcquisitionDateSorter extends CollectionDateSorter {
	public AcquisitionDateSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_acquisition_date;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_acquisition_date;
	}

	@Override
	protected String getSortColumn() {
		return Collection.PRIVATE_INFO_ACQUISITION_DATE;
	}
}
