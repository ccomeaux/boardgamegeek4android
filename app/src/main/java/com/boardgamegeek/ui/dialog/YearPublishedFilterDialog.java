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
		int max = YearPublishedFilterer.MAX_RANGE;
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
		return new YearPublishedFilterer(context, min, max);
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
		return YearPublishedFilterer.MAX_RANGE;
	}

	@Override
	protected String getPinText(String value) {
		int year = StringUtils.parseInt(value, YearPublishedFilterer.MIN_RANGE);
		if (year == YearPublishedFilterer.MIN_RANGE) {
			return "<" + value;
		}
		if (year == YearPublishedFilterer.MAX_RANGE) {
			return value + "+";
		}
		return super.getPinText(value);
	}

	@Override
	protected int getPinValue(String text) {
		if (text.startsWith("<")) {
			return YearPublishedFilterer.MIN_RANGE;
		}
		if (text.endsWith("+")) {
			return YearPublishedFilterer.MAX_RANGE;
		}
		return super.getPinValue(text);
	}

	@Override
	protected String intervalText(int number) {
		return String.valueOf(number);
	}

	@Override
	protected String intervalText(int min, int max) {
		String text = String.valueOf(min);
		if (min == YearPublishedFilterer.MIN_RANGE) {
			text = "<" + text;
		}
		text += " - " + String.valueOf(max);
		if (max == YearPublishedFilterer.MAX_RANGE) {
			text += "+";
		}
		return text;
	}
}
