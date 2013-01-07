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
			SyncService.clearCollection(mContext);
			SyncService.clearBuddies(mContext);
			SyncService.clearPlays(mContext);

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

			return null;
		}

		private int delete(Uri uri) {
			int count = mResolver.delete(uri, null, null);
			LOGI(TAG, "Removed " + count + " " + uri.getLastPathSegment());
			return count;
		}
	}
}
