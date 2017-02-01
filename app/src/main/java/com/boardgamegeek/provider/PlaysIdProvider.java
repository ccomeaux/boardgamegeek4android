package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long internalId = Plays.getInternalId(uri);
		return new SelectionBuilder()
			.table(Tables.PLAYS)
			.whereEquals(Plays._ID, String.valueOf(internalId));
	}

	@Override
	protected String getPath() {
		return "plays/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Plays.CONTENT_ITEM_TYPE;
	}
}
