package com.boardgamegeek.tasks;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.UserRequest;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.User;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ResolverUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class AddUsernameToPlayerTask extends AsyncTask<Void, Void, String> {
	private static final String SELECTION = "play_players." + PlayPlayers.NAME + "=? AND (" + PlayPlayers.USER_NAME + "=? OR " + PlayPlayers.USER_NAME + " IS NULL)";

	private final Context context;
	private final String playerName;
	private final String username;
	private final long startTime;
	private boolean wasSuccessful;

	public AddUsernameToPlayerTask(Context context, String playerName, String username) {
		this.context = context.getApplicationContext();
		this.playerName = playerName;
		this.username = username;
		startTime = System.currentTimeMillis();
	}

	@NonNull
	@Override
	protected String doInBackground(Void... params) {
		if (context == null) {
			return "";
		}

		User user = new UserRequest(Adapter.createForXml(), username).execute();
		if (user == null || user.getId() == BggContract.INVALID_ID) {
			return context.getString(R.string.msg_invalid_username, username);
		}

		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		updatePlays(batch);
		updatePlayers(batch);
		updateColors(batch);
		ResolverUtils.applyBatch(context, batch);

		SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
		wasSuccessful = true;
		return context.getString(R.string.msg_player_add_username, username, playerName);
	}

	private void updatePlays(ArrayList<ContentProviderOperation> batch) {
		List<Long> internalIds = ResolverUtils.queryLongs(context.getContentResolver(),
			Plays.buildPlayersByPlayUri(),
			Plays._ID,
			Plays.SYNC_STATUS + "=? AND (" + SELECTION + ")",
			new String[] { String.valueOf(Play.SYNC_STATUS_SYNCED), playerName, "" });
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

	private void updatePlayers(ArrayList<ContentProviderOperation> batch) {
		batch.add(ContentProviderOperation
			.newUpdate(Plays.buildPlayersByPlayUri())
			.withValue(PlayPlayers.USER_NAME, username)
			.withSelection(SELECTION, new String[] { playerName, "" })
			.build());
	}

	private void updateColors(ArrayList<ContentProviderOperation> batch) {
		Cursor cursor = context.getContentResolver().query(PlayerColors.buildPlayerUri(playerName),
			new String[] { PlayerColors.PLAYER_COLOR, PlayerColors.PLAYER_COLOR_SORT_ORDER },
			null, null, null);
		try {
			if (cursor != null) {
				while (cursor.moveToNext()) {
					batch.add(ContentProviderOperation
						.newInsert(PlayerColors.buildUserUri(username))
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
		batch.add(ContentProviderOperation.newDelete(PlayerColors.buildPlayerUri(playerName)).build());
	}

	@Override
	protected void onPostExecute(String result) {
		EventBus.getDefault().post(new Event(result, username, wasSuccessful));
	}

	public class Event {
		private final String message;
		private final String username;
		private final boolean isSuccessful;

		public Event(String message, String username, boolean isSuccessful) {
			this.message = message;
			this.username = username;
			this.isSuccessful = isSuccessful;
		}

		public String getMessage() {
			return message;
		}

		public String getUsername() {
			return username;
		}

		public boolean isSuccessful() {
			return isSuccessful;
		}
	}
}
