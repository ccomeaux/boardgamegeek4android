package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.annotation.Nullable;
import com.boardgamegeek.pref.SyncPrefUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.pref.SyncPrefs;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Avatars;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggContract.Thumbnails;

import timber.log.Timber;

/**
 * Deletes all data in the local database.
 */
public class ClearDatabaseTask extends ToastingAsyncTask {
	@Nullable private ContentResolver resolver;

	public ClearDatabaseTask(Context context) {
		super(context);
	}

	@Override
	protected int getSuccessMessageResource() {
		return R.string.pref_sync_clear_success;
	}

	@Override
	protected int getFailureMessageResource() {
		return R.string.pref_sync_clear_failure;
	}

	@Override
	protected void onPreExecute() {
		resolver = getContext() == null ? null : getContext().getContentResolver();
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (getContext() == null) return false;

		SharedPreferences syncPrefs = SyncPrefs.getPrefs(getContext());
		SyncPrefUtils.clearCollection(syncPrefs);
		SyncPrefUtils.clearBuddyListTimestamps(syncPrefs);
		SyncPrefUtils.clearPlaysTimestamps(syncPrefs);

		int count = 0;
		count += delete(Games.CONTENT_URI);
		count += delete(Artists.CONTENT_URI);
		count += delete(Designers.CONTENT_URI);
		count += delete(Publishers.CONTENT_URI);
		count += delete(Categories.CONTENT_URI);
		count += delete(Mechanics.CONTENT_URI);
		count += delete(Buddies.CONTENT_URI);
		count += delete(Plays.CONTENT_URI);
		count += delete(CollectionViews.CONTENT_URI);
		Timber.i("Removed %d records", count);

		if (resolver != null) {
			count = 0;
			count += resolver.delete(Thumbnails.CONTENT_URI, null, null);
			count += resolver.delete(Avatars.CONTENT_URI, null, null);
			Timber.i("Removed %d files", count);
		}

		return true;
	}

	private int delete(Uri uri) {
		if (resolver == null) return 0;
		int count = resolver.delete(uri, null, null);
		Timber.i("Removed %1$d %2$s", count, uri.getLastPathSegment());
		return count;
	}
}
