package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysIdItemsIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int playId = Plays.getPlayId(uri);
		int objectId = PlayItems.getPlayItemId(uri);
		return new SelectionBuilder().table(Tables.PLAY_ITEMS).whereEquals(PlayItems.PLAY_ID, playId)
			.whereEquals(PlayItems.OBJECT_ID, objectId);
	}

	@Override
	protected String getPath() {
		return "plays/#/items/#";
	}

	@Override
	protected String getType(Uri uri) {
		return PlayItems.CONTENT_ITEM_TYPE;
	}
}
