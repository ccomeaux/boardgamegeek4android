package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.PlayerNumberFilterData;

public class PlayerNumberFilter extends SliderFilter {
	private int mMinPlayers;
	private int mMaxPlayers;
	private boolean mExact;

	@Override
	protected void captureForm(int min, int max, boolean isChecked) {
		mMinPlayers = min;
		mMaxPlayers = max;
		mExact = isChecked;
	}

	@Override
	protected boolean isChecked() {
		return mExact;
	}

	@Override
	protected int getCheckboxTextId() {
		return R.string.exact;
	}

	@Override
	protected int getDescriptionId() {
		return R.string.filter_description_player_number;
	}

	@Override
	protected int getMax() {
		return mMaxPlayers;
	}

	@Override
	protected int getAbsoluteMax() {
		return PlayerNumberFilterData.MAX_RANGE;
	}

	@Override
	protected int getAbsoluteMin() {
		return PlayerNumberFilterData.MIN_RANGE;
	}

	@Override
	protected CollectionFilterData getNegativeData() {
		return new PlayerNumberFilterData();
	}

	@Override
	protected CollectionFilterData getPositiveData(Context context) {
		return new PlayerNumberFilterData(context, mMinPlayers, mMaxPlayers, mExact);
	}

	@Override
	protected int getMin() {
		return mMinPlayers;
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_number_of_players;
	}

	@Override
	protected void initValues(CollectionFilterData filter) {
		if (filter == null) {
			mMinPlayers = PlayerNumberFilterData.MIN_RANGE;
			mMaxPlayers = PlayerNumberFilterData.MAX_RANGE;
			mExact = false;
		} else {
			PlayerNumberFilterData data = (PlayerNumberFilterData) filter;
			mMinPlayers = data.getMin();
			mMaxPlayers = data.getMax();
			mExact = data.isExact();
		}
	}

	@Override
	protected String intervalText(int number) {
		return String.valueOf(number);
	}

	@Override
	protected String intervalText(int min, int max) {
		return String.valueOf(min) + " - " + String.valueOf(max);
	}
}
