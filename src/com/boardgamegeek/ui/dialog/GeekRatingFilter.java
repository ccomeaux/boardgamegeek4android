package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.GeekRatingFilterData;

public class GeekRatingFilter extends SliderFilter {
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
		return (int) (GeekRatingFilterData.MAX_RANGE * FACTOR);
	}

	@Override
	protected int getMin() {
		return (int) (GeekRatingFilterData.MIN_RANGE * FACTOR);
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new GeekRatingFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(Context context) {
		return new GeekRatingFilterData(context, mMinRating, mMaxRating);
	}

	@Override
	protected int getStart() {
		return (int) (mMinRating * FACTOR);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_geek_rating;
	}

	@Override
	protected void initValues(CollectionFilterData filter) {
		if (filter == null) {
			mMinRating = GeekRatingFilterData.MIN_RANGE;
			mMaxRating = GeekRatingFilterData.MAX_RANGE;
		} else {
			GeekRatingFilterData data = (GeekRatingFilterData) filter;
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