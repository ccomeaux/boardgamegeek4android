package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.PlayPlayers;

public abstract class PlayersSorter extends Sorter {

	public PlayersSorter(@NonNull Context context) {
		super(context);
	}

	@Override
	protected String getDefaultSort() {
		return PlayPlayers.NAME;
	}
}