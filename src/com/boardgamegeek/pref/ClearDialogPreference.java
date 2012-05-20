package com.boardgamegeek.pref;

import android.content.ContentResolver;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.CollectionFilters;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.util.ImageCache;

public class ClearDialogPreference extends AsyncDialogPreference {
	private static final String TAG = "ClearDialogPreference";

	private Context mContext;

	public ClearDialogPreference(Context context) {
		super(context);
		mContext = context;
	}

	public ClearDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	@Override
	protected int getInfoMessageResource() {
		return R.string.pref_sync_clear_info_message;
	}

	@Override
	protected int getConfirmMessageResource() {
		return R.string.pref_sync_clear_confirm_message;
	}

	@Override
	protected Task getTask() {
		return new ClearTask();
	}

	private class ClearTask extends AsyncDialogPreference.Task {

		private ContentResolver mResolver;

		@Override
		protected void onPreExecute() {
			mResolver = mContext.getContentResolver();
		}

		@Override
		protected Void doInBackground(Void... params) {
			BggApplication.getInstance().clearSyncTimestamps();
			BggApplication.getInstance().clearSyncPlaysSettings();

			int count = 0;
			count += mResolver.delete(Games.CONTENT_URI, null, null);
			count += mResolver.delete(Artists.CONTENT_URI, null, null);
			count += mResolver.delete(Designers.CONTENT_URI, null, null);
			count += mResolver.delete(Publishers.CONTENT_URI, null, null);
			count += mResolver.delete(Categories.CONTENT_URI, null, null);
			count += mResolver.delete(Mechanics.CONTENT_URI, null, null);
			count += mResolver.delete(Buddies.CONTENT_URI, null, null);
			count += mResolver.delete(Plays.CONTENT_URI, null, null);
			count += mResolver.delete(CollectionFilters.CONTENT_URI, null, null);
			Log.d(TAG, "Removed " + count + " records");

			if (ImageCache.clear()) {
				Log.d(TAG, "Cleared image cache");
			} else {
				Log.d(TAG, "Unable to clear image cache (expected)");
			}

			return null;
		}
	}
}
