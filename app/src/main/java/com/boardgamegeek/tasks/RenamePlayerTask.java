package com.boardgamegeek.tasks;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ResolverUtils;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Change a player name (either a GeekBuddy or named player), updating and syncing all plays.
 */
public class RenamePlayerTask extends AsyncTask<Void, Void, String> {
	private static final String SELECTION_WITHOUT_USERNAME =
		"play_players." + PlayPlayers.NAME + "=? AND (" + PlayPlayers.USER_NAME + "=? OR " + PlayPlayers.USER_NAME + " IS NULL)";
	private static final String SELECTION_WITH_USERNAME =
		"play_players." + PlayPlayers.NAME + "=? AND " + PlayPlayers.USER_NAME + "=?";

	private final Context context;
	private final String username;
	private final String oldName;
	private final String newName;

	public RenamePlayerTask(@NonNull Context context, String username, String oldName, String newName) {
		this.context = context.getApplicationContext();
		this.username = username;
		this.oldName = oldName;
		this.newName = newName;
	}

	@NonNull
	@Override
	protected String doInBackground(Void... params) {
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		String selection = TextUtils.isEmpty(username) ? SELECTION_WITHOUT_USERNAME : SELECTION_WITH_USERNAME;

		updatePlays(batch, selection);
		updatePlayers(batch, selection);
		updateColors(batch);
		ResolverUtils.applyBatch(context, batch);

		SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);

		return context.getString(R.string.msg_play_player_change, oldName, newName);
	}

	private void updatePlays(ArrayList<ContentProviderOperation> batch, String selection) {
		List<Integer> playIds = ResolverUtils.queryInts(context.getContentResolver(),
			Plays.buildPlayersByPlayUri(),
			Plays.PLAY_ID, Plays.SYNC_STATUS + "=? AND (" + selection + ")",
			TextUtils.isEmpty(username) ?
				new String[] { String.valueOf(Play.SYNC_STATUS_SYNCED), oldName, "" } :
				new String[] { String.valueOf(Play.SYNC_STATUS_SYNCED), oldName, username });
		if (playIds.size() > 0) {
			ContentValues values = new ContentValues();
			values.put(Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_UPDATE);
			for (Integer playId : playIds) {
				if (playId != BggContract.INVALID_ID) {
					Uri uri = Plays.buildPlayUri(playId);
					batch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
				}
			}
		}
	}

	private void updatePlayers(ArrayList<ContentProviderOperation> batch, String selection) {
		batch.add(ContentProviderOperation
			.newUpdate(Plays.buildPlayersByPlayUri())
			.withValue(PlayPlayers.NAME, newName)
			.withSelection(selection,
				TextUtils.isEmpty(username) ? new String[] { oldName, "" } : new String[] { oldName, username })
			.build());
	}

	private void updateColors(ArrayList<ContentProviderOperation> batch) {
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
			if (cursor != null) {
				cursor.close();
			}
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
