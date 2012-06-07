package com.boardgamegeek.ui.dialog;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.GeekRankingFilterData;
import com.boardgamegeek.ui.CollectionActivity;

public class GeekRankingFilter extends SliderFilter {
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
	protected boolean getCheckbox() {
		return mUnranked;
	}

	@Override
	protected int getEnd() {
		return mMaxRanking;
	}

	@Override
	protected int getLineSpacing() {
		return 500;
	}

	@Override
	protected int getMax() {
		return GeekRankingFilterData.MAX_RANGE;
	}

	@Override
	protected int getMin() {
		return GeekRankingFilterData.MIN_RANGE;
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new GeekRankingFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(CollectionActivity activity) {
		return new GeekRankingFilterData(activity, mMinRanking, mMaxRanking, mUnranked);
	}

	@Override
	protected int getStartOffset() {
		return 1;
	}

	@Override
	protected int getStart() {
		return mMinRanking;
	}

	@Override
	protected double getStep() {
		return 100.0;
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_geek_ranking;
	}

	@Override
	protected void initValues(CollectionFilterData filter) {
		if (filter == null) {
			mMinRanking = GeekRankingFilterData.MIN_RANGE;
			mMaxRanking = GeekRankingFilterData.MAX_RANGE;
			mUnranked = false;
		} else {
			GeekRankingFilterData data = (GeekRankingFilterData) filter;
			mMinRanking = data.getMin();
			mMaxRanking = data.getMax();
			mUnranked = data.isUnranked();
		}
	}

	@Override
	protected String intervalText(int number) {
		if (number >= GeekRankingFilterData.MAX_RANGE) {
			return String.valueOf(GeekRankingFilterData.MAX_RANGE) + "+";
		}
		return String.valueOf(number);
	}

	@Override
	protected String intervalText(int min, int max) {
		if (min >= GeekRankingFilterData.MAX_RANGE) {
			return String.valueOf(GeekRankingFilterData.MAX_RANGE) + "+";
		}
		return String.valueOf(min) + " - " + String.valueOf(max);
	}
}
