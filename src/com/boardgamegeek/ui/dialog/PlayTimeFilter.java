package com.boardgamegeek.ui.dialog;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.PlayTimeFilterData;
import com.boardgamegeek.ui.CollectionActivity;

public class PlayTimeFilter extends SliderFilter {
	private int mMinTime;
	private int mMaxTime;
	private boolean mUndefined;

	@Override
	protected int getLineSpacing() {
		return 30;
	}

	@Override
	protected double getStep() {
		return 5.0;
	}

	@Override
	protected void initValues(CollectionFilterData filter) {
		if (filter == null) {
			mMinTime = PlayTimeFilterData.MIN_RANGE;
			mMaxTime = PlayTimeFilterData.MAX_RANGE;
			mUndefined = false;
		} else {
			PlayTimeFilterData data = (PlayTimeFilterData) filter;
			mMinTime = data.getMin();
			mMaxTime = data.getMax();
			mUndefined = data.isUndefined();
		}

	}

	@Override
	protected int getTitleId() {
		return R.string.menu_play_time;
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new PlayTimeFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(CollectionActivity activity) {
		return new PlayTimeFilterData(activity, mMinTime, mMaxTime, mUndefined);
	}

	@Override
	protected int getStart() {
		return mMinTime;
	}

	@Override
	protected int getEnd() {
		return mMaxTime;
	}

	@Override
	protected boolean getCheckbox() {
		return mUndefined;
	}

	@Override
	protected int getMin() {
		return PlayTimeFilterData.MIN_RANGE;
	}

	@Override
	protected int getMax() {
		return PlayTimeFilterData.MAX_RANGE;
	}

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinTime = min;
		mMaxTime = max;
		mUndefined = checkbox;
	}

	@Override
	protected String intervalText(int number) {
		String text = String.valueOf(number);
		if (number == PlayTimeFilterData.MAX_RANGE) {
			text += "+";
		}
		return text;
	}

	@Override
	protected String intervalText(int min, int max) {
		String text = String.valueOf(min) + " - " + String.valueOf(max);
		if (max == PlayTimeFilterData.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
