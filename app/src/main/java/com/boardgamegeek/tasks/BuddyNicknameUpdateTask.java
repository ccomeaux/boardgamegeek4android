package com.boardgamegeek.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
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
	private final Context mContext;
	private final String mUsername;
	private final String mNickName;
	private final boolean mUpdatePlays;

	public BuddyNicknameUpdateTask(Context context, String username, String nickName, boolean updatePlays) {
		mContext = context.getApplicationContext();
		mUsername = username;
		mNickName = nickName;
		mUpdatePlays = updatePlays;
	}

	@Override
	protected String doInBackground(Void... params) {
		String result;
		updateNickname(Buddies.buildBuddyUri(mUsername));
		if (mUpdatePlays) {
			if (TextUtils.isEmpty(mNickName)) {
				result = mContext.getString(R.string.msg_missing_nickname);
			} else {
				int count = updatePlays();
				if (count > 0) {
					updatePlayers();
					SyncService.sync(mContext, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
				}
				result = mContext.getResources().getQuantityString(R.plurals.msg_updated_plays, count, count);
			}
		} else {
			result = mContext.getResources().getString(R.string.msg_updated_nickname);
		}
		return result;
	}

	@Override
	protected void onPostExecute(String result) {
		if (!TextUtils.isEmpty(result)) {
			Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
		}
	}

	private void updateNickname(final Uri uri) {
		ContentValues values = new ContentValues(1);
		values.put(Buddies.PLAY_NICKNAME, mNickName);
		mContext.getContentResolver().update(uri, values, null, null);
	}

	private int updatePlays() {
		ContentValues values = new ContentValues(1);
		values.put(BggContract.Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_UPDATE);
		List<Integer> playIds = ResolverUtils.queryInts(mContext.getContentResolver(), Plays.buildPlayersByPlayUri(), Plays.PLAY_ID, SELECTION, new String[] { mUsername, mNickName });
		for (Integer playId : playIds) {
			mContext.getContentResolver().update(BggContract.Plays.buildPlayUri(playId), values, null, null);
		}
		return playIds.size();
	}

	private void updatePlayers() {
		ContentValues values = new ContentValues(1);
		values.put(BggContract.PlayPlayers.NAME, mNickName);
		mContext.getContentResolver().update(BggContract.Plays.buildPlayersByPlayUri(), values, SELECTION, new String[] { mUsername, mNickName });
	}
}

