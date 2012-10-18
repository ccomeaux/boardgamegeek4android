package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.SuggestedAgeFilterData;

public class SuggestedAgeFilter extends SliderFilter {
	private int mMinAge;
	private int mMaxAge;
	private boolean mUndefined;
	
	@Override
	protected void initValues(CollectionFilterData filter) {
		if (filter == null) {
			mMinAge = SuggestedAgeFilterData.MIN_RANGE;
			mMaxAge = SuggestedAgeFilterData.MAX_RANGE;
			mUndefined = false;
		} else {
			SuggestedAgeFilterData data = (SuggestedAgeFilterData) filter;
			mMinAge = data.getMin();
			mMaxAge = data.getMax();
			mUndefined = data.isUndefined();
		}
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_suggested_age;
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new SuggestedAgeFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(Context context) {
		return new SuggestedAgeFilterData(context, mMinAge, mMaxAge, mUndefined);
	}

	@Override
	protected int getStart() {
		return mMinAge;
	}

	@Override
	protected int getEnd() {
		return mMaxAge;
	}

	@Override
	protected boolean getCheckbox() {
		return mUndefined;
	}

	@Override
	protected int getMin() {
		return SuggestedAgeFilterData.MIN_RANGE;
	}

	@Override
	protected int getMax() {
		return SuggestedAgeFilterData.MAX_RANGE;
	}

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinAge = min;
		mMaxAge = max;
		mUndefined = checkbox;
	}

	@Override
	protected String intervalText(int number) {
		String text = String.valueOf(number);
		if (number == SuggestedAgeFilterData.MAX_RANGE) {
			text += "+";
		}
		return text;
	}

	@Override
	protected String intervalText(int min, int max) {
		String text = String.valueOf(min) + " - " + String.valueOf(max);
		if (max == SuggestedAgeFilterData.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
