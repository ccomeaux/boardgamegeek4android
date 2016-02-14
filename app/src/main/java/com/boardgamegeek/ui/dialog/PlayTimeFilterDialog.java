package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.CollectionFiltererFactory;
import com.boardgamegeek.filterer.PlayTimeFilterer;

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
	protected int getType() {
		return CollectionFiltererFactory.TYPE_PLAY_TIME;
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
	protected String intervalText(int number) {
		String text = String.valueOf(number);
		if (number == PlayTimeFilterer.MAX_RANGE) {
			text += "+";
		}
		return text;
	}

	@Override
	protected String intervalText(int min, int max) {
		String text = String.valueOf(min) + " - " + String.valueOf(max);
		if (max == PlayTimeFilterer.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
