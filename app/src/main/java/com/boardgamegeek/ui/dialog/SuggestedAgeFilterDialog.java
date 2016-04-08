package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.SuggestedAgeFilterer;
import com.boardgamegeek.util.StringUtils;

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
	public int getType(Context context) {
		return new SuggestedAgeFilterer(context).getType();
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
	protected String getPinText(String value) {
		int year = StringUtils.parseInt(value, SuggestedAgeFilterer.MIN_RANGE);
		if (year == SuggestedAgeFilterer.MAX_RANGE) {
			return value + "+";
		}
		return super.getPinText(value);
	}

	@Override
	protected int getPinValue(String text) {
		if (text.endsWith("+")) {
			return SuggestedAgeFilterer.MAX_RANGE;
		}
		return super.getPinValue(text);
	}
}
