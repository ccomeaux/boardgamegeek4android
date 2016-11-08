package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggDatabase.GamesArtists;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdArtistsIdProvider extends BaseProvider {
	final GamesIdArtistsProvider provider = new GamesIdArtistsProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		final long artistId = ContentUris.parseId(uri);
		return provider.buildSimpleSelection(uri).whereEquals(GamesArtists.ARTIST_ID, artistId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(provider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return Artists.CONTENT_ITEM_TYPE;
	}
}
