package com.boardgamegeek.tasks;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

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
	private final Context context;
	private final String oldLocationName;
	private final String newLocationName;

	@DebugLog
	public RenameLocationTask(@NonNull Context context, String oldLocation, String newLocation) {
		this.context = context.getApplicationContext();
		oldLocationName = oldLocation;
		newLocationName = newLocation;
	}

	@DebugLog
	@Override
	protected String doInBackground(String... params) {
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();

		ContentValues values = new ContentValues();
		values.put(BggContract.Plays.LOCATION, newLocationName);
		ContentProviderOperation.Builder cpo = ContentProviderOperation
			.newUpdate(BggContract.Plays.CONTENT_URI)
			.withValues(values)
			.withSelection(
				BggContract.Plays.LOCATION + "=? AND (" + BggContract.Plays.SYNC_STATUS + "=? OR " + BggContract.Plays.SYNC_STATUS + "=?)",
				new String[] { oldLocationName, String.valueOf(Play.SYNC_STATUS_PENDING_UPDATE),
					String.valueOf(Play.SYNC_STATUS_IN_PROGRESS) });
		batch.add(cpo.build());

		values.put(BggContract.Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_UPDATE);
		cpo = ContentProviderOperation
			.newUpdate(BggContract.Plays.CONTENT_URI)
			.withValues(values)
			.withSelection(BggContract.Plays.LOCATION + "=? AND " + BggContract.Plays.SYNC_STATUS + "=?",
				new String[] { oldLocationName, String.valueOf(Play.SYNC_STATUS_SYNCED) });
		batch.add(cpo.build());

		ContentProviderResult[] results = ResolverUtils.applyBatch(context, batch);

		String result;
		if (results.length > 0) {
			int count = 0;
			for (ContentProviderResult r : results) {
				count += r.count;
			}
			result = context.getResources().getQuantityString(R.plurals.msg_play_location_change, count,
				count, oldLocationName, newLocationName);
			SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
		} else {
			result = context.getString(R.string.msg_play_location_change, oldLocationName, newLocationName);
		}

		return result;
	}

	@DebugLog
	@Override
	protected void onPostExecute(String result) {
		EventBus.getDefault().post(new Event(newLocationName, result));
	}

	public class Event {
		private final String locationName;
		private final String message;

		public Event(String locationName, String message) {
			this.locationName = locationName;
			this.message = message;
		}

		public String getLocationName() {
			return locationName;
		}

		public String getMessage() {
			return message;
		}
	}
}
