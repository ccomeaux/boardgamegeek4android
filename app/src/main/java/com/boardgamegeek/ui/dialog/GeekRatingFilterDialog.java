package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.GeekRatingFilterer;

public class GeekRatingFilterDialog extends SliderFilterDialog {
	private static final int FACTOR = 10;

	@Override
	protected int getCheckboxVisibility() {
		return View.VISIBLE;
	}

	@Override
	protected int getCheckboxTextId() {
		return R.string.unrated;
	}

	@Override
	protected int getAbsoluteMax() {
		return (int) (GeekRatingFilterer.MAX_RANGE * FACTOR);
	}

	@Override
	protected int getAbsoluteMin() {
		return (int) (GeekRatingFilterer.MIN_RANGE * FACTOR);
	}

	@Override
	public int getType(Context context) {
		return new GeekRatingFilterer(context).getType();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		return new GeekRatingFilterer(context, (double) (min) / FACTOR, (double) (max) / FACTOR, checkbox);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_geek_rating;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		double min = GeekRatingFilterer.MIN_RANGE;
		double max = GeekRatingFilterer.MAX_RANGE;
		boolean unrated = true;
		if (filter != null) {
			GeekRatingFilterer data = (GeekRatingFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			unrated = data.includeUnrated();
		}
		return new InitialValues((int) (min * FACTOR), (int) (max * FACTOR), unrated);
	}

	@Override
	protected String intervalText(int number) {
		return String.valueOf((double) number / FACTOR);
	}

	@Override
	protected String intervalText(int min, int max) {
		return String.valueOf((double) min / FACTOR) + " - " + String.valueOf((double) max / FACTOR);
	}
}