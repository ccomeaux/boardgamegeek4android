package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.SuggestedAgeFilterer;
import com.boardgamegeek.util.StringUtils;

public class SuggestedAgeFilterDialog extends SliderFilterDialog {
	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = SuggestedAgeFilterer.lowerBound;
		int max = SuggestedAgeFilterer.upperBound;
		boolean includeUndefined = false;
		if (filter != null) {
			SuggestedAgeFilterer data = (SuggestedAgeFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			includeUndefined = data.getIncludeUndefined();
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
		final SuggestedAgeFilterer filterer = new SuggestedAgeFilterer(context);
		filterer.setMin(min);
		filterer.setMax(max);
		filterer.setIncludeUndefined(checkbox);
		return filterer;
	}

	@Override
	protected int getAbsoluteMin() {
		return SuggestedAgeFilterer.lowerBound;
	}

	@Override
	protected int getAbsoluteMax() {
		return SuggestedAgeFilterer.upperBound;
	}

	@Override
	protected String getPinText(String value) {
		int year = StringUtils.parseInt(value, SuggestedAgeFilterer.lowerBound);
		if (year == SuggestedAgeFilterer.upperBound) {
			return value + "+";
		}
		return super.getPinText(value);
	}
}
