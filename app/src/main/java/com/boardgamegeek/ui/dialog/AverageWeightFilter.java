package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.AverageWeightFilterer;
import com.boardgamegeek.filterer.CollectionFilterer;

public class AverageWeightFilter extends SliderFilter {
	private static final int FACTOR = 10;
	private double mMinWeight;
	private double mMaxWeight;
	private boolean mUndefined;

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinWeight = (double) (min) / FACTOR;
		mMaxWeight = (double) (max) / FACTOR;
		mUndefined = checkbox;
	}

	@Override
	protected boolean isChecked() {
		return mUndefined;
	}

	@Override
	protected int getMax() {
		return (int) (mMaxWeight * FACTOR);
	}

	@Override
	protected int getAbsoluteMax() {
		return (int) (AverageWeightFilterer.MAX_RANGE * FACTOR);
	}

	@Override
	protected int getAbsoluteMin() {
		return (int) (AverageWeightFilterer.MIN_RANGE * FACTOR);
	}

	@Override
	protected CollectionFilterer getNegativeData() {
		return new AverageWeightFilterer();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context) {
		return new AverageWeightFilterer(context, mMinWeight, mMaxWeight, mUndefined);
	}

	@Override
	protected int getMin() {
		return (int) (mMinWeight * FACTOR);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_average_weight;
	}

	@Override
	protected int getDescriptionId() {
		return R.string.filter_description_include_missing_average_weight;
	}

	@Override
	protected void initValues(CollectionFilterer filter) {
		if (filter == null) {
			mMinWeight = AverageWeightFilterer.MIN_RANGE;
			mMaxWeight = AverageWeightFilterer.MAX_RANGE;
			mUndefined = false;
		} else {
			AverageWeightFilterer data = (AverageWeightFilterer) filter;
			mMinWeight = data.getMin();
			mMaxWeight = data.getMax();
			mUndefined = data.isUndefined();
		}
	}

	@Override
	protected String intervalText(int number) {
		return String.valueOf((double) number / FACTOR);
	}

	@Override
	protected String intervalText(int min, int max) {
		return String.valueOf((double) min / FACTOR) + " - " + String.valueOf((double) max / FACTOR);
	}
}
