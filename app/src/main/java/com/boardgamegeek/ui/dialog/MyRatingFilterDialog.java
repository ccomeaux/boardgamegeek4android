package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.MyRatingFilterer;

public class MyRatingFilterDialog extends SliderFilterDialog {
	private static final int FACTOR = 10;
	private double minRating;
	private double maxRating;
	private boolean includeUnrated;

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		minRating = (double) (min) / FACTOR;
		maxRating = (double) (max) / FACTOR;
		includeUnrated = checkbox;
	}

	@Override
	protected boolean isChecked() {
		return includeUnrated;
	}

	@Override
	protected int getCheckboxVisibility() {
		return View.VISIBLE;
	}

	@Override
	protected int getMax() {
		return (int) (maxRating * FACTOR);
	}

	@Override
	protected int getAbsoluteMax() {
		return (int) (MyRatingFilterer.MAX_RANGE * FACTOR);
	}

	@Override
	protected int getAbsoluteMin() {
		return (int) (MyRatingFilterer.MIN_RANGE * FACTOR);
	}

	@Override
	protected CollectionFilterer getNegativeData() {
		return new MyRatingFilterer();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context) {
		return new MyRatingFilterer(context, minRating, maxRating, includeUnrated);
	}

	@Override
	protected int getMin() {
		return (int) (minRating * FACTOR);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_geek_rating;
	}

	@Override
	protected void initValues(CollectionFilterer filter) {
		if (filter == null) {
			minRating = MyRatingFilterer.MIN_RANGE;
			maxRating = MyRatingFilterer.MAX_RANGE;
			includeUnrated = true;
		} else {
			MyRatingFilterer data = (MyRatingFilterer) filter;
			minRating = data.getMin();
			maxRating = data.getMax();
			includeUnrated = data.includeUnrated();
		}
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