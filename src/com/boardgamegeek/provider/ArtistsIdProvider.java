package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class ArtistsIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int artistId = Artists.getArtistId(uri);
		return new SelectionBuilder().table(Tables.ARTISTS).whereEquals(Artists.ARTIST_ID, artistId);
	}

	@Override
	protected String getPath() {
		return "artists/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Artists.CONTENT_ITEM_TYPE;
	}
}
