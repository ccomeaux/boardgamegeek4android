package com.boardgamegeek.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BoardGameGeekService;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Artists;

import java.io.IOException;

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
		BoardGameGeekService service = Adapter.create2();
		try {
			Person person = service.person(BoardGameGeekService.PERSON_TYPE_ARTIST, artistId).execute().body();
			Uri uri = Artists.buildArtistUri(artistId);
			context.getContentResolver().update(uri, toValues(person), null, null);
			Timber.i("Synced Artist " + artistId);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
