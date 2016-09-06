package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.PlayTimeFilterer;
import com.boardgamegeek.util.StringUtils;

public class PlayTimeFilterDialog extends SliderFilterDialog {
	@Override
	protected int getAbsoluteMax() {
		return PlayTimeFilterer.MAX_RANGE;
	}

	@Override
	protected int getAbsoluteMin() {
		return PlayTimeFilterer.MIN_RANGE;
	}

	@Override
	public int getType(Context context) {
		return new PlayTimeFilterer(context).getType();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		return new PlayTimeFilterer(context, min, max, checkbox);
	}

	@Override
	protected int getDescriptionId() {
		return R.string.filter_description_include_missing_play_time;
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_play_time;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = PlayTimeFilterer.MIN_RANGE;
		int max = PlayTimeFilterer.MAX_RANGE;
		boolean includeUndefined = false;
		if (filter != null) {
			PlayTimeFilterer data = (PlayTimeFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			includeUndefined = data.includeUndefined();
		}
		return new InitialValues(min, max, includeUndefined);
	}

	@Override
	protected String getPinText(String value) {
		int year = StringUtils.parseInt(value, PlayTimeFilterer.MIN_RANGE);
		if (year == PlayTimeFilterer.MAX_RANGE) {
			return value + "+";
		}
		return super.getPinText(value);
	}

	@Override
	protected int getPinValue(String text) {
		if (text.endsWith("+")) {
			return PlayTimeFilterer.MAX_RANGE;
		}
		return super.getPinValue(text);
	}
}
