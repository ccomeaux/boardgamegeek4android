package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

import java.text.DecimalFormat;

public class GeekRatingSorter extends RatingSorter {
	private static final DecimalFormat DISPLAY_FORMAT = new DecimalFormat("0.000");

	public GeekRatingSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_geek_rating;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_geek_rating;
	}

	@Override
	protected String getSortColumn() {
		return Collection.STATS_BAYES_AVERAGE;
	}

	@Override
	protected DecimalFormat getDisplayFormat() {
		return DISPLAY_FORMAT;
	}
}
