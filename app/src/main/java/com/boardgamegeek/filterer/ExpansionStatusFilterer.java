package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class ExpansionStatusFilterer extends CollectionFilterer {
	private int mSelected;

	public ExpansionStatusFilterer() {
		setType(CollectionFilterDataFactory.TYPE_EXPANSION_STATUS);
	}

	public ExpansionStatusFilterer(Context context, String data) {
		mSelected = Integer.valueOf(data);
		init(context);
	}

	public ExpansionStatusFilterer(Context context, int selected) {
		mSelected = selected;
		init(context);
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_EXPANSION_STATUS);
		createDisplayText(context.getResources());
		setSelection(context.getResources());
	}

	private void createDisplayText(Resources resources) {
		String text = "";
		String[] statuses = resources.getStringArray(R.array.expansion_status_filter);
		if (statuses != null && mSelected != 0 && mSelected < statuses.length) {
			text = statuses[mSelected];
		}
		displayText(text);
	}

	public int getSelected() {
		return mSelected;
	}

	@Override
	public String flatten() {
		return String.valueOf(mSelected);
	}

	private void setSelection(Resources resources) {
		String value = "";
		String[] values = resources.getStringArray(R.array.expansion_status_filter_values);
		if (values != null && mSelected != 0 && mSelected < values.length) {
			value = values[mSelected];
		}

		if (!TextUtils.isEmpty(value)) {
			selection(Games.SUBTYPE + "=?");
			selectionArgs(value);
		} else {
			selection("");
			selectionArgs("");
		}
	}
}
