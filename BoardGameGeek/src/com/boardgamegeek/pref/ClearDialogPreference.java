package com.boardgamegeek.pref;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

import com.boardgamegeek.R;
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
import com.boardgamegeek.service.SyncService;

public class ClearDialogPreference extends AsyncDialogPreference {
	private static final String TAG = makeLogTag(ClearDialogPreference.class);

	public ClearDialogPreference(Context context) {
		super(context);
	}

	public ClearDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
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
	protected Task getTask() {
		return new ClearTask();
	}

	private class ClearTask extends AsyncDialogPreference.Task {
		private ContentResolver mResolver;

		@Override
		protected void onPreExecute() {
			mResolver = getContext().getContentResolver();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean success = SyncService.clearCollection(getContext());
			success &= SyncService.clearBuddies(getContext());
			success &= SyncService.clearPlays(getContext());

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
			LOGI(TAG, "Removed " + count + " records");

			count = 0;
			count += mResolver.delete(Thumbnails.CONTENT_URI, null, null);
			count += mResolver.delete(Avatars.CONTENT_URI, null, null);
			LOGI(TAG, "Removed " + count + " files");

			return success;
		}

		private int delete(Uri uri) {
			int count = mResolver.delete(uri, null, null);
			LOGI(TAG, "Removed " + count + " " + uri.getLastPathSegment());
			return count;
		}
	}
}
