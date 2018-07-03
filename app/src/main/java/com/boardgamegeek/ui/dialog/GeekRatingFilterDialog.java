package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.GeekRatingFilterer;
import com.boardgamegeek.util.StringUtils;

import java.text.DecimalFormat;

public class GeekRatingFilterDialog extends SliderFilterDialog {
	private static final int FACTOR = 10;
	private static final DecimalFormat FORMAT = new DecimalFormat("#.0");

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
		return (int) (GeekRatingFilterer.upperBound * FACTOR);
	}

	@Override
	protected int getAbsoluteMin() {
		return (int) (GeekRatingFilterer.lowerBound * FACTOR);
	}

	@Override
	public int getType(Context context) {
		return new GeekRatingFilterer(context).getType();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		final GeekRatingFilterer filterer = new GeekRatingFilterer(context);
		filterer.setMin((double) (min) / FACTOR);
		filterer.setMax((double) (max) / FACTOR);
		filterer.setIncludeUnrated(checkbox);
		return filterer;
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_geek_rating;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		double min = GeekRatingFilterer.lowerBound;
		double max = GeekRatingFilterer.upperBound;
		boolean unrated = true;
		if (filter != null) {
			GeekRatingFilterer data = (GeekRatingFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			unrated = data.getIncludeUnrated();
		}
		return new InitialValues((int) (min * FACTOR), (int) (max * FACTOR), unrated);
	}

	@Override
	protected String getPinText(String value) {
		return FORMAT.format((double) StringUtils.parseInt(value, 0) / FACTOR);
	}
}