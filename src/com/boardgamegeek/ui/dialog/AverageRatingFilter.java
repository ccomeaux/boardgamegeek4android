package com.boardgamegeek.ui.dialog;

import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.data.AverageRatingFilterData;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.ui.CollectionActivity;

public class AverageRatingFilter extends SliderFilter {
	private static final int FACTOR = 10;
	private double mMinRating;
	private double mMaxRating;

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinRating = (double) (min) / FACTOR;
		mMaxRating = (double) (max) / FACTOR;
	}

	@Override
	protected boolean getCheckbox() {
		return false;
	}

	@Override
	protected int getCheckboxVisibility() {
		return View.GONE;
	}

	@Override
	protected int getEnd() {
		return (int) (mMaxRating * FACTOR);
	}

	@Override
	protected int getLineSpacing() {
		return FACTOR;
	}

	@Override
	protected int getMax() {
		return (int) (AverageRatingFilterData.MAX_RANGE * FACTOR);
	}

	@Override
	protected int getMin() {
		return (int) (AverageRatingFilterData.MIN_RANGE * FACTOR);
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new AverageRatingFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(CollectionActivity activity) {
		return new AverageRatingFilterData(activity, mMinRating, mMaxRating);
	}

	@Override
	protected int getStart() {
		return (int) (mMinRating * FACTOR);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_average_rating;
	}

	@Override
	protected void initValues(CollectionFilterData filter) {
		if (filter == null) {
			mMinRating = AverageRatingFilterData.MIN_RANGE;
			mMaxRating = AverageRatingFilterData.MAX_RANGE;
		} else {
			AverageRatingFilterData data = (AverageRatingFilterData) filter;
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
