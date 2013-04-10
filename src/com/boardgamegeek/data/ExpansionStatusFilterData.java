package com.boardgamegeek.data;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;

public class ExpansionStatusFilterData extends CollectionFilterData {
	private int mSelected;

	public ExpansionStatusFilterData() {
		setType(CollectionFilterDataFactory.TYPE_EXPANSION_STATUS);
	}

	public ExpansionStatusFilterData(Context context, String data) {
		mSelected = Integer.valueOf(data);
		init(context);
	}

	public ExpansionStatusFilterData(Context context, int selected) {
		mSelected = selected;
		init(context);
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_EXPANSION_STATUS);
		createDisplayText(context.getResources());
		createPath();
	}

	private void createDisplayText(Resources resources) {
		String text = "";
		String[] statuses = resources.getStringArray(R.array.expansion_status_filter);
		if (mSelected != 0 && mSelected < statuses.length) {
			text = statuses[mSelected];
		}
		displayText(text);
	}

	private void createPath() {
		String path = "";
		switch (mSelected) {
			case 1:
				path = BggContract.PATH_NOEXPANSIONS;
				break;
			case 2:
				path = BggContract.PATH_EXPANSIONS;
				break;
		}
		path(path);
	}

	public int getSelected() {
		return mSelected;
	}

	@Override
	public String flatten() {
		return String.valueOf(mSelected);
	}
}
