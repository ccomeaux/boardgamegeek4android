package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.PlayTimeFilterer;

public class PlayTimeFilter extends SliderFilter {
	private int mMinTime;
	private int mMaxTime;
	private boolean mUndefined;

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinTime = min;
		mMaxTime = max;
		mUndefined = checkbox;
	}

	@Override
	protected boolean isChecked() {
		return mUndefined;
	}

	@Override
	protected int getMax() {
		return mMaxTime;
	}

	@Override
	protected int getAbsoluteMax() {
		return PlayTimeFilterer.MAX_RANGE;
	}

	@Override
	protected int getAbsoluteMin() {
		return PlayTimeFilterer.MIN_RANGE;
	}

	@Override
	protected CollectionFilterer getNegativeData() {
		return new PlayTimeFilterer();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context) {
		return new PlayTimeFilterer(context, mMinTime, mMaxTime, mUndefined);
	}

	@Override
	protected int getDescriptionId() {
		return R.string.filter_description_include_missing_play_time;
	}

	@Override
	protected int getMin() {
		return mMinTime;
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_play_time;
	}

	@Override
	protected void initValues(CollectionFilterer filter) {
		if (filter == null) {
			mMinTime = PlayTimeFilterer.MIN_RANGE;
			mMaxTime = PlayTimeFilterer.MAX_RANGE;
			mUndefined = false;
		} else {
			PlayTimeFilterer data = (PlayTimeFilterer) filter;
			mMinTime = data.getMin();
			mMaxTime = data.getMax();
			mUndefined = data.includeUndefined();
		}
	}

	@Override
	protected String intervalText(int number) {
		String text = String.valueOf(number);
		if (number == PlayTimeFilterer.MAX_RANGE) {
			text += "+";
		}
		return text;
	}

	@Override
	protected String intervalText(int min, int max) {
		String text = String.valueOf(min) + " - " + String.valueOf(max);
		if (max == PlayTimeFilterer.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
