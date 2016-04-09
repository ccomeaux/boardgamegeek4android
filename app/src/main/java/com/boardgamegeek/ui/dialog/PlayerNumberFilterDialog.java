package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.PlayerNumberFilterer;

public class PlayerNumberFilterDialog extends SliderFilterDialog {
	@Override
	protected int getCheckboxTextId() {
		return R.string.exact;
	}

	@Override
	protected int getDescriptionId() {
		return R.string.filter_description_player_number;
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
	public int getType(Context context) {
		return new PlayerNumberFilterer(context).getType();
	}

	@Override
	protected CollectionFilterer getPositiveData(Context context, int min, int max, boolean checkbox) {
		return new PlayerNumberFilterer(context, min, max, checkbox);
	}

	@Override
	protected int getTitleId() {
		return R.string.menu_number_of_players;
	}

	@Override
	protected InitialValues initValues(CollectionFilterer filter) {
		int min = PlayerNumberFilterer.MIN_RANGE;
		int max = PlayerNumberFilterer.MAX_RANGE;
		boolean isExact = false;
		if (filter != null) {
			PlayerNumberFilterer data = (PlayerNumberFilterer) filter;
			min = data.getMin();
			max = data.getMax();
			isExact = data.isExact();
		}
		return new InitialValues(min, max, isExact);
	}
}
