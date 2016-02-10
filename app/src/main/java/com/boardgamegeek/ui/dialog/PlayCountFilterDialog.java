package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.PlayCountFilterer;

public class PlayCountFilterDialog extends SliderFilterDialog {
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
		return PlayCountFilterer.MIN_RANGE;
	}

	@Override
	protected int getAbsoluteMax() {
		return PlayCountFilterer.MAX_RANGE;
	}

	@Override
	protected CollectionFilterer getNegativeData() {
		return new PlayCountFilterer();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context) {
		return new PlayCountFilterer(context, mMinTime, mMaxTime);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_play_count;
	}

	@Override
	protected void initValues(CollectionFilterer filter) {
		if (filter == null) {
			mMinTime = PlayCountFilterer.MIN_RANGE;
			mMaxTime = PlayCountFilterer.MAX_RANGE;
		} else {
			PlayCountFilterer data = (PlayCountFilterer) filter;
			mMinTime = data.getMin();
			mMaxTime = data.getMax();
		}
	}

	@Override
	protected String intervalText(int number) {
		String text = String.valueOf(number);
		if (number == PlayCountFilterer.MAX_RANGE) {
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
		if (max == PlayCountFilterer.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
