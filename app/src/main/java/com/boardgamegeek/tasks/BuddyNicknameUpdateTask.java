package com.boardgamegeek.tasks;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.SelectionBuilder;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Updates a buddy wih a new nickname, and optionally updates all plays with this new nick ame.
 */
public class BuddyNicknameUpdateTask extends AsyncTask<Void, Void, String> {
	private static final String SELECTION = PlayPlayers.USER_NAME + "=? AND play_players." + PlayPlayers.NAME + "!=?";
	private final Context context;
	private final String username;
	private final String nickname;
	private final boolean shouldUpdatePlays;
	private ArrayList<ContentProviderOperation> batch;
	private final long startTime;

	public BuddyNicknameUpdateTask(Context context, String username, String nickname, boolean shouldUpdatePlays) {
		this.context = (context == null ? null : context.getApplicationContext());
		this.username = username;
		this.nickname = nickname;
		this.shouldUpdatePlays = shouldUpdatePlays;
		this.batch = new ArrayList<>();
		this.startTime = System.currentTimeMillis();
	}

	@Override
	protected String doInBackground(Void... params) {
		if (context == null) {
			return "";
		}

		String result;
		batch.clear();
		updateNickname();
		if (shouldUpdatePlays) {
			if (TextUtils.isEmpty(nickname)) {
				result = context.getString(R.string.msg_missing_nickname);
			} else {
				int count = updatePlays();
				if (count > 0) {
					updatePlayers();
					SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
				}
				result = context.getResources().getQuantityString(R.plurals.msg_updated_plays_buddy_nickname, count, count, username, nickname);
			}
		} else {
			result = context.getString(R.string.msg_updated_nickname, nickname);
		}
		ResolverUtils.applyBatch(context, batch);
		return result;
	}

	@Override
	protected void onPostExecute(String result) {
		EventBus.getDefault().post(new Event(result));
	}

	private void updateNickname() {
		batch.add(ContentProviderOperation
			.newUpdate(Buddies.buildBuddyUri(username))
			.withValue(Buddies.PLAY_NICKNAME, nickname)
			.build());
	}

	private int updatePlays() {
		final ContentResolver resolver = context.getContentResolver();
		List<Long> internalIds = ResolverUtils.queryLongs(resolver,
			Plays.buildPlayersByPlayUri(),
			Plays._ID,
			"(" + SELECTION + ") AND " +
				SelectionBuilder.whereZeroOrNull(Plays.UPDATE_TIMESTAMP) + " AND " +
				SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP) + " AND " +
				SelectionBuilder.whereZeroOrNull(Plays.DIRTY_TIMESTAMP),
			new String[] { username, nickname });
		for (Long internalId : internalIds) {
			if (internalId != BggContract.INVALID_ID) {
				batch.add(ContentProviderOperation
					.newUpdate(Plays.buildPlayUri(internalId))
					.withValue(Plays.UPDATE_TIMESTAMP, startTime)
					.build());
			}
		}
		return internalIds.size();
	}

	private void updatePlayers() {
		batch.add(ContentProviderOperation
			.newUpdate(Plays.buildPlayersByPlayUri())
			.withSelection(SELECTION, new String[] { username, nickname })
			.withValue(PlayPlayers.NAME, nickname)
			.build());
	}

	public class Event {
		private final String message;

		public Event(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}
	}
}

