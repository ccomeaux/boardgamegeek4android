package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.GeekRankingFilterData;

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
	protected boolean isChecked() {
		return mUnranked;
	}

	@Override
	protected int getMax() {
		return mMaxRanking;
	}

	@Override
	protected int getAbsoluteMax() {
		return GeekRankingFilterData.MAX_RANGE;
	}

	@Override
	protected int getAbsoluteMin() {
		return GeekRankingFilterData.MIN_RANGE;
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new GeekRankingFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(Context context) {
		return new GeekRankingFilterData(context, mMinRanking, mMaxRanking, mUnranked);
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
