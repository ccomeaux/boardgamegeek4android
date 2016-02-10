package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.AverageRatingFilterer;
import com.boardgamegeek.filterer.CollectionFilterer;

public class AverageRatingFilterDialog extends SliderFilterDialog {
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
		return (int) (AverageRatingFilterer.MAX_RANGE * FACTOR);
	}

	@Override
	protected int getAbsoluteMin() {
		return (int) (AverageRatingFilterer.MIN_RANGE * FACTOR);
	}

	@Override
	protected CollectionFilterer getNegativeData() {
		return new AverageRatingFilterer();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		return new AverageRatingFilterer(context, (double) (min) / FACTOR, (double) (max) / FACTOR, checkbox);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_average_rating;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		double min = AverageRatingFilterer.MIN_RANGE;
		double max = AverageRatingFilterer.MAX_RANGE;
		boolean unrated = true;
		if (filter != null) {
			AverageRatingFilterer data = (AverageRatingFilterer) filter;
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
