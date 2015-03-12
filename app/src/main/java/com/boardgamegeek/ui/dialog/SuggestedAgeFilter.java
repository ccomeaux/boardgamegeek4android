package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.SuggestedAgeFilterer;

public class SuggestedAgeFilter extends SliderFilter {
	private int mMinAge;
	private int mMaxAge;
	private boolean mUndefined;
	
	@Override
	protected void initValues(CollectionFilterer filter) {
		if (filter == null) {
			mMinAge = SuggestedAgeFilterer.MIN_RANGE;
			mMaxAge = SuggestedAgeFilterer.MAX_RANGE;
			mUndefined = false;
		} else {
			SuggestedAgeFilterer data = (SuggestedAgeFilterer) filter;
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
	protected int getDescriptionId() {
		return R.string.filter_description_include_missing_suggested_age;
	}

	@Override
	protected CollectionFilterer getNegativeData() {
		return new SuggestedAgeFilterer();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context) {
		return new SuggestedAgeFilterer(context, mMinAge, mMaxAge, mUndefined);
	}

	@Override
	protected int getMin() {
		return mMinAge;
	}

	@Override
	protected int getMax() {
		return mMaxAge;
	}

	@Override
	protected boolean isChecked() {
		return mUndefined;
	}

	@Override
	protected int getAbsoluteMin() {
		return SuggestedAgeFilterer.MIN_RANGE;
	}

	@Override
	protected int getAbsoluteMax() {
		return SuggestedAgeFilterer.MAX_RANGE;
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
		if (number == SuggestedAgeFilterer.MAX_RANGE) {
			text += "+";
		}
		return text;
	}

	@Override
	protected String intervalText(int min, int max) {
		String text = String.valueOf(min) + " - " + String.valueOf(max);
		if (max == SuggestedAgeFilterer.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
