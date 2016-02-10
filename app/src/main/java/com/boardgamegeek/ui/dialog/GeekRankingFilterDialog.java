package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.GeekRankingFilterer;

public class GeekRankingFilterDialog extends SliderFilterDialog {
	private int mMinRanking;
	private int mMaxRanking;
	private boolean mUnranked;

	@Override
	protected void captureForm(int min, int max, boolean checkbox) {
		mMinRanking = min;
		mMaxRanking = max;
		mUnranked = checkbox;
	}

	@Override
	protected boolean isChecked() {
		return mUnranked;
	}

	@Override
	protected int getMax() {
		return mMaxRanking;
	}

	@Override
	protected int getAbsoluteMax() {
		return GeekRankingFilterer.MAX_RANGE;
	}

	@Override
	protected int getAbsoluteMin() {
		return GeekRankingFilterer.MIN_RANGE;
	}

	@Override
	protected CollectionFilterer getNegativeData() {
		return new GeekRankingFilterer();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context) {
		return new GeekRankingFilterer(mMinRanking, mMaxRanking, mUnranked);
	}

	@Override
	protected int getMin() {
		return mMinRanking;
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_geek_ranking;
	}

	@Override
	protected void initValues(CollectionFilterer filter) {
		if (filter == null) {
			mMinRanking = GeekRankingFilterer.MIN_RANGE;
			mMaxRanking = GeekRankingFilterer.MAX_RANGE;
			mUnranked = false;
		} else {
			GeekRankingFilterer data = (GeekRankingFilterer) filter;
			mMinRanking = data.getMin();
			mMaxRanking = data.getMax();
			mUnranked = data.includeUnranked();
		}
	}

	@Override
	protected String intervalText(int number) {
		if (number >= GeekRankingFilterer.MAX_RANGE) {
			return String.valueOf(GeekRankingFilterer.MAX_RANGE) + "+";
		}
		return String.valueOf(number);
	}

	@Override
	protected String intervalText(int min, int max) {
		if (min >= GeekRankingFilterer.MAX_RANGE) {
			return String.valueOf(GeekRankingFilterer.MAX_RANGE) + "+";
		}
		return String.valueOf(min) + " - " + String.valueOf(max);
	}
}
