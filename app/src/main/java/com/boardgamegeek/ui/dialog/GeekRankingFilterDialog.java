package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.GeekRankingFilterer;

public class GeekRankingFilterDialog extends SliderFilterDialog {
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
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		return new GeekRankingFilterer(min, max, checkbox);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_geek_ranking;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = GeekRankingFilterer.MIN_RANGE;
		int max = GeekRankingFilterer.MAX_RANGE;
		boolean includeUnranked = false;
		if (filter != null) {
			GeekRankingFilterer data = (GeekRankingFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			includeUnranked = data.includeUnranked();
		}
		return new InitialValues(min, max, includeUnranked);
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
