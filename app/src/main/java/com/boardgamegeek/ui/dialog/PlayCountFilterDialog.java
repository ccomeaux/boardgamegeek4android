package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.PlayCountFilterer;
import com.boardgamegeek.util.StringUtils;

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
	public int getType(Context context) {
		return new PlayCountFilterer(context).getType();
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
	protected String getPinText(String value) {
		int year = StringUtils.parseInt(value, PlayCountFilterer.MIN_RANGE);
		if (year == PlayCountFilterer.MAX_RANGE) {
			return value + "+";
		}
		return super.getPinText(value);
	}

	@Override
	protected int getPinValue(String text) {
		if (text.endsWith("+")) {
			return PlayCountFilterer.MAX_RANGE;
		}
		return super.getPinValue(text);
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
