package com.boardgamegeek.tasks.sync;


import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.tasks.sync.SyncArtistTask.Event;

import retrofit2.Call;
import timber.log.Timber;

public class SyncArtistTask extends SyncTask<Person, Event> {
	private final int artistId;

	public SyncArtistTask(Context context, int artistId) {
		super(context);
		this.artistId = artistId;
	}

	@Override
	@StringRes
	protected int getTypeDescriptionResId() {
		return R.string.title_artist;
	}

	@Override
	protected Call<Person> createCall() {
		return bggService.person(BggService.PERSON_TYPE_ARTIST, artistId);
	}

	@Override
	protected boolean isRequestParamsValid() {
		return super.isRequestParamsValid() && artistId != BggContract.INVALID_ID;
	}

	@Override
	protected void persistResponse(Person artist) {
		Uri uri = Artists.buildArtistUri(artistId);
		context.getContentResolver().update(uri, toValues(artist), null, null);
		Timber.i("Synced artist '%s'", artistId);
	}

	@NonNull
	@Override
	protected Event createEvent(String errorMessage) {
		return new Event(errorMessage, artistId);
	}

	@NonNull
	private static ContentValues toValues(@NonNull Person artist) {
		ContentValues values = new ContentValues();
		values.put(Artists.ARTIST_NAME, artist.name);
		values.put(Artists.ARTIST_DESCRIPTION, artist.description);
		values.put(Artists.UPDATED, System.currentTimeMillis());
		return values;
	}

	public class Event extends SyncTask.Event {
		private final int artistId;

		public Event(String errorMessage, int artistId) {
			super(errorMessage);
			this.artistId = artistId;
		}

		public int getArtistId() {
			return artistId;
		}
	}
}
