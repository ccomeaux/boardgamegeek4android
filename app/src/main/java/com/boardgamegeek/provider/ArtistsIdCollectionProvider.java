package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class ArtistsIdCollectionProvider extends BaseProvider {
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int artistId = Artists.getArtistId(uri);
		return new SelectionBuilder().table(Tables.ARTIST_JOIN_GAMES_JOIN_COLLECTION).whereEquals(Artists.ARTIST_ID, artistId);
	}

	@Override
	protected String getPath() {
		return "artists/#/collection";
	}
}
