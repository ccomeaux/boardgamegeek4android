package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.YearPublishedFilterer;
import com.boardgamegeek.util.StringUtils;

public class YearPublishedFilterDialog extends SliderFilterDialog {
	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = YearPublishedFilterer.MIN_RANGE;
		int max = YearPublishedFilterer.getMAX_RANGE();
		if (filter != null) {
			YearPublishedFilterer data = (YearPublishedFilterer) filter;
			min = data.getMin();
			max = data.getMax();
		}
		return new InitialValues(min, max);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_year_published;
	}

	@Override
	public int getType(Context context) {
		return new YearPublishedFilterer(context).getType();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		final YearPublishedFilterer filterer = new YearPublishedFilterer(context);
		filterer.setMin(min);
		filterer.setMax(max);
		// ignore checkbox
		return filterer;
	}

	@Override
	protected int getCheckboxVisibility() {
		return View.GONE;
	}

	@Override
	protected int getAbsoluteMin() {
		return YearPublishedFilterer.MIN_RANGE;
	}

	@Override
	protected int getAbsoluteMax() {
		return YearPublishedFilterer.getMAX_RANGE();
	}

	@Override
	protected String getPinText(String value) {
		int year = StringUtils.parseInt(value, YearPublishedFilterer.MIN_RANGE);
		if (year == YearPublishedFilterer.MIN_RANGE) {
			return "<" + value;
		}
		if (year == YearPublishedFilterer.getMAX_RANGE()) {
			return value + "+";
		}
		return super.getPinText(value);
	}
}
