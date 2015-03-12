package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.YearPublishedFilterer;

public class YearPublishedFilter extends SliderFilter {
	private int mMinYear;
	private int mMaxYear;

	@Override
	protected void initValues(CollectionFilterer filter) {
		if (filter == null) {
			mMinYear = YearPublishedFilterer.MIN_RANGE;
			mMaxYear = YearPublishedFilterer.MAX_RANGE;
		} else {
			YearPublishedFilterer data = (YearPublishedFilterer) filter;
			mMinYear = data.getMin();
			mMaxYear = data.getMax();
		}
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_year_published;
	}

	@Override
	protected CollectionFilterer getNegativeData() {
		return new YearPublishedFilterer();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context) {
		return new YearPublishedFilterer(context, mMinYear, mMaxYear);
	}

	@Override
	protected int getMin() {
		return mMinYear;
	}

	@Override
	protected int getMax() {
		return mMaxYear;
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
	protected int getAbsoluteMin() {
		return YearPublishedFilterer.MIN_RANGE;
	}

	@Override
	protected int getAbsoluteMax() {
		return YearPublishedFilterer.MAX_RANGE;
	}

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinYear = min;
		mMaxYear = max;
	}

	@Override
	protected String intervalText(int number) {
		return String.valueOf(number);
	}

	@Override
	protected String intervalText(int min, int max) {
		String text = String.valueOf(min);
		if (min == YearPublishedFilterer.MIN_RANGE) {
			text = "<" + text;
		}
		text += " - " + String.valueOf(max);
		if (max == YearPublishedFilterer.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
