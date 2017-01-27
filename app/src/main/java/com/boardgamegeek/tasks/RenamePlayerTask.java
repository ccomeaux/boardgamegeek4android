package com.boardgamegeek.tasks;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.SelectionBuilder;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Change a player name (either a GeekBuddy or named player), updating and syncing all plays.
 */
public class RenamePlayerTask extends AsyncTask<Void, Void, String> {
	private static final String SELECTION = "play_players." + PlayPlayers.NAME + "=? AND (" + PlayPlayers.USER_NAME + "=? OR " + PlayPlayers.USER_NAME + " IS NULL)";

	private final Context context;
	private final String oldName;
	private final String newName;
	private final long startTime;
	private ArrayList<ContentProviderOperation> batch;

	public RenamePlayerTask(Context context, String oldName, String newName) {
		this.context = (context == null ? null : context.getApplicationContext());
		this.oldName = oldName;
		this.newName = newName;
		startTime = System.currentTimeMillis();
		batch = new ArrayList<>();
	}

	@NonNull
	@Override
	protected String doInBackground(Void... params) {
		if (context == null) return "";

		batch.clear();
		updatePlays();
		updatePlayers();
		updateColors();
		ResolverUtils.applyBatch(context, batch);

		SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);

		return context.getString(R.string.msg_play_player_change, oldName, newName);
	}

	private void updatePlays() {
		List<Long> internalIds = ResolverUtils.queryLongs(context.getContentResolver(),
			Plays.buildPlayersByPlayUri(),
			Plays._ID,
			"(" + SELECTION + ") AND " +
				SelectionBuilder.whereZeroOrNull(Plays.UPDATE_TIMESTAMP) + " AND " +
				SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP) + " AND " +
				SelectionBuilder.whereZeroOrNull(Plays.DIRTY_TIMESTAMP),
			new String[] { oldName, "" });
		if (internalIds.size() > 0) {
			for (Long internalId : internalIds) {
				if (internalId != BggContract.INVALID_ID) {
					batch.add(ContentProviderOperation
						.newUpdate(Plays.buildPlayUri(internalId))
						.withValue(Plays.UPDATE_TIMESTAMP, startTime)
						.build());
				}
			}
		}
	}

	private void updatePlayers() {
		batch.add(ContentProviderOperation
			.newUpdate(Plays.buildPlayersByPlayUri())
			.withValue(PlayPlayers.NAME, newName)
			.withSelection(SELECTION, new String[] { oldName, "" })
			.build());
	}

	private void updateColors() {
		Cursor cursor = context.getContentResolver().query(PlayerColors.buildPlayerUri(oldName),
			new String[] { PlayerColors.PLAYER_COLOR, PlayerColors.PLAYER_COLOR_SORT_ORDER },
			null, null, null);
		try {
			if (cursor != null) {
				while (cursor.moveToNext()) {
					batch.add(ContentProviderOperation
						.newInsert(PlayerColors.buildPlayerUri(newName))
						.withValue(PlayerColors.PLAYER_COLOR, cursor.getString(0))
						.withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, cursor.getInt(1))
						.build());
				}
				Timber.i("Updated %,d colors", cursor.getCount());
			} else {
				Timber.i("No colors to update");
			}
		} finally {
			if (cursor != null) cursor.close();
		}
		batch.add(ContentProviderOperation.newDelete(PlayerColors.buildPlayerUri(oldName)).build());
	}

	@Override
	protected void onPostExecute(String result) {
		EventBus.getDefault().post(new Event(newName, result));
	}

	public class Event {
		private final String playerName;
		private final String message;

		public Event(String playerName, String message) {
			this.playerName = playerName;
			this.message = message;
		}

		public String getPlayerName() {
			return playerName;
		}

		public String getMessage() {
			return message;
		}
	}
}
