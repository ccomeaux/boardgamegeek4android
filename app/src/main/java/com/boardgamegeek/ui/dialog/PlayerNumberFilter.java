package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.PlayerNumberFilterer;

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
		return PlayerNumberFilterer.MAX_RANGE;
	}

	@Override
	protected int getAbsoluteMin() {
		return PlayerNumberFilterer.MIN_RANGE;
	}

	@Override
	protected CollectionFilterer getNegativeData() {
		return new PlayerNumberFilterer();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context) {
		return new PlayerNumberFilterer(context, mMinPlayers, mMaxPlayers, mExact);
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
	protected void initValues(CollectionFilterer filter) {
		if (filter == null) {
			mMinPlayers = PlayerNumberFilterer.MIN_RANGE;
			mMaxPlayers = PlayerNumberFilterer.MAX_RANGE;
			mExact = false;
		} else {
			PlayerNumberFilterer data = (PlayerNumberFilterer) filter;
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
