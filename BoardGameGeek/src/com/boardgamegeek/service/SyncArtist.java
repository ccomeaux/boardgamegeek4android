package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.provider.BggContract.Artists;

public class SyncArtist extends UpdateTask {
	private static final String TAG = makeLogTag(SyncArtist.class);
	private int mArtistId;

	public SyncArtist(int artistId) {
		mArtistId = artistId;
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.get().create(BggService.class);
		Person person = service.person(BggService.PERSON_TYPE_ARTIST, mArtistId);
		Uri uri = Artists.buildArtistUri(mArtistId);
		context.getContentResolver().update(uri, toValues(person), null, null);
		LOGI(TAG, "Synced Artist " + mArtistId);
	}

	private static ContentValues toValues(Person artist) {
		ContentValues values = new ContentValues();
		values.put(Artists.ARTIST_NAME, artist.name);
		values.put(Artists.ARTIST_DESCRIPTION, artist.description);
		values.put(Artists.UPDATED, System.currentTimeMillis());
		return values;
	}
}
