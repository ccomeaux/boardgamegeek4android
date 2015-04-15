package com.boardgamegeek.sorter;

import android.content.Context;

import com.boardgamegeek.provider.BggContract.PlayPlayers;

public abstract class PlayersSorter extends Sorter {

	public PlayersSorter(Context context) {
		super(context);
	}

	@Override
	protected String getDefaultSort() {
		return PlayPlayers.NAME;
	}
}