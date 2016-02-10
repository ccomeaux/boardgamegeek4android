package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.GeekRatingFilterer;

public class GeekRatingFilterDialog extends SliderFilterDialog {
	private static final int FACTOR = 10;
	private double mMinRating;
	private double mMaxRating;

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinRating = (double) (min) / FACTOR;
		mMaxRating = (double) (max) / FACTOR;
	}

	@Override
	protected boolean isChecked() {
		return false;
	}

	@Override
	protected int getCheckboxVisibility() {
		return View.GONE;
	}

	@Override
	protected int getMax() {
		return (int) (mMaxRating * FACTOR);
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
	protected CollectionFilterer getNegativeData() {
		return new GeekRatingFilterer();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context) {
		return new GeekRatingFilterer(context, mMinRating, mMaxRating);
	}

	@Override
	protected int getMin() {
		return (int) (mMinRating * FACTOR);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_geek_rating;
	}

	@Override
	protected void initValues(CollectionFilterer filter) {
		if (filter == null) {
			mMinRating = GeekRatingFilterer.MIN_RANGE;
			mMaxRating = GeekRatingFilterer.MAX_RANGE;
		} else {
			GeekRatingFilterer data = (GeekRatingFilterer) filter;
			mMinRating = data.getMin();
			mMaxRating = data.getMax();
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