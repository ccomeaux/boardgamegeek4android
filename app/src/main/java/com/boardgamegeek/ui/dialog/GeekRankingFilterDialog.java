package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.GeekRankingFilterer;

public class GeekRankingFilterDialog extends SliderFilterDialog {
	@Override
	protected int getAbsoluteMax() {
		return GeekRankingFilterer.upperBound;
	}

	@Override
	protected int getAbsoluteMin() {
		return GeekRankingFilterer.lowerBound;
	}

	@Override
	public int getType(Context context) {
		return new GeekRankingFilterer(context).getType();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		final GeekRankingFilterer filterer = new GeekRankingFilterer(context);
		filterer.setMin(min);
		filterer.setMax(max);
		filterer.setIncludeUnranked(checkbox);
		return filterer;
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_geek_ranking;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = GeekRankingFilterer.lowerBound;
		int max = GeekRankingFilterer.upperBound;
		boolean includeUnranked = false;
		if (filter != null) {
			GeekRankingFilterer data = (GeekRankingFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			includeUnranked = data.getIncludeUnranked();
		}
		return new InitialValues(min, max, includeUnranked);
	}
}
