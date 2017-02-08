package com.boardgamegeek.tasks;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.SelectionBuilder;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import hugo.weaving.DebugLog;

/**
 * Renames a location in all plays, then triggers an update.
 */
public class RenameLocationTask extends AsyncTask<String, Void, String> {
	private final Context context;
	private final String oldLocationName;
	private final String newLocationName;
	private final long startTime;

	@DebugLog
	public RenameLocationTask(Context context, String oldLocation, String newLocation) {
		this.context = (context == null ? null : context.getApplicationContext());
		oldLocationName = oldLocation;
		newLocationName = newLocation;
		startTime = System.currentTimeMillis();
	}

	@DebugLog
	@Override
	protected String doInBackground(String... params) {
		if (context == null) {
			return "";
		}

		ArrayList<ContentProviderOperation> batch = new ArrayList<>();

		ContentValues values = new ContentValues();
		values.put(Plays.LOCATION, newLocationName);
		ContentProviderOperation.Builder cpo = ContentProviderOperation
			.newUpdate(Plays.CONTENT_URI)
			.withValues(values)
			.withSelection(
				Plays.LOCATION + "=? AND (" + Plays.UPDATE_TIMESTAMP + ">0 OR " + Plays.DIRTY_TIMESTAMP + ">0)",
				new String[] { oldLocationName });
		batch.add(cpo.build());

		values.put(Plays.UPDATE_TIMESTAMP, startTime);
		cpo = ContentProviderOperation
			.newUpdate(Plays.CONTENT_URI)
			.withValues(values)
			.withSelection(
				Plays.LOCATION + "=? AND " +
					SelectionBuilder.whereZeroOrNull(Plays.UPDATE_TIMESTAMP) + " AND " +
					SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP) + " AND " +
					SelectionBuilder.whereZeroOrNull(Plays.DIRTY_TIMESTAMP),
				new String[] { oldLocationName });
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
