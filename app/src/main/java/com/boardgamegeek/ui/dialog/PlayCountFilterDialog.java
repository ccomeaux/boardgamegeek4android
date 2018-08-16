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
		return PlayCountFilterer.lowerBound;
	}

	@Override
	protected int getAbsoluteMax() {
		return PlayCountFilterer.upperBound;
	}

	@Override
	public int getType(Context context) {
		return new PlayCountFilterer(context).getType();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		final PlayCountFilterer filterer = new PlayCountFilterer(context);
		filterer.setMin(min);
		filterer.setMax(max);
		return filterer;
	}

	@Override
	protected int getTitleResId() {
		return R.string.menu_play_count;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = PlayCountFilterer.lowerBound;
		int max = PlayCountFilterer.upperBound;
		if (filter != null) {
			PlayCountFilterer data = (PlayCountFilterer) filter;
			min = data.getMin();
			max = data.getMax();
		}
		return new InitialValues(min, max);
	}

	@Override
	protected String getPinText(String value) {
		int year = StringUtils.parseInt(value, PlayCountFilterer.lowerBound);
		if (year == PlayCountFilterer.upperBound) {
			return value + "+";
		}
		return super.getPinText(value);
	}
}
