package com.boardgamegeek.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ResolverUtils;

import java.util.List;

/**
 * Updates a buddy wih a new nick name, and optionally updates all plays with this new nick name.
 */
public class BuddyNicknameUpdateTask extends AsyncTask<Void, Void, String> {
	private static final String SELECTION = PlayPlayers.USER_NAME + "=? AND play_players." + PlayPlayers.NAME + "!=?";
	private final Context context;
	private final String username;
	private final String nickName;
	private final boolean shouldUpdatePlays;

	public BuddyNicknameUpdateTask(@NonNull Context context, String username, String nickName, boolean shouldUpdatePlays) {
		this.context = context.getApplicationContext();
		this.username = username;
		this.nickName = nickName;
		this.shouldUpdatePlays = shouldUpdatePlays;
	}

	@Override
	protected String doInBackground(Void... params) {
		String result;
		updateNickname(Buddies.buildBuddyUri(username));
		if (shouldUpdatePlays) {
			if (TextUtils.isEmpty(nickName)) {
				result = context.getString(R.string.msg_missing_nickname);
			} else {
				int count = updatePlays();
				if (count > 0) {
					updatePlayers();
					SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
				}
				result = context.getResources().getQuantityString(R.plurals.msg_updated_plays, count, count);
			}
		} else {
			result = context.getResources().getString(R.string.msg_updated_nickname);
		}
		return result;
	}

	@Override
	protected void onPostExecute(String result) {
		if (!TextUtils.isEmpty(result)) {
			Toast.makeText(context, result, Toast.LENGTH_LONG).show();
		}
	}

	private void updateNickname(@NonNull final Uri uri) {
		ContentValues values = new ContentValues(1);
		values.put(Buddies.PLAY_NICKNAME, nickName);
		context.getContentResolver().update(uri, values, null, null);
	}

	private int updatePlays() {
		ContentValues values = new ContentValues(1);
		values.put(BggContract.Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_UPDATE);
		List<Integer> playIds = ResolverUtils.queryInts(context.getContentResolver(), Plays.buildPlayersByPlayUri(), Plays.PLAY_ID, SELECTION, new String[] { username, nickName });
		for (Integer playId : playIds) {
			if (playId != BggContract.INVALID_ID) {
				context.getContentResolver().update(BggContract.Plays.buildPlayUri(playId), values, null, null);
			}
		}
		return playIds.size();
	}

	private void updatePlayers() {
		ContentValues values = new ContentValues(1);
		values.put(BggContract.PlayPlayers.NAME, nickName);
		context.getContentResolver().update(BggContract.Plays.buildPlayersByPlayUri(), values, SELECTION, new String[] { username, nickName });
	}
}

