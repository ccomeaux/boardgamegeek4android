package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.PlayTimeFilterer;
import com.boardgamegeek.util.StringUtils;

public class PlayTimeFilterDialog extends SliderFilterDialog {
	@Override
	protected int getAbsoluteMax() {
		return PlayTimeFilterer.upperBound;
	}

	@Override
	protected int getAbsoluteMin() {
		return PlayTimeFilterer.lowerBound;
	}

	@Override
	public int getType(Context context) {
		return new PlayTimeFilterer(context).getType();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		final PlayTimeFilterer filterer = new PlayTimeFilterer(context);
		filterer.setMin(min);
		filterer.setMax(max);
		filterer.setIncludeUndefined(checkbox);
		return filterer;
	}

	@Override
	protected int getDescriptionResId() {
		return R.string.filter_description_include_missing_play_time;
	}

	@Override
	protected int getTitleResId() {
		return R.string.menu_play_time;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = PlayTimeFilterer.lowerBound;
		int max = PlayTimeFilterer.upperBound;
		boolean includeUndefined = false;
		if (filter != null) {
			PlayTimeFilterer data = (PlayTimeFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			includeUndefined = data.getIncludeUndefined();
		}
		return new InitialValues(min, max, includeUndefined);
	}

	@Override
	protected String getPinText(String value) {
		int year = StringUtils.parseInt(value, PlayTimeFilterer.lowerBound);
		if (year == PlayTimeFilterer.upperBound) {
			return value + "+";
		}
		return super.getPinText(value);
	}
}
