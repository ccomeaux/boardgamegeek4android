package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.YearPublishedFilterer;

public class YearPublishedFilterDialog extends SliderFilterDialog {
	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = YearPublishedFilterer.MIN_RANGE;
		int max = YearPublishedFilterer.MAX_RANGE;
		if (filter != null) {
			YearPublishedFilterer data = (YearPublishedFilterer) filter;
			min = data.getMin();
			max = data.getMax();
		}
		return new InitialValues(min, max);
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
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		return new YearPublishedFilterer(min, max);
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
