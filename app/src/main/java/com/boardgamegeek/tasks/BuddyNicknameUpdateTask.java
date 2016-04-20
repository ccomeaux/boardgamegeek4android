package com.boardgamegeek.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ResolverUtils;

import org.greenrobot.eventbus.EventBus;

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

	public BuddyNicknameUpdateTask(Context context, String username, String nickname, boolean shouldUpdatePlays) {
		this.context = (context == null ? null : context.getApplicationContext());
		this.username = username;
		this.nickname = nickname;
		this.shouldUpdatePlays = shouldUpdatePlays;
	}

	@Override
	protected String doInBackground(Void... params) {
		if (context == null) {
			return "";
		}

		String result;
		updateNickname(Buddies.buildBuddyUri(username));
		if (shouldUpdatePlays) {
			if (TextUtils.isEmpty(nickname)) {
				result = context.getString(R.string.msg_missing_nickname);
			} else {
				int count = updatePlays();
				if (count > 0) {
					updatePlayers();
					SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
				}
				result = context.getResources().getQuantityString(R.plurals.msg_updated_plays_buddy_nickname, count,
					count, username, nickname);
			}
		} else {
			result = context.getString(R.string.msg_updated_nickname, nickname);
		}
		return result;
	}

	@Override
	protected void onPostExecute(String result) {
		EventBus.getDefault().post(new Event(result));
	}

	private void updateNickname(@NonNull final Uri uri) {
		ContentValues values = new ContentValues(1);
		values.put(Buddies.PLAY_NICKNAME, nickname);
		context.getContentResolver().update(uri, values, null, null);
	}

	private int updatePlays() {
		ContentValues values = new ContentValues(1);
		values.put(BggContract.Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_UPDATE);
		List<Integer> playIds = ResolverUtils.queryInts(context.getContentResolver(), Plays.buildPlayersByPlayUri(), Plays.PLAY_ID, SELECTION, new String[] { username, nickname });
		for (Integer playId : playIds) {
			if (playId != BggContract.INVALID_ID) {
				context.getContentResolver().update(BggContract.Plays.buildPlayUri(playId), values, null, null);
			}
		}
		return playIds.size();
	}

	private void updatePlayers() {
		ContentValues values = new ContentValues(1);
		values.put(BggContract.PlayPlayers.NAME, nickname);
		context.getContentResolver().update(BggContract.Plays.buildPlayersByPlayUri(), values, SELECTION, new String[] { username, nickname });
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

