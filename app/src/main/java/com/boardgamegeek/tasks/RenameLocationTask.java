package com.boardgamegeek.tasks;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ResolverUtils;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

/**
 * Renames a location in all plays, then triggers an update.
 */
public class RenameLocationTask extends AsyncTask<String, Void, String> {
	private final Context mContext;
	private final String mOldLocation;
	private final String mNewLocation;

	@DebugLog
	public RenameLocationTask(Context context, String oldLocation, String newLocation) {
		mContext = context.getApplicationContext();
		mOldLocation = oldLocation;
		mNewLocation = newLocation;
	}

	@DebugLog
	@Override
	protected String doInBackground(String... params) {
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();

		ContentValues values = new ContentValues();
		values.put(BggContract.Plays.LOCATION, mNewLocation);
		ContentProviderOperation.Builder cpo = ContentProviderOperation
			.newUpdate(BggContract.Plays.CONTENT_URI)
			.withValues(values)
			.withSelection(
				BggContract.Plays.LOCATION + "=? AND (" + BggContract.Plays.SYNC_STATUS + "=? OR " + BggContract.Plays.SYNC_STATUS + "=?)",
				new String[] { mOldLocation, String.valueOf(Play.SYNC_STATUS_PENDING_UPDATE),
					String.valueOf(Play.SYNC_STATUS_IN_PROGRESS) });
		batch.add(cpo.build());

		values.put(BggContract.Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_UPDATE);
		cpo = ContentProviderOperation
			.newUpdate(BggContract.Plays.CONTENT_URI)
			.withValues(values)
			.withSelection(BggContract.Plays.LOCATION + "=? AND " + BggContract.Plays.SYNC_STATUS + "=?",
				new String[] { mOldLocation, String.valueOf(Play.SYNC_STATUS_SYNCED) });
		batch.add(cpo.build());

		ContentProviderResult[] results = ResolverUtils.applyBatch(mContext, batch);

		String result;
		if (results.length > 0) {
			result = mContext.getString(R.string.msg_play_location_change, results.length, mOldLocation, mNewLocation);
			SyncService.sync(mContext, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
		} else {
			result = mContext.getString(R.string.msg_play_location_change);
		}

		return result;
	}

	@DebugLog
	@Override
	protected void onPostExecute(String result) {
		if (!TextUtils.isEmpty(result)) {
			Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
		}
		EventBus.getDefault().post(new Event(mNewLocation));
	}

	public class Event {
		public final String locationName;

		public Event(String locationName) {
			this.locationName = locationName;
		}
	}
}
