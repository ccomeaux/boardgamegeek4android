package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.data.AverageWeightFilterData;
import com.boardgamegeek.data.CollectionFilterData;

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
	protected boolean getCheckbox() {
		return mUndefined;
	}

	@Override
	protected int getEnd() {
		return (int) (mMaxWeight * FACTOR);
	}
	
	@Override
	protected int getLineSpacing() {
		return FACTOR;
	}

	@Override
	protected int getMax() {
		return (int) (AverageWeightFilterData.MAX_RANGE * FACTOR);
	}

	@Override
	protected int getMin() {
		return (int) (AverageWeightFilterData.MIN_RANGE * FACTOR);
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new AverageWeightFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(Context context) {
		return new AverageWeightFilterData(context, mMinWeight, mMaxWeight, mUndefined);
	}

	@Override
	protected int getStart() {
		return (int) (mMinWeight * FACTOR);
	}

	@Override
	protected double getStep() {
		return 2.0;
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
	protected void initValues(CollectionFilterData filter) {
		if (filter == null) {
			mMinWeight = AverageWeightFilterData.MIN_RANGE;
			mMaxWeight = AverageWeightFilterData.MAX_RANGE;
			mUndefined = false;
		} else {
			AverageWeightFilterData data = (AverageWeightFilterData) filter;
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
