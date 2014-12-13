package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayLocations;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysLocationsProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.PLAYS).groupBy(Plays.LOCATION);
	}

	@Override
	protected String getDefaultSortOrder() {
		return PlayLocations.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "plays/locations";
	}
}
