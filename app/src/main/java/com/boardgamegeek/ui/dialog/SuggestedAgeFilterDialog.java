package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.SuggestedAgeFilterer;

public class SuggestedAgeFilterDialog extends SliderFilterDialog {
	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = SuggestedAgeFilterer.MIN_RANGE;
		int max = SuggestedAgeFilterer.MAX_RANGE;
		boolean includeUndefined = false;
		if (filter != null) {
			SuggestedAgeFilterer data = (SuggestedAgeFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			includeUndefined = data.includeUndefined();
		}
		return new InitialValues(min, max, includeUndefined);
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
	protected CollectionFilterer getNegativeData(Context context) {
		return new SuggestedAgeFilterer(context);
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		return new SuggestedAgeFilterer(context, min, max, checkbox);
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
