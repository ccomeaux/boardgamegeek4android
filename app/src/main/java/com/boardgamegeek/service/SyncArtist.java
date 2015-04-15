package com.boardgamegeek.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Artists;

import timber.log.Timber;

public class SyncArtist extends UpdateTask {
	private int mArtistId;

	public SyncArtist(int artistId) {
		mArtistId = artistId;
	}

	@Override
	public String getDescription() {
		if (mArtistId == BggContract.INVALID_ID){
			return "update an unknown artist";
		}
		return "update artist " + mArtistId;
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.create();
		Person person = service.person(BggService.PERSON_TYPE_ARTIST, mArtistId);
		Uri uri = Artists.buildArtistUri(mArtistId);
		context.getContentResolver().update(uri, toValues(person), null, null);
		Timber.i("Synced Artist " + mArtistId);
	}

	private static ContentValues toValues(Person artist) {
		ContentValues values = new ContentValues();
		values.put(Artists.ARTIST_NAME, artist.name);
		values.put(Artists.ARTIST_DESCRIPTION, artist.description);
		values.put(Artists.UPDATED, System.currentTimeMillis());
		return values;
	}
}
