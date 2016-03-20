package com.boardgamegeek.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Artists;

import timber.log.Timber;

public class SyncArtist extends UpdateTask {
	private final int artistId;

	public SyncArtist(int artistId) {
		this.artistId = artistId;
	}

	@NonNull
	@Override
	public String getDescription(Context context) {
		if (isValid()) {
			return context.getString(R.string.sync_msg_artist_valid, artistId);
		}
		return context.getString(R.string.sync_msg_artist_invalid);
	}

	@Override
	public boolean isValid() {
		return artistId != BggContract.INVALID_ID;
	}

	@Override
	public void execute(@NonNull Context context) {
		BggService service = Adapter.create();
		Person person = service.person(BggService.PERSON_TYPE_ARTIST, artistId);
		Uri uri = Artists.buildArtistUri(artistId);
		context.getContentResolver().update(uri, toValues(person), null, null);
		Timber.i("Synced Artist " + artistId);
	}

	@NonNull
	private static ContentValues toValues(@NonNull Person artist) {
		ContentValues values = new ContentValues();
		values.put(Artists.ARTIST_NAME, artist.name);
		values.put(Artists.ARTIST_DESCRIPTION, artist.description);
		values.put(Artists.UPDATED, System.currentTimeMillis());
		return values;
	}
}
