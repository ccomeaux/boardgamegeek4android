package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.PlayCountFilterData;

public class PlayCountFilter extends SliderFilter {
	private int mMinTime;
	private int mMaxTime;

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinTime = min;
		mMaxTime = max;
	}

	@Override
	protected boolean isChecked() {
		return false;
	}

	@Override
	protected int getCheckboxVisibility() {
		return View.GONE;
	}

	@Override
	protected int getMin() {
		return mMinTime;
	}

	@Override
	protected int getMax() {
		return mMaxTime;
	}

	@Override
	protected int getAbsoluteMin() {
		return PlayCountFilterData.MIN_RANGE;
	}

	@Override
	protected int getAbsoluteMax() {
		return PlayCountFilterData.MAX_RANGE;
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new PlayCountFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(Context context) {
		return new PlayCountFilterData(context, mMinTime, mMaxTime);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_play_count;
	}

	@Override
	protected void initValues(CollectionFilterData filter) {
		if (filter == null) {
			mMinTime = PlayCountFilterData.MIN_RANGE;
			mMaxTime = PlayCountFilterData.MAX_RANGE;
		} else {
			PlayCountFilterData data = (PlayCountFilterData) filter;
			mMinTime = data.getMin();
			mMaxTime = data.getMax();
		}
	}

	@Override
	protected String intervalText(int number) {
		String text = String.valueOf(number);
		if (number == PlayCountFilterData.MAX_RANGE) {
			text += "+";
		}
		return text;
	}

	@Override
	protected String intervalText(int min, int max) {
		if (min == max) {
			return intervalText(min);
		}
		String text = String.valueOf(min) + " - " + String.valueOf(max);
		if (max == PlayCountFilterData.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
