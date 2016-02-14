package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.CollectionFiltererFactory;
import com.boardgamegeek.filterer.PlayCountFilterer;

public class PlayCountFilterDialog extends SliderFilterDialog {
	@Override
	protected int getCheckboxVisibility() {
		return View.GONE;
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
	protected int getType() {
		return CollectionFiltererFactory.TYPE_PLAY_COUNT;
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		return new PlayCountFilterer(context, min, max);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_play_count;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = PlayCountFilterer.MIN_RANGE;
		int max = PlayCountFilterer.MAX_RANGE;
		if (filter != null) {
			PlayCountFilterer data = (PlayCountFilterer) filter;
			min = data.getMin();
			max = data.getMax();
		}
		return new InitialValues(min, max);
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
